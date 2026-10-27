package audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Procedural sound effects - no audio files needed.
 *
 * Every sound is synthesized in code when the game starts (on a background
 * thread) and played through javax.sound.sampled. If the machine has no audio
 * device, everything silently does nothing.
 *
 * Usage:
 *   SoundManager.init();                       // once, at startup
 *   SoundManager.play(Sfx.CLICK);
 *   SoundManager.play(Sfx.DRONE);              // loops until stopped
 *   SoundManager.fadeOut(Sfx.DRONE, 800);
 *   SoundManager.exportWavs(new File("sounds"));  // optional: dump .wav files
 *
 * Background music is the one thing that IS a file: set its path and volume in
 * AudioConfig, then call startMusic() / stopMusic(ms).
 */
public final class SoundManager {

    public enum Sfx {
        TYPE,      // typewriter key tick (4 variants)
        THUD,      // deep cinematic hit
        LOCK,      // key turning in a lock
        CREAK,     // old door opening
        STEP,      // footstep (2 variants)
        OUTDOORS,  // wind + birds, ~8 s
        CHIME,     // 3-note success chime
        DING,      // single soft bell
        HOVER,     // button hover tick
        CLICK,     // button press
        DRONE,     // low ambient loop
        CRICKETS,  // night crickets loop (intro)
        CLOCK,     // clock tick / tock (2 variants)
        GIGGLE,    // two kids giggling (2 variants)
        SHUT,      // soft wooden door shutting
        BEEP,      // keypad beep
        TEAR,      // paper ripping
        SWISH,     // paper piece flying away
        HEART,     // heartbeat lub-dub
        RATTLE,    // locked door handle rattling
        SPARKLE,   // twinkling magic
        PAPER,     // paper being picked up / unfolded (reading a note)
        SAFE_OPEN, // heavy bolt clunk + hinge swing (safe unlocking)
        DOOR_OPEN, // latch + short hinge creak (room door opening)
        ERROR,     // low double buzz (wrong code / wrong sequence)
        TONE1,     // symbol-lock tones, one per coloured button
        TONE2,
        TONE3,
        TONE4,
        WALK,      // footstep on a wooden floor (4 variants)
        KEY_DROP,  // key clattering onto the floor
        KEY_PICKUP // key jingling into the inventory
    }

