package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.repository.ClientRepository
import com.realestate.sami.domain.matching.MatchingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(
    private val repository: ClientRepository,
    private val matchingEngine: MatchingEngine
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val clients: StateFlow<List<ClientEntity>> = _searchQuery
        .flatMapLatest { query -> repository.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun save(client: ClientEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(client)
            onSaved(id)
        }
    }

    fun delete(client: ClientEntity) {
        viewModelScope.launch { repository.delete(client) }
    }

    /** بعد از ثبت یک متقاضی، ملک‌های سازگار با نیاز او را برمی‌گرداند. */
    fun matchingPropertiesFor(client: ClientEntity) = matchingEngine.matchesForClient(client)
}
