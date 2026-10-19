package fr.perso.managingapps.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Étape d'une installation, signalée par Android. */
sealed interface InstallEvent {
    val packageName: String

    /** Android demande confirmation : [intent] ouvre son écran « Mettre à jour cette appli ? ». */
    data class NeedsConfirmation(override val packageName: String, val intent: Intent) : InstallEvent

    /** Installation terminée ; [error] : la raison de l'échec. */
    data class Finished(override val packageName: String, val error: String?) : InstallEvent
}

/** Relais vers l'écran : les événements arrivés appli fermée attendent qu'elle revienne. */
object InstallEvents {
    private val channel = Channel<InstallEvent>(Channel.UNLIMITED)
    val events = channel.receiveAsFlow()

    fun send(event: InstallEvent) {
        channel.trySend(event)
    }
}

/** Reçoit les statuts des sessions ouvertes par [ApkInstaller]. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val event = when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> InstallEvent.NeedsConfirmation(
                packageName,
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return,
            )
            PackageInstaller.STATUS_SUCCESS -> InstallEvent.Finished(packageName, error = null)
            else -> InstallEvent.Finished(
                packageName,
                error = failureMessage(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)),
            )
        }
        InstallEvents.send(event)
    }

    private fun failureMessage(status: Int, detail: String?) = when (status) {
        PackageInstaller.STATUS_FAILURE_ABORTED -> "installation annulée"
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "l'appli installée n'a pas la même signature : désinstalle-la puis réinstalle-la d'ici"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "APK incompatible avec ce téléphone"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "espace de stockage insuffisant"
        PackageInstaller.STATUS_FAILURE_INVALID -> "APK invalide"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "installation bloquée par le téléphone"
        else -> detail ?: "échec de l'installation"
    }

    companion object {
        const val EXTRA_PACKAGE = "fr.perso.managingapps.PACKAGE"
    }
}
