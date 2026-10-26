# Mes applis

Appli Android qui vérifie que mes applis perso (liste dans `TrackedApps.kt`) sont à jour et les met à jour
d'un geste : elle compare la date d'installation sur le téléphone à celle du dernier APK publié dans la
release GitHub de leur dépôt (**À jour** / **Mettre à jour** / **Installer**).

**Installer** : ouvrir <https://github.com/p-bailly/Managing-Apps/releases/latest/download/Managing-Apps.apk>
sur le téléphone, installer l'APK, puis autoriser l'installation d'applis inconnues au premier « Mettre à
jour ». Ensuite « Mes applis » se met à jour elle-même.

**Suivre une nouvelle appli** : ajouter une ligne dans
[`TrackedApps.kt`](app/src/main/java/fr/perso/managingapps/data/TrackedApps.kt) (conditions dans [CLAUDE.md](CLAUDE.md)) :

```kotlin
TrackedApp("Nom affiché", "p-bailly/Depot", "identifiant.android.de.l.appli", "Fichier.apk"),
```

**Compilation** : `./gradlew testDebugUnitTest assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
(le workflow `android.yml` publie l'APK dans la release « apk » à chaque push sur la branche par défaut).
