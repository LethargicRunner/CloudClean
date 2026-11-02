# CloudClean

Lecteur reseau unifie multi-comptes cloud (Dropbox, Google Drive, OneDrive) :
etat des lieux, detection de doublons et nettoyage assiste, tous comptes reunis dans une seule arborescence.

## V0.1.0 — Dropbox

- Connexion OAuth 2.0 + PKCE, **multi-comptes natif** (autant de comptes Dropbox que voulu, simultanement).
- Navigateur d'arborescence en **lecture seule stricte** (rien n'est modifie ni supprime).
- Zero dependance reseau : HTTP + JSON natifs Android, tout concentre dans `HttpJsonClient`.
- `content_hash` collecte sur chaque fichier : base de la dedup sans telechargement (V0.3).

## Feuille de route

- V0.2 : etat des lieux par type de fichiers (modele StorageClean), export CSV, comptes en parallele
- V0.3 : doublons intra-Dropbox via `content_hash`, deplacement corbeille
- V0.4 : adaptateurs Google Drive + OneDrive, vue unifiee
- V0.5 : doublons inter-cloud, nettoyage assiste, bouton de purge d'un compte

## Setup

1. Creer une app sur dropbox.com/developers/apps (Scoped access, All file types,
   scopes `account_info.read` + `files.metadata.read`, redirect URI `cloudclean://oauth/callback`).
2. Inserer l'App key dans `app/src/main/java/fr/mat/cloudclean/dropbox/DropboxAuth.kt`.

App Android Kotlin + Jetpack Compose, minSdk 26, CI GitHub Actions (APK `CloudClean-v0.1.0.apk`).