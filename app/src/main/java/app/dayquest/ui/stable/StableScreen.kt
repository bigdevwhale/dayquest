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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.Item
import app.dayquest.data.Pet
import app.dayquest.game.Bond
import app.dayquest.game.Catalog
import app.dayquest.game.Collections
import app.dayquest.game.Companions
import app.dayquest.game.Food
import app.dayquest.game.Form
import app.dayquest.game.Game
import app.dayquest.game.PetSet
import app.dayquest.game.PetWear
import app.dayquest.game.Potion
import app.dayquest.game.Rules
import app.dayquest.game.Species
import app.dayquest.game.Trait
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.goldText
import app.dayquest.ui.itemName
import app.dayquest.ui.itemSprite
import app.dayquest.ui.petName
import app.dayquest.ui.petLabel
import app.dayquest.ui.pets.petTitle
import app.dayquest.ui.pets.titleName
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
    var allSets by remember { mutableStateOf(false) }

    val eggs = items.filter { it.key.startsWith("egg:") }
    val potions = items.filter { it.key.startsWith("potion:") }
    val food = items.filter { it.key.startsWith("food:") }
    val saddles = items.firstOrNull { it.key == Rules.SADDLE }?.count ?: 0
    val mounts = pets.filter { it.mount }
    val owned = byKey.keys
    val claimed = Collections.claimed(hero.collections)
    // Ready to claim first, then the ones closest to done; the rest wait behind "show all".
    val sets = Collections.all.filter { it.key !in claimed }
        .sortedWith(compareByDescending<PetSet> { Collections.complete(it, owned) }.thenByDescending { Collections.count(it, owned).toFloat() / it.members.size })

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_stable), style = DType.Title, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.pets_count, pets.size, Catalog.totalPets), style = DType.Number.copy(color = D.c.muted))
            }
        }
        item { app.dayquest.ui.components.Tip("stable", stringResource(R.string.tip_stable), Modifier.padding(vertical = 4.dp)) }
        item { app.dayquest.ui.components.Tip("friends", stringResource(R.string.tip_friends), Modifier.padding(vertical = 4.dp)) }
        item { HatchCard(eggs, potions, byKey) }
        item {
            SectionTitle(stringResource(R.string.mounts)) { Text("${mounts.size}", style = DType.Number.copy(color = D.c.muted)) }
            if (mounts.isEmpty()) Text(stringResource(R.string.mounts_empty), style = DType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mounts.forEach { m -> MountSlot(m, hero.mount == m.key) { open = m.key } }
            }
        }
        item {
            SectionTitle(stringResource(R.string.bag))
            if (items.isEmpty()) Text(stringResource(R.string.bag_empty), style = DType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (eggs + potions + food + items.filter { it.key == Rules.SADDLE }).forEach { BagSlot(it) }
            }
        }
        item {
            SectionTitle(stringResource(R.string.collections)) { Text("${claimed.size} / ${Collections.all.size}", style = DType.Number.copy(color = D.c.muted)) }
            Text(stringResource(R.string.collections_text), style = DType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
        items((if (allSets) sets else sets.take(3)), key = { "set-" + it.key }) { set -> SetRow(set, owned) }
        if (sets.size > 3) item {
            TextButton(onClick = { allSets = !allSets }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (allSets) R.string.show_less else R.string.show_all), style = DType.Button, color = D.c.accent)
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
                        if (pet != null) PixelImage(Sprites.pet(s, p, pet.growth >= 100, form = pet.form, wear = pet.wear), pixel = 1.5.dp)
                        else PixelImage(Sprites.petSilhouette(s, D.c.dark), pixel = 1.5.dp, alpha = 0.6f)
                    }
                }
            }
        }
    }

    open?.let { key -> byKey[key]?.let { PetDialog(it, hero, pets, food, saddles) { open = null } } }
}

