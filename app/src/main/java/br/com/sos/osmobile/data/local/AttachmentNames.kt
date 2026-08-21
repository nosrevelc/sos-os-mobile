package br.com.sos.osmobile.data.local

object AttachmentNames {

    fun originalName(fileName: String): String {
        val designParts = fileName.split("_", limit = 3)
        if (designParts.size == 3 && designParts[0] == "design") {
            return designParts[2]
        }
        val documentParts = fileName.split("_", limit = 4)
        if (documentParts.size == 4 && documentParts[0] == "documento" && documentParts[2] == "Design") {
            return documentParts[3]
        }
        if (documentParts.size == 4 && documentParts[0] == "documento") {
            return documentParts[3]
        }
        val legacyParts = fileName.split("_", limit = 3)
        return when {
            legacyParts.size == 3 && legacyParts[0] in setOf("imagem", "foto", "comprovante") -> legacyParts[2]
            else -> fileName
        }
    }

    fun documentDescription(fileName: String): String? {
        val parts = fileName.split("_", limit = 4)
        return if (parts.size == 4 && parts[0] == "documento" && parts[2] != "Design") {
            parts[2].replace("-", " ").takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    fun isDocument(fileName: String): Boolean =
        (fileName.startsWith("documento_") && !isDesign(fileName)) || fileName.startsWith("comprovante_")

    fun isDesign(fileName: String): Boolean =
        fileName.startsWith("design_") ||
            (fileName.startsWith("documento_") && fileName.split("_", limit = 4).getOrNull(2) == "Design")
}
