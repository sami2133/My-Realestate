package com.realestate.sami.ui.screens.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.maps.android.compose.MapType
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyImage
import com.realestate.sami.data.local.entity.VisitEntity
import com.realestate.sami.data.local.entity.VisitResult
import com.realestate.sami.ui.theme.*
import com.realestate.sami.util.calculateAdjustedRent
import com.realestate.sami.util.openCalendarToAddVisit
import com.realestate.sami.util.pickSliderStepToman
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toPlainPercentString
import com.realestate.sami.util.toTomanDisplay
import com.realestate.sami.util.viewImageExternally
import kotlin.math.roundToInt

fun DealType.color(): Color = when (this) {
    DealType.SALE -> DealSaleColor
    DealType.RENT -> DealRentColor
    DealType.MORTGAGE -> DealMortgageColor
    DealType.EXCHANGE -> DealExchangeColor
}

@Composable
fun DealTypeChip(dealType: DealType, label: String) {
    val color = dealType.color()
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/** ردیف اکشن سریع: تماس مستقیم و ارسال پیامک، بدون نیاز به خروج از حالت مرور. */
@Composable
fun PhoneActionRow(phone: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_call))
        }
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_sms))
        }
    }
}

/** گزینه‌های سریع برای تعیین تاریخ یادآوری پیگیری بعدی (فاز ۵ — به‌جای تقویم کامل شمسی که هنوز پیاده نشده). */
private enum class FollowUpQuickOption(val labelRes: Int, val daysFromNow: Int?) {
    NONE(R.string.follow_up_option_none, null),
    TOMORROW(R.string.follow_up_option_tomorrow, 1),
    IN_3_DAYS(R.string.follow_up_option_3_days, 3),
    IN_1_WEEK(R.string.follow_up_option_1_week, 7)
}

