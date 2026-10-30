package vadiole.opaltest.feature.claimreveal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.VibratorManager
import androidx.annotation.RawRes
import vadiole.opaltest.R

internal enum class RevealSound(@RawRes val res: Int, private val peakMs: Int) {
    Shine1(R.raw.claim_reveal_shine_1, 240),
    Shine2(R.raw.claim_reveal_shine_2, 240),
    Shine3(R.raw.claim_reveal_shine_3, 240),
    Shine5(R.raw.claim_reveal_shine_5, 200),
    Shine6(R.raw.claim_reveal_shine_6, 160);

    internal val leadMs: Float get() = peakMs / SOUND_RATE
}

internal class RevealFeedback(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val sounds = RevealSound.entries.associateWith { pool.load(context, it.res, 1) }

    fun buttonAppear() {
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.startComposition()
            .addPrimitive(Composition.PRIMITIVE_TICK, APPEAR_SCALE)
            .addPrimitive(Composition.PRIMITIVE_TICK, APPEAR_SCALE, APPEAR_GAP_MS)
            .compose()
        vibrator.vibrate(effect, TouchAttributes)
    }

    fun shine(sound: RevealSound) {
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        pool.play(sounds.getValue(sound), VOLUME, VOLUME, 1, 0, SOUND_RATE)
    }

    fun release() = pool.release()
}

private val TouchAttributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
internal const val SOUND_RATE = 0.5f
private const val VOLUME = 0.144f
private const val APPEAR_SCALE = 0.8f
private const val APPEAR_GAP_MS = 90
