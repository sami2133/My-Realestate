package com.realestate.sami.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * فاز ۵ — نرخ کمیسیون دفتر (درصد)، برای برآورد درآمد کمیسیون در داشبورد آماری.
 * چون قیمت هر معامله در PropertyEntity ذخیره شده ولی نرخ کمیسیون به ازای هر دفتر/مشاور فرق
 * می‌کند، این عدد یک تنظیم سراسری ساده است که کاربر از صفحه‌ی گزارش‌ها ویرایش می‌کند.
 */
@Singleton
class ReportPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("report_prefs", Context.MODE_PRIVATE)

    var commissionPercent: Float
        get() = prefs.getFloat(KEY_COMMISSION_PERCENT, DEFAULT_COMMISSION_PERCENT)
        set(value) = prefs.edit().putFloat(KEY_COMMISSION_PERCENT, value).apply()

    companion object {
        private const val KEY_COMMISSION_PERCENT = "commission_percent"
        const val DEFAULT_COMMISSION_PERCENT = 2f
    }
}
