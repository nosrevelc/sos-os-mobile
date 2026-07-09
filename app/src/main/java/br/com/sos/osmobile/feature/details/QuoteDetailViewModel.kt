package br.com.sos.osmobile.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.model.DocumentItem
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.QuoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class QuoteDetailUiState(
    val quote: QuoteSummary? = null,
    val items: List<DocumentItem> = emptyList(),
    val history: List<String> = emptyList(),
)

class QuoteDetailViewModel(
    private val quoteId: Long,
    private val quoteRepository: QuoteRepository,
    private val auditRepository: AuditRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuoteDetailUiState())
    val uiState: StateFlow<QuoteDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = QuoteDetailUiState(
                quote = quoteRepository.findSummaryById(quoteId),
                items = quoteRepository.listDocumentItems(quoteId),
                history = auditRepository.listForRecord("orcamentos", quoteId).map { it.acao },
            )
        }
    }

    companion object {
        fun factory(quoteId: Long, quoteRepository: QuoteRepository, auditRepository: AuditRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuoteDetailViewModel(quoteId, quoteRepository, auditRepository) as T
            }
    }
}
