package app.dayquest.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.dayquest.AppContainer
import app.dayquest.BuildConfig
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Backup
import app.dayquest.data.ThemeMode
import app.dayquest.game.Challenge
import app.dayquest.notify.Reminders
import app.dayquest.pixel.Sprites
import app.dayquest.ui.Language
import app.dayquest.ui.components.Chip
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TimeDialog
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import app.dayquest.ui.timeLabel
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: app.dayquest.data.Settings, onBack: () -> Unit, onGuide: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    var canNotify by remember { mutableStateOf(Reminders.canNotify(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = Reminders.canNotify(context) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }
    var pickEvening by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            SectionTitle(stringResource(R.string.transfer))
            TransferCard()

            SectionTitle(stringResource(R.string.set_appearance))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.theme_system), settings.theme == ThemeMode.SYSTEM, { c.scope.launch { c.settings.setTheme(ThemeMode.SYSTEM) } })
                Chip(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT, { c.scope.launch { c.settings.setTheme(ThemeMode.LIGHT) } })
                Chip(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK, { c.scope.launch { c.settings.setTheme(ThemeMode.DARK) } })
            }

            SectionTitle(stringResource(R.string.language))
            val current = Language.current()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.lang_system), current == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", current == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", current == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.challenge))
            ChallengePicker(settings.challenge) { ch -> c.scope.launch { c.settings.setChallenge(ch) } }

            SectionTitle(stringResource(R.string.set_game))
            DCard {
                Text(stringResource(R.string.day_start), style = DType.Strong)
                Gap(2)
                Text(stringResource(R.string.day_start_text), style = DType.Muted)
                Gap(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..6).forEach { h -> Chip(timeLabel(h * 60), settings.dayStart == h, { c.launch { c.settings.setDayStart(h) } }) }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = D.c.line)
                Toggle(stringResource(R.string.haptics), null, settings.haptics) { v -> c.scope.launch { c.settings.setHaptics(v) } }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = D.c.line)
                Toggle(stringResource(R.string.sounds), stringResource(R.string.sounds_text), settings.sounds) { v ->
                    app.dayquest.ui.Sfx.enabled = v
                    if (v) app.dayquest.ui.Sfx.play(app.dayquest.ui.Sound.DONE)
                    c.scope.launch { c.settings.setSounds(v) }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = D.c.line)
                Toggle(stringResource(R.string.southern), stringResource(R.string.southern_text), settings.southern) { v -> c.scope.launch { c.settings.setSouthern(v) } }
            }

            SectionTitle(stringResource(R.string.set_reminders))
            if (!canNotify) {
                DCard(onClick = { if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.perm_title), style = DType.Strong.copy(color = D.c.accent))
                    Gap(2)
                    Text(stringResource(R.string.perm_text), style = DType.Muted)
                }
                Gap(10)
            }
            DCard {
                Toggle(stringResource(R.string.reminders), stringResource(R.string.reminders_text), settings.reminders) { v -> c.launch { c.settings.setReminders(v) } }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = D.c.line)
                Text(stringResource(R.string.evening), style = DType.Strong)
                Gap(2)
                Text(stringResource(R.string.evening_text), style = DType.Muted)
                Gap(8)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(if (settings.evening >= 0) timeLabel(settings.evening) else timeLabel(20 * 60), settings.evening >= 0, { pickEvening = true })
                    Chip(stringResource(R.string.off), settings.evening < 0, { c.launch { c.settings.setEvening(-1) } })
                }
            }
            Gap(10)
            DCard(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }) {
                Text(stringResource(R.string.autostart_title), style = DType.Strong)
                Gap(2)
                Text(stringResource(R.string.autostart_text), style = DType.Muted)
            }

            SectionTitle(stringResource(R.string.guide))
            DCard(onClick = onGuide) {
                Text(stringResource(R.string.guide), style = DType.Strong.copy(color = D.c.accent))
                Gap(2)
                Text(stringResource(R.string.g1_title), style = DType.Muted)
            }

            SectionTitle(stringResource(R.string.about))
            DCard {
                Text(stringResource(R.string.about_text), style = DType.Sub)
                Gap(8)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = DType.Small)
            }
        }
    }

    if (pickEvening) TimeDialog(if (settings.evening >= 0) settings.evening else 20 * 60, { pickEvening = false }) { m -> c.launch { c.settings.setEvening(m) } }
}

/** Relaxed, normal or heroic: one card each, the chosen one outlined. */
@Composable
fun ChallengePicker(selected: Challenge, onSelect: (Challenge) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Challenge.entries.forEach { ch ->
            val on = ch == selected
            Row(
                Modifier.fillMaxWidth().clip(D.RMd).background(if (on) D.c.accentSoft else D.c.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) D.c.accent else D.c.line, D.RMd)
                    .clickable { onSelect(ch) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelImage(
                    when (ch) {
                        Challenge.RELAXED -> Sprites.heart
                        Challenge.NORMAL -> Sprites.star
                        Challenge.HEROIC -> Sprites.flame
                    },
                    pixel = 3.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(ch.label), style = DType.Strong.copy(color = if (on) D.c.accent else D.c.ink))
                    Gap(2)
                    Text(stringResource(ch.text), style = DType.Muted)
                }
            }
        }
    }
}

