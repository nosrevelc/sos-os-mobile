package br.com.sos.osmobile.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactAccount(
    val name: String,
    val type: String,
) {
    val label: String = if (type == "com.google") "$name (Google)" else name
    val storageKey: String = "$type|$name"
}

class ContactsRepository(
    context: Context,
) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun listContactAccounts(): List<ContactAccount> = withContext(Dispatchers.IO) {
        contentResolver.query(
            ContactsContract.Settings.CONTENT_URI,
            arrayOf(
                ContactsContract.Settings.ACCOUNT_NAME,
                ContactsContract.Settings.ACCOUNT_TYPE,
            ),
            null,
            null,
            ContactsContract.Settings.ACCOUNT_NAME,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Settings.ACCOUNT_NAME)
            val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Settings.ACCOUNT_TYPE)
            buildList<ContactAccount> {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex).orEmpty()
                    val type = cursor.getString(typeIndex).orEmpty()
                    if (name.isNotBlank() && none { it.name == name && it.type == type }) {
                        add(ContactAccount(name = name, type = type))
                    }
                }
            }
        }.orEmpty()
    }

    suspend fun listGoogleContactAccounts(): List<ContactAccount> = listContactAccounts()

    suspend fun contactExists(customer: CustomerEntity): Boolean = withContext(Dispatchers.IO) {
        val phone = customer.telefone.filter { it.isDigit() }
        if (phone.isBlank()) return@withContext false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }

    suspend fun syncCustomer(
        customer: CustomerEntity,
        googleAccount: String?,
        existingRawContactId: Long?,
    ): Long = withContext(Dispatchers.IO) {
        existingRawContactId?.takeIf { it > 0 }?.let { deleteRawContact(it) }

        val operations = arrayListOf<ContentProviderOperation>()
        val account = googleAccount?.trim()?.takeIf { it.isNotBlank() }?.let(::parseAccount)

        val rawContactInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        if (account != null) {
            rawContactInsert
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        } else {
            rawContactInsert
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        }
        operations += rawContactInsert.build()

        operations += dataInsert(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, customer.nome)
            .build()

        operations += dataInsert(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, customer.telefone)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build()

        customer.email?.takeIf { it.isNotBlank() }?.let { email ->
            operations += dataInsert(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build()
        }

        customer.endereco?.takeIf { it.isNotBlank() }?.let { address ->
            operations += dataInsert(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                .build()
        }

        customer.observacoes?.takeIf { it.isNotBlank() }?.let { notes ->
            operations += dataInsert(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                .build()
        }

        val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        results.first().uri?.lastPathSegment?.toLongOrNull()
            ?: error("Nao foi possivel obter o ID do contato criado.")
    }

    private fun dataInsert(mimeType: String): ContentProviderOperation.Builder =
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, mimeType)

    private fun deleteRawContact(rawContactId: Long) {
        contentResolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts._ID} = ?",
            arrayOf(rawContactId.toString()),
        )
    }

    private fun parseAccount(value: String): ContactAccount {
        val parts = value.split("|", limit = 2)
        return if (parts.size == 2) {
            ContactAccount(name = parts[1], type = parts[0])
        } else {
            ContactAccount(name = value, type = GOOGLE_ACCOUNT_TYPE)
        }
    }

    private companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}
