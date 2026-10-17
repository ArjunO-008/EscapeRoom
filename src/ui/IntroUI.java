package ui;

import audio.SoundManager;
import audio.SoundManager.Sfx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Story introduction.
 *
 * The title appears with a low hit and the story is typed out line by line
 * while an animated scene (see IntroScenes) plays behind it: a quiet evening,
 * giggling kids and a lock clicking shut, the stairs, the safe, the torn code,
 * and finally the mission. Every beat has matching procedural sound effects
 * (crickets, clock, giggles, footsteps, paper tearing, heartbeat...).
 * Click / ENTER skips the typing; the BEGIN button appears at the end.
 *
 * GamePanel calls:
 *   start()                       when the intro begins
 *   draw(g, w, h)                 every frame while in the INTRO state
 *   isBeginButtonClicked(x, y)    on mouse press
 *   skipTyping() / isFinished()   on click / ENTER
 *   handleMouseMove(x, y)         for the hover effect
 *   finish()                      when the player begins the game
 */
public class IntroUI {

    // ---------- Colors ----------
    private static final Color PARCHMENT = new Color(235, 218, 180);
    private static final Color GOLD_LIGHT = new Color(255, 232, 130);
    private static final Color GOLD_DARK = new Color(255, 165, 50);
    private static final Color WOOD = new Color(105, 65, 38);

    // ---------- Text ----------
    private static final String TITLE = "A Little Trouble at Home";

    private static final int NORMAL = 0;
    private static final int HEADER = 1;
    private static final int MISSION = 2;

    // text, style, gap in px before the line (> 0 starts a new paragraph)
    private static final Object[][] SCRIPT = {
        { "It was supposed to be a quiet evening.", NORMAL, 0 },

        { "The kids were hiding something. You heard giggling,", NORMAL, 14 },
        { "then the click of a lock, then silence.", NORMAL, 0 },

        { "By the time you got downstairs, the front door key was gone,", NORMAL, 14 },
        { "locked inside the new safe behind a four-digit code.", NORMAL, 0 },

        { "Only the children knew the code. So they tore it into pieces", NORMAL, 14 },
        { "and hid every piece somewhere in the house.", NORMAL, 0 },

        { "The door is locked. The safe is silent. The pieces are out there.", NORMAL, 14 },

        { "YOUR MISSION", HEADER, 30 },
        { "Search every room. Piece the code together.", MISSION, 0 },
        { "Open the safe. Walk out that door.", MISSION, 0 },
    };

    // ---------- Timeline (seconds) ----------
    private static final double DARKEN_TIME = 1.2;
    private static final double THUD_TIME = 0.45;
    private static final double TITLE_START = 0.4;
    private static final double TITLE_FADE = 1.3;
    private static final double TEXT_START = 2.4;
    private static final double CHARS_PER_SEC = 50;
    private static final double ACCEPT_INPUT_AFTER = 0.6;

    private static final int BUTTON_W = 280;
    private static final int BUTTON_H = 46;

    // ---------- State ----------
    private static final class Line {
        String text;
        int style;
        int gap;
        float[] times;   // reveal time of each char (seconds after TEXT_START)
        int x;
        int y;
        int firstChar;   // index of the line's first char in the flat arrays
    }

    private final List<Line> lines = new ArrayList<>();
    private float[] allTimes;
    private boolean[] audible;
    private double lastCharTime;

    private final double[] moteX = new double[26];
    private final double[] moteY = new double[26];
    private final double[] moteVx = new double[26];
    private final double[] moteVy = new double[26];
    private final double[] motePhase = new double[26];

    // ---------- Animated story ----------
    private final IntroScenes scenes = new IntroScenes();

    private static final class Cue {
        final double time;
        final Sfx sfx;
        final double volume;
        boolean fired;

        Cue(double time, Sfx sfx, double volume) {
            this.time = time;
            this.sfx = sfx;
            this.volume = volume;
        }
    }

    /** A looping ambience whose level follows the story. */
    private static final class Ambience {
        final Sfx sfx;
        boolean on;
        double level;