private val Challenge.label get() = when (this) {
    Challenge.RELAXED -> R.string.ch_relaxed
    Challenge.NORMAL -> R.string.ch_normal
    Challenge.HEROIC -> R.string.ch_heroic
}

private val Challenge.text get() = when (this) {
    Challenge.RELAXED -> R.string.ch_relaxed_text
    Challenge.NORMAL -> R.string.ch_normal_text
    Challenge.HEROIC -> R.string.ch_heroic_text
}

@Composable
private fun Toggle(title: String, text: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DType.Strong)
            if (text != null) {
                Gap(2)
                Text(text, style = DType.Muted)
            }
        }
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = D.c.accent))
    }
}

private suspend fun exportJson(c: AppContainer) = Backup.encode(c.db.game().snapshot(), c.settings.now())

/** Everything needed to carry a hero to another phone without an account. */
@Composable
fun TransferCard(onImported: () -> Unit = {}) {
    val context = LocalContext.current
    val c = context.container
    var pendingText by remember { mutableStateOf<String?>(null) }
    var pasting by remember { mutableStateOf(false) }
    val saved = stringResource(R.string.saved)
    val failed = stringResource(R.string.save_failed)
    val copied = stringResource(R.string.code_copied)
    val bad = stringResource(R.string.import_failed)
    val shareTitle = stringResource(R.string.share_via)
    val welcome = R.string.imported

    fun toast(s: String) = Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    val fileName = "dayquest-${LocalDate.now()}.json"

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val ok = runCatching {
                val json = exportJson(c)
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
            withContext(Dispatchers.Main) { toast(if (ok) saved else failed) }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
            withContext(Dispatchers.Main) {
                if (text == null || runCatching { Backup.decode(text) }.isFailure) toast(bad) else pendingText = text
            }
        }
    }

    DCard {
        Text(stringResource(R.string.transfer_text), style = DType.Sub)
        Gap(12)
        FlowRowButtons {
            SoftButton(stringResource(R.string.share_file), {
                c.scope.launch {
                    val uri = runCatching {
                        val dir = File(context.cacheDir, "transfer").apply { mkdirs() }
                        val f = File(dir, fileName).apply { writeText(exportJson(c)) }
                        FileProvider.getUriForFile(context, context.packageName + ".files", f)
                    }.getOrNull()
                    withContext(Dispatchers.Main) {
                        if (uri == null) toast(failed)
                        else context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                shareTitle,
                            )
                        )
                    }
                }
            }, icon = Icons.Rounded.Share)
            SoftButton(stringResource(R.string.export_file), { export.launch(fileName) }, icon = Icons.Rounded.Save)
            SoftButton(stringResource(R.string.copy_code), {
                c.scope.launch {
                    val code = Backup.pack(exportJson(c))
                    withContext(Dispatchers.Main) {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Dayquest", code))
                        toast(copied)
                    }
                }
            }, icon = Icons.Rounded.ContentCopy)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = D.c.line)
        FlowRowButtons {
            SoftButton(stringResource(R.string.import_file), { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, icon = Icons.Rounded.FileOpen)
            SoftButton(stringResource(R.string.paste_code), { pasting = true }, icon = Icons.Rounded.ContentPaste)
        }
    }

    if (pasting) PasteDialog(onDismiss = { pasting = false }) { text ->
        pasting = false
        if (runCatching { Backup.decode(text) }.isFailure) toast(bad) else pendingText = text
    }
    pendingText?.let { text ->
        ConfirmDialog(
            stringResource(R.string.import_title), stringResource(R.string.import_text), stringResource(R.string.import_do),
            onConfirm = {
                c.scope.launch {
                    val name = runCatching { doImport(context, c, text) }.getOrNull()
                    withContext(Dispatchers.Main) {
                        if (name == null) toast(bad) else { toast(context.getString(welcome, name)); onImported() }
                    }
                }
            },
            onDismiss = { pendingText = null }, danger = true,
        )
    }
}

private suspend fun doImport(context: Context, c: AppContainer, text: String): String? {
    val (snapshot, settings) = runCatching { Backup.decode(text) }.getOrNull() ?: return null
    c.db.game().replaceAll(snapshot)
    settings?.let { c.settings.restore(it) }
    Reminders.reschedule(context)
    return snapshot.hero?.name.orEmpty()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowButtons(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onPaste: (String) -> Unit) {
    val context = LocalContext.current
    val clip = remember {
        context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.takeIf { it.startsWith("DQ1:") || it.trimStart().startsWith("{") }.orEmpty()
    }
    var text by remember { mutableStateOf(clip) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        title = { Text(stringResource(R.string.paste_title), style = DType.Heading) },
        text = { DTextField(text, { text = it }, placeholder = stringResource(R.string.paste_hint), minLines = 4) },
        confirmButton = { TextButton(onClick = { onPaste(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.next), color = D.c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = D.c.muted) } },
    )
}

