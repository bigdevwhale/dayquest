package app.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.pixel.Sprites
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import app.dayquest.ui.theme.Inter
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun DCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.line, D.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = DType.Eyebrow, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = D.c.accent) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = D.RMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = D.c.onAccent,
            disabledContainerColor = D.c.line,
            disabledContentColor = D.c.faint,
        ),
    ) { Text(text, style = DType.Button) }
}

/** Quiet tinted button for secondary actions. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = D.c.accent, soft: Color = D.c.accentSoft, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(44.dp)
            .clip(D.RSm)
            .background(if (enabled) soft else D.c.line.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) color else D.c.faint)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = DType.Button.copy(fontSize = 14.sp), color = if (enabled) color else D.c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A buy button showing the price with a pixel coin. */
@Composable
fun PriceButton(price: Double, affordable: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(40.dp)
            .clip(D.RSm)
            .background(if (affordable) D.c.xpSoft else D.c.line.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(Sprites.coin, pixel = 2.dp, alpha = if (affordable) 1f else 0.5f)
        Spacer(Modifier.width(6.dp))
        Text(app.dayquest.ui.goldText(price), style = DType.Number.copy(fontSize = 15.sp, color = if (affordable) D.c.gold else D.c.faint))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = D.c.accent) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
        shape = D.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = D.c.surface,
            labelColor = D.c.muted,
            selectedContainerColor = color.copy(alpha = 0.16f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = D.c.line, selectedBorderColor = color.copy(alpha = 0.5f)),
    )
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(D.RSm)
            .background(D.c.surface2)
            .border(1.dp, D.c.line, D.RSm)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(D.RXs)
                    .background(if (on) D.c.surface else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, D.c.line, D.RXs) else Modifier)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = DType.Button.copy(fontSize = 14.sp), color = if (on) D.c.accent else D.c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = D.c.ink) }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = DType.Heading.copy(fontSize = 24.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun DTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    number: Boolean = false,
    minLines: Int = 1,
    onDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = D.c.faint) },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = D.RSm,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = D.c.surface,
            unfocusedContainerColor = D.c.surface,
            unfocusedBorderColor = D.c.line,
            focusedBorderColor = D.c.accent,
        ),
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        title = { Text(title, style = DType.Heading) },
        text = { Text(text, style = DType.Sub) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (danger) D.c.hp else D.c.accent, style = DType.Button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = D.c.muted, style = DType.Button) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initialMinutes: Int, onDismiss: () -> Unit, onPicked: (Int) -> Unit) {
    val state = rememberTimePickerState(initialMinutes / 60, initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.save), color = D.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = D.c.muted) } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = D.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = D.c.muted) } },
    ) { DatePicker(state) }
}

/** Amount with a pixel icon: coin, heart or star. */
@Composable
fun IconAmount(sprite: app.dayquest.pixel.Sprite, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PixelImage(sprite, pixel = 2.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, style = DType.Number.copy(fontSize = 15.sp, color = color))
    }
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))
