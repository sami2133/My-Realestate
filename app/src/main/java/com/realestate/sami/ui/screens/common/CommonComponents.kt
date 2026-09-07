package com.realestate.sami.ui.screens.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.ui.theme.*
import com.realestate.sami.util.toPersianDateString

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

/** بخش تاریخچه تماس + فرم افزودن یادداشت جدید؛ در صفحه جزئیات ملک و متقاضی به‌کار می‌رود. */
@Composable
fun ContactLogSection(
    logs: List<ContactLogEntity>,
    onAddLog: (note: String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    Column {
        Text(stringResource(R.string.contact_log_section_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text(stringResource(R.string.contact_log_placeholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    if (noteText.isNotBlank()) {
                        onAddLog(noteText)
                        noteText = ""
                    }
                }
            ) { Text(stringResource(R.string.action_submit)) }
        }
        Spacer(Modifier.height(12.dp))
        if (logs.isEmpty()) {
            Text(
                stringResource(R.string.contact_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logs.forEach { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(log.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        log.contactDate.toPersianDateString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
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
