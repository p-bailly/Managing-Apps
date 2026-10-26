# Mes applis — notes pour travailler dans le repo

## Commandes et CI
- `./gradlew testDebugUnitTest assembleRelease` (tests JVM : `SortedForDisplayTest`, `VersionsTest`).
- `.github/workflows/android.yml` : sur push de la branche par défaut (ou `workflow_dispatch`), tests puis
  publication de `Managing-Apps.apk` dans la release « apk » (JDK 21 en CI, code en 17, `minSdk 28`).
- **Numéro de version = nombre de commits** (`git rev-list --count HEAD`) ; le CI fait `fetch-depth: 0`. Un clone
  shallow donne un numéro faux. `checkApk` refuse un APK dont le `versionCode` est inférieur à celui installé.
- `app/managing-apps.keystore` est **volontairement versionné** (mots de passe dans `app/build.gradle.kts`), comme
  pour NBA, Scan et Météo : même signature partout, donc les APK s'installent les uns sur les autres. Si une appli
  installée est signée autrement, Android répond « conflit » (message : désinstaller puis réinstaller d'ici).

## Principe de comparaison
- Les workflows publient l'APK sous le **même nom à chaque build**, sans numéro de version lisible : on compare
  des dates. `InstalledVersion.lastUpdateTime >= RemoteApk.updatedAt` ⇒ à jour (`Versions.kt`). Relancer un
  workflow sans changement fait apparaître une « mise à jour » vers la même version : sans danger.
- Sans jeton et avec `apkName` connu : requête HEAD sur `github.com/<repo>/releases/latest/download/<apk>`, date =
  `Last-Modified` (évite l'API : 60 requêtes/h/IP sans jeton, partagées entre appareils). Repli sur l'API si pas de
  date. Avec jeton : API `releases/latest` (asset `updated_at`) ; le téléchargement par l'API redirige vers un lien
  signé qui refuse `Authorization` : la redirection est suivie à la main, sans jeton (`GitHub.download`).
- Jeton GitHub (⚙, facultatif, dépôts privés seulement) : fine-grained token, *Only select repositories*,
  *Contents : Read-only* ; stocké en SharedPreferences privées (`settings`/`githubToken`). Le texte d'aide est aussi
  dans `ui/AppsScreen.kt`.
- Rafraîchissement : à chaque `onResume` (GitHub au plus 1 fois/minute) et par le bouton ⟳. Tri : « à mettre à
  jour » d'abord, puis publication la plus récente ; sans date (vérification en cours/échec) : en fin de groupe,
  ordre de `TRACKED_APPS` (`sortedForDisplay`).

## Installation des APK
- `install/ApkInstaller` : session `PackageInstaller` (MODE_FULL_INSTALL) ; résultat via `InstallReceiver` →
  `InstallEvents` (canal non borné : les événements reçus appli fermée attendent son retour) → `MainViewModel`.
  Les écrans de confirmation d'Android ne peuvent être lancés que par l'activité.
- Sans confirmation (Android 12+, `USER_ACTION_NOT_REQUIRED` + permission `UPDATE_PACKAGES_WITHOUT_USER_ACTION`) :
  seulement pour une appli déjà mise à jour par celle-ci, et si le téléphone l'accepte.
- `<queries>` du manifeste : intent MAIN, pour lire la version installée des applis qui ont un lanceur.
- APK téléchargés dans `cacheDir/apk/<package>.apk`, supprimés après l'installation ; `checkApk` vérifie le package.

## Ajouter une appli suivie
Ligne dans `TRACKED_APPS`. Son dépôt doit publier l'APK dans la dernière release, sous un nom stable (`apkName` ;
`null` = premier `.apk`), signé avec la même clé et avec un `versionCode` croissant. Une fois la ligne poussée,
« Mes applis » propose sa propre mise à jour ; la nouvelle appli apparaît ensuite.

## Logo
`python3 design/logo/generer.py` écrit `design/logo/logo.svg` et les calques `ic_launcher_{foreground,monochrome}.xml`
(ne pas les éditer à la main). Style « crayon » commun aux applis, repris de p-bailly/Meteo.
