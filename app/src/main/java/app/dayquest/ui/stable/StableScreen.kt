package app.dayquest.ui.stable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.Item
import app.dayquest.data.Pet
import app.dayquest.game.Catalog
import app.dayquest.game.Food
import app.dayquest.game.Potion
import app.dayquest.game.Species
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.itemName
import app.dayquest.ui.itemSprite
import app.dayquest.ui.petName
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StableScreen(hero: Hero) {
    val c = LocalContext.current.container
    val items by remember { c.db.game().observeItems() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val pets by remember { c.db.game().observePets() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val byKey = pets.associateBy { it.key }
    var open by remember { mutableStateOf<String?>(null) }

    val eggs = items.filter { it.key.startsWith("egg:") }
    val potions = items.filter { it.key.startsWith("potion:") }
    val food = items.filter { it.key.startsWith("food:") }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_stable), style = DType.Title, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.pets_count, pets.size, Catalog.totalPets), style = DType.Number.copy(color = D.c.muted))
            }
        }
        item { app.dayquest.ui.components.Tip("stable", stringResource(R.string.tip_stable), Modifier.padding(vertical = 4.dp)) }
        item { HatchCard(eggs, potions, byKey) }
        item {
            SectionTitle(stringResource(R.string.bag))
            if (items.isEmpty()) Text(stringResource(R.string.bag_empty), style = DType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (eggs + potions + food).forEach { BagSlot(it) }
            }
        }
        items(Species.entries.toList(), key = { it.key }) { s ->
            if (s == Species.entries.first()) SectionTitle(stringResource(R.string.collection))
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Potion.entries.forEach { p ->
                    val key = Catalog.petKey(s, p)
                    val pet = byKey[key]
                    val companion = hero.pet == key
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(D.RSm)
                            .background(if (companion) D.c.accentSoft else D.c.surface)
                            .border(1.dp, if (companion) D.c.accent else D.c.line, D.RSm)
                            .clickable(enabled = pet != null) { open = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (pet != null) PixelImage(Sprites.pet(s, p, pet.growth >= 100), pixel = 1.5.dp)
                        else PixelImage(Sprites.petSilhouette(s, D.c.dark), pixel = 1.5.dp, alpha = 0.6f)
                    }
                }
            }
        }
    }

    open?.let { key -> byKey[key]?.let { PetDialog(it, hero, food) { open = null } } }
}

