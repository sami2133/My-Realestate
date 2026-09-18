package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.ClientStatus
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.RelatedType
import com.realestate.sami.data.local.entity.VisitEntity
import com.realestate.sami.data.local.entity.VisitResult
import com.realestate.sami.data.repository.ClientRepository
import com.realestate.sami.data.repository.ContactLogRepository
import com.realestate.sami.data.repository.VisitRepository
import com.realestate.sami.domain.matching.MatchingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clientRepository: ClientRepository,
    private val contactLogRepository: ContactLogRepository,
    private val visitRepository: VisitRepository,
    private val matchingEngine: MatchingEngine
) : ViewModel() {

    private val clientId: Long = checkNotNull(savedStateHandle["clientId"])

    private val _client = MutableStateFlow<ClientEntity?>(null)
    val client: StateFlow<ClientEntity?> = _client

    val matchingProperties: StateFlow<List<PropertyEntity>> = _client
        .filterNotNull()
        .flatMapLatest { matchingEngine.matchesForClient(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactLogs: StateFlow<List<ContactLogEntity>> =
        contactLogRepository.getForEntity(clientId, RelatedType.CLIENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** فاز ۵: قرارهای بازدید ثبت‌شده برای این متقاضی (با ملک‌های مختلف). */
    val visits: StateFlow<List<VisitEntity>> = visitRepository.getForClient(clientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _client.value = clientRepository.getById(clientId)
        }
    }

    fun updateStatus(status: ClientStatus) {
        viewModelScope.launch {
            _client.value?.let {
                val updated = it.copy(status = status)
                clientRepository.save(updated)
                _client.value = updated
            }
        }
    }

    fun addContactLog(note: String, followUpDate: Long?) {
        viewModelScope.launch {
            contactLogRepository.add(
                ContactLogEntity(
                    relatedId = clientId,
                    relatedType = RelatedType.CLIENT,
                    note = note,
                    followUpDate = followUpDate
                )
            )
        }
    }

    /** فاز ۵: زمان‌بندی بازدید این متقاضی از یک ملک سازگار. */
    fun scheduleVisit(propertyId: Long, visitDateMillis: Long) {
        viewModelScope.launch {
            visitRepository.add(
                VisitEntity(
                    propertyId = propertyId,
                    clientId = clientId,
                    visitDate = visitDateMillis
                )
            )
        }
    }

    fun updateVisitResult(visit: VisitEntity, result: VisitResult) {
        viewModelScope.launch {
            visitRepository.update(visit.copy(result = result))
        }
    }

    /** حذف نرم (soft delete) متقاضی جاری؛ پس از اتمام، callback برای بازگشت از صفحه فراخوانی می‌شود. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _client.value?.let {
                clientRepository.delete(it)
                onDeleted()
            }
        }
    }
}
