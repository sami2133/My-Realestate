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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyType

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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint, style = MaterialTheme.typography.bodySmall) } },
        leadingIcon = icon?.let { i -> { Icon(i, contentDescription = null) } },
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
    PropertyType.OFFICE -> Icons.Filled.Business
}

// آیکون هر نوع معامله.
fun DealType.icon(): ImageVector = when (this) {
    DealType.SALE -> Icons.Filled.Sell
    DealType.RENT -> Icons.Filled.CalendarMonth
    DealType.MORTGAGE -> Icons.Filled.AccountBalance
    DealType.EXCHANGE -> Icons.Filled.SwapHoriz
}
