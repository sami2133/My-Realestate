package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sami.data.local.entity.ClientStatus
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.data.local.entity.VisitEntity
import com.realestate.sami.data.repository.ClientRepository
import com.realestate.sami.data.repository.PropertyRepository
import com.realestate.sami.data.repository.VisitRepository
import com.realestate.sami.util.ReportPreferences
import com.realestate.sami.util.RentPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** فاز ۵ — گزارش‌ها و داشبورد آماری: تعداد معاملات موفق، درآمد کمیسیون تخمینی، بازدیدهای پیش‌رو. */
data class ReportStats(
    val totalProperties: Int = 0,
    val availableProperties: Int = 0,
    val closedDeals: Int = 0,
    val activeClients: Int = 0,
    val upcomingVisits: Int = 0,
    val estimatedCommission: Long = 0L
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val clientRepository: ClientRepository,
    private val visitRepository: VisitRepository,
    private val reportPreferences: ReportPreferences,
    private val rentPreferences: RentPreferences
) : ViewModel() {

    private val _commissionPercent = MutableStateFlow(reportPreferences.commissionPercent)
    val commissionPercent: StateFlow<Float> = _commissionPercent

    /** فاز ۵.۲ — نرخ تبدیل رهن↔اجاره (درصد ماهانه)، قابل‌شخصی‌سازی از همین صفحه. */
    private val _rentConversionPercent = MutableStateFlow(rentPreferences.conversionPercent)
    val rentConversionPercent: StateFlow<Float> = _rentConversionPercent

    fun setRentConversionPercent(percent: Float) {
        rentPreferences.conversionPercent = percent
        _rentConversionPercent.value = percent
    }

    val stats: StateFlow<ReportStats> = combine(
        propertyRepository.getAll(),
        clientRepository.getAll(),
        visitRepository.getUpcoming(),
        _commissionPercent
    ) { properties, clients, upcomingVisits, commissionPercent ->
        val closed = properties.filter { it.status == PropertyStatus.SOLD_OR_RENTED }
        val commissionableAmount = closed.sumOf { it.commissionableAmount() }
        ReportStats(
            totalProperties = properties.size,
            availableProperties = properties.count { it.status == PropertyStatus.AVAILABLE },
            closedDeals = closed.size,
            activeClients = clients.count { it.status == ClientStatus.SEARCHING },
            upcomingVisits = upcomingVisits.size,
            estimatedCommission = (commissionableAmount * commissionPercent / 100f).toLong()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportStats())

    /** آخرین بازدیدهای پیش‌رو، برای فهرست کوتاه در پایین داشبورد. */
    val upcomingVisits: StateFlow<List<VisitEntity>> = visitRepository.getUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProperties: StateFlow<List<PropertyEntity>> = propertyRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCommissionPercent(percent: Float) {
        reportPreferences.commissionPercent = percent
        _commissionPercent.value = percent
    }
}

/**
 * مبلغ مبنای محاسبه‌ی کمیسیون. برای رهن‌واجاره (که حالا هم رهن‌های خیلی سنگین با اجاره‌ی نزدیک
 * صفر، هم اجاره‌های سنگین با رهن کم رو شامل می‌شه) جمع رهن + اجاره در نظر گرفته می‌شه تا معامله‌های
 * تماماً-رهنی (قبلاً MORTGAGE) دستِ‌کم‌گرفته نشن.
 */
private fun PropertyEntity.commissionableAmount(): Long = when (dealType) {
    DealType.SALE, DealType.EXCHANGE -> totalPrice ?: 0L
    DealType.RENT, DealType.MORTGAGE -> (depositPrice ?: 0L) + (rentPrice ?: 0L)
}
