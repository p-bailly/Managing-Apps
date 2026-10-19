package fr.perso.managingapps.data

/** Version installée sur le téléphone. */
data class InstalledVersion(
    val versionName: String?,
    val versionCode: Long,
    /** Date de la dernière installation ou mise à jour (ms depuis 1970). */
    val lastUpdateTime: Long,
)

/** Dernier APK publié sur GitHub. */
data class RemoteApk(
    /** Taille en octets. */
    val size: Long,
    /** Date d'envoi du fichier sur GitHub (ms depuis 1970). */
    val updatedAt: Long,
    /** Adresse de téléchargement par l'API (fonctionne aussi pour les dépôts privés). */
    val apiUrl: String,
)

enum class UpdateStatus { NOT_INSTALLED, UP_TO_DATE, UPDATE_AVAILABLE }

/**
 * Les workflows publient l'APK sous le même nom à chaque build, sans numéro de version lisible
 * dans la release : on compare donc les dates. Installée après l'envoi de l'APK sur GitHub,
 * l'appli est à jour ; installée avant, un APK plus récent attend.
 */
fun updateStatus(installed: InstalledVersion?, remote: RemoteApk): UpdateStatus = when {
    installed == null -> UpdateStatus.NOT_INSTALLED
    installed.lastUpdateTime >= remote.updatedAt -> UpdateStatus.UP_TO_DATE
    else -> UpdateStatus.UPDATE_AVAILABLE
}
