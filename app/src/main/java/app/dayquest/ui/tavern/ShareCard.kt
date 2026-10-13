package app.dayquest.ui.tavern

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.content.res.ResourcesCompat
import app.dayquest.AppContainer
import app.dayquest.R
import app.dayquest.game.Catalog
import app.dayquest.game.HeroClass
import app.dayquest.game.Party
import app.dayquest.game.Season
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneRenderer
import app.dayquest.pixel.SceneState
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A picture of the hero to post anywhere: the living scene at this hour, the name, level and class,
 * and four proud numbers. The hero code rides along as text, so a friend can add you to their tavern.
 */
object ShareCard {
    private const val VW = 172
    private const val VH = 72
    private const val SCALE = 6
    private const val PANEL = 330

    suspend fun share(context: Context, c: AppContainer) {
        val card = c.game.heroCard()
        val hero = c.game.hero()
        val settings = c.settings.now()
        val pets = c.db.game().pets()
        val created = app.dayquest.game.Rules.logicalDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(hero.createdAt), ZoneId.systemDefault()), settings.dayStart)
        val together = c.game.today() - created + 1

        // The scene, rendered a few frames in so clouds and particles have settled.
        val r = SceneRenderer(VW, VH)
        val now = LocalDateTime.now()
        val state = SceneState(
            Catalog.scene(hero.scene).kind, now.hour + now.minute / 60f,
            Look(hero.skin, hero.hairStyle, hero.hairColor, hero.weapon, hero.armor, hero.head, hero.shield),
            hero.pet, (pets.firstOrNull { it.key == hero.pet }?.growth ?: 0) >= 100, Catalog.boss(hero.quest), hero.resting,
            Season.of(LocalDate.now(), settings.southern), mount = hero.mount,
        )
        repeat(20) { r.render(state, 10_000L + it * 66) }
        val scene = createBitmap(VW, VH)
        scene.setPixels(r.buffer.px, 0, VW, 0, 0, VW, VH)

        val w = VW * SCALE
        val h = VH * SCALE + PANEL
        val out = createBitmap(w, h)
        val canvas = Canvas(out)
        canvas.drawBitmap(scene, Rect(0, 0, VW, VH), Rect(0, 0, w, VH * SCALE), Paint().apply { isFilterBitmap = false })

        val top = VH * SCALE
        canvas.drawRect(0f, top.toFloat(), w.toFloat(), h.toFloat(), Paint().apply { color = 0xFF1C1930.toInt() })
        canvas.drawRect(0f, top.toFloat(), w.toFloat(), top + 8f, Paint().apply { color = 0xFF7C5CFF.toInt() })
        val pixel = ResourcesCompat.getFont(context, R.font.pixel_bold)
        val inter = ResourcesCompat.getFont(context, R.font.inter_medium)
        fun paint(size: Float, color: Int, face: android.graphics.Typeface? = pixel, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face; textAlign = align }

        val pad = 48f
        canvas.drawText(hero.name, pad, top + 100f, paint(84f, 0xFFF4F1FF.toInt()))
        val cls = HeroClass.of(hero.heroClass)?.let { context.getString(it.label) }
        canvas.drawText(listOfNotNull(context.getString(R.string.lv, hero.level), cls).joinToString("  ·  "), pad, top + 160f, paint(48f, 0xFFF2C14E.toInt()))

        val stats = listOf(
            card.bestStreak to R.string.share_streak,
            card.bosses to R.string.share_bosses,
            card.pets to R.string.share_pets,
            together.toInt() to R.string.share_days,
        )
        val col = (w - pad * 2) / stats.size
        stats.forEachIndexed { i, (v, label) ->
            val x = pad + col * i
            canvas.drawText(v.toString(), x, top + 250f, paint(64f, 0xFFF4F1FF.toInt()))
            canvas.drawText(context.getString(label), x, top + 290f, paint(28f, 0xFFA59FC0.toInt(), inter))
        }
        canvas.drawText("Dayquest", w - pad, top + 100f, paint(44f, 0xFF9B82FF.toInt(), align = Paint.Align.RIGHT))

        val dir = File(context.cacheDir, "transfer").apply { mkdirs() }
        val file = File(dir, "hero.png")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, Party.encode(card))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Lets the share sheet show the picture itself, not a generic file icon.
        send.clipData = android.content.ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