/** بخش تاریخچه تماس + فرم افزودن یادداشت جدید (با امکان تعیین یادآوری پیگیری)؛ در صفحه جزئیات ملک و متقاضی به‌کار می‌رود. */
@Composable
fun ContactLogSection(
    logs: List<ContactLogEntity>,
    onAddLog: (note: String, followUpDate: Long?) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf(FollowUpQuickOption.NONE) }

    Column {
        Text(stringResource(R.string.contact_log_section_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            placeholder = { Text(stringResource(R.string.contact_log_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.follow_up_section_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FollowUpQuickOption.entries.forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = { selectedOption = option },
                    label = { Text(stringResource(option.labelRes), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FilledTonalButton(
            onClick = {
                if (noteText.isNotBlank()) {
                    val followUpDate = selectedOption.daysFromNow?.let {
                        System.currentTimeMillis() + it * 24L * 60 * 60 * 1000
                    }
                    onAddLog(noteText, followUpDate)
                    noteText = ""
                    selectedOption = FollowUpQuickOption.NONE
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) { Text(stringResource(R.string.action_submit)) }

        Spacer(Modifier.height(12.dp))
        if (logs.isEmpty()) {
            Text(
                stringResource(R.string.contact_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logs.forEach { log ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(log.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            log.contactDate.toPersianDateString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (log.followUpDate != null) {
                        Text(
                            stringResource(
                                if (log.isFollowUpDone) R.string.follow_up_done_badge else R.string.follow_up_pending_badge,
                                log.followUpDate.toPersianDateString()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (log.isFollowUpDone) MaterialTheme.colorScheme.onSurfaceVariant else GoldAccent
                        )
                    }
                }
                Divider()
            }
        }
    }
}

/** برچسب فارسیِ هر نوع نقشه، برای نمایش در منوی سوییچر. */
@Composable
private fun MapType.label(): String = when (this) {
    MapType.NORMAL -> stringResource(R.string.map_type_normal)
    MapType.SATELLITE -> stringResource(R.string.map_type_satellite)
    MapType.TERRAIN -> stringResource(R.string.map_type_terrain)
    MapType.HYBRID -> stringResource(R.string.map_type_hybrid)
    MapType.NONE -> stringResource(R.string.map_type_normal)
}

/**
 * دکمه‌ی شناور که با کلیک، منوی انتخاب نوع نقشه (عادی/ماهواره‌ای/توپوگرافی/ترکیبی) را باز می‌کند.
 * روی گوشه‌ای از نقشه (مثلاً بالا-راست) قرار بگیرد.
 */
@Composable
fun MapTypeSwitcher(
    currentType: MapType,
    onTypeSelected: (MapType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
        ) {
            Icon(Icons.Filled.Layers, contentDescription = stringResource(R.string.map_type_switcher_cd))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(MapType.NORMAL, MapType.SATELLITE, MapType.TERRAIN, MapType.HYBRID).forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    trailingIcon = {
                        if (type == currentType) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/** یک گزینه‌ی قابل انتخاب برای طرف مقابل بازدید (متقاضی هنگام ثبت از صفحه ملک، یا ملک هنگام ثبت از صفحه متقاضی). */
data class VisitCandidate(val id: Long, val label: String, val subLabel: String)

/**
 * فاز ۵ — بخش «بازدیدها»: زمان‌بندی بازدید بین یک ملک و یک متقاضی سازگار + افزودن به تقویم سیستم
 * + ثبت نتیجه‌ی بازدید. هم در صفحه جزئیات ملک و هم صفحه جزئیات متقاضی استفاده می‌شود.
 *
 * توجه: انتخاب تاریخ/ساعت از طریق DatePickerDialog/TimePickerDialog استاندارد اندروید انجام می‌شود
 * که میلادی است (تقویم شمسی اختصاصی برای انتخاب تاریخ هنوز پیاده نشده، مثل فونت اختصاصی که در
 * ui/theme/Type.kt یادداشت شده)؛ تاریخ نهایی ذخیره‌شده در همه‌جای اپ به‌صورت شمسی نمایش داده می‌شود.
 */
@Composable
fun VisitScheduleSection(
    visits: List<VisitEntity>,
    candidates: List<VisitCandidate>,
    otherPartyIdOf: (VisitEntity) -> Long,
    eventTitleFor: (VisitCandidate) -> String,
    onSchedule: (candidateId: Long, visitDateMillis: Long) -> Unit,
    onResultChange: (VisitEntity, VisitResult) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.visit_section_title), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showDialog = true }, enabled = candidates.isNotEmpty()) {
                Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.visit_schedule_action))
            }
        }

        if (candidates.isEmpty() && visits.isEmpty()) {
            Text(
                stringResource(R.string.visit_no_candidates),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (visits.isEmpty()) {
            if (candidates.isNotEmpty()) {
                Text(
                    stringResource(R.string.visit_section_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            visits.forEach { visit ->
                val candidate = candidates.find { it.id == otherPartyIdOf(visit) }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            candidate?.label ?: stringResource(R.string.visit_unknown_party),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            visit.visitDate.toPersianDateString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        context.openCalendarToAddVisit(
                            title = candidate?.let { eventTitleFor(it) } ?: context.getString(R.string.visit_section_title),
                            beginMillis = visit.visitDate
                        )
                    }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.visit_add_to_calendar))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    visitResultLabels().forEach { (result, label) ->
                        FilterChip(
                            selected = visit.result == result,
                            onClick = { onResultChange(visit, result) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Divider(Modifier.padding(top = 8.dp))
            }
        }
    }

    if (showDialog) {
        VisitScheduleDialog(
            candidates = candidates,
            onDismiss = { showDialog = false },
            onConfirm = { candidateId, dateMillis ->
                onSchedule(candidateId, dateMillis)
                showDialog = false
            }
        )
    }
}

@Composable
private fun visitResultLabels(): List<Pair<VisitResult, String>> = listOf(
    VisitResult.PENDING to stringResource(R.string.visit_result_pending),
    VisitResult.INTERESTED to stringResource(R.string.visit_result_interested),
    VisitResult.NOT_INTERESTED to stringResource(R.string.visit_result_not_interested),
    VisitResult.DEAL_CLOSED to stringResource(R.string.visit_result_deal_closed)
)

@Composable
private fun VisitScheduleDialog(
    candidates: List<VisitCandidate>,
    onDismiss: () -> Unit,
    onConfirm: (candidateId: Long, dateMillis: Long) -> Unit
) {
    val context = LocalContext.current
    var selectedCandidate by remember { mutableStateOf(candidates.firstOrNull()) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.visit_schedule_dialog_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.visit_pick_party_label), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 180.dp)) {
                    candidates.forEach { candidate ->
                        ListItem(
                            headlineContent = { Text(candidate.label) },
                            supportingContent = { Text(candidate.subLabel) },
                            trailingContent = {
                                if (selectedCandidate?.id == candidate.id) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable { selectedCandidate = candidate }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.visit_pick_datetime_label), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        pickVisitDateTime(context) { millis -> selectedDateMillis = millis }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(selectedDateMillis?.toPersianDateString() ?: stringResource(R.string.visit_pick_datetime_placeholder))
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            val candidate = selectedCandidate
                            val date = selectedDateMillis
                            if (candidate != null && date != null) onConfirm(candidate.id, date)
                        },
                        enabled = selectedCandidate != null && selectedDateMillis != null,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.visit_schedule_confirm)) }
                }
            }
        }
    }
}

/** انتخاب تاریخ (DatePickerDialog) و سپس ساعت (TimePickerDialog) پشت سر هم — دیالوگ‌های استاندارد اندروید. */
private fun pickVisitDateTime(context: android.content.Context, onPicked: (Long) -> Unit) {
    val now = java.util.Calendar.getInstance()
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val cal = java.util.Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                    }
                    onPicked(cal.timeInMillis)
                },
                now.get(java.util.Calendar.HOUR_OF_DAY),
                now.get(java.util.Calendar.MINUTE),
                true
            ).show()
        },
        now.get(java.util.Calendar.YEAR),
        now.get(java.util.Calendar.MONTH),
        now.get(java.util.Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }.show()
}

/**
 * نمایش‌گر تمام‌صفحه‌ی مجموعه‌ی عکس‌های یک رکورد، با امکان swipe بین آن‌ها — داخل خودِ اپ،
 * بدون اینکه عکس‌ها به گالری سیستم گوشی اضافه بشن (حریم خصوصی عکس‌ها تغییری نمی‌کنه).
 * [startIndex] عکسی که کاربر رویش لمس کرده اول نمایش داده می‌شود.
 */
@Composable
fun ImageGalleryDialog(
    images: List<PropertyImage>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val image = images[page]
                if (image.localUri != null) {
                    AsyncImage(
                        model = image.localUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // این عکس روی یک دستگاه دیگه‌ی تیم ثبت شده؛ با «همگام‌سازی الان» دانلود می‌شود
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = stringResource(R.string.image_not_downloaded_yet),
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // نوار بالا: شمارنده‌ی صفحه، «باز کردن با…» برای عکس جاری، و بستن
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
                }
                Text(
                    "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(onClick = {
                    images.getOrNull(pagerState.currentPage)?.localUri?.let { uri ->
                        context.viewImageExternally(uri)
                    }
                }) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = stringResource(R.string.property_image_open_with), tint = Color.White)
                }
            }
        }
    }
}

