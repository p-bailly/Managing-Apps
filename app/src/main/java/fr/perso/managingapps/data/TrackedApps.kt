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
    val apkName: String?,
    /**
     * Empreinte SHA-256 du certificat qui signe l'appli (64 chiffres hexadécimaux, les « : » de keytool sont admis).
     * Un APK signé par une autre clé est refusé avant l'installation : c'est la seule protection pour une première
     * installation (pour une mise à jour, Android refuse aussi une autre signature). Relevée sur l'APK publié par
     * `apksigner verify --print-certs`, ou sur le keystore par `keytool -list -v`.
     */
    val certSha256: String,
)

val TRACKED_APPS = listOf(
    TrackedApp("NBA", "p-bailly/NBA", "com.pbailly.superfanbasket", "basket-live.apk",
        "3b648f0ca0994eb407121dde895c5d9fb80eb002b9cb1941fa6ca4290124d7d7"),
    TrackedApp("LoL Live", "p-bailly/LOL-games", "com.pbailly.riftlive", "rift-live.apk",
        "3b648f0ca0994eb407121dde895c5d9fb80eb002b9cb1941fa6ca4290124d7d7"),
    TrackedApp("Scan", "p-bailly/Scan", "fr.perso.scan", "Scan.apk",
        "86b151bb6c20d5065e2e42dcc765a843a36985f5c239918a19e7436ea18d314f"),
    TrackedApp("Météo", "p-bailly/Meteo", "io.github.pbailly.meteo", "meteo.apk",
        "08919f980dd5d32675ede6e2999acf2ff6a405c1854e3eb1bf25f4f40e60e7c2"),
    TrackedApp("Nimbus", "p-bailly/Nimbus", "fr.perso.nimbus", "Nimbus.apk",
        "72563272d9204b8347bfcb45e6c67039213b73b4eedf783007e5f62325bd221c"),
    TrackedApp("Solfège Piano", "p-bailly/Musique", "fr.solfegepiano.app", "SolfegePiano.apk",
        "ce4c103510963b130a2d7ee9eed0172c246c3fad677cf356243b97ebaeeab5e7"),
    TrackedApp("Scrabble", "p-bailly/Scrabble", "fr.perso.scrabble", "scrabble.apk",
        "0bd9375bf39cc9bad5f55b5a484646509f8174765c66476c7b6885bc8685178c"),
    TrackedApp("Prépa Agreg", "p-bailly/Prepa-Agreg", "fr.perso.prepaagreg", "prepa-agreg.apk",
        "0bd9375bf39cc9bad5f55b5a484646509f8174765c66476c7b6885bc8685178c"),
    TrackedApp("Cuisine", "p-bailly/Cuisine", "fr.perso.cuisine", "Cuisine.apk",
        "4661bff2e5667d7b99ab271c726ae4c398a50e972372f3482557b927da2997d0"),
    // Cette appli elle-même : elle se met à jour comme les autres.
    TrackedApp("Mes applis", "p-bailly/Managing-Apps", BuildConfig.APPLICATION_ID, "Managing-Apps.apk",
        "4661bff2e5667d7b99ab271c726ae4c398a50e972372f3482557b927da2997d0"),
)
