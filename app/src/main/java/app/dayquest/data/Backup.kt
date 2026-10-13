package app.dayquest.data

import android.util.Base64
import app.dayquest.game.Challenge
import app.dayquest.game.Difficulty
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Progress travels as plain JSON: a file to save or send, or a compact "transfer code"
 * (gzip + base64) to paste into the app on another phone. No account, no server.
 */
object Backup {
    private const val VERSION = 1
    private const val CODE_PREFIX = "DQ1:"

    fun encode(s: Snapshot, settings: Settings?): String = JSONObject()
        .put("app", "dayquest")
        .put("version", VERSION)
        .put("exportedAt", System.currentTimeMillis())
        .put("hero", s.hero?.let(::hero) ?: JSONObject.NULL)
        .put("tasks", JSONArray(s.tasks.map(::task)))
        .put("checklist", JSONArray(s.checklist.map {
            JSONObject().put("id", it.id).put("taskId", it.taskId).put("text", it.text).put("done", it.done).put("position", it.position)
        }))
        .put("log", JSONArray(s.log.map {
            JSONObject().put("id", it.id).put("taskId", it.taskId).put("at", it.at).put("day", it.day).put("kind", it.kind.name)
                .put("xp", it.xp).put("gold", it.gold).put("hp", it.hp).put("valueDelta", it.valueDelta).put("boss", it.boss)
                .put("drop", it.drop).put("undone", it.undone)
        }))
        .put("items", JSONArray(s.items.map { JSONObject().put("key", it.key).put("count", it.count) }))
        .put("pets", JSONArray(s.pets.map { JSONObject().put("key", it.key).put("growth", it.growth).put("hatchedAt", it.hatchedAt) }))
        .put("settings", settings?.let {
            JSONObject().put("theme", it.theme.name).put("dayStart", it.dayStart).put("reminders", it.reminders)
                .put("evening", it.evening).put("haptics", it.haptics).put("southern", it.southern).put("sounds", it.sounds).put("challenge", it.challenge.name).put("tips", JSONArray(it.tips.toList()))
        } ?: JSONObject.NULL)
        .toString()

    private fun hero(h: Hero) = JSONObject()
        .put("name", h.name).put("level", h.level).put("xp", h.xp).put("hp", h.hp).put("gold", h.gold).put("lastDay", h.lastDay)
        .put("skin", h.skin).put("hairStyle", h.hairStyle).put("hairColor", h.hairColor)
        .put("weapon", h.weapon).put("armor", h.armor).put("head", h.head).put("shield", h.shield)
        .put("pet", h.pet).put("scene", h.scene).put("ownedGear", h.ownedGear).put("ownedScenes", h.ownedScenes)
        .put("quest", h.quest).put("questHp", h.questHp).put("questsDone", h.questsDone)
        .put("dropsToday", h.dropsToday).put("deaths", h.deaths).put("resting", h.resting).put("createdAt", h.createdAt)
        .put("achievements", h.achievements).put("home", h.home).put("ownedDecor", h.ownedDecor).put("wallpaper", h.wallpaper).put("floor", h.floor)
        .put("perfectDay", h.perfectDay).put("chests", h.chests).put("eventDay", h.eventDay).put("bountyDay", h.bountyDay)

    private fun task(t: Task) = JSONObject()
        .put("id", t.id).put("type", t.type.name).put("title", t.title).put("notes", t.notes).put("difficulty", t.difficulty.name)
        .put("position", t.position).put("createdAt", t.createdAt).put("value", t.value)
        .put("up", t.up).put("down", t.down).put("countUp", t.countUp).put("countDown", t.countDown)
        .put("repeatMask", t.repeatMask).put("everyDays", t.everyDays).put("startDay", t.startDay)
        .put("streak", t.streak).put("bestStreak", t.bestStreak).put("doneDay", t.doneDay)
        .put("done", t.done).put("doneAt", t.doneAt).put("dueDay", t.dueDay).put("cost", t.cost).put("reminder", t.reminder)