        Ambience(Sfx sfx) {
            this.sfx = sfx;
        }
    }

    private static final Sfx[] ONE_SHOTS = {
        Sfx.GIGGLE, Sfx.SHUT, Sfx.LOCK, Sfx.STEP, Sfx.BEEP, Sfx.TEAR, Sfx.SWISH,
        Sfx.HEART, Sfx.RATTLE, Sfx.SPARKLE, Sfx.CHIME, Sfx.CREAK, Sfx.CLOCK
    };

    private final List<Cue> cues = new ArrayList<>();
    private final Ambience crickets = new Ambience(Sfx.CRICKETS);
    private final Ambience drone = new Ambience(Sfx.DRONE);
    private int lastTick;

    private long startNanos;
    private boolean laidOut;
    private int layoutW = -1;
    private int lastVisible;
    private boolean thudPlayed;
    private boolean dingPlayed;
    private boolean hovering;

    private int buttonX;
    private int buttonY;
    private double buttonAlpha;

    public IntroUI() {
        buildSchedule();
        buildTimeline();
        Random r = new Random(5);
        for (int i = 0; i < moteX.length; i++) {
            moteX[i] = r.nextDouble() * 960;
            moteY[i] = r.nextDouble() * 640;
            moteVx[i] = (r.nextDouble() - 0.5) * 8;
            moteVy[i] = -(4 + r.nextDouble() * 9);
            motePhase[i] = r.nextDouble() * Math.PI * 2;
        }
    }

    // =================================================
    // Lifecycle / input helpers
    // =================================================

    /** Restarts the animation and starts the ambient drone. */
    public void start() {
        startNanos = System.nanoTime();
        lastVisible = 0;
        thudPlayed = false;
        dingPlayed = false;
        hovering = false;
        buttonAlpha = 0;

        stopAllIntroSounds();
        for (Cue c : cues) {
            c.fired = false;
        }
        lastTick = 0;
        crickets.on = false;
        drone.on = false;
    }

    /** Called when the player begins the game. */
    public void finish() {
        SoundManager.play(Sfx.CLICK);
        SoundManager.fadeOut(Sfx.DRONE, 900);
        SoundManager.fadeOut(Sfx.CRICKETS, 600);
        stopOneShots();
        crickets.on = false;
        drone.on = false;
    }

    private void stopOneShots() {
        for (Sfx s : ONE_SHOTS) {
            SoundManager.stop(s);
        }
    }

    private void stopAllIntroSounds() {
        stopOneShots();
        SoundManager.stop(Sfx.CRICKETS);
        SoundManager.stop(Sfx.DRONE);
    }

    /** Jumps to the end of the typing so the BEGIN button shows. */
    public void skipTyping() {
        if (!isFinished()) {
            double target = TEXT_START + lastCharTime + 0.05;
            startNanos = System.nanoTime() - (long) (target * 1e9);
            lastVisible = allTimes.length;     // no burst of typing sounds
            thudPlayed = true;

            // Jump straight to the final scene without replaying its sounds.
            for (Cue c : cues) {
                c.fired = true;
            }
            lastTick = Integer.MAX_VALUE;
            stopOneShots();
        }
    }

    public boolean isFinished() {
        return elapsed() >= TEXT_START + lastCharTime + 0.2;
    }

    /** Ignores input for a moment so the keypress that opened the intro can't skip it. */
    public boolean isAcceptingInput() {
        return elapsed() >= ACCEPT_INPUT_AFTER;
    }

    public boolean isBeginButtonClicked(int mouseX, int mouseY) {
        return buttonAlpha > 0.6
                && new Rectangle(buttonX, buttonY, BUTTON_W, BUTTON_H)
                        .contains(mouseX, mouseY);
    }

    public void handleMouseMove(int mouseX, int mouseY) {
        boolean was = hovering;
        hovering = buttonAlpha > 0.6
                && new Rectangle(buttonX, buttonY, BUTTON_W, BUTTON_H)
                        .contains(mouseX, mouseY);
        if (hovering && !was) {
            SoundManager.play(Sfx.HOVER);
        }
    }

    public boolean isHovering() {
        return hovering;
    }

