package com.realestate.sami.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.util.toEnglishDigits
import com.realestate.sami.util.toPersianDigits

// توجه: کارت بخش‌های فرم (SectionCard) و برچسب زیربخش (FormSubsectionLabel) از قبل در
// CommonComponents.kt همین پکیج تعریف شده‌ن (فقط SectionCard رو با پارامتر icon اختیاری
// گسترش دادیم) — این‌جا فقط کامپوننت‌های تازه‌ای که فرم ثبت ملک بهشون نیاز داشت اضافه شده.

/**
 * OutlinedTextField با آیکون پیشرو — جایگزین فراخوانی‌های خامِ OutlinedTextField که در
 * فرم قبلی هیچ نشانه‌ی بصری‌ای نداشتن و همه شبیه هم به‌نظر می‌رسیدن.
 */
@Composable
fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    placeholder: String? = null,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    /** آیکون فشرده‌ی سمت‌دیگر فیلد — مثلاً «انتخاب موقعیت از روی نقشه» داخل کادر آدرس، بدون اشغال یک ردیف جدا. */
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    trailingIconContentDescription: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint, style = MaterialTheme.typography.bodySmall) } },
        leadingIcon = icon?.let { i -> { Icon(i, contentDescription = null) } },
        trailingIcon = trailingIcon?.let { i ->
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(i, contentDescription = trailingIconContentDescription)
                    }
                } else {
                    Icon(i, contentDescription = trailingIconContentDescription)
                }
            }
        },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * چیپ امکانات با آیکون — جایگزین CheckboxRow برای امکانات (پارکینگ، آسانسور، استخر...).
 * یک چیپِ FilterChip قابل‌لمسِ بزرگ‌تر از چک‌باکس، که در یک FlowRow کنار هم چیده می‌شه؛
 * برخلاف ستونِ چک‌باکس‌های قبلی، عرضش با متن خودش تنظیم می‌شه، نه با کل عرض صفحه.
 */
@Composable
fun AmenityChip(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onChange(!checked) },
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = if (checked) Icons.Filled.Check else icon, contentDescription = null)
        }
    )
}

/**
 * VisualTransformation برای فیلدهای قیمت‌گذاری: رشته‌ی رقم خامِ ذخیره‌شده در state رو با
 * جداکننده‌ی هزارگان فارسی («٬») و ارقام فارسی نمایش می‌ده، بدون این‌که مقدار واقعی state
 * (که برای parseTomanInput لازمه رقم خام بمونه) تغییر کنه. نگاشت مکان‌نما (cursor) با شمردن
 * رقم‌ها محاسبه می‌شه تا تایپ وسط عدد هم درست کار کنه.
 */
private class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.toEnglishDigits().filter { it.isDigit() }
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val n = digits.length
        val grouped = StringBuilder()
        val digitEndOffset = IntArray(n + 1)
        digitEndOffset[0] = 0
        for (i in 0 until n) {
            val posFromEnd = n - i
            if (i != 0 && posFromEnd % 3 == 0) grouped.append('٬')
            grouped.append(digits[i])
            digitEndOffset[i + 1] = grouped.length
        }
        val transformed = AnnotatedString(grouped.toString().toPersianDigits())

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                digitEndOffset[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int {
                for (i in 0..n) if (digitEndOffset[i] >= offset) return i
                return n
            }
        }
        return TransformedText(transformed, offsetMapping)
    }
}

/**
 * فیلد قیمت‌گذاری با جداکننده‌ی هزارگان زنده (هنگام تایپ) — برای مبالغ فروش/رهن/اجاره.
 * [value] همیشه رشته‌ی رقم خام (بدون جداکننده) است؛ فقط نمایش گروه‌بندی می‌شود، نه مقدار state
 * (که با parseTomanInput مستقیماً به Long تبدیل می‌شود).
 */
@Composable
fun PriceField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.toEnglishDigits().filter { it.isDigit() }) },
        label = { Text(label) },
        leadingIcon = icon?.let { i -> { Icon(i, contentDescription = null) } },
        visualTransformation = ThousandsSeparatorTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * ردیف چک‌باکس ساده با برچسب — برای فرم‌های ساده‌تر مثل ثبت مشتری (نیازمندی‌های بولی مثل پارکینگ/آسانسور).
 * برخلاف [AmenityChip] که یک چیپ بزرگ‌تره، این یک چک‌باکس معمولی کنار متنه؛ برای لیست کوتاه از
 * نیازمندی‌ها (نه امکانات ملک) مناسب‌تره.
 */
@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

/** ردیف سوییچ برای گزینه‌های «روشن/خاموش» که فیلدهای اضافی رو نمایان می‌کنن (مثل «قابل معاوضه»). */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// آیکون هر نوع ملک — برای نمایش در دراپ‌داون «نوع ملک» و هرجای دیگه که لازم باشه.
fun PropertyType.icon(): ImageVector = when (this) {
    PropertyType.APARTMENT -> Icons.Filled.Apartment
    PropertyType.VILLA -> Icons.Filled.Villa
    PropertyType.LAND -> Icons.Filled.Terrain
    PropertyType.COMMERCIAL -> Icons.Filled.Storefront
}

// آیکون هر نوع معامله.
fun DealType.icon(): ImageVector = when (this) {
    DealType.SALE -> Icons.Filled.Sell
    DealType.RENT -> Icons.Filled.CalendarMonth
    DealType.MORTGAGE -> Icons.Filled.AccountBalance
    DealType.EXCHANGE -> Icons.Filled.SwapHoriz
}