/** One collection: how far along it is, and its reward once complete. */
@Composable
private fun SetRow(set: PetSet, owned: Set<String>) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val ctx = LocalContext.current
    val have = Collections.count(set, owned)
    val done = have >= set.members.size
    val title = titleName(set.key)
    val name = when {
        set.species != null -> stringResource(R.string.set_species, stringResource(set.species.label))
        set.potion != null -> stringResource(R.string.set_potion, stringResource(set.potion.label))
        else -> stringResource(R.string.set_all)
    }
    Row(
        Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(D.RMd).background(if (done) D.c.accentSoft else D.c.surface)
            .border(1.dp, if (done) D.c.accent else D.c.line, D.RMd).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) {
            val sample = Catalog.petParts(set.members.first { it in owned || it == set.members.last() })
            when {
                set.species != null -> PixelImage(Sprites.statue(set.species), pixel = 2.dp)
                sample != null -> PixelImage(Sprites.pet(sample.first, sample.second), pixel = 2.dp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = DType.Strong)
            Text(
                listOfNotNull(stringResource(R.string.set_reward_gold, goldText(set.gold)), stringResource(R.string.set_reward_statue).takeIf { set.decor.isNotEmpty() }, "«$title»")
                    .joinToString(" · "),
                style = DType.Muted, maxLines = 2,
            )
            Gap(6)
            PixelBar(have / set.members.size.toFloat(), if (done) D.c.gold else D.c.good)
        }
        Spacer(Modifier.width(10.dp))
        if (done) PrimaryButton(stringResource(R.string.claim), {
            c.scope.launch {
                val got = c.game.claimCollection(set.key)
                withContext(Dispatchers.Main) {
                    if (got != null) { fb.thud(); Sfx.play(Sound.JACKPOT); fb.show(ctx.getString(R.string.set_claimed, title)) }
                }
            }
        })
        else Text("$have/${set.members.size}", style = DType.Number.copy(color = D.c.muted, fontSize = 14.sp))
    }
}

@Composable
private fun MountSlot(pet: Pet, riding: Boolean, onClick: () -> Unit) {
    val (s, p) = Catalog.petParts(pet.key) ?: return
    Column(
        Modifier.width(96.dp).clip(D.RSm).background(if (riding) D.c.accentSoft else D.c.surface)
            .border(if (riding) 2.dp else 1.dp, if (riding) D.c.accent else D.c.line, D.RSm).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { PixelImage(Sprites.mount(s, p, form = pet.form), pixel = 2.dp) }
        Text(
            if (riding) stringResource(R.string.riding) else petTitle(pet),
            style = DType.Small.copy(fontSize = 10.sp, color = if (riding) D.c.accent else D.c.muted), textAlign = TextAlign.Center, maxLines = 2,
        )
    }
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
                withContext(Dispatchers.Main) { if (ok) { fb.thud(); Sfx.play(Sound.DROP); fb.show(hatched) } }
            }
        }, Modifier.fillMaxWidth())
    }
}

/** The companion's perk: what it gives now, and what it will give once grown. */
@Composable
private fun PerkLine(pet: Pet, s: Species) {
    val perk = Companions.perk(s)
    val now = Companions.amount(pet)
    val grown = Companions.amount(pet.copy(growth = 100))
    fun fmt(v: Double) = if (perk == app.dayquest.game.Perk.HEAL) goldText(v) else "${kotlin.math.round(v * 100).toInt()}%"
    Box(Modifier.clip(D.RXs).background(D.c.accentSoft).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(stringResource(perk.text, fmt(now)), style = DType.Small.copy(color = D.c.accent), textAlign = TextAlign.Center)
    }
    if (pet.growth < 100) {
        Gap(2)
        Text(stringResource(R.string.perk_grown, fmt(grown)), style = DType.Small, textAlign = TextAlign.Center)
    }
}

/** Ten hearts: friendship so far, the next one filling up. */
@Composable
fun Hearts(pet: Pet, pixel: Float = 1.6f) {
    val level = Bond.level(pet.bond)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(Bond.MAX) { i ->
            PixelImage(Sprites.heart, pixel = pixel.dp, alpha = if (i < level) 1f else 0.22f)
        }
    }
}