@Composable
private fun BagSlot(it: Item) {
    Column(
        Modifier.width(76.dp).clip(D.RSm).background(D.c.surface).border(1.dp, D.c.line, D.RSm).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { itemSprite(it.key)?.let { s -> PixelImage(s, pixel = 3.dp) } }
        Text("×${it.count}", style = DType.Number.copy(fontSize = 13.sp))
        Text(itemName(it.key), style = DType.Small.copy(fontSize = 10.sp), textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Picker(options: List<String>, selected: String?, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { key ->
            val on = key == selected
            Box(
                Modifier.size(52.dp).clip(D.RSm).background(if (on) D.c.accentSoft else D.c.surface2)
                    .border(if (on) 2.dp else 1.dp, if (on) D.c.accent else D.c.line, D.RSm).clickable { onPick(key) },
                contentAlignment = Alignment.Center,
            ) { itemSprite(key)?.let { PixelImage(it, pixel = 3.dp) } }
        }
    }
}

@Composable
private fun HatchCard(eggs: List<Item>, potions: List<Item>, pets: Map<String, Pet>) {
    if (eggs.isEmpty() || potions.isEmpty()) return
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    var egg by remember { mutableStateOf<String?>(null) }
    var potion by remember { mutableStateOf<String?>(null) }
    // Start on a pair that would hatch something new, if there is one.
    val fresh = remember(eggs, potions, pets.keys) {
        eggs.flatMap { e -> potions.map { p -> e.key to p.key } }.firstOrNull { (e, p) -> "${e.substringAfter(':')}:${p.substringAfter(':')}" !in pets }
    }
    val e = egg?.takeIf { k -> eggs.any { it.key == k } } ?: fresh?.first ?: eggs.first().key
    val p = potion?.takeIf { k -> potions.any { it.key == k } } ?: fresh?.second ?: potions.first().key
    val species = Species.of(e.substringAfter(':'))!!
    val pot = Potion.of(p.substringAfter(':'))!!
    val key = Catalog.petKey(species, pot)
    val have = pets.containsKey(key)
    val hatched = stringResource(R.string.hatched, petName(key))
    Gap(8)
    DCard {
        Text(stringResource(R.string.hatch_title), style = DType.Heading)
        Gap(12)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.hatch_pick_egg), style = DType.Small)
                Gap(4)
                Picker(eggs.map { it.key }, e) { egg = it }
                Gap(10)
                Text(stringResource(R.string.hatch_pick_potion), style = DType.Small)
                Gap(4)
                Picker(potions.map { it.key }, p) { potion = it }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                Box(Modifier.size(84.dp).clip(D.RMd).background(D.c.surface2), contentAlignment = Alignment.Center) {
                    PixelImage(Sprites.pet(species, pot), pixel = 4.dp, alpha = if (have) 0.35f else 1f)
                }
                Gap(4)
                Text(petName(key), style = DType.Small, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        Gap(12)
        if (have) Text(stringResource(R.string.already_have), style = DType.Muted)
        else PrimaryButton(stringResource(R.string.hatch), {
            c.scope.launch {
                val ok = c.game.hatch(species, pot)
                withContext(Dispatchers.Main) { if (ok) { fb.thud(); app.dayquest.ui.Sfx.play(app.dayquest.ui.Sound.DROP); fb.show(hatched) } }
            }
        }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PetDialog(pet: Pet, hero: Hero, food: List<Item>, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val (s, p) = Catalog.petParts(pet.key) ?: return
    val grown = pet.growth >= 100
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelImage(Sprites.pet(s, p, grown), pixel = 7.dp)
                Gap(10)
                Text(petName(pet.key), style = DType.Title, textAlign = TextAlign.Center)
                Gap(10)
                if (grown) Text(stringResource(R.string.fully_grown), style = DType.Pixel.copy(color = D.c.gold))
                else {
                    PixelBar(pet.growth / 100f, D.c.good)
                    Gap(4)
                    Text(stringResource(R.string.growth, pet.growth), style = DType.Small)
                    Gap(4)
                    Text(stringResource(R.string.loves, stringResource(s.favorite.label)), style = DType.Muted)
                    Gap(12)
                    if (food.isEmpty()) Text(stringResource(R.string.no_food), style = DType.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        food.forEach { f ->
                            val kind = Food.of(f.key.substringAfter(':')) ?: return@forEach
                            val fav = kind == s.favorite
                            val ctx = LocalContext.current
                            Column(
                                Modifier.clip(D.RSm).background(if (fav) D.c.xpSoft else D.c.surface).border(1.dp, D.c.line, D.RSm)
                                    .clickable {
                                        c.scope.launch {
                                            val n = c.game.feed(pet.key, kind)
                                            withContext(Dispatchers.Main) { if (n > 0) { fb.tap(); app.dayquest.ui.Sfx.play(app.dayquest.ui.Sound.DONE); fb.show(ctx.getString(R.string.grew, n)) } }
                                        }
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PixelImage(Sprites.food(kind), pixel = 3.dp)
                                Text("×${f.count}", style = DType.Number.copy(fontSize = 12.sp))
                            }
                        }
                    }
                }
                Gap(16)
                if (hero.pet == pet.key) Text(stringResource(R.string.companion), style = DType.Pixel.copy(color = D.c.accent))
                else SoftButton(stringResource(R.string.set_companion), { c.launch { c.game.setCompanion(pet.key) }; onDismiss() }, Modifier.fillMaxWidth())
            }
        },
    )
}
