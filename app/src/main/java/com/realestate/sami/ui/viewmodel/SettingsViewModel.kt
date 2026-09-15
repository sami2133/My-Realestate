package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.realestate.sami.util.CommissionTariffPreferences
import com.realestate.sami.util.ReportPreferences
import com.realestate.sami.util.RentPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * فاز ۵.۳ — تنظیمات سراسری اپ، در یک صفحه‌ی مستقل («تنظیمات») به‌جای پخش‌شدن داخل صفحه‌های دیگر.
 * هر تنظیم جدیدی که بعداً لازم شد (مثلاً واحد پول، زبان نمایش اعداد، پیش‌فرض نوع ملک و…) همینجا
 * به‌عنوان یک StateFlow/متد جدید اضافه می‌شود؛ صفحه‌ی UI هم به همین شکل با یک SectionCard جدید
 * گسترش پیدا می‌کند — نیازی به تغییر ساختار نیست.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reportPreferences: ReportPreferences,
    private val rentPreferences: RentPreferences,
    private val commissionTariffPreferences: CommissionTariffPreferences
) : ViewModel() {

    private val _commissionPercent = MutableStateFlow(reportPreferences.commissionPercent)
    val commissionPercent: StateFlow<Float> = _commissionPercent

    fun setCommissionPercent(percent: Float) {
        reportPreferences.commissionPercent = percent
        _commissionPercent.value = percent
    }

    private val _rentConversionPercent = MutableStateFlow(rentPreferences.conversionPercent)
    val rentConversionPercent: StateFlow<Float> = _rentConversionPercent

    fun setRentConversionPercent(percent: Float) {
        rentPreferences.conversionPercent = percent
        _rentConversionPercent.value = percent
    }

    // ===== فاز ۶ — نرخ‌نامه‌ی حق‌العمل رسمی (پلکانی فروش + اجاره)، قابل ویرایش چون ممکنه اتحادیه
    // در آینده تغییرش بده. مقادیر پیش‌فرض دقیقاً از روی نرخ‌نامه‌ی ابلاغی گرفته شده‌اند. =====

    private val _saleThreshold1 = MutableStateFlow(commissionTariffPreferences.saleThreshold1)
    val saleThreshold1: StateFlow<Long> = _saleThreshold1
    private val _saleThreshold2 = MutableStateFlow(commissionTariffPreferences.saleThreshold2)
    val saleThreshold2: StateFlow<Long> = _saleThreshold2
    private val _saleThreshold3 = MutableStateFlow(commissionTariffPreferences.saleThreshold3)
    val saleThreshold3: StateFlow<Long> = _saleThreshold3

    private val _saleRate1 = MutableStateFlow(commissionTariffPreferences.saleRate1)
    val saleRate1: StateFlow<Float> = _saleRate1
    private val _saleRate2 = MutableStateFlow(commissionTariffPreferences.saleRate2)
    val saleRate2: StateFlow<Float> = _saleRate2
    private val _saleRate3 = MutableStateFlow(commissionTariffPreferences.saleRate3)
    val saleRate3: StateFlow<Float> = _saleRate3
    private val _saleRate4 = MutableStateFlow(commissionTariffPreferences.saleRate4)
    val saleRate4: StateFlow<Float> = _saleRate4

    private val _rentCommissionPercent = MutableStateFlow(commissionTariffPreferences.rentCommissionPercent)
    val rentCommissionPercent: StateFlow<Float> = _rentCommissionPercent

    fun setSaleThreshold1(value: Long) { commissionTariffPreferences.saleThreshold1 = value; _saleThreshold1.value = value }
    fun setSaleThreshold2(value: Long) { commissionTariffPreferences.saleThreshold2 = value; _saleThreshold2.value = value }
    fun setSaleThreshold3(value: Long) { commissionTariffPreferences.saleThreshold3 = value; _saleThreshold3.value = value }
    fun setSaleRate1(value: Float) { commissionTariffPreferences.saleRate1 = value; _saleRate1.value = value }
    fun setSaleRate2(value: Float) { commissionTariffPreferences.saleRate2 = value; _saleRate2.value = value }
    fun setSaleRate3(value: Float) { commissionTariffPreferences.saleRate3 = value; _saleRate3.value = value }
    fun setSaleRate4(value: Float) { commissionTariffPreferences.saleRate4 = value; _saleRate4.value = value }
    fun setRentCommissionPercent(value: Float) {
        commissionTariffPreferences.rentCommissionPercent = value
        _rentCommissionPercent.value = value
    }
}