    private double elapsed() {
        return startNanos == 0 ? 0 : (System.nanoTime() - startNanos) / 1e9;
    }

    // =================================================
    // Typewriter schedule
    // =================================================

    private void buildSchedule() {

        double time = 0;
        int flat = 0;
        List<Float> flatTimes = new ArrayList<>();
        List<Boolean> flatAudible = new ArrayList<>();

        for (Object[] row : SCRIPT) {
            Line line = new Line();
            line.text = (String) row[0];
            line.style = (Integer) row[1];
            line.gap = (Integer) row[2];
            line.times = new float[line.text.length()];
            line.firstChar = flat;

            if (line.gap > 0) {
                time += 0.45; // breath between paragraphs
            }

            for (int i = 0; i < line.text.length(); i++) {
                char c = line.text.charAt(i);
                time += 1.0 / CHARS_PER_SEC;
                line.times[i] = (float) time;
                flatTimes.add((float) time);
                flatAudible.add(c != ' ');
                flat++;

                if (c == '.') {
                    time += 0.28;
                } else if (c == ',') {
                    time += 0.12;
                }
            }
            time += 0.12;
            lines.add(line);
        }

        allTimes = new float[flatTimes.size()];
        audible = new boolean[flatTimes.size()];
        for (int i = 0; i < allTimes.length; i++) {
            allTimes[i] = flatTimes.get(i);
            audible[i] = flatAudible.get(i);
        }
        lastCharTime = allTimes.length > 0 ? allTimes[allTimes.length - 1] : 0;
    }

    private void layout(Graphics2D g, int w) {

        int y = 152;
        for (Line line : lines) {
            y += line.gap;
            Font f = fontFor(line.style);
            FontMetrics fm = g.getFontMetrics(f);
            int width = fm.stringWidth(line.text);
            if (line.style == HEADER) {
                width = headerWidth(fm, line.text);
            }
            line.x = (w - width) / 2;
            line.y = y;
            y += 28;
        }
        laidOut = true;
        layoutW = w;
    }

    private static Font fontFor(int style) {
        switch (style) {
            case HEADER:  return new Font("Serif", Font.BOLD, 17);
            case MISSION: return new Font("Serif", Font.BOLD, 20);
            default:      return new Font("Serif", Font.PLAIN, 21);
        }
    }

