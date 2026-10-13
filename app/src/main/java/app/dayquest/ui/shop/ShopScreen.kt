package app.dayquest.ui.shop

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Catalog
import app.dayquest.game.Food
import app.dayquest.game.Forge
import app.dayquest.game.Gear
import app.dayquest.game.Rules
import app.dayquest.game.SceneDef
import app.dayquest.game.Slot
import app.dayquest.game.Stats
import app.dayquest.pixel.SceneRenderer
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PriceButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.Segmented
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.goldText
import app.dayquest.ui.look
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.slotLabel
import app.dayquest.ui.tasks.quickAdd
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Aisle { REWARDS, GEAR, FOOD, SCENES }

@Composable
fun ShopScreen(hero: Hero, onEdit: (Long) -> Unit, onNew: (String) -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val fb = LocalFeedback.current
    val tasks by remember { c.db.game().observeTasks() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rewards = tasks.filter { it.type == TaskType.REWARD }
    val bag by remember { c.db.game().observeItems() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var aisle by rememberSaveable { mutableStateOf(Aisle.REWARDS) }
    val season = app.dayquest.ui.rememberSeason()
    val poor = stringResource(R.string.not_enough_gold)

    fun spend(ok: Boolean, msg: String) {
        if (ok) fb.buy() else fb.show(poor)
        if (ok && msg.isNotEmpty()) fb.show(msg)
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_shop), style = DType.Title, modifier = Modifier.weight(1f))
                PixelImage(Sprites.coin, pixel = 3.dp)
                Spacer(Modifier.width(8.dp))
                Text(goldText(hero.gold), style = DType.Number.copy(color = D.c.gold, fontSize = 22.sp))
            }
            Segmented(
                Aisle.entries.toList(), aisle,
                {
                    stringResource(
                        when (it) {
                            Aisle.REWARDS -> R.string.shop_rewards
                            Aisle.GEAR -> R.string.shop_gear
                            Aisle.FOOD -> R.string.shop_food
                            Aisle.SCENES -> R.string.shop_scenes
                        }
                    )
                },
                { aisle = it },
            )
            Gap(8)
            app.dayquest.ui.components.Tip("shop", stringResource(R.string.tip_shop), Modifier.padding(vertical = 4.dp))
        }
        when (aisle) {
            Aisle.REWARDS -> {
                item {
                    val today = Rules.logicalDay(java.time.LocalDateTime.now(), app.dayquest.ui.rememberDayStart())
                    val cost = Rules.HEALTH_POTION_COST * app.dayquest.game.DayEvent.of(today).potion
                    val bought = stringResource(R.string.snack_healed, goldText(Rules.HEALTH_POTION_HP))
                    ShopRow(
                        icon = { PixelImage(Sprites.healthPotion, pixel = 4.dp) },
                        title = stringResource(R.string.health_potion),
                        sub = stringResource(R.string.health_potion_text, goldText(Rules.HEALTH_POTION_HP)),
                    ) {
                        PriceButton(cost, hero.gold >= cost && hero.hp < Rules.MAX_HP, {
                            c.scope.launch {
                                val o = c.game.buyHealthPotion()
                                withContext(Dispatchers.Main) { if (o != null) spend(true, bought) else if (hero.hp < Rules.MAX_HP) spend(false, "") }
                            }
                        })
                    }
                }
                items(rewards, key = { it.id }) { r -> RewardRow(r, hero, onEdit = { onEdit(r.id) }) }
                if (rewards.isEmpty()) item { Text(stringResource(R.string.empty_rewards), style = DType.Sub, modifier = Modifier.padding(8.dp)) }
                item {
                    var text by rememberSaveable { mutableStateOf("") }
                    Gap(8)
                    DTextField(
                        text, { text = it }, placeholder = stringResource(R.string.add_reward_hint),
                        onDone = { if (text.isNotBlank()) { val t = text; c.launch { quickAdd(c, TaskType.REWARD, t, 0) }; text = "" } },
                        trailing = {
                            IconButton(onClick = {
                                if (text.isNotBlank()) { val t = text; c.launch { quickAdd(c, TaskType.REWARD, t, 0) }; text = "" } else onNew("")
                            }) { Icon(Icons.Rounded.Add, null, tint = D.c.accent) }
                        },
                    )
                }
            }
            Aisle.GEAR -> {
                item(key = "seasonal") {
                    SectionTitle(stringResource(R.string.seasonal, stringResource(season.label)))
                    Text(stringResource(R.string.seasonal_text), style = DType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                items(Catalog.fixedGear.filter { it.season == season }, key = { it.key }) { g -> GearRow(g, hero) }
                Slot.entries.forEach { slot ->
                    item(key = "slot-$slot") { SectionTitle(slotLabel(slot)) }
                    items(Catalog.fixedGear.filter { it.slot == slot && it.season == null }, key = { it.key }) { g -> GearRow(g, hero) }
                }
                item(key = "forge") {
                    SectionTitle(stringResource(R.string.forge))
                    Text(stringResource(R.string.forge_text), style = DType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                // Newest first; the next locked tier is shown as something to aim for.
                (Forge.tierFor(hero.level) + 1 downTo 1).forEach { t ->
                    item(key = "tier-$t") {
                        Text(
                            stringResource(R.string.tier, t) + " · " + stringResource(R.string.from_level, Forge.tierLevel(t)),
                            style = DType.Pixel.copy(color = D.c.muted), modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(Slot.entries.map { Forge.item(t, it) }, key = { it.key }) { g -> GearRow(g, hero) }
                }
            }
            Aisle.FOOD -> {
                item(key = "food-text") { Text(stringResource(R.string.shop_food_text), style = DType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) }
                items(Food.entries.toList(), key = { it.key }) { f -> FoodRow(f, hero, bag.firstOrNull { it.key == "food:${f.key}" }?.count ?: 0) }
                item(key = "saddle") { SaddleRow(hero, bag.firstOrNull { it.key == Rules.SADDLE }?.count ?: 0) }
            }
            Aisle.SCENES -> items(Catalog.scenes.chunked(2)) { pair ->
                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { sc -> SceneCard(sc, hero, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShopRow(icon: @Composable () -> Unit, title: String, sub: String?, onClick: (() -> Unit)? = null, action: @Composable () -> Unit) {
    Row(
        Modifier
            .padding(vertical = 5.dp)
            .fillMaxWidth()
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.line, D.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DType.Strong.copy(fontSize = 15.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, style = DType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        action()
    }
}

@Composable
private fun RewardRow(r: Task, hero: Hero, onEdit: () -> Unit) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    val enjoy = stringResource(R.string.snack_bought, r.title)
    val undo = stringResource(R.string.undo)
    ShopRow({ PixelImage(Sprites.chest, pixel = 2.dp) }, r.title, r.notes, onEdit) {
        PriceButton(r.cost, hero.gold >= r.cost, {
            c.scope.launch {
                val o = c.game.buyReward(r.id)
                withContext(Dispatchers.Main) {
                    if (o == null) fb.show(poor) else { fb.buy(); fb.show(enjoy, undo, o.logId) }
                }
            }
        })
    }
}

@Composable
private fun FoodRow(f: Food, hero: Hero, count: Int) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    val ctx = LocalContext.current
    val fans = app.dayquest.game.Species.entries.filter { it.favorite == f }.joinToString(", ") { ctx.getString(it.label) }
    val cost = Rules.foodPrice(f)
    ShopRow(
        icon = { PixelImage(Sprites.food(f), pixel = 4.dp) },
        title = stringResource(f.label) + if (count > 0) "  ×$count" else "",
        sub = stringResource(R.string.food_fans, fans),
    ) {
        PriceButton(cost, hero.gold >= cost, {
            c.scope.launch {
                val ok = c.game.buyFood(f)
                withContext(Dispatchers.Main) { if (ok) fb.buy() else fb.show(poor) }
            }
        })
    }
}

@Composable
private fun SaddleRow(hero: Hero, count: Int) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    ShopRow(
        icon = { PixelImage(app.dayquest.pixel.Badges.saddle, pixel = 4.dp) },
        title = stringResource(R.string.saddle) + if (count > 0) "  ×$count" else "",
        sub = stringResource(R.string.saddle_text),
    ) {
        PriceButton(Rules.SADDLE_COST, hero.gold >= Rules.SADDLE_COST, {
            c.scope.launch {
                val ok = c.game.buySaddle()
                withContext(Dispatchers.Main) { if (ok) fb.buy() else fb.show(poor) }
            }
        })
    }
}

@Composable
fun StatChips(s: Stats) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.stat_str to s.str, R.string.stat_int to s.int, R.string.stat_con to s.con, R.string.stat_per to s.per)
            .filter { it.second > 0 }
            .forEach { (label, v) ->
                Box(Modifier.clip(D.RXs).background(D.c.accentSoft).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${stringResource(label)} +$v", style = DType.Number.copy(fontSize = 12.sp, color = D.c.accent))
                }
            }
    }
}

@Composable
private fun GearRow(g: Gear, hero: Hero) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    val owned = hero.owns(g.key)
    val equipped = listOf(hero.weapon, hero.armor, hero.head, hero.shield).contains(g.key)
    val locked = hero.level < g.level
    // Try-on: the hero as they would look wearing this.
    val tryOn = when (g.slot) {
        Slot.WEAPON -> hero.look().copy(weapon = g.key)
        Slot.ARMOR -> hero.look().copy(armor = g.key)
        Slot.HEAD -> hero.look().copy(head = g.key)
        Slot.SHIELD -> hero.look().copy(shield = g.key)
    }
    Row(
        Modifier
            .padding(vertical = 5.dp)
            .fillMaxWidth()
            .clip(D.RMd)
            .background(if (equipped) D.c.accentSoft else D.c.surface)
            .border(1.dp, if (equipped) D.c.accent.copy(alpha = 0.5f) else D.c.line, D.RMd)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) {
            PixelImage(Sprites.hero(tryOn), pixel = 2.5.dp, alpha = if (locked) 0.4f else 1f)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (g.rarity != app.dayquest.game.Rarity.COMMON) {
                    Box(Modifier.size(8.dp).clip(D.RXs).background(Color(g.rarity.color)))
                    Spacer(Modifier.width(6.dp))
                }
                Text(app.dayquest.ui.gearName(g), style = DType.Strong.copy(fontSize = 15.5.sp, color = if (locked) D.c.faint else D.c.ink))
            }
            if (g.rarity != app.dayquest.game.Rarity.COMMON) Text(stringResource(g.rarity.label), style = DType.Small.copy(color = Color(g.rarity.color)))
            Gap(4)
            StatChips(g.stats)
        }
        Spacer(Modifier.width(8.dp))
        when {
            equipped -> Text(stringResource(R.string.equipped), style = DType.Pixel.copy(color = D.c.accent))
            owned -> SoftButton(stringResource(R.string.equip), { fb.tap(); c.launch { c.game.equip(g) } })
            locked -> Text(stringResource(R.string.from_level, g.level), style = DType.Small)
            else -> PriceButton(g.price, hero.gold >= g.price, {
                c.scope.launch {
                    val ok = c.game.buyGear(g)
                    withContext(Dispatchers.Main) { if (ok) fb.buy() else fb.show(poor) }
                }
            })
        }
    }
}

/** One still frame of a scene, rendered once. */
@Composable
fun SceneThumb(def: SceneDef, hero: Hero, modifier: Modifier = Modifier) {
    val bmp = remember(def.key, hero.look()) {
        val r = SceneRenderer(96, 56)
        r.render(SceneState(def.kind, 11f, hero.look(), hero.pet), 2000L)
        Bitmap.createBitmap(r.buffer.px, 96, 56, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Canvas(modifier.aspectRatio(96f / 56f)) {
        val scale = (size.width / 96f)
        drawImage(bmp, IntOffset.Zero, IntSize(96, 56), dstSize = IntSize((96 * scale).toInt(), (56 * scale).toInt()), filterQuality = FilterQuality.None)
    }
}

@Composable
private fun SceneCard(sc: SceneDef, hero: Hero, modifier: Modifier) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    val owned = hero.ownsScene(sc.key)
    val current = hero.scene == sc.key
    Column(
        modifier
            .clip(D.RMd)
            .background(D.c.surface)
            .border(if (current) 2.dp else 1.dp, if (current) D.c.accent else D.c.line, D.RMd)
            .clickable {
                c.scope.launch {
                    val ok = c.game.buyScene(sc)
                    withContext(Dispatchers.Main) { if (ok) fb.buy() else fb.show(poor) }
                }
            },
    ) {
        SceneThumb(sc, hero, Modifier.fillMaxWidth())
        Column(Modifier.padding(10.dp)) {
            Text(stringResource(sc.name), style = DType.Strong.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Gap(4)
            when {
                current -> Text(stringResource(R.string.scene_current), style = DType.Pixel.copy(color = D.c.accent, fontSize = 16.sp))
                owned -> Text(stringResource(R.string.scene_use), style = DType.Pixel.copy(color = D.c.muted, fontSize = 16.sp))
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelImage(Sprites.coin, pixel = 1.5.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(goldText(sc.price), style = DType.Number.copy(color = if (hero.gold >= sc.price) D.c.gold else D.c.faint, fontSize = 13.sp))
                }
            }
        }
    }
}

