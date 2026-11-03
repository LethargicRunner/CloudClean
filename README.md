# CloudClean

Lecteur reseau unifie multi-comptes cloud (pCloud, Google Drive, puis OneDrive...) :
etat des lieux, detection de doublons et nettoyage assiste, tous comptes reunis dans une seule arborescence.

## V0.1.0 — pCloud + Google Drive

- Connexion OAuth 2.0, **multi-comptes natif** (autant de comptes que voulu, simultanement, tous providers confondus).
- pCloud : code flow, jeton permanent. Google : code flow + PKCE, refresh token.
- Navigateur d'arborescence en **lecture seule stricte** (rien n'est modifie ni supprime).
- Zero dependance reseau : HTTP + JSON natifs Android, tout concentre dans `HttpJsonClient`.
- Hashs de contenu collectes (pCloud `hash`, Drive `md5Checksum`) : base de la dedup sans telechargement (V0.3).

## Feuille de route

- V0.2 : etat des lieux par type de fichiers (modele StorageClean), export CSV, comptes en parallele
- V0.3 : doublons intra-provider via hash de contenu, deplacement corbeille
- V0.4 : adaptateur OneDrive, vue unifiee
- V0.5 : doublons inter-cloud, nettoyage assiste, bouton de purge d'un compte

## Setup

- **pCloud** : app a creer via docs.pcloud.com/my_apps (ou support pCloud). Redirect URI : `cloudclean://oauth/callback`.
  Inserer Client ID + Secret dans `pcloud/PCloudAuth.kt`.
- **Google** : Google Cloud Console, activer Drive API, ecran de consentement (Testing), client OAuth (Web) avec redirect
  `cloudclean://oauth/google`. Inserer Client ID + Secret dans `google/GoogleAuth.kt`.

App Android Kotlin + Jetpack Compose, minSdk 26, CI GitHub Actions (APK `CloudClean-v0.1.0.apk`).