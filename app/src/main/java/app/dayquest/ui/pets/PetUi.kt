package app.dayquest.ui.pets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.Pet
import app.dayquest.game.Bond
import app.dayquest.game.Boss
import app.dayquest.game.Catalog
import app.dayquest.game.Collections
import app.dayquest.game.Species
import app.dayquest.pixel.PetLook
import app.dayquest.pixel.RoomState
import app.dayquest.pixel.SceneState
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.look
import app.dayquest.ui.petLabel
import app.dayquest.ui.petName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** All hatched pets, live. */
@Composable
fun rememberPets(): List<Pet> {
    val c = LocalContext.current.container
    val pets by remember { c.db.game().observePets() }.collectAsStateWithLifecycle(initialValue = emptyList())
    return pets
}

fun Pet.look() = PetLook(key, growth >= 100, form, wear)

/** The hero's scene as it is right now: their place, the companion as it looks today, the mount, the boss. */
@Composable
fun heroScene(hero: Hero, pets: List<Pet>, boss: Boss? = null): SceneState {
    val pet = pets.firstOrNull { it.key == hero.pet }
    return SceneState(
        Catalog.scene(hero.scene).kind, 12f, hero.look(), hero.pet, (pet?.growth ?: 0) >= 100, boss, hero.resting,
        app.dayquest.ui.rememberSeason(), mount = hero.mount,
        petForm = pet?.form.orEmpty(), petWear = pet?.wear.orEmpty(), petTrick = pet != null && Bond.level(pet.bond) >= Bond.TRICK,
        mountForm = pets.firstOrNull { it.key == hero.mount }?.form.orEmpty(),
    )
}

/** Up to three pets live at home besides the companion and the mount: the closest friends first. */
fun residents(hero: Hero, pets: List<Pet>, n: Int = 3): List<PetLook> =
    pets.filter { it.key != hero.pet && it.key != hero.mount }.sortedWith(compareByDescending<Pet> { it.bond }.thenBy { it.hatchedAt }).take(n).map { it.look() }

fun roomState(hero: Hero, pets: List<Pet>, base: RoomState): RoomState {
    val pet = pets.firstOrNull { it.key == hero.pet }
    return base.copy(petForm = pet?.form.orEmpty(), petWear = pet?.wear.orEmpty(), residents = residents(hero, pets))
}

/** The pet's own name if it has one, otherwise its kind and colour. */
@Composable
fun petTitle(pet: Pet?): String = pet?.name?.takeIf { it.isNotBlank() } ?: petName(pet?.key.orEmpty())

fun android.content.Context.petTitle(pet: Pet?): String =
    pet?.name?.takeIf { it.isNotBlank() } ?: petLabel(pet?.key.orEmpty())

/** A pat on the companion in the scene: hearts in the scene, and once a day a little friendship. */
@Composable
fun rememberPat(hero: Hero): () -> Unit {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val ctx = LocalContext.current
    return remember(hero.pet) {
        {
            fb.tap()
            Sfx.play(Sound.PAT)
            c.scope.launch {
                val n = c.game.pat(hero.pet)
                if (n > 0) withContext(Dispatchers.Main) { fb.show(ctx.getString(R.string.patted, n)) }
            }
        }
    }
}

/** The title a claimed collection gives. */
@Composable
fun titleName(key: String): String {
    val set = Collections.of(key) ?: return ""
    return when {
        set.species != null -> stringResource(speciesTitle(set.species))
        set.potion != null -> stringResource(R.string.title_potion, stringResource(set.potion.label))
        else -> stringResource(R.string.title_all)
    }
}

private fun speciesTitle(s: Species) = when (s) {
    Species.FOX -> R.string.title_fox
    Species.CAT -> R.string.title_cat
    Species.OWL -> R.string.title_owl
    Species.SLIME -> R.string.title_slime
    Species.BUNNY -> R.string.title_bunny
    Species.WOLF -> R.string.title_wolf
    Species.TURTLE -> R.string.title_turtle
    Species.DRAGON -> R.string.title_dragon
}
