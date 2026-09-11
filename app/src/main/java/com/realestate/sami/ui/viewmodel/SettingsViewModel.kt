package com.realestate.sami.ui.viewmodel

import androidx.lifecycle.ViewModel
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
    private val rentPreferences: RentPreferences
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
}