@Composable
private fun PetDialog(pet: Pet, hero: Hero, pets: List<Pet>, food: List<Item>, saddles: Int, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val ctx = LocalContext.current
    val (s, p) = Catalog.petParts(pet.key) ?: return
    val grown = pet.growth >= 100
    val trait = Trait.of(pet)
    val level = Bond.level(pet.bond)
    val today = Rules.logicalDay(java.time.LocalDateTime.now(), app.dayquest.ui.rememberDayStart())
    var naming by remember { mutableStateOf(false) }
    var road by remember { mutableStateOf<Form?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                PixelImage(Sprites.pet(s, p, grown, form = pet.form, wear = pet.wear), pixel = 7.dp)
                Gap(10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(petTitle(pet), style = DType.Title, textAlign = TextAlign.Center)
                    IconButton(onClick = { naming = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.pet_rename), tint = D.c.muted)
                    }
                }
                if (pet.name.isNotBlank()) Text(petName(pet.key), style = DType.Small, textAlign = TextAlign.Center)
                Form.of(pet.form)?.let { Text(stringResource(it.label), style = DType.Pixel.copy(color = D.c.gold), textAlign = TextAlign.Center) }
                Gap(8)
                Text(stringResource(R.string.trait_line, stringResource(trait.label)), style = DType.Strong, textAlign = TextAlign.Center)
                Text(stringResource(trait.text), style = DType.Muted, textAlign = TextAlign.Center)
                Gap(10)
                PerkLine(pet, s)

                // ---- Friendship
                Gap(14)
                Hearts(pet)
                Gap(4)
                if (level < Bond.MAX) {
                    PixelBar(Bond.progress(pet.bond), D.c.hp, Modifier.padding(horizontal = 24.dp))
                    Gap(2)
                }
                Text(
                    when {
                        level >= Bond.MAX -> stringResource(R.string.bond_best)
                        level >= Bond.GIFT -> stringResource(R.string.bond_next_best)
                        level >= Bond.TRICK -> stringResource(R.string.bond_next_gift)
                        else -> stringResource(R.string.bond_next_trick)
                    },
                    style = DType.Small, textAlign = TextAlign.Center,
                )
                Gap(8)
                if (pet.pattedDay != today) SoftButton(stringResource(R.string.pat), {
                    fb.tap(); Sfx.play(Sound.PAT)
                    c.scope.launch {
                        val n = c.game.pat(pet.key)
                        withContext(Dispatchers.Main) { if (n > 0) fb.show(ctx.getString(R.string.patted, n)) }
                    }
                }, Modifier.fillMaxWidth())
                else Text(stringResource(R.string.patted_today), style = DType.Muted, textAlign = TextAlign.Center)

                // ---- Food: growth while young, friendship always
                Gap(14)
                if (!grown) {
                    PixelBar(pet.growth / 100f, D.c.good)
                    Gap(4)
                    Text(stringResource(R.string.growth, pet.growth), style = DType.Small)
                } else Text(stringResource(R.string.fully_grown), style = DType.Pixel.copy(color = D.c.gold))
                Gap(4)
                Text(stringResource(R.string.loves, stringResource(s.favorite.label)), style = DType.Muted)
                Gap(8)
                if (food.isEmpty()) Text(stringResource(R.string.no_food), style = DType.Muted, textAlign = TextAlign.Center)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    food.forEach { f ->
                        val kind = Food.of(f.key.substringAfter(':')) ?: return@forEach
                        val fav = kind == s.favorite
                        Column(
                            Modifier.clip(D.RSm).background(if (fav) D.c.xpSoft else D.c.surface).border(1.dp, D.c.line, D.RSm)
                                .clickable {
                                    c.scope.launch {
                                        val fed = c.game.feed(pet.key, kind)
                                        withContext(Dispatchers.Main) {
                                            if (fed != null) {
                                                fb.tap(); Sfx.play(Sound.DONE)
                                                fb.show(if (fed.growth > 0) ctx.getString(R.string.grew_bond, fed.growth, fed.bond) else ctx.getString(R.string.treat_bond, fed.bond))
                                            }
                                        }
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

                // ---- Clothes
                Gap(14)
                Wardrobe(pet, hero, pets)

                // ---- Evolution
                if (grown) {
                    Gap(14)
                    if (pet.form.isEmpty()) {
                        Text(stringResource(R.string.evolve_title), style = DType.Strong)
                        Text(stringResource(R.string.evolve_text), style = DType.Muted, textAlign = TextAlign.Center)
                        Gap(8)
                        Form.entries.forEach { f ->
                            val n = f.progress(pet).coerceAtMost(f.need)
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(D.RSm).background(D.c.surface).border(1.dp, D.c.line, D.RSm).padding(10.dp)) {
                                Text(stringResource(f.label), style = DType.Strong)
                                Text(stringResource(f.text), style = DType.Muted)
                                Gap(6)
                                PixelBar(n / f.need.toFloat(), D.c.xp)
                                Gap(4)
                                if (f.ready(pet)) PrimaryButton(stringResource(R.string.evolve), { road = f }, Modifier.fillMaxWidth())
                                else Text("$n / ${f.need}", style = DType.Small)
                            }
                        }
                    }

                    // ---- Saddle and ride
                    Gap(12)
                    when {
                        pet.mount -> {
                            val perk = Companions.perk(s)
                            val full = Companions.mountBoost(pet).let { b ->
                                if (perk == app.dayquest.game.Perk.HEAL) goldText(b.heal)
                                else "${kotlin.math.round(listOf(b.gold, b.xp, b.drop, b.guard, b.boss, b.crit).max() * 100).toInt()}%"
                            }
                            Text(stringResource(R.string.mount_perk, stringResource(perk.text, full)), style = DType.Small, textAlign = TextAlign.Center)
                            Gap(8)
                            if (hero.mount == pet.key) SoftButton(stringResource(R.string.dismount), { c.launch { c.game.ride("") }; onDismiss() }, Modifier.fillMaxWidth())
                            else PrimaryButton(stringResource(R.string.ride), { fb.thud(); c.launch { c.game.ride(pet.key) }; onDismiss() }, Modifier.fillMaxWidth())
                        }
                        saddles > 0 -> PrimaryButton(stringResource(R.string.saddle_up) + "  ×$saddles", {
                            c.scope.launch {
                                val ok = c.game.saddle(pet.key)
                                withContext(Dispatchers.Main) {
                                    if (ok) { fb.thud(); Sfx.play(Sound.LEVEL); fb.show(ctx.getString(R.string.saddled, ctx.petLabel(pet.key))); onDismiss() }
                                }
                            }
                        }, Modifier.fillMaxWidth())
                        else -> Text(stringResource(R.string.saddle_need), style = DType.Muted, textAlign = TextAlign.Center)
                    }
                }
                Gap(16)
                if (hero.pet == pet.key) Text(stringResource(R.string.companion), style = DType.Pixel.copy(color = D.c.accent))
                else if (hero.mount == pet.key) Text(stringResource(R.string.riding), style = DType.Pixel.copy(color = D.c.accent))
                else SoftButton(stringResource(R.string.set_companion), { c.launch { c.game.setCompanion(pet.key) }; onDismiss() }, Modifier.fillMaxWidth())
            }
        },
    )
    if (naming) RenameDialog(pet) { naming = false }
    road?.let { f ->
        ConfirmDialog(
            stringResource(R.string.evolve_confirm_title, stringResource(f.label)), stringResource(R.string.evolve_confirm_text), stringResource(R.string.evolve),
            onConfirm = {
                road = null
                c.scope.launch {
                    val ok = c.game.evolve(pet.key, f)
                    withContext(Dispatchers.Main) { if (ok) { fb.thud(); Sfx.play(Sound.SPELL); fb.show(ctx.getString(R.string.evolved, ctx.getString(f.label))) } }
                }
            },
            onDismiss = { road = null },
        )
    }
}

