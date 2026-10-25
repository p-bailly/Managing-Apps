package fr.perso.managingapps.data

import fr.perso.managingapps.BuildConfig

/**
 * Appli suivie : son APK est publié dans la dernière release (« Latest ») de son dépôt GitHub.
 * Pour en suivre une nouvelle, il suffit d'ajouter une ligne à [TRACKED_APPS].
 */
data class TrackedApp(
    /** Nom affiché. */
    val name: String,
    /** Dépôt GitHub, « propriétaire/nom ». */
    val repo: String,
    /** Identifiant Android de l'appli (applicationId). */
    val packageName: String,
    /** Nom du fichier APK dans la release ; null : le premier « .apk » trouvé. */
    val apkName: String? = null,
)

val TRACKED_APPS = listOf(
    TrackedApp("NBA", "p-bailly/NBA", "com.pbailly.superfanbasket", "basket-live.apk"),
    TrackedApp("LoL Live", "p-bailly/LOL-games", "com.pbailly.riftlive", "rift-live.apk"),
    TrackedApp("Scan", "p-bailly/Scan", "fr.perso.scan", "Scan.apk"),
    TrackedApp("Météo", "p-bailly/Meteo", "io.github.pbailly.meteo", "meteo.apk"),
    TrackedApp("Nimbus", "p-bailly/Nimbus", "fr.perso.nimbus", "Nimbus.apk"),
    TrackedApp("Solfège Piano", "p-bailly/Musique", "fr.solfegepiano.app", "SolfegePiano.apk"),
    // Cette appli elle-même : elle se met à jour comme les autres.
    TrackedApp("Mes applis", "p-bailly/Managing-Apps", BuildConfig.APPLICATION_ID, "Managing-Apps.apk"),
)