    /** Throws [IllegalArgumentException] when the text is not a Dayquest backup. */
    fun decode(text: String): Pair<Snapshot, Settings?> {
        val json = text.trim().let { if (it.startsWith(CODE_PREFIX)) unpack(it) else it }
        val root = runCatching { JSONObject(json) }.getOrNull()
        require(root != null && root.optString("app") == "dayquest") { "Not a Dayquest backup" }
        val h = root.optJSONObject("hero")?.let {
            with(it) {
                Hero(
                    name = optString("name"), level = getInt("level"), xp = getDouble("xp"), hp = getDouble("hp"), gold = getDouble("gold"),
                    lastDay = getLong("lastDay"), skin = optInt("skin", 1), hairStyle = optInt("hairStyle"), hairColor = optInt("hairColor", 1),
                    weapon = optString("weapon", "wooden_sword"), armor = optString("armor", "tunic"), head = optString("head"), shield = optString("shield"),
                    pet = optString("pet"), scene = optString("scene", "meadow"),
                    ownedGear = optString("ownedGear", "wooden_sword,tunic"), ownedScenes = optString("ownedScenes", "meadow"),
                    quest = optString("quest"), questHp = optDouble("questHp", 0.0), questsDone = optString("questsDone"),
                    dropsToday = optInt("dropsToday"), deaths = optInt("deaths"), resting = optBoolean("resting"),
                    createdAt = optLong("createdAt", System.currentTimeMillis()),
                    achievements = optString("achievements"), home = optString("home"),
                    ownedDecor = optString("ownedDecor", "wp_plain,fl_planks"), wallpaper = optString("wallpaper", "wp_plain"), floor = optString("floor", "fl_planks"),
                    perfectDay = optLong("perfectDay", -1), chests = optInt("chests"), eventDay = optLong("eventDay", -1), bountyDay = optLong("bountyDay", -1),
                )
            }
        }
        val snapshot = Snapshot(
            hero = h,
            tasks = root.list("tasks") {
                Task(
                    id = getLong("id"), type = TaskType.valueOf(getString("type")), title = getString("title"), notes = optString("notes"),
                    difficulty = Difficulty.valueOf(optString("difficulty", "EASY")), position = optInt("position"),
                    createdAt = optLong("createdAt"), value = optDouble("value", 0.0),
                    up = optBoolean("up", true), down = optBoolean("down"), countUp = optInt("countUp"), countDown = optInt("countDown"),
                    repeatMask = optInt("repeatMask", 0b1111111), everyDays = optInt("everyDays"), startDay = optLong("startDay"),
                    streak = optInt("streak"), bestStreak = optInt("bestStreak"), doneDay = optLong("doneDay", -1),
                    done = optBoolean("done"), doneAt = optLong("doneAt"), dueDay = optLong("dueDay", -1),
                    cost = optDouble("cost", 10.0), reminder = optInt("reminder", -1),
                )
            },
            checklist = root.list("checklist") { CheckItem(getLong("id"), getLong("taskId"), getString("text"), optBoolean("done"), optInt("position")) },
            log = root.list("log") {
                LogEntry(
                    id = getLong("id"), taskId = optLong("taskId"), at = optLong("at"), day = getLong("day"), kind = runCatching { LogKind.valueOf(getString("kind")) }.getOrDefault(LogKind.BUY),
                    xp = optDouble("xp", 0.0), gold = optDouble("gold", 0.0), hp = optDouble("hp", 0.0), valueDelta = optDouble("valueDelta", 0.0),
                    boss = optDouble("boss", 0.0), drop = optString("drop"), undone = optBoolean("undone"),
                )
            },
            items = root.list("items") { Item(getString("key"), getInt("count")) },
            pets = root.list("pets") { Pet(getString("key"), optInt("growth"), optLong("hatchedAt")) },
        )
        val settings = root.optJSONObject("settings")?.let {
            Settings(
                theme = runCatching { ThemeMode.valueOf(it.optString("theme")) }.getOrDefault(ThemeMode.SYSTEM),
                dayStart = it.optInt("dayStart"), reminders = it.optBoolean("reminders", true),
                evening = it.optInt("evening", 20 * 60), haptics = it.optBoolean("haptics", true), southern = it.optBoolean("southern"),
                sounds = it.optBoolean("sounds", true),
                challenge = runCatching { Challenge.valueOf(it.optString("challenge")) }.getOrDefault(Challenge.NORMAL),
                tips = it.optJSONArray("tips")?.let { a -> (0 until a.length()).map { i -> a.getString(i) }.toSet() } ?: emptySet(),
            )
        }
        return snapshot to settings
    }

    /** Compact single-line code, safe to paste into any messenger. */
    fun pack(json: String): String {
        val bytes = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(json.toByteArray()) } }.toByteArray()
        return CODE_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun unpack(code: String): String {
        val raw = code.removePrefix(CODE_PREFIX).filterNot { it.isWhitespace() }
        val bytes = runCatching { Base64.decode(raw, Base64.URL_SAFE) }.getOrNull() ?: throw IllegalArgumentException("Bad code")
        return GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().decodeToString() }
    }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
