package app.dayquest.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.Settings
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Boss
import app.dayquest.game.Catalog
import app.dayquest.game.DayEvent
import app.dayquest.game.DayReport
import app.dayquest.game.Rules
import app.dayquest.notify.Reminders
import app.dayquest.pixel.Look
import app.dayquest.pixel.Sprites
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.edit.EditTaskScreen
import app.dayquest.ui.events.ChestDialog
import app.dayquest.ui.events.EventCardDialog
import app.dayquest.ui.hero.HeroScreen
import app.dayquest.ui.onboarding.OnboardingScreen
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.quests.QuestsScreen
import app.dayquest.ui.settings.SettingsScreen
import app.dayquest.ui.shop.ShopScreen
import app.dayquest.ui.stable.StableScreen
import app.dayquest.ui.tasks.CheckBoxPixel
import app.dayquest.ui.tasks.TasksScreen
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Snackbars with undo, and the tap buzz. */
class Feedback(private val host: SnackbarHostState, private val scope: CoroutineScope, private val haptic: HapticFeedback, private val onUndo: (Long) -> Unit) {
    var haptics = true

    fun show(message: String, undoLabel: String? = null, undoLog: Long = 0) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short, withDismissAction = false)
            if (r == SnackbarResult.ActionPerformed && undoLog > 0) onUndo(undoLog)
        }
    }

    /** A purchase went through: a tap and a coin. */
    fun buy() {
        tap()
        Sfx.play(Sound.COIN)
    }

    fun tap() {
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun thud() {
        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { error("No feedback") }

fun Hero.look() = Look(skin, hairStyle, hairColor, weapon, armor, head, shield)

private class HeroBox(val hero: Hero?)

enum class Tab { TASKS, SHOP, STABLE, QUESTS, HERO }

@Composable
fun DayquestRoot(settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val heroBox by remember { c.db.game().observeHero().map { HeroBox(it) } }.collectAsStateWithLifecycle(initialValue = null)
    val box = heroBox ?: run {
        Box(Modifier.fillMaxSize().background(D.c.bg))
        return
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val feedback = remember { Feedback(snackbar, scope, haptic) { id -> Sfx.play(Sound.UNDO); c.launch { c.game.undo(id) } } }
    feedback.haptics = settings.haptics
    Sfx.enabled = settings.sounds

    CompositionLocalProvider(LocalFeedback provides feedback) {
        Box(Modifier.fillMaxSize().background(D.c.bg)) {
            val hero = box.hero
            if (hero == null || hero.name.isEmpty()) {
                OnboardingScreen()
            } else {
                Main(hero, settings)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp)) { data ->
                Snackbar(
                    data,
                    shape = D.RMd,
                    containerColor = if (D.c.dark) D.c.surface2 else D.c.ink,
                    contentColor = if (D.c.dark) D.c.ink else D.c.bg,
                    actionColor = D.c.xp,
                )
            }
        }
    }
}

@Composable
private fun Main(hero: Hero, settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.TASKS) }

    // ---- New day ----
    var recap by remember { mutableStateOf<Pair<Long, List<Task>>?>(null) }
    var report by remember { mutableStateOf<DayReport?>(null) }
    suspend fun checkDay() {
        val pending = c.game.pendingRecap() ?: return
        val h = c.game.hero()
        if (pending.second.isNotEmpty() && !h.resting) withContext(kotlinx.coroutines.Dispatchers.Main) { recap = pending }
        else {
            val r = c.game.startNewDay()
            if (r != null && (r.hp + r.bossHit > 0 || r.fainted || r.found.isNotEmpty())) withContext(kotlinx.coroutines.Dispatchers.Main) { report = r }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { c.scope.launch { checkDay() } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            if (recap == null) c.scope.launch { checkDay() }
        }
    }

    // ---- Big moments from any action ----
    var levelUp by remember { mutableIntStateOf(0) }
    var fainted by remember { mutableStateOf(false) }
    var victory by remember { mutableStateOf<Boss?>(null) }
    val unlocked = remember { mutableStateListOf<app.dayquest.game.Achievement>() }
    suspend fun checkAchievements() {
        val fresh = c.game.checkAchievements()
        if (fresh.isNotEmpty()) withContext(kotlinx.coroutines.Dispatchers.Main) { unlocked.addAll(fresh) }
    }
    LaunchedEffect(hero.level, hero.questsDone, hero.ownedDecor) { c.scope.launch { checkAchievements() } }
    // ---- Perfect day chest and the morning card ----
    var chest by remember { mutableStateOf(false) }
    var chestPerfect by remember { mutableStateOf(false) }
    var cardOpened by remember { mutableStateOf(-1L) }

    LaunchedEffect(Unit) {
        c.game.fx.collect { o ->
            c.scope.launch { checkAchievements() }
            if (o.fainted) fainted = true
            o.bossDefeated?.let { victory = it }
            if (o.levelUp > 0 && !o.fainted) levelUp = o.levelUp
            soundOf(o)?.let { Sfx.play(it) }
            if (o.perfect) launch {
                // Let the fireworks go off over the scene before the chest covers it.
                delay(450)
                Sfx.play(Sound.PERFECT)
                delay(1600)
                chestPerfect = true
                chest = true
            }
        }
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (!app.dayquest.BuildConfig.DEMO && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                        when (t) {
                            Tab.TASKS -> TasksScreen(
                                hero = hero,
                                onChest = { chestPerfect = false; chest = true },
                                onEdit = { nav.navigate("edit/$it") },
                                onNew = { type, title -> nav.navigate("new/${type.name}?title=${android.net.Uri.encode(title)}") },
                                onSettings = { nav.navigate("settings") },
                            )
                            Tab.SHOP -> ShopScreen(hero, onEdit = { nav.navigate("edit/$it") }, onNew = { nav.navigate("new/REWARD?title=${android.net.Uri.encode(it)}") })
                            Tab.STABLE -> StableScreen(hero)
                            Tab.QUESTS -> QuestsScreen(hero)
                            Tab.HERO -> HeroScreen(hero, onSettings = { nav.navigate("settings") }, onHome = { nav.navigate("home") })
                        }
                    }
                }
                BottomBar(tab, hero) { tab = it }
            }
        }
        composable(
            "new/{type}?title={title}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("title") { type = NavType.StringType; defaultValue = "" }),
        ) { e ->
            EditTaskScreen(
                id = 0, type = TaskType.valueOf(e.arguments?.getString("type") ?: "TODO"), initialTitle = e.arguments?.getString("title").orEmpty(),
                onDone = { nav.popBackStack() },
            )
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            EditTaskScreen(id = e.arguments?.getLong("id") ?: 0, type = TaskType.TODO, initialTitle = "", onDone = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(settings, onBack = { nav.popBackStack() }, onGuide = { nav.navigate("guide") }) }
        composable("guide") {
            app.dayquest.ui.onboarding.HowItWorks(hero.look(), stringResource(R.string.guide_done), onFinish = { nav.popBackStack() })
        }
        composable("home") { app.dayquest.ui.home.HomeScreen(hero, onBack = { nav.popBackStack() }) }
    }

    recap?.let { (day, missed) ->
        RecapDialog(missed) { checked ->
            recap = null
            c.scope.launch {
                checked.forEach { c.game.setDaily(it, true, day) }
                val r = c.game.startNewDay()
                if (r != null && (r.hp + r.bossHit > 0 || r.fainted || r.found.isNotEmpty())) withContext(kotlinx.coroutines.Dispatchers.Main) { report = r }
            }
        }
    }
    report?.takeIf { !it.fainted }?.let { r ->
        val hurt = r.hp + r.bossHit > 0
        MomentDialog(
            sprite = if (hurt) Sprites.heart else itemSprite(r.found) ?: Sprites.heart,
            title = stringResource(if (hurt) R.string.recap_title else R.string.found_title),
            text = listOfNotNull(
                stringResource(R.string.report_hurt, gainText(r.hp)).takeIf { r.hp > 0 },
                stringResource(R.string.report_boss, gainText(r.bossHit)).takeIf { r.bossHit > 0 },
                stringResource(R.string.report_found, petName(hero.pet), itemName(r.found)).takeIf { r.found.isNotEmpty() },
            ).joinToString("\n"),
            button = stringResource(R.string.next),
        ) { report = null }
    }
    if (fainted || report?.fainted == true) {
        MomentDialog(Sprites.tombstone, stringResource(R.string.faint_title), stringResource(R.string.faint_text), stringResource(R.string.faint_ok)) {
            fainted = false; report = null
        }
    }
    if (levelUp > 0) {
        val newGear = Catalog.fixedGear.any { it.level == levelUp && it.season == null } ||
            (levelUp >= app.dayquest.game.Forge.FIRST_LEVEL && (levelUp - app.dayquest.game.Forge.FIRST_LEVEL) % app.dayquest.game.Forge.STEP == 0)
        MomentDialog(
            Sprites.star, stringResource(R.string.level_title),
            stringResource(R.string.level_text, levelUp) + if (newGear) "\n" + stringResource(R.string.level_unlocked) else "",
            stringResource(R.string.hooray),
        ) { levelUp = 0 }
    }
    // Several at once (e.g. after an update) come as one celebration, not a queue of dialogs.
    if (victory == null && levelUp == 0 && !fainted && unlocked.isNotEmpty()) {
        val best = unlocked.maxBy { it.tier }
        MomentDialog(
            Sprites.medal(best.tier), stringResource(R.string.ach_new),
            unlocked.map { androidx.compose.ui.res.pluralStringResource(it.metric.title, it.target, it.target) }.joinToString("\n") +
                "\n" + stringResource(R.string.ach_reward, goldText(unlocked.sumOf { it.gold })),
            stringResource(R.string.hooray), pixel = 6,
        ) { unlocked.clear() }
    }
    val today = Rules.logicalDay(java.time.LocalDateTime.now(), settings.dayStart)
    val calm = recap == null && report == null && !fainted && levelUp == 0 && victory == null && unlocked.isEmpty()
    if (calm && !chest && hero.lastDay == today && hero.eventDay != today && cardOpened != today) {
        EventCardDialog(DayEvent.of(today)) {
            cardOpened = today
            c.launch { c.game.openEvent() }
        }
    }
    if (calm && chest) {
        ChestDialog(chestPerfect, hero.chests) { loot ->
            chest = false
            if (loot != null && loot.levelUp > 0) levelUp = loot.levelUp
        }
    }
    victory?.let { b ->
        val loot = b.rewards.map { (k, n) -> "${itemName(k)} ×$n" }.joinToString(", ")
        MomentDialog(
            Sprites.boss(b.shape, b.primary, b.secondary), stringResource(R.string.victory_title),
            stringResource(R.string.victory_text, bossName(b), num(b.gold), num(b.xp), loot),
            stringResource(R.string.hooray), pixel = 4,
        ) { victory = null }
    }
}

