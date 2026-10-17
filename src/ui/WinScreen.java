package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import audio.SoundManager;
import audio.SoundManager.Sfx;
import save.SaveManager;

/**
 * Victory sequence.
 *
 *  1. The key turns: the lock clicks and the door shakes.
 *  2. The door swings open and daylight floods in.
 *  3. First-person walk through the doorway (camera push-in with head bob).
 *  4. Whiteout, then a "STATUS" window pops up over the sunlit garden
 *     with a RETURN TO MENU button.
 *
 * Everything is drawn procedurally and is time based (System.nanoTime),
 * so it needs no extra update() call from the game loop.
 */
public class WinScreen {

    // ---------- Colors ----------
    private static final Color PARCHMENT = new Color(235, 218, 180);
    private static final Color GOLD_LIGHT = new Color(255, 232, 130);
    private static final Color GOLD_DARK = new Color(255, 165, 50);
    private static final Color BRASS = new Color(214, 170, 70);

    // ---------- Door geometry (scene units) ----------
    private static final int DOOR_W = 170;
    private static final int DOOR_H = 310;

    // ---------- Status window layout ----------
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 400;
    private static final int BUTTON_W = 260;
    private static final int BUTTON_H = 55;
    private static final int BUTTON_OFFSET_Y = 322; // from panel top (centre)

    // ---------- Timeline (seconds) ----------
    private static final double FADE_IN_END = 0.7;
    private static final double UNLOCK_START = 0.8;
    private static final double UNLOCK_END = 1.4;
    private static final double OPEN_START = 1.4;
    private static final double OPEN_END = 3.5;
    private static final double WALK_START = 3.1;
    private static final double WALK_END = 6.9;
    private static final double WHITE_START = 5.6;
    private static final double WHITE_FULL = 6.9;
    private static final double WHITE_OUT_END = 7.7;
    private static final double PANEL_START = 7.2;
    private static final double PANEL_POP = 0.7;
    private static final double CHECK_START = 7.9;
    private static final double CHECK_DURATION = 0.6;
    private static final double BUTTON_START = 8.1;
    private static final double READY_TIME = 8.5;   // clicks / ENTER accepted
    private static final double SKIP_TARGET = 7.0;  // where skip() jumps to

    private final Random random = new Random();
    private final List<Mote> motes = new ArrayList<>();

    // Sound cues already fired (bit flags) and the last footstep index.
    private static final int CUE_LOCK = 1;
    private static final int CUE_CREAK = 2;
    private static final int CUE_OUTDOORS = 4;
    private static final int CUE_PANEL = 8;
    private static final int CUE_CHECK = 16;
    private int cues;
    private int lastStep;

    private long startNanos;
    private long lastNanos;
    private boolean hovering;
    private boolean needsInit;
    private int lastWidth = 960;
    private int lastHeight = 640;

    // =================================================
    // Lifecycle
    // =================================================

    public void show() {

        System.out.println("You Win");

        // The game has been completed.
        // There should no longer be a continuation save.
        SaveManager.deleteSave();

        reset();
    }

    private void reset() {
        startNanos = System.nanoTime();
        lastNanos = startNanos;
        hovering = false;
        needsInit = true;
        cues = 0;
        lastStep = 0;
        motes.clear();
    }

    /** Jumps from the cinematic straight to the status window. */
    public void skip() {
        if (elapsed() < SKIP_TARGET) {
            startNanos = System.nanoTime() - (long) (SKIP_TARGET * 1e9);

            // Drop the cinematic sounds and carry on with the outdoor ambience.
            cues |= CUE_LOCK | CUE_CREAK | CUE_OUTDOORS;
            lastStep = Integer.MAX_VALUE;
            SoundManager.stop(Sfx.LOCK);
            SoundManager.stop(Sfx.CREAK);
            SoundManager.stop(Sfx.STEP);
            SoundManager.stop(Sfx.OUTDOORS);
            SoundManager.play(Sfx.OUTDOORS, 0.8);
        }
    }

    // =================================================
    // Input helpers (used by GamePanel)
    // =================================================

    /** True once the status window is fully shown and accepts input. */
    public boolean isReady() {
        return elapsed() >= READY_TIME;
    }

    public boolean isButtonClicked(int mouseX, int mouseY, int width) {
        return isReady() && buttonRect(width).contains(mouseX, mouseY);
    }

    public void handleMouseMove(int mouseX, int mouseY, int width) {
        boolean was = hovering;
        hovering = isReady() && buttonRect(width).contains(mouseX, mouseY);
        if (hovering && !was) {
            SoundManager.play(Sfx.HOVER);
        }
    }

    public boolean isHovering() {
        return hovering;
    }

    private int panelTop(int height) {
        return (height - PANEL_H) / 2;
    }

