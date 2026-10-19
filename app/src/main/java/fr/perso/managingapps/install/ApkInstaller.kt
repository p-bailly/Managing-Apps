package fr.perso.managingapps.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

/** Installation des APK téléchargés, par le PackageInstaller d'Android. */
class ApkInstaller(private val context: Context) {

    /** Lance l'installation de [apk] ; la suite (confirmation, résultat) arrive par [InstallEvents]. */
    fun install(apk: File, packageName: String) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apk.length())
            // Sans confirmation quand Android le permet (12+) : mise à jour d'une appli déjà installée par celle-ci.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        try {
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val callback = Intent(context, InstallReceiver::class.java)
                .setPackage(context.packageName)
                .putExtra(InstallReceiver.EXTRA_PACKAGE, packageName)
            // Modifiable : Android y ajoute le statut de l'installation.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(context, sessionId, callback, flags).intentSender)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
        }
    }
}
