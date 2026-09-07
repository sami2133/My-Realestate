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

    private val _sortOption = MutableStateFlow(PropertySortOption.NEWEST)
    val sortOption: StateFlow<PropertySortOption> = _sortOption

    /** رکورد در حال ویرایش (وقتی از صفحه ثبت ملک در حالت ویرایش استفاده می‌شود). */
    private val _editingProperty = MutableStateFlow<PropertyEntity?>(null)
    val editingProperty: StateFlow<PropertyEntity?> = _editingProperty

    val properties: StateFlow<List<PropertyEntity>> = combine(
        _searchQuery.flatMapLatest { query -> repository.search(query) },
        _sortOption
    ) { list, sort -> list.sortedWith(sort.comparator()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChanged(option: PropertySortOption) {
        _sortOption.value = option
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch { _editingProperty.value = repository.getById(id) }
    }

    fun clearEditing() {
        _editingProperty.value = null
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

private fun PropertySortOption.comparator(): Comparator<PropertyEntity> = when (this) {
    PropertySortOption.NEWEST -> compareByDescending { it.createdAt }
    PropertySortOption.OLDEST -> compareBy { it.createdAt }
    PropertySortOption.PRICE_LOW_TO_HIGH -> compareBy { it.totalPrice ?: it.rentPrice ?: Long.MAX_VALUE }
    PropertySortOption.PRICE_HIGH_TO_LOW -> compareByDescending { it.totalPrice ?: it.rentPrice ?: 0L }
    PropertySortOption.AREA_LARGE_TO_SMALL -> compareByDescending { it.area }
}
