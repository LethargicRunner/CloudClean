# CloudClean

Lecteur reseau unifie multi-comptes cloud (pCloud, puis Google Drive, OneDrive...) :
etat des lieux, detection de doublons et nettoyage assiste, tous comptes reunis dans une seule arborescence.

## V0.1.0 — pCloud

- Connexion OAuth 2.0 (code flow), **multi-comptes natif** (autant de comptes pCloud que voulu, simultanement).
- Jeton pCloud permanent : pas de refresh a gerer.
- Navigateur d'arborescence en **lecture seule stricte** (rien n'est modifie ni supprime).
- Zero dependance reseau : HTTP + JSON natifs Android, tout concentre dans `HttpJsonClient`.
- `hash` de contenu collecte sur chaque fichier : base de la dedup sans telechargement (V0.3).

## Feuille de route

- V0.2 : etat des lieux par type de fichiers (modele StorageClean), export CSV, comptes en parallele
- V0.3 : doublons intra-pCloud via hash de contenu, deplacement corbeille
- V0.4 : adaptateurs Google Drive + OneDrive, vue unifiee
- V0.5 : doublons inter-cloud, nettoyage assiste, bouton de purge d'un compte

## Setup

1. Creer une application pCloud (Client ID + Client Secret) via la console developpeur / support pCloud.
   Redirect URI : `cloudclean://oauth/callback`.
2. Inserer Client ID et Client Secret dans `app/src/main/java/fr/mat/cloudclean/pcloud/PCloudAuth.kt`.

App Android Kotlin + Jetpack Compose, minSdk 26, CI GitHub Actions (APK `CloudClean-v0.1.0.apk`).