    private Rectangle buttonRect(int width) {
        int cy = panelTop(lastHeight) + BUTTON_OFFSET_Y;
        return new Rectangle(
                width / 2 - BUTTON_W / 2,
                cy - BUTTON_H / 2,
                BUTTON_W,
                BUTTON_H);
    }

    private double elapsed() {
        if (startNanos == 0) {
            return 0;
        }
        return (System.nanoTime() - startNanos) / 1e9;
    }

    // =================================================
    // Main draw
    // =================================================

    public void draw(Graphics2D g, int width, int height) {

        if (startNanos == 0) {
            reset();
        }

        lastWidth = width;
        lastHeight = height;

        if (needsInit) {
            initMotes(width, height);
        }

        long nowNanos = System.nanoTime();
        double t = (nowNanos - startNanos) / 1e9;
        double dt = Math.min(0.05, (nowNanos - lastNanos) / 1e9);
        lastNanos = nowNanos;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        fireCues(t);

        if (t < WHITE_FULL) {
            drawCinematic(g, width, height, t);
        } else {
            drawResult(g, width, height, t, dt);
        }

        // Bright flash while stepping into the light.
        double wa = whiteAlpha(t);
        if (wa > 0) {
            g.setColor(rgba(255, 250, 235, wa));
            g.fillRect(0, 0, width, height);
        }

        // Fade in from black at the very start.
        if (t < FADE_IN_END) {
            g.setColor(rgba(0, 0, 0, 1.0 - t / FADE_IN_END));
            g.fillRect(0, 0, width, height);
        }
    }

    /** Fires each sound once, when the timeline reaches it. */
    private void fireCues(double t) {

        if (t >= UNLOCK_START && (cues & CUE_LOCK) == 0) {
            cues |= CUE_LOCK;
            SoundManager.play(Sfx.LOCK);
        }

        if (t >= OPEN_START && (cues & CUE_CREAK) == 0) {
            cues |= CUE_CREAK;
            SoundManager.play(Sfx.CREAK);
        }

        if (t >= WALK_START - 0.3 && (cues & CUE_OUTDOORS) == 0) {
            cues |= CUE_OUTDOORS;
            SoundManager.play(Sfx.OUTDOORS);
        }

        // Footsteps land when the camera bob is at its lowest point.
        if (t >= WALK_START && t < WALK_END - 0.4) {
            int idx = (int) Math.floor((t - WALK_START) * 1.8 + 0.5);
            if (idx > lastStep) {
                lastStep = idx;
                SoundManager.play(Sfx.STEP, idx % 2 == 0 ? 1.0 : 0.85);
            }
        }

        if (t >= PANEL_START && (cues & CUE_PANEL) == 0) {
            cues |= CUE_PANEL;
            SoundManager.play(Sfx.CHIME);
        }

        if (t >= CHECK_START + CHECK_DURATION && (cues & CUE_CHECK) == 0) {
            cues |= CUE_CHECK;
            SoundManager.play(Sfx.DING);
        }
    }

    private double whiteAlpha(double t) {
        if (t < WHITE_START) {
            return 0;
        }
        if (t < WHITE_FULL) {
            return smooth((t - WHITE_START) / (WHITE_FULL - WHITE_START));
        }
        if (t < WHITE_OUT_END) {
            return 1 - smooth((t - WHITE_FULL) / (WHITE_OUT_END - WHITE_FULL));
        }
        return 0;
    }

    // =================================================
    // PART 1-3: door cinematic
    // =================================================