/** The one sound an outcome makes: the biggest moment wins. */
private fun soundOf(o: app.dayquest.game.Outcome): Sound? = when {
    o.fainted -> Sound.FAINT
    o.bossDefeated != null -> Sound.VICTORY
    o.levelUp > 0 -> Sound.LEVEL
    o.jackpot || o.bounty -> Sound.JACKPOT
    o.crit -> Sound.CRIT
    o.hp < 0 -> Sound.HURT
    o.drop.isNotEmpty() -> Sound.DROP
    o.xp > 0 -> Sound.DONE
    o.gold != 0.0 || o.hp > 0 -> Sound.COIN
    else -> null
}

@Composable
fun MomentDialog(sprite: app.dayquest.pixel.Sprite, title: String, text: String, button: String, pixel: Int = 6, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelImage(sprite, pixel = pixel.dp)
                Gap(16)
                Text(title, style = DType.Title, textAlign = TextAlign.Center)
                Gap(8)
                Text(text, style = DType.Sub, textAlign = TextAlign.Center)
                Gap(20)
                PrimaryButton(button, onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
private fun RecapDialog(missed: List<Task>, onDone: (List<Long>) -> Unit) {
    val checked = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = {},
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(stringResource(R.string.recap_title), style = DType.Title) },
        text = {
            Column {
                Text(stringResource(R.string.recap_text), style = DType.Sub)
                Gap(14)
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    missed.forEach { t ->
                        val on = t.id in checked
                        Row(
                            Modifier.fillMaxWidth().clip(D.RSm).clickable { if (on) checked.remove(t.id) else checked.add(t.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckBoxPixel(on, if (on) D.c.good else D.c.hp, size = 34)
                            Spacer(Modifier.width(12.dp))
                            Text(t.title, style = DType.Body)
                        }
                    }
                }
                Gap(16)
                PrimaryButton(stringResource(R.string.recap_start), { onDone(checked.toList()) }, Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
private fun BottomBar(tab: Tab, hero: Hero, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(D.c.surface)
            .border(1.dp, D.c.line)
            .navigationBarsPadding()
            .height(68.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            val on = t == tab
            Column(
                Modifier
                    .weight(1f)
                    .clip(D.RSm)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 52.dp, height = 34.dp).clip(D.RSm).background(if (on) D.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    val sprite = if (t == Tab.HERO) Sprites.heroBust(hero.look()) else Sprites.navIcon(t.ordinal)
                    PixelImage(sprite, pixel = if (t == Tab.HERO) 1.4.dp else 2.dp, alpha = if (on) 1f else 0.75f)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(
                        when (t) {
                            Tab.TASKS -> R.string.nav_tasks
                            Tab.SHOP -> R.string.nav_shop
                            Tab.STABLE -> R.string.nav_stable
                            Tab.QUESTS -> R.string.nav_quests
                            Tab.HERO -> R.string.nav_hero
                        }
                    ),
                    style = DType.Pixel.copy(fontSize = 15.sp, color = if (on) D.c.accent else D.c.faint),
                    maxLines = 1,
                )
            }
        }
    }
}
