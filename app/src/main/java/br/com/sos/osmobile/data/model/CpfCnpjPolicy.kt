package br.com.sos.osmobile.data.model

enum class CpfCnpjPolicy(val storageValue: String) {
    NotUsed("not_used"),
    Optional("optional"),
    Required("required");

    companion object {
        fun fromStorage(value: String?): CpfCnpjPolicy =
            entries.firstOrNull { it.storageValue == value } ?: Optional
    }
}
