package app.dayquest.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.game.Dungeon
import app.dayquest.pixel.Badges
import app.dayquest.pixel.Sprites
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType

/** The weekend dungeon: five rooms in a row, the foe of the current one, and how many steps are left. */
@Composable
fun DungeonCard(hero: Hero) {
    val c = LocalContext.current.container
    var today by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    if (today == 0L) return
    val open = Dungeon.isOpen(today)
    val run = Dungeon.current(hero.dungeon, today)
    val foes = Dungeon.foes(run.year, run.week)
    SectionTitle(stringResource(R.string.dungeon)) {
        if (!open) Text(pluralStringResource(R.plurals.dungeon_in, Dungeon.daysUntilOpen(today), Dungeon.daysUntilOpen(today)), style = DType.Small)
    }
    DCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            foes.forEachIndexed { i, f ->
                val cleared = i < run.room
                val current = open && i == run.room
                Box(
                    Modifier.weight(1f).aspectRatio(0.85f).clip(D.RSm)
                        .background(if (current) D.c.accentSoft else D.c.surface2)
                        .border(if (current) 2.dp else 0.dp, if (current) D.c.accent else D.c.surface2, D.RSm),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        cleared -> PixelImage(Sprites.chestOpen, pixel = 1.5.dp, alpha = 0.6f)
                        current || (open && i == Dungeon.last) -> PixelImage(Badges.foe(f), pixel = if (i == Dungeon.last) 0.9.dp else 0.7.dp)
                        else -> PixelImage(Badges.door, pixel = 2.dp, alpha = if (open) 1f else 0.5f)
                    }
                }
            }
        }
        Gap(10)
        when {
            run.finished -> Text(stringResource(R.string.dungeon_done), style = DType.Sub.copy(color = D.c.good))
            open -> {
                val need = Dungeon.ROOMS[run.room]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dungeon_room, run.room + 1, Dungeon.ROOMS.size), style = DType.Strong, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.dungeon_steps, run.steps, need), style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted))
                }
                Gap(6)
                PixelBar(run.steps / need.toFloat(), D.c.accent)
                Gap(8)
                Text(stringResource(R.string.dungeon_text), style = DType.Muted)
            }
            else -> Text(stringResource(R.string.dungeon_text), style = DType.Muted)
        }
    }
}
