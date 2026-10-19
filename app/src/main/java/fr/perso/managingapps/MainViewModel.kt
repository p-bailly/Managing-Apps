package fr.perso.managingapps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.perso.managingapps.data.GitHub
import fr.perso.managingapps.data.GitHubException
import fr.perso.managingapps.data.InstalledVersion
import fr.perso.managingapps.data.RemoteApk
import fr.perso.managingapps.data.SettingsStore
import fr.perso.managingapps.data.TRACKED_APPS
import fr.perso.managingapps.data.TrackedApp
import fr.perso.managingapps.data.UpdateStatus
import fr.perso.managingapps.data.updateStatus
import fr.perso.managingapps.install.ApkInstaller
import fr.perso.managingapps.install.InstallEvent
import fr.perso.managingapps.install.InstallEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Une appli suivie et où elle en est. */
data class AppRow(
    val app: TrackedApp,
    val installed: InstalledVersion? = null,
    val remote: RemoteApk? = null,
    /** Vérification sur GitHub en cours. */
    val checking: Boolean = false,
    /** Échec de la dernière vérification. */
    val error: String? = null,
    /** Téléchargement en cours : avancement de 0 à 1. */
    val download: Float? = null,
    /** Installation lancée, résultat attendu. */
    val installing: Boolean = false,
) {
    val status: UpdateStatus? get() = remote?.let { updateStatus(installed, it) }
    val busy: Boolean get() = download != null || installing
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val github = GitHub { settings.githubToken }
    private val installer = ApkInstaller(application)
    private val packageManager = application.packageManager
    private val apkDir = File(application.cacheDir, "apk")

    private val _rows = MutableStateFlow(TRACKED_APPS.map { AppRow(it, installed = installedVersion(it.packageName)) })
    val rows: StateFlow<List<AppRow>> = _rows

    private val _token = MutableStateFlow(settings.githubToken)
    val token: StateFlow<String?> = _token

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** Écrans de confirmation d'Android à ouvrir (seule l'activité peut les lancer). */
    private val confirmationChannel = Channel<Intent>(Channel.UNLIMITED)
    val confirmations = confirmationChannel.receiveAsFlow()

    /** Dernière vérification sur GitHub (horloge depuis le démarrage du téléphone). */
    private var lastCheck: Long? = null

    init {
        viewModelScope.launch { InstallEvents.events.collect(::onInstallEvent) }
    }

    /** Retour dans l'appli : versions installées relues, GitHub revérifié au plus une fois par minute. */
    fun onResume() {
        // Installation abandonnée sans réponse d'Android : plus de session ouverte pour elle.
        val sessions = packageManager.packageInstaller.mySessions.map { it.appPackageName }
        _rows.update { rows -> rows.map { if (it.installing && it.app.packageName !in sessions) it.copy(installing = false) else it } }
        refreshInstalled()
        val last = lastCheck
        if (last == null || SystemClock.elapsedRealtime() - last > 60_000) checkAll()
    }

    /** Cherche le dernier APK publié de chaque appli. */
    fun checkAll() {
        if (_rows.value.any { it.checking }) return
        lastCheck = SystemClock.elapsedRealtime()
        refreshInstalled()
        for (app in TRACKED_APPS) {
            viewModelScope.launch {
                updateRow(app) { it.copy(checking = true) }
                val result = withContext(Dispatchers.IO) { runCatching { github.latestApk(app) } }
                updateRow(app) { row ->
                    result.fold(
                        onSuccess = { row.copy(checking = false, remote = it, error = null) },
                        onFailure = { row.copy(checking = false, remote = null, error = userMessage(it)) },
                    )
                }
            }
        }
    }

    /** Télécharge le dernier APK de [app] puis l'installe. */
    fun install(app: TrackedApp) {
        val row = _rows.value.first { it.app == app }
        val remote = row.remote ?: return
        if (row.busy) return
        updateRow(app) { it.copy(download = 0f) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val apk = File(apkDir, "${app.packageName}.apk")
                    var shown = -1
                    github.download(remote, apk) { progress ->
                        // Une mise à jour de l'écran par pour cent, pas par paquet reçu.
                        val percent = (progress * 100).toInt()
                        if (percent != shown) {
                            shown = percent
                            updateRow(app) { it.copy(download = progress) }
                        }
                    }
                    checkApk(app, apk)
                    updateRow(app) { it.copy(download = null, installing = true) }
                    installer.install(apk, app.packageName)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                updateRow(app) { it.copy(download = null, installing = false) }
                messageChannel.trySend("${app.name} : ${userMessage(e).replaceFirstChar { it.lowercase() }}")
            }
        }
    }

    fun setToken(value: String?) {
        settings.githubToken = value
        _token.value = settings.githubToken
        lastCheck = null
        checkAll()
    }

    private fun onInstallEvent(event: InstallEvent) {
        val app = TRACKED_APPS.firstOrNull { it.packageName == event.packageName } ?: return
        when (event) {
            is InstallEvent.NeedsConfirmation -> confirmationChannel.trySend(event.intent)
            is InstallEvent.Finished -> {
                File(apkDir, "${app.packageName}.apk").delete()
                updateRow(app) { it.copy(installing = false) }
                refreshInstalled()
                messageChannel.trySend(if (event.error == null) "${app.name} est à jour" else "${app.name} : ${event.error}")
            }
        }
    }

    /** Refuse l'APK d'une autre appli, ou plus ancien que la version installée (Android le refuserait). */
    private fun checkApk(app: TrackedApp, apk: File) {
        @Suppress("DEPRECATION")
        val info = packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: throw GitHubException("Le fichier téléchargé n'est pas un APK valide")
        if (info.packageName != app.packageName) {
            throw GitHubException("L'APK publié est celui d'une autre appli (${info.packageName})")
        }
        val installed = installedVersion(app.packageName)
        if (installed != null && PackageInfoCompat.getLongVersionCode(info) < installed.versionCode) {
            throw GitHubException(
                "L'APK publié (${info.versionName}) est plus ancien que la version installée (${installed.versionName})"
            )
        }
    }

    private fun installedVersion(packageName: String): InstalledVersion? = try {
        @Suppress("DEPRECATION")
        val info = packageManager.getPackageInfo(packageName, 0)
        InstalledVersion(info.versionName, PackageInfoCompat.getLongVersionCode(info), info.lastUpdateTime)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun refreshInstalled() {
        _rows.update { rows -> rows.map { it.copy(installed = installedVersion(it.app.packageName)) } }
    }

    private fun updateRow(app: TrackedApp, change: (AppRow) -> AppRow) {
        _rows.update { rows -> rows.map { if (it.app == app) change(it) else it } }
    }

    private fun userMessage(e: Throwable): String = when (e) {
        is GitHubException -> e.message.orEmpty()
        is UnknownHostException -> "Pas de connexion à Internet"
        is SocketTimeoutException -> "GitHub ne répond pas"
        is IOException -> "Erreur réseau (${e.message})"
        else -> "Réponse inattendue (${e.message ?: e.javaClass.simpleName})"
    }
}