/** The clothes this hero owns: tap to put one on, tap again to take it off. */
@Composable
private fun Wardrobe(pet: Pet, hero: Hero, pets: List<Pet>) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val ribbon = pets.any { Bond.level(it.bond) >= Bond.GIFT }
    val owned = PetWear.entries.filter { if (it.gift) ribbon else it.key in hero.ownedWear.split(',') }
    val worn = PetWear.parse(pet.wear)
    Text(stringResource(R.string.wardrobe), style = DType.Strong)
    Gap(6)
    if (owned.isEmpty()) {
        Text(stringResource(R.string.wardrobe_empty), style = DType.Muted, textAlign = TextAlign.Center)
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        owned.forEach { w ->
            val on = w in worn
            Column(
                Modifier.width(64.dp).clip(D.RSm).background(if (on) D.c.accentSoft else D.c.surface)
                    .border(if (on) 2.dp else 1.dp, if (on) D.c.accent else D.c.line, D.RSm)
                    .clickable { fb.tap(); c.launch { c.game.wear(pet.key, w) } }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelImage(wearIcon(w), pixel = 2.dp)
                Text(stringResource(w.label), style = DType.Small.copy(fontSize = 10.sp), textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** A piece of clothing shown on a plain fox, so it reads at a glance. */
fun wearIcon(w: PetWear) = Sprites.pet(Species.FOX, Potion.BASE, wear = w.key)

@Composable
private fun RenameDialog(pet: Pet, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    var text by remember { mutableStateOf(pet.name) }
    fun save() {
        c.launch { c.game.rename(pet.key, text) }
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        title = { Text(stringResource(R.string.pet_rename), style = DType.Heading) },
        text = {
            Column {
                Text(stringResource(R.string.pet_rename_text), style = DType.Muted)
                Gap(10)
                DTextField(text, { text = it.take(Game.NAME_MAX) }, placeholder = petName(pet.key), onDone = { save() })
            }
        },
        confirmButton = { TextButton(onClick = { save() }) { Text(stringResource(R.string.save), style = DType.Button, color = D.c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = DType.Button, color = D.c.muted) } },
    )
}