    private void drawCinematic(Graphics2D g, int w, int h, double t) {

        int cx = w / 2;
        int floorY = (int) (h * 0.80);
        int doorTop = floorY - DOOR_H;
        int eyeY = doorTop + (int) (DOOR_H * 0.45);

        // --- First-person camera: push-in toward the doorway ---
        double u = clamp((t - WALK_START) / (WALK_END - WALK_START), 0, 1);
        double scale = Math.pow(9.0, smooth(u));

        double walkT = Math.max(0, t - WALK_START);
        double ramp = clamp(walkT / 0.6, 0, 1);
        double bobY = -Math.abs(Math.sin(walkT * Math.PI * 1.8)) * 9 * ramp;
        double swayX = Math.sin(walkT * Math.PI * 0.9) * 6 * ramp;
        double roll = Math.sin(walkT * Math.PI * 0.9) * 0.006 * ramp;

        AffineTransform base = g.getTransform();
        g.translate(cx + swayX, eyeY + bobY);
        g.rotate(roll);
        g.scale(scale, scale);
        g.translate(-cx, -eyeY);

        drawRoom(g, w, h, t, scale);

        g.setTransform(base);

        // --- Screen-space effects ---
        double vig = 175 * (1 - clamp((t - 5.0) / 1.5, 0, 1));
        if (vig > 1) {
            float r = (float) (Math.max(w, h) * 0.78);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(w / 2f, h / 2f), r,
                    new float[] { 0.5f, 1f },
                    new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, vig / 255.0) }));
            g.fillRect(0, 0, w, h);
        }

        // Cinematic letterbox bars
        double barIn = easeOutCubic(clamp(t / 0.8, 0, 1));
        double barOut = 1 - clamp((t - 6.0) / 0.9, 0, 1);
        int bar = (int) (56 * barIn * barOut);
        if (bar > 0) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, bar);
            g.fillRect(0, h - bar, w, bar);
        }

        // Skip hint
        if (t > 1.5 && t < 6.2) {
            g.setFont(new Font("Serif", Font.ITALIC, 14));
            String hint = "Press ENTER or click to skip";
            int tw = g.getFontMetrics().stringWidth(hint);
            g.setColor(rgba(235, 218, 180, 0.55));
            g.drawString(hint, w - tw - 24, h - 22);
        }
    }

    private void drawRoom(Graphics2D g, int w, int h, double t, double walkScale) {

        int cx = w / 2;
        int floorY = (int) (h * 0.80);
        int dl = cx - DOOR_W / 2;
        int dt = floorY - DOOR_H;
        double eyeY = dt + DOOR_H * 0.45;

        double open = easeInOutCubic(clamp((t - OPEN_START) / (OPEN_END - OPEN_START), 0, 1));
        double theta = Math.toRadians(104) * open;

        // ----- Wall -----
        g.setPaint(new GradientPaint(0, 0, new Color(76, 54, 40),
                0, floorY, new Color(52, 35, 26)));
        g.fillRect(-w, -h, w * 3, floorY + h);

        // subtle wallpaper stripes
        g.setColor(rgba(0, 0, 0, 0.10));
        for (int x = -w; x < w * 2; x += 60) {
            g.fillRect(cx + x, -h, 2, floorY + h);
        }

        // wainscot
        g.setColor(new Color(58, 40, 29));
        g.fillRect(-w, floorY - 110, w * 3, 110);
        g.setColor(new Color(96, 68, 47));
        g.fillRect(-w, floorY - 114, w * 3, 5);
        g.setColor(new Color(44, 30, 22));
        for (int k = -14; k <= 14; k++) {
            int x = cx + k * 90 + 45;
            if (Math.abs(x - cx) < DOOR_W / 2 + 40) {
                continue;
            }
            g.fillRect(x - 1, floorY - 108, 3, 94);
        }
        g.setColor(new Color(34, 23, 17));
        g.fillRect(-w, floorY - 14, w * 3, 14);

        // ----- Floor -----
        g.setPaint(new GradientPaint(0, floorY, new Color(46, 31, 23),
                0, h, new Color(20, 14, 11)));
        g.fillRect(-w, floorY, w * 3, h * 2);
        g.setColor(rgba(0, 0, 0, 0.25));
        for (int k = -12; k <= 12; k++) {
            int x0 = cx + k * 40;
            int x1 = cx + k * 140;
            g.drawLine(x0, floorY, x1, h + 200);
        }

        // ----- Wall sconces -----
        for (int k = -1; k <= 1; k += 2) {
            int sx = cx + k * 300;
            int sy = dt + 70;
            double flick = 0.85 + 0.15 * Math.sin(t * 13 + k) * Math.sin(t * 7.3);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(sx, sy), 150f,
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 190, 110, 0.38 * flick),
                            rgba(255, 190, 110, 0) }));
            g.fillRect(sx - 160, sy - 160, 320, 320);
            g.setColor(new Color(40, 28, 20));
            g.fillRect(sx - 7, sy, 14, 26);
            g.setColor(rgba(255, 224, 160, 0.9));
            g.fillOval(sx - 6, sy - 12, 12, 16);
        }

        // ----- Doorway contents (clipped) -----
        Shape oldClip = g.getClip();
        g.clipRect(dl, dt, DOOR_W, DOOR_H);

        if (open > 0.001) {
            AffineTransform at = g.getTransform();
            double ocy = dt + DOOR_H / 2.0;
            double k = Math.pow(walkScale, -0.5); // outside moves slower = parallax
            g.translate(cx, ocy);
            g.scale(k, k);
            g.translate(-cx, -ocy);
            drawOutside(g, cx, eyeY, DOOR_W, DOOR_W * 3.2,
                    eyeY - DOOR_H * 2.2, eyeY + DOOR_H * 2.2, t);
            g.setTransform(at);
        } else {
            g.setColor(new Color(20, 14, 12));
            g.fillRect(dl, dt, DOOR_W, DOOR_H);
        }

        drawDoorLeaf(g, cx, dl, dt, floorY, eyeY, theta, open, t);

        g.setClip(oldClip);

        // ----- Frame / trim -----
        g.setColor(new Color(66, 43, 28));
        g.fillRect(dl - 16, dt - 16, 16, DOOR_H + 16);
        g.fillRect(dl + DOOR_W, dt - 16, 16, DOOR_H + 16);
        g.fillRect(dl - 16, dt - 16, DOOR_W + 32, 16);
        g.setColor(new Color(88, 60, 40));
        g.fillRect(dl - 28, dt - 32, DOOR_W + 56, 14);
        g.setColor(new Color(40, 26, 18));
        g.drawRect(dl - 16, dt - 16, DOOR_W + 32, DOOR_H + 16);
        g.setColor(new Color(110, 78, 50));
        g.fillRect(dl - 8, floorY - 4, DOOR_W + 16, 6);

        // ----- Light spilling into the room -----
        if (open > 0.01) {
            // glow around the doorway
            float rad = (float) (120 + 330 * open);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, (float) eyeY), rad,
                    new float[] { 0f, 0.35f, 1f },
                    new Color[] { rgba(255, 248, 225, 0.55 * open),
                            rgba(255, 240, 205, 0.22 * open),
                            rgba(255, 235, 200, 0) }));
            g.fillRect(-w, -h, w * 3, h * 3);

            // light shaft across the floor
            Path2D.Double shaft = new Path2D.Double();
            shaft.moveTo(dl + 6, floorY);
            shaft.lineTo(dl + DOOR_W - 6, floorY);
            shaft.lineTo(cx + DOOR_W / 2.0 + 300, h + 220);
            shaft.lineTo(cx - DOOR_W / 2.0 - 300, h + 220);
            shaft.closePath();
            g.setPaint(new GradientPaint(0, floorY, rgba(255, 244, 210, 0.65 * open),
                    0, h + 220, rgba(255, 244, 210, 0)));
            g.fill(shaft);
        }
    }

    private void drawDoorLeaf(Graphics2D g, int cx, int dl, int dt, int floorY,
                              double eyeY, double theta, double open, double t) {

        // Perspective: the door swings AWAY from the viewer, hinged on the left.
        double depth = DOOR_W * 2.2;
        double zFree = DOOR_W * Math.sin(theta);
        double f = 1.0 / (1.0 + zFree / depth);

        double xHinge = dl;
        double xFree = cx + ((dl - cx) + DOOR_W * Math.cos(theta)) * f;
        double topHinge = dt, botHinge = floorY;
        double topFree = eyeY + (dt - eyeY) * f;
        double botFree = eyeY + (floorY - eyeY) * f;

        if (Math.abs(xFree - xHinge) < 1) {
            return;
        }

        // Shake while the lock clicks.
        double shake = 0;
        if (t >= UNLOCK_START && t < UNLOCK_END) {
            double p = (t - UNLOCK_START) / (UNLOCK_END - UNLOCK_START);
            shake = Math.sin(t * 90) * 2.0 * (1 - p);
        }
        AffineTransform old = g.getTransform();
        g.translate(shake, 0);

        double shade = 1.0 - 0.55 * open;

        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(xHinge, topHinge);
        leaf.lineTo(xFree, topFree);
        leaf.lineTo(xFree, botFree);
        leaf.lineTo(xHinge, botHinge);
        leaf.closePath();

        g.setColor(shaded(128, 82, 48, shade));
        g.fill(leaf);

        // Panels
        double[][] panels = { { 0.14, 0.86, 0.07, 0.45 }, { 0.14, 0.86, 0.55, 0.93 } };
        for (double[] p : panels) {
            Path2D.Double q = new Path2D.Double();
            q.moveTo(px(p[0], p[2], xHinge, xFree, topHinge, topFree, botHinge, botFree),
                    py(p[0], p[2], topHinge, topFree, botHinge, botFree));
            q.lineTo(px(p[1], p[2], xHinge, xFree, topHinge, topFree, botHinge, botFree),
                    py(p[1], p[2], topHinge, topFree, botHinge, botFree));
            q.lineTo(px(p[1], p[3], xHinge, xFree, topHinge, topFree, botHinge, botFree),
                    py(p[1], p[3], topHinge, topFree, botHinge, botFree));
            q.lineTo(px(p[0], p[3], xHinge, xFree, topHinge, topFree, botHinge, botFree),
                    py(p[0], p[3], topHinge, topFree, botHinge, botFree));
            q.closePath();
            g.setColor(shaded(100, 62, 36, shade));
            g.fill(q);
            g.setColor(shaded(150, 100, 62, shade));
            g.setStroke(new BasicStroke(2f));
            g.draw(q);
        }

        // Outer edge
        g.setColor(shaded(60, 38, 22, shade));
        g.setStroke(new BasicStroke(3f));
        g.draw(leaf);

        // Knob + keyhole (only while the door still faces us)
        if (theta < 1.1) {
            double ku = 0.88, kv = 0.52;
            double kx = px(ku, kv, xHinge, xFree, topHinge, topFree, botHinge, botFree);
            double ky = py(ku, kv, topHinge, topFree, botHinge, botFree);
            double kf = lerp(1, f, ku);

            // unlock flash
            if (t >= UNLOCK_START && t < OPEN_START + 0.4) {
                double p = (t - UNLOCK_START) / 0.9;
                double flash = Math.max(0, Math.sin(clamp(p, 0, 1) * Math.PI));
                g.setPaint(new RadialGradientPaint(
                        new Point2D.Double(kx, ky + 12), 46f,
                        new float[] { 0f, 1f },
                        new Color[] { rgba(255, 230, 150, 0.85 * flash),
                                rgba(255, 230, 150, 0) }));
                g.fill(new Ellipse2D.Double(kx - 50, ky - 38, 100, 100));
            }

            double r = 7 * kf;
            g.setColor(shaded(60, 45, 20, shade));
            g.fill(new Ellipse2D.Double(kx - r - 2, ky - r - 2, 2 * r + 4, 2 * r + 4));
            g.setColor(shaded(BRASS.getRed(), BRASS.getGreen(), BRASS.getBlue(), shade));
            g.fill(new Ellipse2D.Double(kx - r, ky - r, 2 * r, 2 * r));
            g.setColor(rgba(255, 245, 200, 0.6 * shade));
            g.fill(new Ellipse2D.Double(kx - r * 0.5, ky - r * 0.6, r * 0.7, r * 0.7));

            g.setColor(new Color(20, 14, 10));
            g.fill(new Ellipse2D.Double(kx - 2 * kf, ky + 14 * kf, 4 * kf, 4 * kf));
            g.fillRect((int) (kx - 1 * kf), (int) (ky + 16 * kf),
                    Math.max(1, (int) (2 * kf)), (int) (7 * kf));
        }

        g.setTransform(old);
    }

    // =================================================
    // Outdoor scene (seen through the door and behind the status window)
    // =================================================

    private void drawOutside(Graphics2D g, double cx, double horizonY,
                             double unit, double halfW, double top,
                             double bottom, double t) {

        double left = cx - halfW;
        double width = halfW * 2;

        // Sky
        g.setPaint(new GradientPaint(0, (float) top, new Color(112, 176, 236),
                0, (float) horizonY, new Color(255, 238, 205)));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, top, width, horizonY - top));

        // Sun glow
        g.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx + unit * 0.12, horizonY - unit * 0.5),
                (float) (unit * 2.4),
                new float[] { 0f, 0.22f, 1f },
                new Color[] { new Color(255, 255, 245, 255),
                        new Color(255, 240, 195, 210),
                        new Color(255, 232, 175, 0) }));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, top, width, bottom - top));

        // Slowly drifting clouds
        g.setColor(new Color(255, 255, 255, 150));
        for (int i = 0; i < 4; i++) {
            double span = unit * 6;
            double x = cx - span / 2 + ((i * unit * 1.7 + t * 5) % span);
            double y = horizonY - unit * (1.0 + 0.35 * i);
            g.fill(new Ellipse2D.Double(x, y, unit * 0.9, unit * 0.22));
            g.fill(new Ellipse2D.Double(x + unit * 0.2, y - unit * 0.09, unit * 0.5, unit * 0.22));
        }

        // Far hills
        fillHills(g, left, left + width, horizonY, unit * 0.30, 1.1, 0.8,
                new Color(166, 200, 186));
        // Near hills
        fillHills(g, left, left + width, horizonY, unit * 0.18, 1.9, 2.4,
                new Color(122, 170, 124));

        // Ground
        g.setPaint(new GradientPaint(0, (float) horizonY, new Color(128, 172, 96),
                0, (float) bottom, new Color(62, 104, 50)));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, horizonY, width, bottom - horizonY));

        // Garden path leading away
        double halfBottom = (bottom - horizonY) * 0.9;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(cx - unit * 0.04, horizonY);
        path.lineTo(cx + unit * 0.04, horizonY);
        path.lineTo(cx + halfBottom, bottom);
        path.lineTo(cx - halfBottom, bottom);
        path.closePath();
        g.setPaint(new GradientPaint(0, (float) horizonY, new Color(240, 224, 184),
                0, (float) bottom, new Color(188, 160, 116)));
        g.fill(path);

        // Trees
        double[] tx = { -1.8, -1.15, -0.62, 0.66, 1.2, 1.9 };
        double[] tm = { 1.0, 1.3, 0.85, 0.95, 1.35, 1.0 };
        for (int i = 0; i < tx.length; i++) {
            double x = cx + tx[i] * unit;
            double m = tm[i];
            double baseY = horizonY + unit * 0.12 * m;
            g.setColor(new Color(84, 58, 38));
            g.fill(new java.awt.geom.Rectangle2D.Double(
                    x - unit * 0.03 * m, baseY - unit * 0.30 * m,
                    unit * 0.06 * m, unit * 0.30 * m));
            g.setColor(new Color(52, 100, 56));
            g.fill(new Ellipse2D.Double(x - unit * 0.22 * m, baseY - unit * 0.62 * m,
                    unit * 0.44 * m, unit * 0.42 * m));
            g.setColor(new Color(70, 124, 66));
            g.fill(new Ellipse2D.Double(x - unit * 0.13 * m, baseY - unit * 0.66 * m,
                    unit * 0.26 * m, unit * 0.26 * m));
        }

        // Horizon haze
        g.setPaint(new GradientPaint(0, (float) (horizonY - unit * 0.35),
                new Color(255, 246, 222, 0),
                0, (float) horizonY, new Color(255, 246, 222, 170)));
        g.fill(new java.awt.geom.Rectangle2D.Double(left, horizonY - unit * 0.35, width, unit * 0.35));
    }

    private void fillHills(Graphics2D g, double x0, double x1, double horizonY,
                           double amp, double freq, double phase, Color c) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0, horizonY + 2);
        double unitFreq = freq / 120.0;
        for (double x = x0; x <= x1; x += 10) {
            double y = horizonY - amp * (0.5 + 0.5 * Math.sin(x * unitFreq + phase));
            p.lineTo(x, y);
        }
        p.lineTo(x1, horizonY + 2);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    // =================================================
    // PART 4: sunlit backdrop + status window
    // =================================================

    private void initMotes(int w, int h) {
        motes.clear();
        for (int i = 0; i < 28; i++) {
            Mote m = new Mote();
            m.x = random.nextDouble() * w;
            m.y = random.nextDouble() * h;
            m.size = 1.5 + random.nextDouble() * 2.5;
            m.vy = -(6 + random.nextDouble() * 12);
            m.vx = (random.nextDouble() - 0.5) * 8;
            m.phase = random.nextDouble() * Math.PI * 2;
            motes.add(m);
        }
        needsInit = false;
    }

    private void drawResult(Graphics2D g, int w, int h, double t, double dt) {

        // Garden in daylight
        drawOutside(g, w / 2.0, h * 0.60, 300, w, 0, h, t);

        // Dim it so the window stands out
        g.setColor(new Color(12, 10, 16, 150));
        g.fillRect(0, 0, w, h);

        float r = (float) (Math.max(w, h) * 0.8);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f), r,
                new float[] { 0.45f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 150) }));
        g.fillRect(0, 0, w, h);

        // Drifting light motes (very subtle)
        for (Mote m : motes) {
            m.x += (m.vx + Math.sin(t + m.phase) * 6) * dt;
            m.y += m.vy * dt;
            if (m.y < -10) {
                m.y = h + 10;
                m.x = random.nextDouble() * w;
            }
            double tw = 0.5 + 0.5 * Math.sin(t * 1.5 + m.phase);
            g.setColor(rgba(255, 244, 210, 0.15 + 0.35 * tw));
            g.fill(new Ellipse2D.Double(m.x, m.y, m.size, m.size));
        }

        drawPanel(g, w, h, t);
    }

    private void drawPanel(Graphics2D g, int w, int h, double t) {

        double pp = clamp((t - PANEL_START) / PANEL_POP, 0, 1);
        if (pp <= 0) {
            return;
        }

        double sc = 0.55 + 0.45 * easeOutBack(pp);
        float alpha = (float) clamp(pp * 1.6, 0, 1);

        int cx = w / 2;
        int py = panelTop(h);
        int px = (w - PANEL_W) / 2;

        AffineTransform oldT = g.getTransform();
        Composite oldC = g.getComposite();

        g.translate(w / 2.0, h / 2.0);
        g.scale(sc, sc);
        g.translate(-w / 2.0, -h / 2.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Soft shadow
        for (int i = 6; i >= 1; i--) {
            g.setColor(rgba(0, 0, 0, 0.07));
            g.fill(new RoundRectangle2D.Double(px - i * 3, py - i * 3 + 10,
                    PANEL_W + i * 6, PANEL_H + i * 6, 30 + i * 3, 30 + i * 3));
        }

        // Body
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(
                px, py, PANEL_W, PANEL_H, 26, 26);
        g.setPaint(new GradientPaint(0, py, new Color(48, 35, 28, 245),
                0, py + PANEL_H, new Color(24, 17, 15, 248)));
        g.fill(body);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(205, 155, 75));
        g.draw(body);
        g.setStroke(new BasicStroke(1f));
        g.setColor(rgba(235, 218, 180, 0.35));
        g.draw(new RoundRectangle2D.Double(px + 9, py + 9, PANEL_W - 18, PANEL_H - 18, 18, 18));

        // Header
        g.setFont(new Font("Serif", Font.BOLD, 14));
        String header = "STATUS";
        FontMetrics hfm = g.getFontMetrics();
        int spacing = 6;
        int hw = 0;
        for (char c : header.toCharArray()) {
            hw += hfm.charWidth(c) + spacing;
        }
        hw -= spacing;
        int hx = cx - hw / 2;
        g.setColor(new Color(230, 190, 100));
        for (char c : header.toCharArray()) {
            g.drawString(String.valueOf(c), hx, py + 36);
            hx += hfm.charWidth(c) + spacing;
        }
        g.setColor(rgba(205, 155, 75, 0.7));
        g.drawLine(px + 36, py + 31, cx - hw / 2 - 16, py + 31);
        g.drawLine(cx + hw / 2 + 16, py + 31, px + PANEL_W - 36, py + 31);

        // Check badge
        int bcy = py + 92;
        double ringPulse = (t * 22) % 20;
        if (t > CHECK_START + CHECK_DURATION) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(rgba(255, 226, 130, 0.45 * (1 - ringPulse / 20)));
            g.draw(new Ellipse2D.Double(cx - 32 - ringPulse, bcy - 32 - ringPulse,
                    64 + ringPulse * 2, 64 + ringPulse * 2));
        }
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, bcy - 8), 40f,
                new float[] { 0f, 1f },
                new Color[] { new Color(60, 120, 80), new Color(28, 66, 44) }));
        g.fill(new Ellipse2D.Double(cx - 32, bcy - 32, 64, 64));
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(235, 195, 100));
        g.draw(new Ellipse2D.Double(cx - 32, bcy - 32, 64, 64));

        double cp = clamp((t - CHECK_START) / CHECK_DURATION, 0, 1);
        if (cp > 0) {
            // tick: A -> B (34%) -> C (66%)
            double ax = cx - 14, ay = bcy + 1;
            double bx = cx - 4, by = bcy + 12;
            double ex = cx + 16, ey = bcy - 10;
            Path2D.Double tick = new Path2D.Double();
            tick.moveTo(ax, ay);
            if (cp < 0.34) {
                double k = cp / 0.34;
                tick.lineTo(lerp(ax, bx, k), lerp(ay, by, k));
            } else {
                tick.lineTo(bx, by);
                double k = (cp - 0.34) / 0.66;
                tick.lineTo(lerp(bx, ex, k), lerp(by, ey, k));
            }
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 232, 140));
            g.draw(tick);
        }

        // Title with a gentle shimmer
        int tSize = 46;
        g.setFont(new Font("Serif", Font.BOLD, tSize));
        FontMetrics tfm = g.getFontMetrics();
        String title = "YOU ESCAPED!";
        int tw = tfm.stringWidth(title);
        int tx = cx - tw / 2;
        int tBase = py + 178;

        double sweep = ((t - PANEL_START - 0.8) * 7) % (title.length() + 9) - 3;
        for (int i = 0; i < title.length(); i++) {
            String s = String.valueOf(title.charAt(i));
            int cw = tfm.stringWidth(s);

            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(s, tx + 3, tBase + 3);

            g.setPaint(new GradientPaint(0, tBase - tSize * 0.7f, GOLD_LIGHT,
                    0, tBase, GOLD_DARK));
            g.drawString(s, tx, tBase);

            double dist = Math.abs(i - sweep);
            if (dist < 2.0 && t > PANEL_START + 0.8) {
                g.setColor(rgba(255, 255, 255, (1 - dist / 2.0) * 0.8));
                g.drawString(s, tx, tBase);
            }
            tx += cw;
        }

        // Subtitle
        g.setFont(new Font("Serif", Font.ITALIC, 20));
        drawCentered(g, "Congratulations, you solved the mystery.",
                cx, py + 214, PARCHMENT);

        // Divider with diamond
        int dy = py + 234;
        g.setStroke(new BasicStroke(2f));
        g.setColor(rgba(255, 165, 50, 0.85));
        g.drawLine(cx - 130, dy, cx - 10, dy);
        g.drawLine(cx + 10, dy, cx + 130, dy);
        AffineTransform dOld = g.getTransform();
        g.translate(cx, dy);
        g.rotate(Math.PI / 4);
        g.setColor(GOLD_LIGHT);
        g.fillRect(-4, -4, 8, 8);
        g.setTransform(dOld);

        // Message
        g.setFont(new Font("Serif", Font.PLAIN, 17));
        drawCentered(g, "The door is open. You made it out.",
                cx, py + 264, new Color(210, 195, 160));

        // Button
        drawButton(g, cx, py + BUTTON_OFFSET_Y, t);

        // Footer
        if (t >= READY_TIME) {
            double blink = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(t * 3.0));
            g.setFont(new Font("Serif", Font.PLAIN, 14));
            drawCentered(g, "Press ENTER to return to the main menu",
                    cx, py + 380, rgba(235, 218, 180, blink));
        }

        g.setComposite(oldC);
        g.setTransform(oldT);
    }

    private void drawButton(Graphics2D g, int cx, int cy, double t) {

        float appear = (float) clamp((t - BUTTON_START) / 0.4, 0, 1);
        if (appear <= 0) {
            return;
        }

        double pulse = 1 + 0.025 * Math.sin(t * 4.0);
        double scale = (hovering ? 1.07 : pulse) * (0.85 + 0.15 * easeOutCubic(appear));

        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);

        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(
                -BUTTON_W / 2.0, -BUTTON_H / 2.0, BUTTON_W, BUTTON_H, 18, 18);

        int glow = hovering ? 90 : (int) (40 + 30 * (0.5 + 0.5 * Math.sin(t * 4.0)));
        for (int i = 3; i >= 1; i--) {
            g.setColor(rgba(255, 190, 70, glow * appear / i / 255.0));
            g.setStroke(new BasicStroke(i * 5f));
            g.draw(shape);
        }

        Color top = hovering ? new Color(160, 105, 60) : new Color(125, 78, 46);
        Color bottom = hovering ? new Color(110, 66, 36) : new Color(85, 52, 30);
        g.setPaint(new GradientPaint(0, -BUTTON_H / 2f, withAlpha(top, appear),
                0, BUTTON_H / 2f, withAlpha(bottom, appear)));
        g.fill(shape);

        g.setStroke(new BasicStroke(2f));
        g.setColor(withAlpha(hovering ? GOLD_LIGHT : PARCHMENT, appear));
        g.draw(shape);

        // Moving shine
        Shape oldClip = g.getClip();
        double shineX = ((t * 120) % 520) - 260;
        g.clip(shape);
        g.setColor(rgba(255, 255, 255, 0.14 * appear));
        Path2D.Double shine = new Path2D.Double();
        shine.moveTo(shineX, -BUTTON_H);
        shine.lineTo(shineX + 30, -BUTTON_H);
        shine.lineTo(shineX + 10, BUTTON_H);
        shine.lineTo(shineX - 20, BUTTON_H);
        shine.closePath();
        g.fill(shine);
        g.setClip(oldClip);

        g.setFont(new Font("Serif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String label = "RETURN TO MENU";
        g.setColor(withAlpha(hovering ? Color.WHITE : PARCHMENT, appear));
        g.drawString(label, -fm.stringWidth(label) / 2, fm.getAscent() / 2 - 1);

        g.setTransform(old);
    }

    // =================================================
    // Helpers
    // =================================================

    private void drawCentered(Graphics2D g, String text, int cx, int y, Color color) {
        int w = g.getFontMetrics().stringWidth(text);
        g.setColor(new Color(0, 0, 0, color.getAlpha() / 2));
        g.drawString(text, cx - w / 2 + 2, y + 2);
        g.setColor(color);
        g.drawString(text, cx - w / 2, y);
    }

    // Maps leaf coordinates (u across hinge->free edge, v top->bottom) to screen.
    private static double px(double u, double v, double xh, double xf,
                             double th, double tf, double bh, double bf) {
        return lerp(xh, xf, u);
    }

    private static double py(double u, double v,
                             double th, double tf, double bh, double bf) {
        double top = lerp(th, tf, u);
        double bot = lerp(bh, bf, u);
        return lerp(top, bot, v);
    }

    private static Color shaded(int r, int g, int b, double s) {
        return new Color(
                (int) clamp(r * s, 0, 255),
                (int) clamp(g * s, 0, 255),
                (int) clamp(b * s, 0, 255));
    }

    private static Color rgba(int r, int g, int b, double a) {
        return new Color(r, g, b, (int) clamp(a * 255, 0, 255));
    }

    private static Color withAlpha(Color c, float a) {
        return rgba(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static double easeOutCubic(double x) {
        double inv = 1 - clamp(x, 0, 1);
        return 1 - inv * inv * inv;
    }

    private static double easeInOutCubic(double x) {
        x = clamp(x, 0, 1);
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        x = clamp(x, 0, 1);
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    private static class Mote {
        double x, y, vx, vy, size, phase;
    }
}
