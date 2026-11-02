package fr.mat.cloudclean.model

/** Une entree (dossier ou fichier) dans l'arborescence virtuelle. */
data class CloudEntry(
    val id: String,
    val name: String,
    val pathLower: String,
    val isFolder: Boolean,
    val size: Long,
    val contentHash: String?,
    val lastModified: String?
)