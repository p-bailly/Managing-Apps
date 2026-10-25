# Mes applis

Appli Android qui vérifie que mes applis perso sont à jour, et les met à jour d'un geste.

Pour chaque appli suivie (NBA, LoL Live, Scan, Météo, Nimbus, Solfège Piano, et « Mes applis » elle-même), elle compare la date
d'installation sur le téléphone avec celle du dernier APK publié dans la release GitHub du dépôt :

- **À jour** : rien à faire ;
- **Mettre à jour** : un APK plus récent est publié, le bouton le télécharge et l'installe ;
- **Installer** : l'appli n'est pas encore sur le téléphone.

La vérification se fait à chaque ouverture (au plus une fois par minute) et avec le bouton ⟳.

## Installation

1. Sur le téléphone, ouvrir
   <https://github.com/p-bailly/Managing-Apps/releases/latest/download/Managing-Apps.apk> et installer l'APK.
2. Au premier « Mettre à jour », autoriser « Mes applis » à installer des applis inconnues (Android ouvre le réglage).

Ensuite, « Mes applis » se met à jour elle-même, comme les autres.

## Jeton GitHub (facultatif)

Tous les dépôts suivis sont publics : aucun jeton n'est nécessaire. Pour suivre un jour un dépôt privé,
bouton ⚙ › coller un jeton créé sur github.com : Settings › Developer settings › Personal access tokens ›
**Fine-grained tokens** › Generate new token :

- *Repository access* : Only select repositories › les dépôts privés à suivre ;
- *Permissions* › Repository permissions › **Contents : Read-only**.

Le jeton reste sur le téléphone, dans les réglages privés de l'appli. Sans jeton, l'appli lit la date
du dernier APK par son lien de téléchargement public, et non par l'API de GitHub : celle-ci n'accepte
que 60 requêtes par heure sans jeton, partagées par tous les appareils de la même connexion.

## Suivre une nouvelle appli

Ajouter une ligne dans [`TrackedApps.kt`](app/src/main/java/fr/perso/managingapps/data/TrackedApps.kt) :

```kotlin
TrackedApp("Nom affiché", "p-bailly/Depot", "identifiant.android.de.l.appli", "Fichier.apk"),
```

L'appli doit publier son APK dans la dernière release de son dépôt (comme les workflows de NBA, Scan,
Météo et Nimbus), toujours signé avec la même clé et avec un `versionCode` croissant. Une fois la ligne
envoyée sur GitHub, « Mes applis » se propose sa propre mise à jour ; après celle-ci, la nouvelle appli
apparaît dans la liste.

## Bon à savoir

- Sur Android 12 et plus, une appli déjà mise à jour une fois par « Mes applis » peut ensuite l'être
  sans écran de confirmation (si le téléphone le permet).
- La comparaison se fait sur les dates : relancer le workflow d'une appli sans rien changer fait
  apparaître une « mise à jour » vers la même version, sans danger.
- La clé de signature (`app/managing-apps.keystore`) est versionnée exprès, comme pour NBA, Scan et
  Météo : tous les APK s'installent par-dessus les précédents. Usage perso uniquement.

## Compilation

```sh
./gradlew testDebugUnitTest assembleRelease   # → app/build/outputs/apk/release/app-release.apk
```

Le workflow [`android.yml`](.github/workflows/android.yml) compile, teste et publie l'APK dans la release
« apk » à chaque envoi sur la branche par défaut.
