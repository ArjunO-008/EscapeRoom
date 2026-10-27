package audio;

/**
 * One place for the audio settings you are most likely to tweak.
 *
 * Change a value here and recompile, or change it while the game is running:
 *
 *   SoundManager.setMusicVolume(0.2);   // background music, 0.0 - 1.0
 *   SoundManager.setSfxVolume(0.8);     // every sound effect, 0.0 - 1.0
 */
public final class AudioConfig {

    private AudioConfig() {
    }

    /**
     * The background music file. Java plays .wav / .aiff / .au natively
     * (convert an mp3 first, see src/audio/music/README.txt).
     *
     * If this exact file does not exist, the game falls back to the first
     * audio file it finds in the same folder.
     */
    public static final String MUSIC_FILE = "src/audio/music/bgm.wav";

    /** Starting volume of the background music (0.0 silent - 1.0 full). */
    public static final double MUSIC_VOLUME = 0.35;

    /** Starting master volume of all sound effects (1.0 = as designed). */
    public static final double SFX_VOLUME = 1.0;

    /**
     * Makes the loop invisible: the last N milliseconds of the track are
     * blended into its beginning, so the end flows straight back into the start
     * instead of cutting and restarting. The first play-through is unchanged,
     * only the repeats use the blend. 0 turns it off (plain loop).
     * 3000 - 6000 suits most background music.
     */
    public static final int MUSIC_LOOP_CROSSFADE_MS = 4000;

    /** How long the music takes to fade out when you escape, in milliseconds. */
    public static final int MUSIC_FADE_OUT_MS = 1200;
}
