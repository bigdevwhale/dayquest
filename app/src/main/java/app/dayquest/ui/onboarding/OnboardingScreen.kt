package app.dayquest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Difficulty
import app.dayquest.game.SceneKind
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneState
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.hero.LookEditor
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.settings.TransferCard
import app.dayquest.ui.tasks.CheckBoxPixel
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType

private data class Starter(val type: TaskType, val title: Int, val on: Boolean, val up: Boolean = true, val down: Boolean = false, val cost: Double = 0.0, val difficulty: Difficulty = Difficulty.EASY)

private val starters = listOf(
    Starter(TaskType.HABIT, R.string.st_water, true, difficulty = Difficulty.TRIVIAL),
    Starter(TaskType.HABIT, R.string.st_walk, false),
    Starter(TaskType.HABIT, R.string.st_junk, false, up = false, down = true),
    Starter(TaskType.HABIT, R.string.st_scroll, true, up = false, down = true),
    Starter(TaskType.DAILY, R.string.st_bed, true, difficulty = Difficulty.TRIVIAL),
    Starter(TaskType.DAILY, R.string.st_read, true),
    Starter(TaskType.DAILY, R.string.st_exercise, false, difficulty = Difficulty.MEDIUM),
    Starter(TaskType.DAILY, R.string.st_meditate, false),
    Starter(TaskType.DAILY, R.string.st_plan, false),
    Starter(TaskType.TODO, R.string.st_todo_board, true),
    Starter(TaskType.TODO, R.string.st_todo_desk, false),
    Starter(TaskType.REWARD, R.string.st_reward_episode, true, cost = 15.0),
    Starter(TaskType.REWARD, R.string.st_reward_treat, true, cost = 10.0),
)

@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val c = context.container
    var step by rememberSaveable { mutableIntStateOf(0) }
    val defaultName = stringResource(R.string.default_hero_name)
    var name by rememberSaveable { mutableStateOf("") }
    var look by remember { mutableStateOf(Look(1, 0, 1, "wooden_sword", "tunic", "", "")) }
    val picked = remember { mutableStateListOf<Int>().apply { starters.forEachIndexed { i, s -> if (s.on) add(i) } } }

    BackHandler(enabled = step > 0) { step = if (step == 3) 0 else step - 1 }

    Column(Modifier.fillMaxSize().background(D.c.bg)) {
        when (step) {
            0 -> {
                SceneView(SceneState(SceneKind.MEADOW, 12f, look, "fox:base", season = app.dayquest.ui.rememberSeason()), virtualHeight = 96)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Gap(8)
                    Text(stringResource(R.string.welcome_title), style = DType.Display, textAlign = TextAlign.Center)
                    Gap(14)
                    Text(stringResource(R.string.welcome_text), style = DType.Sub, textAlign = TextAlign.Center)
                }
                Column(Modifier.padding(24.dp).navigationBarsPadding()) {
                    PrimaryButton(stringResource(R.string.welcome_start), { step = 1 }, Modifier.fillMaxWidth())
                    TextButton(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.welcome_import), style = DType.Button, color = D.c.accent)
                    }
                }
            }
            1 -> {
                Column(Modifier.statusBarsPadding()) { TopBar(stringResource(R.string.create_title), onBack = { step = 0 }) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).imePadding()) {
                    Gap(8)
                    LookEditor(name, look) { n, l -> name = n; look = l }
                }
                Column(Modifier.padding(18.dp).navigationBarsPadding()) {
                    PrimaryButton(stringResource(R.string.next), { step = 2 }, Modifier.fillMaxWidth())
                }
            }
            2 -> {
                Column(Modifier.statusBarsPadding()) { TopBar(stringResource(R.string.starter_title), onBack = { step = 1 }) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                    Text(stringResource(R.string.starter_text), style = DType.Sub)
                    listOf(TaskType.HABIT to R.string.tab_habits, TaskType.DAILY to R.string.tab_dailies, TaskType.TODO to R.string.tab_todos, TaskType.REWARD to R.string.shop_rewards)
                        .forEach { (type, label) ->
                            SectionTitle(stringResource(label))
                            starters.withIndex().filter { it.value.type == type }.forEach { (i, s) ->
                                val on = i in picked
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd)
                                        .clickable { if (on) picked.remove(i) else picked.add(i) }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CheckBoxPixel(on, D.c.accent, 32)
                                    Spacer(Modifier.width(12.dp))
                                    Text((if (s.down && !s.up) "− " else if (type == TaskType.HABIT) "+ " else "") + stringResource(s.title), style = DType.Body)
                                }
                            }
                        }
                    Gap(16)
                }
                Column(Modifier.padding(18.dp).navigationBarsPadding()) {
                    PrimaryButton(stringResource(R.string.starter_go), {
                        val chosen = picked.sorted().map { starters[it] }.map { s -> s to context.getString(s.title) }
                        val heroName = name.trim().ifEmpty { defaultName }
                        c.launch {
                            val today = c.game.today()
                            val db = c.db.game()
                            chosen.forEachIndexed { i, (s, title) ->
                                db.insert(Task(type = s.type, title = title, up = s.up, down = s.down, cost = if (s.cost > 0) s.cost else 10.0, difficulty = s.difficulty, startDay = today, position = i))
                            }
                            c.game.createHero(heroName, look.skin, look.hairStyle, look.hairColor)
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
            else -> {
                Column(Modifier.statusBarsPadding()) { TopBar(stringResource(R.string.transfer), onBack = { step = 0 }) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding()) {
                    Gap(8)
                    TransferCard()
                }
            }
        }
    }
}

