package app.dayquest.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.random.Random

enum class Sound { DONE, UNDO, HURT, HIT, CRIT, DROP, COIN, JACKPOT, LEVEL, VICTORY, PERFECT, CHEST, CARD, FAINT, SPELL, CHIME, PAT, LETTER }

/**
 * 8-bit sound effects, synthesised once on first use: square, triangle and noise voices, like an
 * old handheld. No audio files ship with the app. Silent when the phone is on silent or vibrate.
 */
object Sfx {
    @Volatile var enabled = true
    private const val RATE = 22050
    private lateinit var audio: AudioManager
    private val thread = Executors.newSingleThreadExecutor()
    private val tracks = HashMap<Sound, AudioTrack>()

    fun init(context: Context) {
        audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun play(s: Sound) {
        if (!enabled || !::audio.isInitialized || audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        thread.execute {
            runCatching {
                val t = tracks.getOrPut(s) { track(render(s)) }
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
                t.reloadStaticData()
                t.play()
            }
        }
    }

    private fun track(pcm: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        return t
    }

    // ---- Synth ---------------------------------------------------------------------------

    private enum class Wave { SQUARE, PULSE, TRIANGLE, NOISE }

    /** One note: [hz] slides to [to] over [ms]; starts [at] ms into the sound. */
    private class Note(val at: Int, val ms: Int, val hz: Double, val to: Double = hz, val wave: Wave = Wave.SQUARE, val vol: Double = 0.5)

    private fun n(name: String): Double {
        val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octave = name.last().digitToInt()
        val semis = names.indexOf(name.dropLast(1)) + (octave + 1) * 12
        return 440.0 * Math.pow(2.0, (semis - 69) / 12.0)
    }

    /** Notes one after another, each [ms] long. */
    private fun seq(start: Int, ms: Int, vararg notes: String, wave: Wave = Wave.SQUARE, vol: Double = 0.45) =
        notes.mapIndexed { i, s -> Note(start + i * ms, ms, n(s), wave = wave, vol = vol) }

    private fun render(s: Sound): ShortArray = mix(
        when (s) {
            Sound.DONE -> seq(0, 55, "E6", "B6", vol = 0.35)
            Sound.UNDO -> seq(0, 55, "B5", "E5", wave = Wave.TRIANGLE, vol = 0.5)
            Sound.HURT -> listOf(Note(0, 180, 220.0, 70.0, Wave.SQUARE, 0.4), Note(0, 120, 0.0, wave = Wave.NOISE, vol = 0.25))
            Sound.HIT -> listOf(Note(0, 90, 0.0, wave = Wave.NOISE, vol = 0.45), Note(0, 70, 180.0, 90.0, Wave.SQUARE, 0.3))
            Sound.CRIT -> listOf(Note(0, 140, 0.0, wave = Wave.NOISE, vol = 0.5)) + seq(40, 45, "C6", "G6", "C7", vol = 0.35)
            Sound.DROP -> seq(0, 50, "C6", "E6", "G6", "C7", "E7", wave = Wave.PULSE, vol = 0.3)
            Sound.COIN -> seq(0, 60, "B5", "E6", vol = 0.35) + listOf(Note(120, 160, n("E6"), wave = Wave.SQUARE, vol = 0.18))
            Sound.JACKPOT -> (0 until 4).flatMap { seq(it * 110, 55, "B5", "E6", vol = 0.33) } + seq(440, 90, "E6", "G#6", "B6", "E7", vol = 0.35)
            Sound.LEVEL -> seq(0, 90, "C5", "E5", "G5", "C6", vol = 0.4) + listOf(Note(360, 380, n("C6"), wave = Wave.SQUARE, vol = 0.35), Note(360, 380, n("G5"), wave = Wave.TRIANGLE, vol = 0.4)) +
                seq(0, 180, "C3", "G3", wave = Wave.TRIANGLE, vol = 0.5)
            Sound.VICTORY -> seq(0, 110, "G4", "C5", "E5", "G5", vol = 0.38) + seq(440, 110, "E5", "G5", vol = 0.38) +
                listOf(Note(660, 520, n("C6"), wave = Wave.SQUARE, vol = 0.36), Note(660, 520, n("E5"), wave = Wave.TRIANGLE, vol = 0.45)) +
                seq(0, 220, "C3", "G3", "E3", "C3", wave = Wave.TRIANGLE, vol = 0.5)
            Sound.PERFECT -> {
                val rnd = Random(3)
                // Rockets whistle up and pop, then a little fanfare.
                (0 until 4).flatMap { i ->
                    val at = i * 230
                    listOf(Note(at, 160, 500.0 + rnd.nextInt(300), 1400.0 + rnd.nextInt(600), Wave.TRIANGLE, 0.22), Note(at + 160, 200, 0.0, wave = Wave.NOISE, vol = 0.4))
                } + seq(960, 100, "C5", "E5", "G5", "C6", "E6", vol = 0.36) + listOf(Note(1460, 500, n("G6"), wave = Wave.PULSE, vol = 0.3), Note(1460, 500, n("C4"), wave = Wave.TRIANGLE, vol = 0.45))
            }
            Sound.CHEST -> listOf(Note(0, 220, 90.0, 140.0, Wave.SQUARE, 0.22)) + seq(240, 55, "G5", "B5", "D6", "G6", "B6", "D7", wave = Wave.PULSE, vol = 0.3) +
                listOf(Note(570, 400, n("G6"), wave = Wave.TRIANGLE, vol = 0.4))
            Sound.CARD -> listOf(Note(0, 90, 0.0, wave = Wave.NOISE, vol = 0.12)) + seq(80, 70, "A5", "C#6", "E6", "A6", wave = Wave.TRIANGLE, vol = 0.5)
            Sound.FAINT -> seq(0, 200, "G4", "F#4", "F4", vol = 0.35) + listOf(Note(600, 600, n("E4"), 196.0, Wave.SQUARE, 0.35))
            // A rising shimmer: the skill goes off.
            Sound.SPELL -> listOf(Note(0, 260, 300.0, 1200.0, Wave.TRIANGLE, 0.35)) + seq(120, 45, "E6", "G#6", "B6", "E7", wave = Wave.PULSE, vol = 0.25)
            // A soft bell, twice: time is up.
            Sound.CHIME -> seq(0, 260, "E6", "B5", wave = Wave.TRIANGLE, vol = 0.55) + seq(560, 420, "E6", wave = Wave.TRIANGLE, vol = 0.5)
            // A happy little chirp, up and up: a pat.
            Sound.PAT -> listOf(Note(0, 90, n("G5"), n("C6"), Wave.TRIANGLE, 0.5), Note(100, 120, n("E6"), n("G6"), Wave.TRIANGLE, 0.45))
            // Paper rustle, then a soft two-note hello: a note from the companion.
            Sound.LETTER -> listOf(Note(0, 120, 0.0, wave = Wave.NOISE, vol = 0.1)) + seq(120, 120, "C6", "G5", "C6", wave = Wave.TRIANGLE, vol = 0.45)
        }
    )

    private fun mix(notes: List<Note>): ShortArray {
        val len = notes.maxOf { it.at + it.ms } * RATE / 1000 + RATE / 50
        val out = DoubleArray(len)
        val rnd = Random(11)
        for (note in notes) {
            val start = note.at * RATE / 1000
            val count = note.ms * RATE / 1000
            var phase = 0.0
            var noise = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val hz = note.hz + (note.to - note.hz) * t
                phase += hz / RATE
                val p = phase - kotlin.math.floor(phase)
                val v = when (note.wave) {
                    Wave.SQUARE -> if (p < 0.5) 1.0 else -1.0
                    Wave.PULSE -> if (p < 0.25) 1.0 else -1.0
                    Wave.TRIANGLE -> 4 * kotlin.math.abs(p - 0.5) - 1
                    // Sample-and-hold noise, about 4 kHz: the crunchy NES kind.
                    Wave.NOISE -> { if (i % 5 == 0) noise = rnd.nextDouble() * 2 - 1; noise }
                }
                // Short attack, linear decay: no clicks, still snappy.
                val env = minOf(1.0, i / (RATE * 0.004)) * (1 - t).let { it * (0.6 + 0.4 * it) }
                if (start + i < len) out[start + i] += v * env * note.vol
            }
        }
        // Soft clip so stacked voices never crackle.
        return ShortArray(len) { i -> (kotlin.math.tanh(out[i] * 0.9) * 0.7 * Short.MAX_VALUE).toInt().toShort() }
    }
}
