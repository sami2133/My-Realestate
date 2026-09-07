package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.repository.PropertyRepository
import com.realestate.sami.domain.matching.MatchingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val repository: PropertyRepository,
    private val matchingEngine: MatchingEngine
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val properties: StateFlow<List<PropertyEntity>> = _searchQuery
        .flatMapLatest { query -> repository.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun save(property: PropertyEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(property)
            onSaved(id)
        }
    }

    fun delete(property: PropertyEntity) {
        viewModelScope.launch { repository.delete(property) }
    }

    /** بعد از ثبت یک ملک، متقاضیان سازگار با آن را برمی‌گرداند تا فوراً به مشاور نشان داده شود. */
    fun matchingClientsFor(property: PropertyEntity) = matchingEngine.matchesForProperty(property)
}
