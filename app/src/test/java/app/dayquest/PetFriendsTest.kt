package app.dayquest

import app.dayquest.data.Hero
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Pet
import app.dayquest.data.Settings
import app.dayquest.game.Bond
import app.dayquest.game.Catalog
import app.dayquest.game.Collections
import app.dayquest.game.Companions
import app.dayquest.game.Food
import app.dayquest.game.Form
import app.dayquest.game.Game
import app.dayquest.game.MomentKind
import app.dayquest.game.NoteKind
import app.dayquest.game.PetWear
import app.dayquest.game.Potion
import app.dayquest.game.SceneKind
import app.dayquest.game.Season
import app.dayquest.game.Snap
import app.dayquest.game.Species
import app.dayquest.game.Trait
import app.dayquest.game.Visits
import app.dayquest.game.Weather
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetFriendsTest {
    private fun game(db: FakeDao) = Game(db, { Settings() }, Random(1))

    @Test
    fun aPetKeepsItsCharacterAndEveryCharacterTurnsUp() {
        assertEquals(Trait.of("fox:base", 1234L), Trait.of("fox:base", 1234L))
        val seen = (0L until 200L).map { Trait.of("cat:rose", 1_700_000_000_000L + it * 7919) }.toSet()
        assertEquals(Trait.entries.toSet(), seen)
    }

    @Test
    fun heartsNeedMoreAndMorePoints() {
        assertEquals(15, Bond.need(1))
        assertEquals(825, Bond.need(Bond.MAX))
        assertEquals(0, Bond.level(14))
        assertEquals(1, Bond.level(15))
        assertEquals(3, Bond.level(Bond.need(3)))
        assertEquals(Bond.MAX, Bond.level(5000))
    }

    @Test
    fun aPatCountsOnceADay() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), pet = "fox:base"))
        val pet = Pet("fox:base", hatchedAt = 1)
        db.savePet(pet)
        val n = g.pat("fox:base")
        assertEquals(Bond.gain(pet, Bond.PAT), n)
        assertEquals(0, g.pat("fox:base"))
        assertEquals(n, db.pets["fox:base"]!!.bond)
    }

    @Test
    fun aGrownPetStillTakesTreats() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today()))
        val pet = Pet("fox:base", growth = 100, hatchedAt = 1)
        db.savePet(pet)
        db.addItem("food:berry", 1)
        val fed = g.feed("fox:base", Food.BERRY)!!
        assertEquals(0, fed.growth)
        assertEquals(Bond.gain(pet, Bond.FAVORITE), fed.bond)
        assertNull(g.feed("fox:base", Food.BERRY))
    }

    @Test
    fun growingUpGoesInTheAlbum() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today()))
        db.addItem("egg:fox", 1)
        db.addItem("potion:base", 1)
        assertTrue(g.hatch(Species.FOX, Potion.BASE))
        db.savePet(db.pets["fox:base"]!!.copy(growth = 95))
        db.addItem("food:berry", 1)
        g.feed("fox:base", Food.BERRY)
        assertEquals(listOf(MomentKind.HATCH.name, MomentKind.GROWN.name), db.moments.map { it.kind })
        assertNotNull(Snap.decode(db.moments.last().scene))
    }

    @Test
    fun aDayOfWorkTogetherBringsTheCompanionCloserAndAMorningNote() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        val today = g.today()
        db.saveHero(Hero(name = "x", lastDay = today - 1, pet = "owl:base"))
        val pet = Pet("owl:base", hatchedAt = 5)
        db.savePet(pet)
        db.insertLog(LogEntry(day = today - 1, kind = LogKind.TODO))
        val r = g.startNewDay()!!
        val after = db.pets["owl:base"]!!
        assertEquals(1, after.days)
        assertEquals(Bond.gain(pet, Bond.DAY), after.bond)
        assertNotNull(r.note)
        assertTrue(r.note!!.kind != NoteKind.MISSED)
    }

    @Test
    fun aCompanionMissesYouAfterDaysAway() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        val today = g.today()
        db.saveHero(Hero(name = "x", lastDay = today - 3, pet = "owl:base"))
        db.savePet(Pet("owl:base"))
        val note = g.startNewDay()!!.note!!
        assertEquals(NoteKind.MISSED, note.kind)
        assertEquals(2, note.n)
        // No work that day: no friendship for it.
        assertEquals(0, db.pets["owl:base"]!!.bond)
    }

    @Test
    fun theVisitLadderClimbsAndAMissedDayKeepsFinishedWeeks() {
        assertEquals(1, Visits.streak(-1, 0, 100))
        assertEquals(4, Visits.streak(99, 3, 100))
        assertEquals(3, Visits.streak(100, 3, 100))
        assertEquals(8, Visits.streak(90, 9, 100))
        assertEquals(1, Visits.streak(90, 6, 100))
        assertEquals(15, Visits.streak(90, 14, 100))
        assertEquals(7, Visits.step(7))
        assertEquals(1, Visits.step(8))
        // The seventh day brings an egg; the thirtieth adds a rare potion.
        assertTrue(Visits.gift(7, 5, 100).items.any { it.startsWith("egg:") })
        assertTrue(Visits.gift(30, 5, 100).items.any { it in setOf("potion:frost", "potion:shadow", "potion:ember") })
        assertEquals(Visits.gift(12, 5, 100), Visits.gift(12, 5, 100))
    }

    @Test
    fun theMorningCardPaysTheVisitOnce() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        val today = g.today()
        db.saveHero(Hero(name = "x", lastDay = today, visitDay = today - 1, visitStreak = 2, gold = 100.0))
        g.openEvent()
        assertEquals(today, db.hero!!.visitDay)
        assertEquals(3, db.hero!!.visitStreak)
        assertNull(g.openEvent())
        assertEquals(3, db.hero!!.visitStreak)
    }

    @Test
    fun aGrownPetEvolvesDownOneRoadForGood() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today()))
        db.savePet(Pet("wolf:base", growth = 100, days = 12, wins = 10))
        assertFalse(g.evolve("wolf:base", Form.STAR))
        val before = Companions.amount(db.pets["wolf:base"]!!)
        assertTrue(g.evolve("wolf:base", Form.BATTLE))
        assertFalse(g.evolve("wolf:base", Form.BATTLE))
        db.savePet(db.pets["wolf:base"]!!.copy(days = 30))
        assertFalse(g.evolve("wolf:base", Form.STAR))
        assertEquals("battle", db.pets["wolf:base"]!!.form)
        assertEquals(before * 1.25, Companions.amount(db.pets["wolf:base"]!!), 1e-9)
        assertEquals(1, db.moments.count { it.kind == MomentKind.EVOLVE.name })
    }

    @Test
    fun aBestFriendHelpsAFifthMore() {
        val pet = Pet("fox:base", growth = 100)
        assertEquals(Companions.amount(pet) * 1.2, Companions.amount(pet.copy(bond = Bond.need(Bond.MAX))), 1e-9)
    }

    @Test
    fun clothesMustBeOwnedAndOneFitsEachSlot() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), level = 20, gold = 1000.0))
        db.savePet(Pet("cat:base"))
        assertFalse(g.wear("cat:base", PetWear.BOW))
        assertTrue(g.buyWear(PetWear.BOW))
        assertFalse(g.buyWear(PetWear.BOW))
        assertTrue(g.buyWear(PetWear.TOP_HAT))
        assertTrue(g.buyWear(PetWear.SCARF))
        g.wear("cat:base", PetWear.BOW)
        g.wear("cat:base", PetWear.SCARF)
        g.wear("cat:base", PetWear.TOP_HAT)
        assertEquals(listOf(PetWear.SCARF, PetWear.TOP_HAT), PetWear.parse(db.pets["cat:base"]!!.wear))
        g.wear("cat:base", PetWear.TOP_HAT)
        assertEquals(listOf(PetWear.SCARF), PetWear.parse(db.pets["cat:base"]!!.wear))
        // The ribbon is never sold: six hearts with any pet unlock it.
        assertFalse(g.buyWear(PetWear.RIBBON))
        assertFalse(g.wear("cat:base", PetWear.RIBBON))
        db.savePet(db.pets["cat:base"]!!.copy(bond = Bond.need(Bond.GIFT)))
        assertTrue(g.wear("cat:base", PetWear.RIBBON))
    }

    @Test
    fun aFinishedCollectionPaysOnceWithAStatueAndATitle() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today()))
        Potion.entries.dropLast(1).forEach { db.savePet(Pet(Catalog.petKey(Species.FOX, it))) }
        assertNull(g.claimCollection("sp:fox"))
        db.savePet(Pet(Catalog.petKey(Species.FOX, Potion.entries.last())))
        val set = g.claimCollection("sp:fox")!!
        assertEquals(set.gold, db.hero!!.gold, 1e-9)
        assertTrue(db.hero!!.ownsDecor("statue_fox"))
        assertEquals("sp:fox", db.hero!!.title)
        assertNull(g.claimCollection("sp:fox"))
        assertEquals(Species.entries.size + Potion.entries.size + 1, Collections.all.size)
    }

    @Test
    fun weatherIsTheSameForEveryoneAndFitsThePlace() {
        val day = 20_400L
        assertEquals(Weather.at(day, 10f, Season.SPRING, SceneKind.MEADOW), Weather.at(day, 10f, Season.SPRING, SceneKind.MEADOW))
        val year = (0 until 365L).flatMap { d -> (0 until 24).map { h -> Weather.at(day + d, h + 0.5f, Season.AUTUMN, SceneKind.MEADOW) } }
        assertTrue(Weather.RAIN in year && Weather.RAINBOW in year && Weather.CLEAR in year)
        val winter = (0 until 200L).map { Weather.at(day + it, 12f, Season.WINTER, SceneKind.MEADOW) }
        assertTrue(winter.none { it.wet } && Weather.SNOW in winter)
        assertTrue((0 until 200L).all { Weather.at(day + it, 12f, Season.SPRING, SceneKind.CAVE) == Weather.CLEAR })
    }

    @Test
    fun aMomentsSceneSurvivesAsALine() {
        val s = Snap("forest", 18.5f, "autumn", 2, 3, 4, "gen:3:WEAPON", "tunic", "", "aegis", "fox:rose", true, "star", "bow,scarf", "wolf:base", "slime_king#2", "RAIN")
        assertEquals(s, Snap.decode(s.encode()))
        assertNull(Snap.decode("broken"))
    }
}
