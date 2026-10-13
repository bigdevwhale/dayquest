package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R
import app.dayquest.data.Hero

/** A calling chosen from level [Classes.UNLOCK]: one stat grows twice as fast, and three skills cost mana. */
enum class HeroClass(val main: Stat, val color: Int, @StringRes val label: Int, @StringRes val text: Int) {
    WARRIOR(Stat.STR, 0xFFE5574F.toInt(), R.string.cls_warrior, R.string.cls_warrior_d),
    MAGE(Stat.INT, 0xFF5EC8FF.toInt(), R.string.cls_mage, R.string.cls_mage_d),
    ROGUE(Stat.PER, 0xFF7DAA4A.toInt(), R.string.cls_rogue, R.string.cls_rogue_d),
    HEALER(Stat.CON, 0xFFF2C14E.toInt(), R.string.cls_healer, R.string.cls_healer_d);

    val skills get() = Skill.entries.filter { it.cls == this }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.name == key }
    }
}

enum class Stat { STR, INT, CON, PER }

/** What a skill does: right away, or until the day it was cast on closes (overnight included). */
enum class SkillKind { INSTANT, DAY }

enum class Skill(
    val key: String,
    val cls: HeroClass,
    val level: Int,
    val mana: Double,
    val kind: SkillKind,
    @StringRes val label: Int,
    @StringRes val text: Int,
) {
    SMASH("smash", HeroClass.WARRIOR, 5, 10.0, SkillKind.INSTANT, R.string.sk_smash, R.string.sk_smash_d),
    BATTLE_CRY("battle_cry", HeroClass.WARRIOR, 8, 15.0, SkillKind.DAY, R.string.sk_battle_cry, R.string.sk_battle_cry_d),
    IRON_SKIN("iron_skin", HeroClass.WARRIOR, 12, 20.0, SkillKind.DAY, R.string.sk_iron_skin, R.string.sk_iron_skin_d),

    FIREBALL("fireball", HeroClass.MAGE, 5, 10.0, SkillKind.INSTANT, R.string.sk_fireball, R.string.sk_fireball_d),
    ARCANE_FOCUS("arcane_focus", HeroClass.MAGE, 8, 15.0, SkillKind.DAY, R.string.sk_arcane_focus, R.string.sk_arcane_focus_d),
    FROST_WARD("frost_ward", HeroClass.MAGE, 12, 20.0, SkillKind.DAY, R.string.sk_frost_ward, R.string.sk_frost_ward_d),

    PICKPOCKET("pickpocket", HeroClass.ROGUE, 5, 10.0, SkillKind.INSTANT, R.string.sk_pickpocket, R.string.sk_pickpocket_d),
    KEEN_EYE("keen_eye", HeroClass.ROGUE, 8, 15.0, SkillKind.DAY, R.string.sk_keen_eye, R.string.sk_keen_eye_d),
    SHADOW_STEP("shadow_step", HeroClass.ROGUE, 12, 20.0, SkillKind.DAY, R.string.sk_shadow_step, R.string.sk_shadow_step_d),

    HEAL("heal", HeroClass.HEALER, 5, 10.0, SkillKind.INSTANT, R.string.sk_heal, R.string.sk_heal_d),
    RENEWAL("renewal", HeroClass.HEALER, 8, 15.0, SkillKind.DAY, R.string.sk_renewal, R.string.sk_renewal_d),
    SANCTUARY("sanctuary", HeroClass.HEALER, 12, 25.0, SkillKind.DAY, R.string.sk_sanctuary, R.string.sk_sanctuary_d);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** What the skills cast for one day add up to; all neutral when nothing is active. */
data class Buffs(
    val crit: Double = 0.0,
    val xp: Double = 0.0,
    val drop: Double = 0.0,
    val dropCap: Int = 0,
    /** Damage taken (bad habits today, missed dailies tonight) is multiplied by this. */
    val hurt: Double = 1.0,
    /** The boss doesn't strike back tonight. */
    val bossWard: Boolean = false,
    /** Missed dailies keep their streaks tonight. */
    val keepStreaks: Boolean = false,
    /** HP healed with every finished task. */
    val heal: Double = 0.0,
)

/** The numbers of classes and skills. Pure, like [Rules]. */
object Classes {
    /** Level a class can be chosen at. */
    const val UNLOCK = 5

    /** Changing an already chosen class costs gold (the first choice is free). */
    const val CHANGE_COST = 100.0

    fun of(h: Hero) = HeroClass.of(h.heroClass)

    fun maxMp(s: Stats) = 20.0 + s.int

    /** Mana from one finished task: a little more with Intelligence. */
    fun mpGain(s: Stats) = 1.0 + s.int / 40.0

    /** Overnight the hero gets back a quarter of the pool. */
    fun mpOvernight(s: Stats) = maxMp(s) * 0.25

    /** The class's own stat grows twice as fast: +level / 2 on top of the base. */
    fun bonus(c: HeroClass?, level: Int): Stats {
        val v = level / 2
        return when (c?.main) {
            Stat.STR -> Stats(str = v)
            Stat.INT -> Stats(int = v)
            Stat.CON -> Stats(con = v)
            Stat.PER -> Stats(per = v)
            null -> Stats()
        }
    }

    fun decode(buffs: String): Map<String, Long> = buffs.split(',').filter { '=' in it }.associate {
        it.substringBefore('=') to (it.substringAfter('=').toLongOrNull() ?: -1)
    }

    /** Keeps only buffs of [day] (older ones have run out) and adds [skill]. */
    fun encode(buffs: String, skill: Skill, day: Long): String =
        (decode(buffs).filterValues { it == day } + (skill.key to day)).entries.joinToString(",") { "${it.key}=${it.value}" }

    fun active(buffs: String, day: Long): Set<Skill> = decode(buffs).filterValues { it == day }.keys.mapNotNull(Skill::of).toSet()

    fun buffs(h: Hero, day: Long): Buffs = active(h.buffs, day).fold(Buffs()) { b, s ->
        when (s) {
            Skill.BATTLE_CRY -> b.copy(crit = b.crit + 0.15)
            Skill.ARCANE_FOCUS -> b.copy(xp = b.xp + 0.3)
            Skill.KEEN_EYE -> b.copy(drop = b.drop + 0.15, dropCap = b.dropCap + 2)
            Skill.IRON_SKIN -> b.copy(hurt = b.hurt * 0.5)
            Skill.SANCTUARY -> b.copy(hurt = b.hurt * 0.25)
            Skill.FROST_WARD -> b.copy(bossWard = true)
            Skill.SHADOW_STEP -> b.copy(keepStreaks = true)
            Skill.RENEWAL -> b.copy(heal = b.heal + 2.0)
            else -> b
        }
    }

    /** Instant skills: boss damage, gold or HP, from the hero's stats. */
    fun smash(s: Stats) = 8.0 + s.str * 0.6
    fun fireball(s: Stats) = 6.0 + s.int * 0.5
    fun fireballXp(s: Stats) = 3.0 + s.int * 0.1
    fun pickpocket(s: Stats) = 4.0 + s.per * 0.3
    fun heal(s: Stats) = 12.0 + s.con * 0.3
}

/** Why a skill button is greyed out. */
enum class SkillBlock { CLASS, LEVEL, ACTIVE, NO_BOSS, FULL_HP, MANA }
