package app.dayquest.ui.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.game.Classes
import app.dayquest.game.HeroClass
import app.dayquest.game.Skill
import app.dayquest.game.SkillBlock
import app.dayquest.game.SkillKind
import app.dayquest.pixel.Badges
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.goldText
import app.dayquest.ui.num
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.skillText
import app.dayquest.ui.tasks.ManaBlue
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The hero screen's class block: locked until level 5, then a pick, then mana and skills. */
@Composable
fun ClassSection(hero: Hero) {
    val cls = Classes.of(hero)
    var picking by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.class_title))
    when {
        hero.level < Classes.UNLOCK && cls == null -> DCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroClass.entries.forEach { PixelImage(Badges.emblem(it), pixel = 2.dp, alpha = 0.4f); Spacer(Modifier.width(4.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.class_locked, Classes.UNLOCK), style = DType.Muted, modifier = Modifier.weight(1f))
            }
        }
        cls == null -> DCard {
            Text(stringResource(R.string.class_pick_text), style = DType.Sub)
            Gap(12)
            PrimaryButton(stringResource(R.string.class_choose), { picking = true }, Modifier.fillMaxWidth())
        }
        else -> DCard(padding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) { PixelImage(Badges.emblem(cls), pixel = 3.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(cls.label), style = DType.Heading)
                    ManaLine(hero)
                }
            }
            Gap(8)
            SkillList(hero)
            Gap(4)
            Text(
                stringResource(R.string.class_change), style = DType.Small.copy(color = D.c.accent),
                modifier = Modifier.clip(D.RXs).clickable { picking = true }.padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }
    }
    if (picking) ClassPicker(hero) { picking = false }
}

@Composable
private fun ManaLine(hero: Hero) {
    val max = Classes.maxMp(LocalContext.current.container.game.stats(hero))
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelImage(Badges.mana, pixel = 1.5.dp)
        Spacer(Modifier.width(6.dp))
        PixelBar((hero.mp / max).toFloat(), ManaBlue, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("${num(hero.mp.coerceAtMost(max))} / ${num(max)}", style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted))
    }
}

/** The class's skills, each with what it does and a cast button (or why it can't be cast). */
@Composable
fun SkillList(hero: Hero) {
    val context = LocalContext.current
    val c = context.container
    val fb = LocalFeedback.current
    val cls = Classes.of(hero) ?: return
    var today by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    val stats = c.game.stats(hero)
    cls.skills.forEachIndexed { i, skill ->
        val block = c.game.blocked(hero, skill, today)
        if (i > 0) HorizontalDivider(color = D.c.line)
        val locked = block == SkillBlock.LEVEL
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(skill.label), style = DType.Strong.copy(color = if (locked) D.c.faint else D.c.ink))
                Gap(2)
                Text(skillText(skill, stats), style = DType.Muted)
                // One quiet line under the text: how long it lasts, or why it can't be cast now.
                val note = when (block) {
                    null -> null
                    SkillBlock.ACTIVE -> stringResource(R.string.skill_active)
                    SkillBlock.LEVEL -> stringResource(R.string.skill_unlock, skill.level)
                    SkillBlock.NO_BOSS -> stringResource(R.string.skill_no_boss)
                    SkillBlock.FULL_HP -> stringResource(R.string.skill_full_hp)
                    else -> stringResource(R.string.skill_mana)
                }
                if (note != null) {
                    Gap(2)
                    Text(note, style = DType.Small.copy(fontSize = 12.sp, color = if (block == SkillBlock.ACTIVE) D.c.good else D.c.faint))
                }
            }
            Spacer(Modifier.width(12.dp))
            CastButton(skill.mana, enabled = block == null) {
                c.scope.launch {
                    val o = c.game.cast(skill)
                    withContext(Dispatchers.Main) {
                        if (o != null) { fb.thud(); fb.show(context.getString(R.string.skill_cast_done, context.getString(skill.label))) }
                    }
                }
            }
        }
    }
}

/** Casts a skill: a mana drop and its cost, shaped like the shop's price buttons. */
@Composable
private fun CastButton(mana: Double, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .clip(D.RSm)
            .background(if (enabled) ManaBlue.copy(alpha = 0.16f) else D.c.line.copy(alpha = 0.6f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(Badges.mana, pixel = 2.dp, alpha = if (enabled) 1f else 0.45f)
        Spacer(Modifier.width(6.dp))
        Text(num(mana), style = DType.Number.copy(fontSize = 15.sp, color = if (enabled) ManaBlue else D.c.faint))
    }
}

@Composable
private fun ClassPicker(hero: Hero, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val fb = LocalFeedback.current
    val first = hero.heroClass.isEmpty()
    val canPay = first || hero.gold + 1e-9 >= Classes.CHANGE_COST
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(stringResource(R.string.class_pick_title), style = DType.Heading) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (first) stringResource(R.string.class_first_free) else stringResource(R.string.class_change_cost, goldText(Classes.CHANGE_COST)),
                    style = DType.Muted,
                )
                Gap(10)
                HeroClass.entries.forEach { cls ->
                    val current = hero.heroClass == cls.name
                    Row(
                        Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(D.RMd)
                            .background(if (current) D.c.accentSoft else D.c.surface).border(1.dp, if (current) D.c.accent else D.c.line, D.RMd)
                            .clickable(enabled = !current && canPay) {
                                c.scope.launch {
                                    val ok = c.game.chooseClass(cls)
                                    withContext(Dispatchers.Main) {
                                        if (ok) { fb.thud(); Sfx.play(Sound.SPELL); fb.show(context.getString(R.string.class_chosen, context.getString(cls.label))); onDismiss() }
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelImage(Badges.emblem(cls), pixel = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(cls.label), style = DType.Strong)
                            Text(stringResource(cls.text), style = DType.Muted)
                            Gap(2)
                            Text(cls.skills.joinToString(" · ") { s: Skill -> context.getString(s.label) }, style = DType.Small.copy(fontSize = 11.sp, color = ManaBlue))
                        }
                    }
                }
            }
        },
    )
}
