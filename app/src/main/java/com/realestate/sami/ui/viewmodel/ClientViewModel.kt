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

    private val _sortOption = MutableStateFlow(ClientSortOption.NEWEST)
    val sortOption: StateFlow<ClientSortOption> = _sortOption

    /** رکورد در حال ویرایش (وقتی از صفحه ثبت متقاضی در حالت ویرایش استفاده می‌شود). */
    private val _editingClient = MutableStateFlow<ClientEntity?>(null)
    val editingClient: StateFlow<ClientEntity?> = _editingClient

    val clients: StateFlow<List<ClientEntity>> = combine(
        _searchQuery.flatMapLatest { query -> repository.search(query) },
        _sortOption
    ) { list, sort -> list.sortedWith(sort.comparator()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChanged(option: ClientSortOption) {
        _sortOption.value = option
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch { _editingClient.value = repository.getById(id) }
    }

    fun clearEditing() {
        _editingClient.value = null
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

private fun ClientSortOption.comparator(): Comparator<ClientEntity> = when (this) {
    ClientSortOption.NEWEST -> compareByDescending { it.createdAt }
    ClientSortOption.OLDEST -> compareBy { it.createdAt }
    ClientSortOption.NAME_A_TO_Z -> compareBy { it.fullName }
    ClientSortOption.BUDGET_HIGH_TO_LOW -> compareByDescending {
        it.maxTotalPrice ?: it.maxDepositPrice ?: it.maxRentPrice ?: 0L
    }
}
