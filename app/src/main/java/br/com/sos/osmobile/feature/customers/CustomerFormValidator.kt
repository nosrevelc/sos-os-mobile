package br.com.sos.osmobile.feature.customers

import br.com.sos.osmobile.data.model.CpfCnpjPolicy

object CustomerFormValidator {
    fun validate(form: CustomerFormState, cpfCnpjPolicy: CpfCnpjPolicy): String? {
        if (form.name.isBlank()) {
            return "Nome e obrigatorio."
        }
        if (form.phone.isBlank()) {
            return "Telefone e obrigatorio."
        }
        if (cpfCnpjPolicy == CpfCnpjPolicy.Required && form.cpfCnpj.isBlank()) {
            return "CPF/CNPJ e obrigatorio."
        }
        return null
    }
}