/**
 * فاز ۵.۲ — نوار لغزنده‌ی تعدیل رهن↔اجاره: بین [minDeposit] (کف قابل‌مذاکره) و [baseDeposit]
 * (رهن پایه‌ی ثبت‌شده، سقف بازه) حرکت می‌کنه و اجاره‌ی متناظر رو زنده محاسبه و نشون می‌ده.
 * روی گام‌های رند (پیک‌شده با [pickSliderStepToman]) می‌ایسته تا رسیدن به یک عدد گرد راحت باشه.
 * موقعیت به‌صورت شمارنده‌ی صحیح گام (نه مبلغ خام) نگه داشته می‌شه تا خطای گرد کردن اعداد
 * اعشاری روی مبالغ چند صد میلیونی (که از محدوده‌ی دقیق Float می‌گذرن) پیش نیاد.
 */
@Composable
fun RentDepositAdjustmentSlider(
    baseDeposit: Long,
    baseRent: Long,
    minDeposit: Long,
    conversionPercent: Float,
    modifier: Modifier = Modifier
) {
    if (minDeposit >= baseDeposit) return

    val span = baseDeposit - minDeposit
    val step = remember(span) { pickSliderStepToman(span) }
    val stepsCount = (span / step).toInt().coerceAtLeast(1)
    var position by remember(baseDeposit, minDeposit) { mutableStateOf(stepsCount) } // شروع از رهن پایه (کامل)
    val currentDeposit = minDeposit + position.toLong() * step
    val adjustedRent = calculateAdjustedRent(baseDeposit, baseRent, currentDeposit, conversionPercent)

    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.rent_calc_title), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.rent_calc_deposit_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(currentDeposit.toTomanDisplay(), style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = position.toFloat(),
            onValueChange = { position = it.roundToInt() },
            valueRange = 0f..stepsCount.toFloat(),
            steps = (stepsCount - 1).coerceAtLeast(0)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.rent_calc_rent_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(adjustedRent.toTomanDisplay(), style = MaterialTheme.typography.titleMedium, color = GoldAccent)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.rent_calc_hint, conversionPercent.toPlainPercentString()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