    private static final float SR = 44100f;
    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, SR, 16, 1, 2, SR, false);

    private static final Map<Sfx, Voice> VOICES = new EnumMap<>(Sfx.class);

    private static volatile boolean started;
    private static volatile boolean ready;
    private static volatile boolean muted;

    // ----- Background music (file based, streamed from a single Clip) -----
    private static volatile double musicVolume = AudioConfig.MUSIC_VOLUME;
    private static volatile double sfxVolume = AudioConfig.SFX_VOLUME;
    private static final Object MUSIC_LOCK = new Object();
    private static Clip musicClip;           // guarded by MUSIC_LOCK
    private static boolean musicLoadStarted; // guarded by MUSIC_LOCK
    private static boolean musicWanted;      // guarded by MUSIC_LOCK
    private static int musicGen;             // guarded by MUSIC_LOCK; bumps on start/stop

    private SoundManager() {
    }

    // =================================================
    // Public API
    // =================================================

    /** Synthesizes and loads all sounds in the background. Safe to call twice. */
    public static synchronized void init() {
        if (started) {
            return;
        }
        started = true;

        preloadMusic();

        Thread t = new Thread(() -> {
            try {
                Map<Sfx, byte[][]> data = synthesizeAll();
                for (Map.Entry<Sfx, byte[][]> e : data.entrySet()) {
                    VOICES.put(e.getKey(), new Voice(e.getValue(), poolSize(e.getKey())));
                }
                ready = true;
            } catch (Throwable ex) {
                // No audio device / headless: stay silent.
                System.out.println("Sound disabled: " + ex.getMessage());
                VOICES.clear();
                ready = false;
            }
        }, "sound-init");
        t.setDaemon(true);
        t.start();
    }

    public static void play(Sfx sfx) {
        play(sfx, 1.0);
    }

    /** @param volume multiplier on the sound's default level (1.0 = default). */
    public static void play(Sfx sfx, double volume) {
        if (!ready || muted) {
            return;
        }
        Voice v = VOICES.get(sfx);
        if (v == null) {
            return;
        }
        try {
            v.play(volume * sfxVolume * defaultVolume(sfx), sfx == Sfx.DRONE || sfx == Sfx.CRICKETS);
        } catch (Throwable ignored) {
            // never let audio break the game
        }
    }

    public static boolean isReady() {
        return ready;
    }

    /** Changes the volume of a sound that is already playing (1.0 = default level). */
    public static void setVolume(Sfx sfx, double volume) {
        Voice v = VOICES.get(sfx);
        if (v != null && ready) {
            v.setVolume(volume * sfxVolume * defaultVolume(sfx));
        }
    }

    public static void stop(Sfx sfx) {
        Voice v = VOICES.get(sfx);
        if (v != null) {
            v.stop();
        }
    }

    public static void stopAll() {
        for (Voice v : VOICES.values()) {
            v.stop();
        }
    }

    /** Fades the sound out over the given time, then stops it. */
    public static void fadeOut(Sfx sfx, int millis) {
        Voice v = VOICES.get(sfx);
        if (v == null || !ready) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                int steps = 20;
                for (int i = 1; i <= steps; i++) {
                    v.scaleGain(1.0 - (double) i / steps);
                    Thread.sleep(Math.max(1, millis / steps));
                }
            } catch (Throwable ignored) {
            } finally {
                v.stop();
            }
        }, "sound-fade");
        t.setDaemon(true);
        t.start();
    }

    public static void setMuted(boolean value) {
        muted = value;
        if (value) {
            stopAll();
            pauseMusic();
        } else {
            synchronized (MUSIC_LOCK) {
                if (musicWanted) {
                    playMusicLocked();
                }
            }
        }
    }

    public static boolean isMuted() {
        return muted;
    }

    // =================================================
    // Background music
    // =================================================

    /** Changes the music volume (0.0 - 1.0), also while it is playing. */
    public static void setMusicVolume(double volume) {
        synchronized (MUSIC_LOCK) {
            musicVolume = Math.max(0.0, Math.min(1.0, volume));
            if (musicClip != null && musicClip.isRunning()) {
                Voice.setGain(musicClip, musicVolume);
            }
        }
    }

    public static double getMusicVolume() {
        return musicVolume;
    }

    /** Master volume for all sound effects (0.0 - 1.0). Applies to sounds played from now on. */
    public static void setSfxVolume(double volume) {
        sfxVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public static double getSfxVolume() {
        return sfxVolume;
    }

    /** Starts (or resumes) the looping background music. Safe to call repeatedly. */
    public static void startMusic() {
        synchronized (MUSIC_LOCK) {
            musicWanted = true;
            musicGen++;
            if (!muted) {
                playMusicLocked(); // does nothing until the file has finished loading
            }
        }
        preloadMusic(); // no-op if already loading / loaded
    }

    /** Fades the music out over the given time, then rewinds it. */
    public static void stopMusic(int fadeMillis) {
        final int gen;
        final Clip c;
        synchronized (MUSIC_LOCK) {
            musicWanted = false;
            gen = ++musicGen;
            c = musicClip;
        }
        if (c == null) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                if (fadeMillis > 0 && c.isRunning()) {
                    int steps = 20;
                    for (int i = 1; i <= steps; i++) {
                        synchronized (MUSIC_LOCK) {
                            if (gen != musicGen) {
                                return; // music was started again mid-fade
                            }
                            Voice.setGain(c, musicVolume * (1.0 - (double) i / steps));
                        }
                        Thread.sleep(Math.max(1, fadeMillis / steps));
                    }
                }
            } catch (Throwable ignored) {
            }
            synchronized (MUSIC_LOCK) {
                if (gen == musicGen) {
                    c.stop();
                    c.setFramePosition(0);
                }
            }
        }, "music-fade");
        t.setDaemon(true);
        t.start();
    }

    private static void pauseMusic() {
        synchronized (MUSIC_LOCK) {
            if (musicClip != null) {
                musicClip.stop(); // keeps its position, so unmuting resumes it
            }
        }
    }

    /** Caller must hold MUSIC_LOCK. */
    private static void playMusicLocked() {
        Clip c = musicClip;
        if (c == null) {
            return;
        }
        try {
            Voice.setGain(c, musicVolume);
            if (!c.isRunning()) {
                c.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Throwable ignored) {
            // never let audio break the game
        }
    }

    /** Loads the music file on a background thread; starts it if it was requested meanwhile. */
    private static void preloadMusic() {
        synchronized (MUSIC_LOCK) {
            if (musicLoadStarted) {
                return;
            }
            musicLoadStarted = true;
        }
        Thread t = new Thread(() -> {
            Clip c = loadMusicClip();
            synchronized (MUSIC_LOCK) {
                musicClip = c;
                if (musicWanted && !muted) {
                    playMusicLocked();
                }
            }
        }, "music-load");
        t.setDaemon(true);
        t.start();
    }

    private static Clip loadMusicClip() {
        try {
            File music = new File(AudioConfig.MUSIC_FILE);
            boolean packaged = !music.isFile() && core.Assets.exists(AudioConfig.MUSIC_FILE);
            if (!music.isFile() && !packaged) {
                // Not found: fall back to the first audio file in the same folder.
                File dir = music.getAbsoluteFile().getParentFile();
                File[] files = dir == null ? null : dir.listFiles((d, name) -> {
                    String n = name.toLowerCase();
                    return n.endsWith(".wav") || n.endsWith(".aif")
                            || n.endsWith(".aiff") || n.endsWith(".au");
                });
                if (files == null || files.length == 0) {
                    System.out.println("No background music found. Set MUSIC_FILE in "
                            + "audio/AudioConfig.java (now: " + AudioConfig.MUSIC_FILE + ").");
                    return null;
                }
                Arrays.sort(files);
                music = files[0];
                System.out.println("Music file " + AudioConfig.MUSIC_FILE
                        + " not found, using " + music.getName() + " instead.");
            }

            try (AudioInputStream raw = packaged
                    ? AudioSystem.getAudioInputStream(core.Assets.open(AudioConfig.MUSIC_FILE))
                    : AudioSystem.getAudioInputStream(music)) {
                AudioFormat src = raw.getFormat();
                AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        src.getSampleRate(), 16, src.getChannels(),
                        src.getChannels() * 2, src.getSampleRate(), false);
                try (AudioInputStream in = AudioSystem.isConversionSupported(pcm, src)
                        ? AudioSystem.getAudioInputStream(pcm, raw) : raw) {
                    AudioFormat f = in.getFormat();
                    byte[] data = in.readAllBytes();
                    int frames = data.length / f.getFrameSize();

                    // Seamless loop: blend the end of the track into its start.
                    int fade = (int) (AudioConfig.MUSIC_LOOP_CROSSFADE_MS / 1000.0
                            * f.getSampleRate());
                    fade = Math.min(fade, frames / 3);
                    boolean pcm16 = f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                            && f.getSampleSizeInBits() == 16 && !f.isBigEndian();
                    boolean blend = fade > 0 && pcm16;
                    if (blend) {
                        blendLoopTail(data, f, fade);
                    }

                    Clip c = AudioSystem.getClip();
                    c.open(f, data, 0, frames * f.getFrameSize());
                    if (blend) {
                        // Plays from the very start once, then repeats from after
                        // the intro, jumping from the blended end to the matching spot.
                        c.setLoopPoints(fade, -1);
                    }
                    System.out.println("Background music loaded: " + (packaged ? AudioConfig.MUSIC_FILE : music.getName()));
                    return c;
                }
            }
        } catch (Throwable ex) {
            System.out.println("Background music disabled: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Rewrites the last fadeFrames of the track so they crossfade into the first
     * fadeFrames (equal-power). Looping from the very last frame back to frame
     * fadeFrames then continues the music with no jump. Expects 16-bit signed
     * little-endian PCM and fadeFrames <= frames / 3.
     */
    static void blendLoopTail(byte[] d, AudioFormat f, int fadeFrames) {
        int channels = f.getChannels();
        int frameSize = f.getFrameSize();
        int total = d.length / frameSize;
        for (int j = 0; j < fadeFrames; j++) {
            double u = (j + 0.5) / fadeFrames;
            double gOut = Math.cos(u * Math.PI / 2);
            double gIn = Math.sin(u * Math.PI / 2);
            int tail = (total - fadeFrames + j) * frameSize;
            int head = j * frameSize;
            for (int ch = 0; ch < channels; ch++) {
                int a = tail + ch * 2;
                int b = head + ch * 2;
                int xt = (short) ((d[a] & 0xff) | (d[a + 1] << 8));
                int xh = (short) ((d[b] & 0xff) | (d[b + 1] << 8));
                long v = Math.round(xt * gOut + xh * gIn);
                v = Math.max(-32768, Math.min(32767, v));
                d[a] = (byte) (v & 0xff);
                d[a + 1] = (byte) ((v >> 8) & 0xff);
            }
        }
    }

    /** Plays the symbol-lock tone for button 0..3. */
    public static void playSymbol(int index) {
        Sfx[] tones = { Sfx.TONE1, Sfx.TONE2, Sfx.TONE3, Sfx.TONE4 };
        if (index >= 0 && index < tones.length) {
            play(tones[index]);
        }
    }

    /** Writes every synthesized sound to a .wav file (works without audio hardware). */
    public static void exportWavs(File dir) throws IOException {
        dir.mkdirs();
        for (Map.Entry<Sfx, byte[][]> e : synthesizeAll().entrySet()) {
            byte[][] variants = e.getValue();
            for (int i = 0; i < variants.length; i++) {
                byte[] pcm = variants[i];
                AudioInputStream ais = new AudioInputStream(
                        new ByteArrayInputStream(pcm), FORMAT, pcm.length / 2);
                File out = new File(dir, e.getKey().name().toLowerCase()
                        + (variants.length > 1 ? "_" + (i + 1) : "") + ".wav");
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
            }
        }
    }

    // =================================================
    // Voice: a small pool of Clips so sounds can overlap
    // =================================================

    private static int poolSize(Sfx s) {
        switch (s) {
            case TYPE:  return 4;
            case SWISH: return 3;
            case CLOCK:
            case GIGGLE:
            case BEEP:
            case HEART:
            case SPARKLE:
            case STEP:  return 2;
            case WALK:  return 4;
            case LOCK:
            case DING:
            case HOVER:
            case CLICK:
            case PAPER:
            case DOOR_OPEN: return 2;
            default:    return 1;
        }
    }

    private static double defaultVolume(Sfx s) {
        switch (s) {
            case TYPE:     return 0.45;
            case THUD:     return 0.90;
            case LOCK:     return 0.80;
            case CREAK:    return 0.60;
            case STEP:     return 0.55;
            case WALK:     return 0.50;
            case KEY_DROP: return 0.60;
            case KEY_PICKUP: return 0.55;
            case OUTDOORS: return 0.50;
            case CHIME:    return 0.70;
            case DING:     return 0.55;
            case HOVER:    return 0.35;
            case CLICK:    return 0.60;
            case DRONE:    return 0.35;
            case CRICKETS: return 0.22;
            case CLOCK:    return 0.35;
            case GIGGLE:   return 0.50;
            case SHUT:     return 0.60;
            case BEEP:     return 0.35;
            case TEAR:     return 0.55;
            case SWISH:    return 0.40;
            case HEART:    return 0.80;
            case RATTLE:   return 0.55;
            case SPARKLE:  return 0.35;
            case PAPER:     return 0.55;
            case SAFE_OPEN: return 0.80;
            case DOOR_OPEN: return 0.60;
            case ERROR:     return 0.45;
            case TONE1:
            case TONE2:
            case TONE3:
            case TONE4:     return 0.45;
            default:       return 0.6;
        }
    }

    private static final class Voice {
        private final Clip[] clips;
        private int next;
        private double baseVolume = 1.0;

        Voice(byte[][] variants, int pool) throws Exception {
            clips = new Clip[pool];
            for (int i = 0; i < pool; i++) {
                byte[] d = variants[i % variants.length];
                Clip c = AudioSystem.getClip();
                c.open(FORMAT, d, 0, d.length);
                clips[i] = c;
            }
        }

        synchronized void play(double volume, boolean loop) {
            Clip c = null;
            for (int i = 0; i < clips.length; i++) {
                Clip cand = clips[(next + i) % clips.length];
                if (!cand.isRunning()) {
                    c = cand;
                    next = (next + i + 1) % clips.length;
                    break;
                }
            }
            if (c == null) {
                c = clips[next];
                next = (next + 1) % clips.length;
            }
            c.stop();
            c.setFramePosition(0);
            baseVolume = volume;
            setGain(c, volume);
            if (loop) {
                c.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                c.start();
            }
        }

        /** Sets running clips to (volume at start) x factor. */
        synchronized void scaleGain(double factor) {
            for (Clip c : clips) {
                if (c.isRunning()) {
                    setGain(c, baseVolume * factor);
                }
            }
        }

        /** Sets the level of every running clip and remembers it as the base. */
        synchronized void setVolume(double volume) {
            baseVolume = volume;
            for (Clip c : clips) {
                if (c.isRunning()) {
                    setGain(c, volume);
                }
            }
        }

        synchronized void stop() {
            for (Clip c : clips) {
                c.stop();
            }
        }

        private static void setGain(Clip c, double volume) {
            if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl fc = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            double db = 20 * Math.log10(Math.max(volume, 0.0001));
            fc.setValue((float) Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), db)));
        }
    }

    // =================================================
    // Synthesis
    // =================================================

    private static Map<Sfx, byte[][]> synthesizeAll() {
        Map<Sfx, byte[][]> m = new EnumMap<>(Sfx.class);
        m.put(Sfx.TYPE, synthType());
        m.put(Sfx.THUD, new byte[][] { synthThud() });
        m.put(Sfx.LOCK, new byte[][] { synthLock() });
        m.put(Sfx.CREAK, new byte[][] { synthCreak() });
        m.put(Sfx.STEP, synthSteps());
        m.put(Sfx.OUTDOORS, new byte[][] { synthOutdoors() });
        m.put(Sfx.CHIME, new byte[][] { synthChime() });
        m.put(Sfx.DING, new byte[][] { synthDing() });
        m.put(Sfx.HOVER, new byte[][] { synthHover() });
        m.put(Sfx.CLICK, new byte[][] { synthClick() });
        m.put(Sfx.DRONE, new byte[][] { synthDrone() });
        m.put(Sfx.CRICKETS, new byte[][] { synthCrickets() });
        m.put(Sfx.CLOCK, synthClock());
        m.put(Sfx.GIGGLE, synthGiggle());
        m.put(Sfx.SHUT, new byte[][] { synthShut() });
        m.put(Sfx.BEEP, new byte[][] { synthBeep() });
        m.put(Sfx.TEAR, new byte[][] { synthTear() });
        m.put(Sfx.SWISH, new byte[][] { synthSwish() });
        m.put(Sfx.HEART, new byte[][] { synthHeart() });
        m.put(Sfx.RATTLE, new byte[][] { synthRattle() });
        m.put(Sfx.SPARKLE, new byte[][] { synthSparkle() });
        m.put(Sfx.PAPER, new byte[][] { synthPaper() });
        m.put(Sfx.SAFE_OPEN, new byte[][] { synthSafeOpen() });
        m.put(Sfx.DOOR_OPEN, new byte[][] { synthDoorOpen() });
        m.put(Sfx.ERROR, new byte[][] { synthError() });
        m.put(Sfx.WALK, synthWoodSteps());
        m.put(Sfx.KEY_DROP, new byte[][] { synthKeyDrop() });
        m.put(Sfx.KEY_PICKUP, new byte[][] { synthKeyPickup() });
        double[] toneHz = { 523.25, 659.25, 783.99, 1046.5 };
        Sfx[] toneSfx = { Sfx.TONE1, Sfx.TONE2, Sfx.TONE3, Sfx.TONE4 };
        for (int i = 0; i < toneSfx.length; i++) {
            m.put(toneSfx[i], new byte[][] { synthSymbolTone(toneHz[i]) });
        }
        return m;
    }

    // ----- Individual sounds -----

    private static byte[][] synthType() {
        double[] tone = { 1500, 1800, 2100, 1650 };
        double[] body = { 380, 420, 360, 400 };
        byte[][] out = new byte[4][];
        for (int k = 0; k < 4; k++) {
            double[] b = zeros(0.04);
            addNoiseBurst(b, 0, 0.003, 0.9, 100 + k, 1500);
            addTone(b, 0, tone[k], 0.5, 0.006);
            addTone(b, 0, body[k], 0.4, 0.010);
            out[k] = toBytes(b, 0.55, true);
        }
        return out;
    }

    private static byte[] synthThud() {
        double[] b = zeros(1.6);
        double phase = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / SR;
            double f = 34 + 60 * Math.exp(-t * 5);
            phase += f / SR;
            b[i] += Math.sin(2 * Math.PI * phase) * Math.exp(-t * 3.2);
        }
        double[] n = lowpass(noise(b.length, 7), 180);
        for (int i = 0; i < b.length; i++) {
            double t = i / SR;
            b[i] += n[i] * 2.5 * Math.exp(-t * 6);
        }
        return toBytes(b, 0.85, true);
    }

    private static byte[] synthLock() {
        double[] b = zeros(0.5);
        // first tick: bolt catches
        addNoiseBurst(b, 0.0, 0.002, 0.8, 11, 2500);
        addTone(b, 0.0, 2600, 0.5, 0.012);
        addTone(b, 0.0, 3900, 0.3, 0.008);
        // second tick + clunk: bolt slides home
        addNoiseBurst(b, 0.15, 0.003, 1.0, 12, 1200);
        addTone(b, 0.15, 1700, 0.6, 0.020);
        addTone(b, 0.15, 2500, 0.4, 0.015);
        addTone(b, 0.15, 150, 0.9, 0.060);
        addTone(b, 0.15, 95, 0.6, 0.090);
        return toBytes(b, 0.7, true);
    }

    private static byte[] synthCreak() {
        double dur = 2.6;
        int n = (int) (dur * SR);
        double[] jitter = lowpass(noise(n, 21), 25);
        normalize(jitter);

        double[] b = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double u = t / dur;
            double s = u * u * (3 - 2 * u);
            double f = 90 + 70 * s + 28 * jitter[i];
            phase += f / SR;
            double saw = 2 * (phase - Math.floor(phase)) - 1;
            double am = 0.55 + 0.45 * Math.sin(2 * Math.PI * (7 + 5 * u) * t + 3 * jitter[i]);
            b[i] = saw * am;
        }
        b = lowpass(lowpass(b, 1200), 1500);

        double[] hiss = lowpass(noise(n, 22), 2500);
        normalize(hiss);
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double env = Math.min(1, t / 0.25) * Math.min(1, (dur - t) / 0.5);
            b[i] = (b[i] * 3.0 + hiss[i] * 0.12) * env;
        }
        return toBytes(b, 0.5, true);
    }

    private static byte[][] synthSteps() {
        byte[][] out = new byte[2][];
        for (int k = 0; k < 2; k++) {
            double[] b = zeros(0.32);
            double phase = 0;
            for (int i = 0; i < b.length; i++) {
                double t = i / SR;
                double f = 48 + k * 6 + 85 * Math.exp(-t * 20);
                phase += f / SR;
                b[i] = Math.sin(2 * Math.PI * phase) * Math.exp(-t * 13) * 0.9;
            }
            double[] n = lowpass(noise(b.length, 31 + k), 350);
            for (int i = 0; i < b.length; i++) {
                b[i] += n[i] * 1.6 * Math.exp(-(i / SR) * 28);
            }
            out[k] = toBytes(b, 0.75, true);
        }
        return out;
    }

    private static byte[] synthOutdoors() {
        double dur = 8.0;
        int n = (int) (dur * SR);

        double[] wind = lowpass(noise(n, 41), 700);
        normalize(wind);
        double[] rustle = highpass(lowpass(noise(n, 42), 3500), 1200);
        normalize(rustle);

        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double swell = 0.45 + 0.35 * Math.sin(2 * Math.PI * 0.21 * t + 0.6)
                    + 0.20 * Math.sin(2 * Math.PI * 0.07 * t + 2.1);
            double leaf = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.33 * t);
            b[i] = wind[i] * 0.5 * swell + rustle[i] * 0.12 * leaf;
        }

        // Birds
        Random r = new Random(43);
        for (int p = 0; p < 11; p++) {
            double start = 0.8 + r.nextDouble() * 6.2;
            double f0 = 2600 + r.nextDouble() * 1700;
            int syll = 2 + r.nextInt(3);
            double amp = 0.10 + r.nextDouble() * 0.12;
            double t0 = start;
            for (int s = 0; s < syll; s++) {
                double len = 0.07 + r.nextDouble() * 0.05;
                double f1 = f0 * (r.nextBoolean() ? 0.8 : 1.25);
                addChirp(b, t0, len, f0, f1, amp);
                t0 += len + 0.05;
            }
        }

        for (int i = 0; i < n; i++) {
            double t = i / SR;
            b[i] *= Math.min(1, t / 1.5) * Math.min(1, (dur - t) / 1.8);
        }
        return toBytes(b, 0.45, true);
    }

    private static byte[] synthChime() {
        double[] b = zeros(3.2);
        addBell(b, 0.00, 784, 0.7, 1.0);
        addBell(b, 0.16, 1175, 0.7, 1.0);
        addBell(b, 0.32, 1568, 0.8, 1.0);
        return toBytes(b, 0.55, true);
    }

    private static byte[] synthDing() {
        double[] b = zeros(1.2);
        addBell(b, 0, 1319, 1.0, 0.7);
        return toBytes(b, 0.45, true);
    }

    private static byte[] synthHover() {
        double[] b = zeros(0.06);
        addTone(b, 0, 1300, 0.5, 0.012);
        return toBytes(b, 0.3, true);
    }

    private static byte[] synthClick() {
        double[] b = zeros(0.12);
        addNoiseBurst(b, 0, 0.004, 0.6, 51, 800);
        addTone(b, 0, 650, 0.7, 0.030);
        return toBytes(b, 0.6, true);
    }

    private static byte[] synthDrone() {
        // Exactly 8 s with whole cycles of every component, so it loops seamlessly.
        int n = (int) (8.0 * SR);
        int xf = (int) (0.5 * SR);

        double[] raw = noise(n + xf, 61);
        raw = lowpass(raw, 180);
        double[] nz = new double[n];
        for (int i = 0; i < n; i++) {
            nz[i] = raw[i];
        }
        for (int i = 0; i < xf; i++) {
            double a = (double) i / xf;
            nz[i] = raw[i] * a + raw[n + i] * (1 - a);
        }
        normalize(nz);

        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double swell = 0.78 + 0.22 * Math.sin(2 * Math.PI * t / 8.0);
            double tones = 0.50 * Math.sin(2 * Math.PI * 55.0 * t)
                    + 0.35 * Math.sin(2 * Math.PI * 55.5 * t)
                    + 0.20 * Math.sin(2 * Math.PI * 82.5 * t)
                    + 0.12 * Math.sin(2 * Math.PI * 110.25 * t);
            b[i] = tones * swell + nz[i] * 0.18;
        }
        return toBytes(b, 0.35, false);
    }

    // ----- Intro story sounds -----

    /** 6 s seamless loop of a few crickets (every period divides 6 s). */
    private static byte[] synthCrickets() {
        double dur = 6.0;
        double[] b = zeros(dur);
        double[] periods = { 0.5, 0.6, 0.75, 1.0, 1.2 };
        double[] freqs = { 4300, 4700, 5100, 4500, 4900 };
        Random r = new Random(71);
        for (int k = 0; k < periods.length; k++) {
            double p = periods[k];
            double off = r.nextDouble() * (p - 0.2);
            double amp = 0.35 + r.nextDouble() * 0.3;
            for (double t0 = off; t0 + 0.2 < dur; t0 += p) {
                for (int pulse = 0; pulse < 3; pulse++) {
                    addChirpPulse(b, t0 + pulse * 0.038, 0.022, freqs[k], amp);
                }
            }
        }
        return toBytes(b, 0.5, false);
    }

    private static void addChirpPulse(double[] b, double start, double len, double f, double amp) {
        int s = (int) (start * SR);
        int n = (int) (len * SR);
        for (int i = 0; i < n && s + i < b.length; i++) {
            double u = (double) i / n;
            double env = Math.sin(Math.PI * u);
            b[s + i] += amp * env * env * Math.sin(2 * Math.PI * f * (i / SR));
        }
    }

    /** Index 0 = tick, 1 = tock. */
    private static byte[][] synthClock() {
        byte[][] out = new byte[2][];
        double[] f = { 1900, 1300 };
        double[] body = { 700, 480 };
        for (int k = 0; k < 2; k++) {
            double[] b = zeros(0.12);
            addNoiseBurst(b, 0, 0.003, 0.9, 80 + k, 2000);
            addTone(b, 0, f[k], 0.6, 0.012);
            addTone(b, 0, body[k], 0.5, 0.020);
            out[k] = toBytes(b, 0.5, true);
        }
        return out;
    }

    /** Two kids: a higher and a lower giggle. */
    private static byte[][] synthGiggle() {
        double[] base = { 620, 470 };
        byte[][] out = new byte[2][];
        for (int k = 0; k < 2; k++) {
            double[] b = zeros(0.85);
            Random r = new Random(90 + k);
            for (int burst = 0; burst < 6; burst++) {
                double start = 0.02 + burst * 0.125;
                double len = 0.085;
                double amp = 1.0 - burst * 0.11;
                double f0 = base[k] * (1.0 + 0.08 * r.nextDouble() - 0.03 * burst);
                addVoicedBurst(b, start, len, f0, amp);
                addNoiseBurst(b, start, 0.012, 0.05 * amp, 120 + burst + k * 10, 3000);
            }
            out[k] = toBytes(b, 0.55, true);
        }
        return out;
    }

    /** A short "hee" with formants near 1.1 kHz and 3 kHz. */
    private static void addVoicedBurst(double[] b, double start, double len, double f0, double amp) {
        int s = (int) (start * SR);
        int n = (int) (len * SR);
        double[] phase = new double[16];
        for (int i = 0; i < n && s + i < b.length; i++) {
            double u = (double) i / n;
            double env = Math.pow(Math.sin(Math.PI * u), 1.3);
            double f = f0 * (0.92 + 0.16 * u);          // pitch glides up
            double v = 0;
            for (int h = 1; h < 16; h++) {
                double hf = h * f;
                double g = Math.exp(-Math.pow((hf - 1100) / 650, 2))
                        + 0.55 * Math.exp(-Math.pow((hf - 3000) / 800, 2));
                phase[h] += hf / SR;
                v += g * Math.sin(2 * Math.PI * phase[h]) / h;
            }
            b[s + i] += amp * env * v;
        }
    }

    private static byte[] synthShut() {
        double[] b = zeros(0.4);
        addTone(b, 0, 150, 0.9, 0.05);
        addTone(b, 0, 90, 0.7, 0.08);
        double[] n = lowpass(noise(b.length, 95), 700);
        for (int i = 0; i < b.length; i++) {
            b[i] += n[i] * 1.8 * Math.exp(-(i / SR) * 30);
        }
        return toBytes(b, 0.7, true);
    }

    private static byte[] synthBeep() {
        double[] b = zeros(0.16);
        addTone(b, 0, 1760, 0.8, 0.045);
        addTone(b, 0, 2640, 0.3, 0.030);
        return toBytes(b, 0.45, true);
    }

    private static byte[] synthTear() {
        double dur = 0.95;
        double[] b = zeros(dur);
        Random r = new Random(101);
        for (int i = 0; i < 70; i++) {
            double u = r.nextDouble();
            double t0 = Math.pow(u, 0.9) * 0.7;
            double amp = 0.3 + r.nextDouble() * 0.7;
            addNoiseBurst(b, t0, 0.004 + r.nextDouble() * 0.004, amp, 500 + i, 1500);
        }
        double[] body = lowpass(highpass(noise(b.length, 102), 500), 5000);
        for (int i = 0; i < b.length; i++) {
            double t = i / SR;
            double env = Math.min(1, t / 0.05) * Math.max(0, 1 - Math.max(0, t - 0.55) / 0.35);
            b[i] += body[i] * 0.9 * env;
        }
        return toBytes(b, 0.7, true);
    }

    private static byte[] synthSwish() {
        double dur = 0.38;
        double[] n = highpass(lowpass(noise((int) (dur * SR), 111), 3500), 500);
        double[] b = new double[n.length];
        for (int i = 0; i < n.length; i++) {
            double u = (double) i / n.length;
            double env = Math.pow(Math.sin(Math.PI * u), 2);
            b[i] = n[i] * env;
        }
        return toBytes(b, 0.5, true);
    }

    private static byte[] synthHeart() {
        double[] b = zeros(0.6);
        for (int beat = 0; beat < 2; beat++) {
            double start = beat * 0.22;
            double amp = beat == 0 ? 1.0 : 0.7;
            int s = (int) (start * SR);
            double phase = 0;
            for (int i = s; i < b.length; i++) {
                double t = (i - s) / SR;
                if (t > 0.3) {
                    break;
                }
                double f = 48 + 40 * Math.exp(-t * 30);
                phase += f / SR;
                b[i] += amp * Math.sin(2 * Math.PI * phase) * Math.exp(-t * 14) * Math.min(1, t / 0.004);
            }
        }
        return toBytes(b, 0.8, true);
    }

    private static byte[] synthRattle() {
        double[] b = zeros(0.8);
        double[] times = { 0.00, 0.07, 0.15, 0.26, 0.34, 0.47, 0.58 };
        for (int i = 0; i < times.length; i++) {
            double amp = 1.0 - i * 0.11;
            addNoiseBurst(b, times[i], 0.006, 0.7 * amp, 130 + i, 1500);
            addTone(b, times[i], 900 + 90 * (i % 3), 0.4 * amp, 0.035);
            addTone(b, times[i], 1500, 0.2 * amp, 0.025);
            addTone(b, times[i], 140, 0.5 * amp, 0.05);
        }
        return toBytes(b, 0.65, true);
    }

    private static byte[] synthSparkle() {
        double[] b = zeros(1.1);
        double[] notes = { 2093, 2637, 3136, 2349, 3951 };
        for (int i = 0; i < notes.length; i++) {
            addTone(b, i * 0.08, notes[i], 0.6, 0.16);
            addTone(b, i * 0.08, notes[i] * 2, 0.2, 0.08);
        }
        return toBytes(b, 0.4, true);
    }

    private static byte[] synthPaper() {
        double[] b = zeros(0.7);
        Random r = new Random(301);
        // lots of tiny crinkles scattered over the first half second
        for (int i = 0; i < 28; i++) {
            double t0 = r.nextDouble() * 0.5;
            double amp = 0.25 + r.nextDouble() * 0.6;
            addNoiseBurst(b, t0, 0.003 + r.nextDouble() * 0.005, amp, 600 + i, 2000);
        }
        // soft rustling body underneath
        double[] body = lowpass(highpass(noise(b.length, 302), 1500), 6000);
        for (int i = 0; i < b.length; i++) {
            double t = i / SR;
            double env = Math.min(1, t / 0.04) * Math.exp(-t / 0.28);
            b[i] += body[i] * 0.5 * env;
        }
        return toBytes(b, 0.6, true);
    }

    /** A creaking hinge: a lowpassed saw whose pitch drifts, added into b at start. */
    private static void addHinge(double[] b, double start, double dur,
                                 double f0, double f1, double amp, long seed) {
        int s = (int) (start * SR);
        int n = Math.min((int) (dur * SR), b.length - s);
        if (n <= 0) {
            return;
        }
        double[] jitter = lowpass(noise(n, seed), 25);
        normalize(jitter);
        double[] c = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double u = (double) i / n;
            double f = f0 + (f1 - f0) * u + 28 * jitter[i];
            phase += f / SR;
            double saw = 2 * (phase - Math.floor(phase)) - 1;
            c[i] = saw * Math.pow(Math.sin(Math.PI * u), 1.5);
        }
        c = lowpass(lowpass(c, 1400), 1800);
        for (int i = 0; i < n; i++) {
            b[s + i] += c[i] * amp;
        }
    }

    private static byte[] synthSafeOpen() {
        double[] b = zeros(1.5);
        // bolt ticks
        addNoiseBurst(b, 0.00, 0.002, 0.8, 311, 2500);
        addTone(b, 0.00, 2600, 0.4, 0.012);
        // heavy clunk as the bolt retracts
        addNoiseBurst(b, 0.18, 0.004, 1.0, 312, 900);
        addTone(b, 0.18, 120, 1.0, 0.08);
        addTone(b, 0.18, 80, 0.7, 0.11);
        // metal ringing
        addTone(b, 0.18, 520, 0.25, 0.18);
        addTone(b, 0.18, 790, 0.2, 0.15);
        addTone(b, 0.18, 1180, 0.12, 0.10);
        // thick door swinging open
        addHinge(b, 0.45, 0.8, 180, 300, 0.45, 313);
        return toBytes(b, 0.7, true);
    }

    private static byte[] synthDoorOpen() {
        double[] b = zeros(1.0);
        // latch click
        addNoiseBurst(b, 0.00, 0.003, 0.9, 321, 1800);
        addTone(b, 0.00, 1800, 0.5, 0.010);
        addTone(b, 0.02, 300, 0.7, 0.030);
        // short hinge creak
        addHinge(b, 0.08, 0.85, 95, 165, 1.0, 322);
        return toBytes(b, 0.55, true);
    }

    private static byte[] synthError() {
        double[] b = zeros(0.4);
        for (int k = 0; k < 2; k++) {
            int s = (int) (k * 0.17 * SR);
            int n = (int) (0.13 * SR);
            for (int i = 0; i < n && s + i < b.length; i++) {
                double t = i / SR;
                double env = Math.min(1, t / 0.005) * Math.min(1, (0.13 - t) / 0.02);
                double ph = 170 * t;
                double saw = 2 * (ph - Math.floor(ph)) - 1;
                b[s + i] += (saw * 0.6 + Math.sin(2 * Math.PI * 340 * t) * 0.2) * env;
            }
        }
        b = lowpass(b, 1200);
        return toBytes(b, 0.5, true);
    }

    private static byte[] synthSymbolTone(double hz) {
        double[] b = zeros(0.5);
        addTone(b, 0, hz, 0.8, 0.12);
        addTone(b, 0, hz * 2, 0.25, 0.07);
        addTone(b, 0, hz * 3, 0.10, 0.04);
        return toBytes(b, 0.55, true);
    }

    /** Footsteps on a wooden floor: heel knock + hollow board resonance + toe tap. 4 variants. */
    private static byte[][] synthWoodSteps() {
        byte[][] out = new byte[4][];
        double[] knock = { 165, 180, 155, 190 };   // board pitch differs a little per step
        double[] hollow = { 330, 350, 310, 370 };
        for (int k = 0; k < 4; k++) {
            double[] b = zeros(0.32);
            // heel strike: short bright click of shoe on wood
            addNoiseBurst(b, 0.000, 0.0025, 0.75, 700 + k, 1400);
            addTone(b, 0.000, 1900 + k * 120, 0.18, 0.004);
            // floorboard knock + hollow resonance underneath
            addTone(b, 0.002, knock[k], 0.95, 0.050);
            addTone(b, 0.002, hollow[k], 0.55, 0.030);
            addTone(b, 0.002, knock[k] * 2.9, 0.22, 0.018);
            // toe tap a moment later, softer
            addNoiseBurst(b, 0.070, 0.0025, 0.35, 710 + k, 1600);
            addTone(b, 0.072, knock[k] * 1.25, 0.40, 0.030);
            addTone(b, 0.072, hollow[k] * 1.1, 0.22, 0.020);
            // a touch of room around it
            double[] room = lowpass(noise(b.length, 720 + k), 900);
            for (int i = 0; i < b.length; i++) {
                double t = i / SR;
                b[i] += room[i] * 0.35 * Math.exp(-t / 0.07);
            }
            out[k] = toBytes(b, 0.7, true);
        }
        return out;
    }

    private static byte[] synthKeyDrop() {
        double[] b = zeros(0.6);
        // a small metal key bouncing on the floor: three hits, each softer and higher-pitched
        double[] t0 = { 0.00, 0.11, 0.18, 0.22 };
        double[] amp = { 1.0, 0.55, 0.30, 0.15 };
        for (int i = 0; i < t0.length; i++) {
            addNoiseBurst(b, t0[i], 0.002, 0.6 * amp[i], 800 + i, 2500);
            addTone(b, t0[i], 2300 + i * 260, 0.55 * amp[i], 0.045);
            addTone(b, t0[i], 3350 + i * 300, 0.35 * amp[i], 0.030);
            addTone(b, t0[i], 4700, 0.15 * amp[i], 0.020);
        }
        // the first landing also gives a little knock on the floor
        addTone(b, 0.00, 190, 0.7, 0.030);
        return toBytes(b, 0.65, true);
    }

    private static byte[] synthKeyPickup() {
        double[] b = zeros(0.55);
        // quick rising jingle of metal on metal
        double[] t0 = { 0.00, 0.045, 0.095, 0.15 };
        double[] f = { 2200, 2750, 3300, 2600 };
        for (int i = 0; i < t0.length; i++) {
            addNoiseBurst(b, t0[i], 0.002, 0.35, 850 + i, 3000);
            addTone(b, t0[i], f[i], 0.6, 0.060);
            addTone(b, t0[i], f[i] * 1.52, 0.30, 0.040);
            addTone(b, t0[i], f[i] * 2.3, 0.14, 0.025);
        }
        // soft rustle of it going into a pocket
        double[] r = lowpass(highpass(noise(b.length, 860), 800), 3500);
        for (int i = 0; i < b.length; i++) {
            double t = i / SR;
            double env = Math.min(1, t / 0.02) * Math.exp(-t / 0.10);
            b[i] += r[i] * 0.25 * env;
        }
        return toBytes(b, 0.6, true);
    }

    // =================================================
    // DSP helpers
    // =================================================

    private static double[] zeros(double seconds) {
        return new double[(int) (seconds * SR)];
    }

    private static double[] noise(int n, long seed) {
        Random r = new Random(seed);
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            d[i] = r.nextDouble() * 2 - 1;
        }
        return d;
    }

    private static double[] lowpass(double[] x, double cutoff) {
        double a = 1 - Math.exp(-2 * Math.PI * cutoff / SR);
        double[] y = new double[x.length];
        double s = 0;
        for (int i = 0; i < x.length; i++) {
            s += a * (x[i] - s);
            y[i] = s;
        }
        return y;
    }

    private static double[] highpass(double[] x, double cutoff) {
        double[] lp = lowpass(x, cutoff);
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            y[i] = x[i] - lp[i];
        }
        return y;
    }

    private static void normalize(double[] x) {
        double max = 0;
        for (double v : x) {
            max = Math.max(max, Math.abs(v));
        }
        if (max > 0) {
            for (int i = 0; i < x.length; i++) {
                x[i] /= max;
            }
        }
    }

    private static void addTone(double[] b, double start, double freq, double amp, double decay) {
        int s = (int) (start * SR);
        for (int i = s; i < b.length; i++) {
            double t = (i - s) / SR;
            if (t > decay * 9) {
                break;
            }
            double env = Math.exp(-t / decay) * Math.min(1, t / 0.002);
            b[i] += amp * env * Math.sin(2 * Math.PI * freq * t);
        }
    }

    private static void addNoiseBurst(double[] b, double start, double decay, double amp,
                                      long seed, double highpassHz) {
        int s = (int) (start * SR);
        int len = Math.min(b.length - s, (int) (decay * 9 * SR));
        if (len <= 0) {
            return;
        }
        double[] n = highpass(noise(len, seed), highpassHz);
        for (int i = 0; i < len; i++) {
            double t = i / SR;
            b[s + i] += amp * n[i] * Math.exp(-t / decay);
        }
    }

    private static void addBell(double[] b, double start, double freq, double amp, double decayScale) {
        double[] ratio = { 1.0, 2.0, 2.76, 5.4 };
        double[] a = { 1.0, 0.45, 0.28, 0.10 };
        double[] d = { 1.7, 1.1, 0.7, 0.35 };
        for (int k = 0; k < 4; k++) {
            addTone(b, start, freq * ratio[k], amp * a[k], d[k] * decayScale);
        }
    }

    private static void addChirp(double[] b, double start, double len, double f0, double f1, double amp) {
        int s = (int) (start * SR);
        int n = (int) (len * SR);
        double phase = 0;
        for (int i = 0; i < n && s + i < b.length; i++) {
            double u = (double) i / n;
            double f = f0 + (f1 - f0) * u + 60 * Math.sin(2 * Math.PI * 38 * (i / SR));
            phase += f / SR;
            double env = Math.pow(Math.sin(Math.PI * u), 1.5);
            b[s + i] += amp * env * Math.sin(2 * Math.PI * phase);
        }
    }

    /** Scales to the given peak and converts to 16-bit little-endian PCM. */
    private static byte[] toBytes(double[] x, double peak, boolean edgeFade) {
        double max = 0;
        for (double v : x) {
            max = Math.max(max, Math.abs(v));
        }
        double scale = max > 0 ? peak / max : 0;
        int fade = edgeFade ? (int) (0.003 * SR) : 0;

        byte[] out = new byte[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            double v = x[i] * scale;
            if (fade > 0) {
                if (i < fade) {
                    v *= (double) i / fade;
                } else if (i >= x.length - fade) {
                    v *= (double) (x.length - 1 - i) / fade;
                }
            }
            int s = (int) Math.round(Math.max(-1, Math.min(1, v)) * 32767);
            out[2 * i] = (byte) (s & 0xff);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }
}