    private static int headerWidth(FontMetrics fm, String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += fm.charWidth(c) + 5;
        }
        return w - 5;
    }

    // =================================================
    // Drawing
    // =================================================

    public void draw(Graphics2D original, int w, int h) {

        if (startNanos == 0) {
            start();
        }

        Graphics2D g = (Graphics2D) original.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (!laidOut || layoutW != w) {
                layout(g, w);
            }

            double t = elapsed();
            double tt = t - TEXT_START;

            if (!thudPlayed && t >= THUD_TIME) {
                thudPlayed = true;
                SoundManager.play(Sfx.THUD);
            }

            drawBackdrop(g, w, h, t);
            drawMotes(g, w, h, t);
            fireCues(t);
            updateAmbience(t);
            drawTitle(g, w, t);
            int visible = drawStory(g, tt);
            playTypingSounds(visible);

            drawButtonAndHints(g, w, h, t, tt);

        } finally {
            g.dispose();
        }
    }

    private void drawBackdrop(Graphics2D g, int w, int h, double t) {

        // Animated scene for the part of the story being told (cross-fades).
        int c = 0;
        for (int i = 1; i < scenes.sceneStart.length; i++) {
            if (t >= scenes.sceneStart[i]) {
                c = i;
            }
        }
        double a = c == 0 ? 1 : smooth((t - scenes.sceneStart[c]) / 1.0);
        if (c > 0 && a < 1) {
            scenes.draw(g, c - 1, w, h, t, 1.0);
        }
        scenes.draw(g, c, w, h, t, a);

        // Fade in from black at the very start.
        double fade = 1 - smooth(t / 1.4);
        if (fade > 0.002) {
            g.setColor(rgba(0, 0, 0, fade));
            g.fillRect(0, 0, w, h);
        }

        // Soft dark band behind the text so it stays readable,
        // while the scene stays bright at the left and right edges.
        double text = smooth((t - 0.4) / 1.2);
        g.setPaint(new LinearGradientPaint(0, 0, w, 0,
                new float[] { 0f, 0.13f, 0.30f, 0.70f, 0.87f, 1f },
                new Color[] { rgba(5, 3, 8, 0), rgba(5, 3, 8, 0), rgba(5, 3, 8, 0.58 * text),
                        rgba(5, 3, 8, 0.58 * text), rgba(5, 3, 8, 0), rgba(5, 3, 8, 0) }));
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, 0, rgba(5, 3, 8, 0.45 * text), 0, 160, rgba(5, 3, 8, 0)));
        g.fillRect(0, 0, w, 160);

        // Vignette
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f), (float) (Math.max(w, h) * 0.78),
                new float[] { 0.5f, 1f },
                new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.45) }));
        g.fillRect(0, 0, w, h);
    }

    // =================================================
    // Story timeline: scene changes, sound cues, ambience
    // =================================================

    /** Time (seconds after TEXT_START) at which {@code part} of a line starts being typed. */
    private double at(int lineIndex, String part) {
        Line l = lines.get(lineIndex);
        int idx = l.text.indexOf(part);
        if (idx < 0) {
            idx = 0;
        }
        return TEXT_START + l.times[idx];
    }

    private double lineStart(int lineIndex) {
        return TEXT_START + lines.get(lineIndex).times[0];
    }

    private void buildTimeline() {

        double[] m = scenes.mark;
        m[IntroScenes.M_GIGGLE] = at(1, "giggling");
        m[IntroScenes.M_LOCK] = at(2, "click");
        m[IntroScenes.M_SILENCE] = at(2, "silence");
        m[IntroScenes.M_STAIRS] = at(3, "By") + 0.1;
        m[IntroScenes.M_KEYGONE] = at(3, "key was gone");
        m[IntroScenes.M_SAFE] = at(4, "safe");
        m[IntroScenes.M_DIGITS] = at(4, "four-digit");
        m[IntroScenes.M_TORE] = at(5, "tore");
        m[IntroScenes.M_HID] = at(6, "hid");
        m[IntroScenes.M_DOORLOCKED] = at(7, "locked");
        m[IntroScenes.M_SAFESILENT] = at(7, "silent");
        m[IntroScenes.M_PIECES] = at(7, "pieces are");
        m[IntroScenes.M_MISSION] = at(8, "YOUR");
        m[IntroScenes.M_SEARCH] = at(9, "Search");
        m[IntroScenes.M_PIECE] = at(9, "Piece");
        m[IntroScenes.M_OPEN] = at(10, "Open");
        m[IntroScenes.M_WALK] = at(10, "Walk");

        double[] s = scenes.sceneStart;
        s[0] = -1;
        s[1] = lineStart(1) - 0.5;
        s[2] = lineStart(3) - 0.5;
        s[3] = lineStart(5) - 0.5;
        s[4] = lineStart(7) - 0.5;
        s[5] = lineStart(8) - 0.5;

        m[IntroScenes.M_BEAT] = s[4] + 0.5;
        m[IntroScenes.M_BEATEND] = s[5] - 0.25;

        // ----- sound cues, in step with the pictures -----
        cue(m[IntroScenes.M_GIGGLE], Sfx.GIGGLE, 0.9);
        cue(m[IntroScenes.M_GIGGLE] + IntroScenes.GIGGLE_GAP, Sfx.GIGGLE, 0.7);
        cue(m[IntroScenes.M_LOCK] - 0.25, Sfx.SHUT, 0.9);
        cue(m[IntroScenes.M_LOCK], Sfx.LOCK, 1.0);

        for (int i = 0; i < 6; i++) {
            cue(m[IntroScenes.M_STAIRS] + IntroScenes.STEP_PERIOD * (i + 1), Sfx.STEP, i % 2 == 0 ? 1.0 : 0.85);
        }
        cue(m[IntroScenes.M_SAFE], Sfx.SHUT, 1.0);
        for (int i = 0; i < 4; i++) {
            cue(m[IntroScenes.M_DIGITS] + i * 0.28, Sfx.BEEP, 0.8 + 0.06 * i);
        }

        cue(m[IntroScenes.M_TORE], Sfx.TEAR, 1.0);
        for (int i = 0; i < 4; i++) {
            cue(m[IntroScenes.M_HID] + i * 0.22, Sfx.SWISH, 0.8);
        }
        cue(m[IntroScenes.M_HID] + 0.1, Sfx.GIGGLE, 0.6);

        cue(m[IntroScenes.M_DOORLOCKED], Sfx.RATTLE, 1.0);
        cue(m[IntroScenes.M_PIECES], Sfx.SPARKLE, 0.9);
        for (double b = m[IntroScenes.M_BEAT]; b < m[IntroScenes.M_BEATEND]; b += IntroScenes.BEAT_PERIOD) {
            cue(b, Sfx.HEART, 1.0);
        }

        cue(m[IntroScenes.M_MISSION], Sfx.CHIME, 0.55);
        for (int i = 0; i < 4; i++) {
            cue(m[IntroScenes.M_PIECE] + i * 0.28, Sfx.SWISH, 0.7);
        }
        cue(m[IntroScenes.M_PIECE] + 3 * 0.28 + 1.0, Sfx.SPARKLE, 1.0);
        cue(m[IntroScenes.M_OPEN], Sfx.LOCK, 1.0);
        cue(m[IntroScenes.M_OPEN] + 0.7, Sfx.SPARKLE, 0.8);
        cue(m[IntroScenes.M_WALK], Sfx.CREAK, 0.45);
    }

    private void cue(double time, Sfx sfx, double volume) {
        cues.add(new Cue(time, sfx, volume));
    }

    private void fireCues(double t) {
        for (Cue c : cues) {
            if (!c.fired && t >= c.time) {
                c.fired = true;
                SoundManager.play(c.sfx, c.volume);
            }
        }

        // The clock ticks once a second until the house goes silent.
        double[] m = scenes.mark;
        if (t >= 1.0 && t < m[IntroScenes.M_SILENCE]) {
            int tick = (int) Math.floor(t);
            if (tick > lastTick) {
                lastTick = tick;
                SoundManager.play(Sfx.CLOCK);
            }
        }
    }

    /** Crickets until "silence", then a low drone that swells with the tension. */
    private void updateAmbience(double t) {

        double[] m = scenes.mark;
        double[] s = scenes.sceneStart;

        double cr = smooth(t / 2.0)
                * (1 - smooth((t - m[IntroScenes.M_SILENCE]) / 0.45));
        applyAmbience(crickets, cr);

        double d;
        if (t < s[2]) {
            d = 0;
        } else if (t < s[2] + 2.0) {
            d = 0.55 * (t - s[2]) / 2.0;
        } else if (t < s[3] + 1.0) {
            d = lerp(0.55, 0.8, (t - s[2] - 2.0) / Math.max(0.1, s[3] + 1.0 - s[2] - 2.0));
        } else if (t < s[4] + 0.5) {
            d = lerp(0.8, 1.0, (t - s[3] - 1.0) / Math.max(0.1, s[4] + 0.5 - s[3] - 1.0));
        } else if (t < s[5] + 0.5) {
            d = lerp(1.0, 0.4, (t - s[4] - 0.5) / Math.max(0.1, s[5] - s[4]));
        } else {
            d = 0.4;
        }
        applyAmbience(drone, clamp(d, 0, 1));
    }

    private void applyAmbience(Ambience a, double level) {

        if (SoundManager.isMuted() || !SoundManager.isReady()) {
            a.on = false;
            return;
        }
        if (level > 0.01) {
            if (!a.on) {
                SoundManager.play(a.sfx, level);
                a.on = true;
                a.level = level;
            } else if (Math.abs(level - a.level) > 0.015) {
                SoundManager.setVolume(a.sfx, level);
                a.level = level;
            }
        } else if (a.on) {
            SoundManager.stop(a.sfx);
            a.on = false;
        }
    }

    private void drawMotes(Graphics2D g, int w, int h, double t) {
        double fade = smooth((t - 0.8) / 1.5);
        for (int i = 0; i < moteX.length; i++) {
            double x = mod(moteX[i] + moteVx[i] * t + Math.sin(t * 0.6 + motePhase[i]) * 10, w);
            double y = mod(moteY[i] + moteVy[i] * t, h);
            double tw = 0.5 + 0.5 * Math.sin(t * 1.4 + motePhase[i]);
            g.setColor(rgba(255, 230, 180, (0.08 + 0.25 * tw) * fade));
            g.fill(new Ellipse2D.Double(x, y, 2.2, 2.2));
        }
    }

    private void drawTitle(Graphics2D g, int w, double t) {

        double a = smooth((t - TITLE_START) / TITLE_FADE);
        if (a <= 0) {
            return;
        }

        Font f = new Font("Serif", Font.BOLD, 40);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        // Letters start spread out and settle together
        double spacing = (1 - a) * 12;
        double total = 0;
        for (char c : TITLE.toCharArray()) {
            total += fm.charWidth(c) + spacing;
        }
        total -= spacing;

        double x = (w - total) / 2;
        int base = 98;

        for (char c : TITLE.toCharArray()) {
            String s = String.valueOf(c);
            g.setColor(rgba(0, 0, 0, 0.55 * a));
            g.drawString(s, (float) x + 3, base + 3);
            g.setPaint(new GradientPaint(0, base - 30,
                    rgba(255, 232, 130, a), 0, base, rgba(255, 165, 50, a)));
            g.drawString(s, (float) x, base);
            x += fm.charWidth(c) + spacing;
        }

        // Ornament that grows out from the centre
        double p = smooth((t - (TITLE_START + 1.0)) / 0.8);
        if (p > 0) {
            int cx = w / 2;
            int y = base + 20;
            int half = (int) (170 * p);
            g.setStroke(new BasicStroke(2f));
            g.setColor(rgba(255, 165, 50, 0.85 * p));
            g.drawLine(cx - half, y, cx - 10, y);
            g.drawLine(cx + 10, y, cx + half, y);
            AffineTransform old = g.getTransform();
            g.translate(cx, y);
            g.rotate(Math.PI / 4);
            g.setColor(rgba(255, 232, 130, p));
            g.fillRect(-4, -4, 8, 8);
            g.setTransform(old);
        }
    }

    /** Draws the typed text and returns how many characters are visible. */
    private int drawStory(Graphics2D g, double tt) {

        if (tt < 0) {
            return 0;
        }

        int visibleTotal = 0;
        Line cursorLine = null;
        int cursorChars = 0;

        for (Line line : lines) {

            int n = 0;
            while (n < line.times.length && line.times[n] <= tt) {
                n++;
            }
            visibleTotal += n;

            if (n == 0) {
                continue;
            }
            cursorLine = line;
            cursorChars = n;

            Font f = fontFor(line.style);
            g.setFont(f);
            String shown = line.text.substring(0, n);

            if (line.style == HEADER) {
                drawHeader(g, shown, line.x, line.y);
            } else {
                Color c = line.style == MISSION ? GOLD_LIGHT : PARCHMENT;
                g.setColor(rgba(0, 0, 0, 0.6));
                g.drawString(shown, line.x + 2, line.y + 2);
                g.setColor(c);
                g.drawString(shown, line.x, line.y);
            }
        }

        // Blinking cursor while typing
        if (!isFinished() && cursorLine != null) {
            Font f = fontFor(cursorLine.style);
            FontMetrics fm = g.getFontMetrics(f);
            String shown = cursorLine.text.substring(0, cursorChars);
            int cx = cursorLine.x + (cursorLine.style == HEADER
                    ? headerWidth(fm, shown) + 6
                    : fm.stringWidth(shown) + 3);
            boolean on = Math.sin(tt * 9) > -0.2;
            if (on) {
                g.setColor(rgba(255, 220, 150, 0.9));
                g.fillRect(cx, cursorLine.y - fm.getAscent() + 3, 2, fm.getAscent() - 2);
            }
        }

        return visibleTotal;
    }

    private void drawHeader(Graphics2D g, String s, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(rgba(255, 190, 100, 1));
        for (char c : s.toCharArray()) {
            g.drawString(String.valueOf(c), x, y);
            x += fm.charWidth(c) + 5;
        }
    }

    private void playTypingSounds(int visible) {
        if (visible <= lastVisible) {
            return;
        }
        int played = 0;
        for (int i = lastVisible; i < visible && i < audible.length; i++) {
            if (audible[i] && i % 2 == 0 && played < 3) {
                SoundManager.play(Sfx.TYPE);
                played++;
            }
        }
        lastVisible = visible;
    }

    private void drawButtonAndHints(Graphics2D g, int w, int h, double t, double tt) {

        buttonAlpha = smooth((tt - lastCharTime - 0.2) / 0.5);

        if (buttonAlpha > 0.05 && !dingPlayed) {
            dingPlayed = true;
            SoundManager.play(Sfx.DING);
        }

        buttonX = (w - BUTTON_W) / 2;
        buttonY = h - BUTTON_H - 46;

        if (buttonAlpha > 0) {
            double pulse = 1 + 0.02 * Math.sin(t * 4);
            double scale = (hovering ? 1.06 : pulse) * (0.88 + 0.12 * buttonAlpha);

            AffineTransform old = g.getTransform();
            g.translate(w / 2.0, buttonY + BUTTON_H / 2.0);
            g.scale(scale, scale);

            RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                    -BUTTON_W / 2.0, -BUTTON_H / 2.0, BUTTON_W, BUTTON_H, 14, 14);

            int glow = hovering ? 90 : (int) (36 + 28 * (0.5 + 0.5 * Math.sin(t * 4)));
            for (int i = 3; i >= 1; i--) {
                g.setColor(rgba(255, 190, 70, glow * buttonAlpha / i / 255.0));
                g.setStroke(new BasicStroke(i * 5f));
                g.draw(shape);
            }

            Color top = hovering ? new Color(160, 105, 60) : new Color(125, 78, 46);
            Color bottom = hovering ? new Color(110, 66, 36) : WOOD;
            g.setPaint(new GradientPaint(0, -BUTTON_H / 2f, withAlpha(top, buttonAlpha),
                    0, BUTTON_H / 2f, withAlpha(bottom, buttonAlpha)));
            g.fill(shape);

            g.setStroke(new BasicStroke(2f));
            g.setColor(withAlpha(hovering ? GOLD_LIGHT : PARCHMENT, buttonAlpha));
            g.draw(shape);

            g.setFont(new Font("Serif", Font.BOLD, 19));
            FontMetrics fm = g.getFontMetrics();
            String label = "BEGIN EXPLORATION";
            g.setColor(withAlpha(hovering ? Color.WHITE : PARCHMENT, buttonAlpha));
            g.drawString(label, -fm.stringWidth(label) / 2, fm.getAscent() / 2 - 1);

            g.setTransform(old);
        }

        // Footer hint
        g.setFont(new Font("Serif", Font.ITALIC, 14));
        String hint = null;
        double alpha = 0.5;
        if (tt > 0.3 && !isFinished()) {
            hint = "Click or press ENTER to skip";
        } else if (isFinished()) {
            hint = "Press ENTER to begin";
            alpha = 0.45 + 0.4 * (0.5 + 0.5 * Math.sin(t * 3));
        }
        if (hint != null) {
            int tw = g.getFontMetrics().stringWidth(hint);
            g.setColor(rgba(235, 218, 180, alpha));
            g.drawString(hint, (w - tw) / 2, h - 18);
        }
    }

    // =================================================
    // Helpers
    // =================================================

    private static Color rgba(int r, int g, int b, double a) {
        return new Color(r, g, b, (int) clamp(a * 255, 0, 255));
    }

    private static Color withAlpha(Color c, double a) {
        return rgba(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0, 1);
    }

    private static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static double mod(double v, double m) {
        double r = v % m;
        return r < 0 ? r + m : r;
    }
}
