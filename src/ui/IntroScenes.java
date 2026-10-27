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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Animated background scenes for the story intro. Everything is drawn
 * procedurally (no image files) and driven purely by time, so IntroUI only
 * has to say "draw scene N at time t".
 *
 *   0  A quiet evening        - living room at sunset, ticking clock, steaming mug
 *   1  Giggling / lock / hush - kids peek out of a door, it shuts and locks
 *   2  Downstairs             - someone comes down the stairs, empty key hook, the safe
 *   3  The torn code          - the kids tear the code and hide the pieces
 *   4  Locked in              - chained door, silent safe, pieces glinting, heartbeat
 *   5  The mission            - golden light, the code reassembles, safe opens
 *
 * IntroUI fills {@link #mark} with the moment (seconds) each story beat is
 * typed so the pictures line up with the words and the sound effects.
 */
final class IntroScenes {

    // ---------- Story beats (seconds on the intro clock) ----------
    static final int M_GIGGLE = 0;      // "giggling"
    static final int M_LOCK = 1;        // "click of a lock"
    static final int M_SILENCE = 2;     // "silence"
    static final int M_STAIRS = 3;      // "By the time you got downstairs"
    static final int M_KEYGONE = 4;     // "the front door key was gone"
    static final int M_SAFE = 5;        // "the new safe"
    static final int M_DIGITS = 6;      // "four-digit code"
    static final int M_TORE = 7;        // "tore it into pieces"
    static final int M_HID = 8;         // "hid every piece"
    static final int M_DOORLOCKED = 9;  // "The door is locked"
    static final int M_SAFESILENT = 10; // "The safe is silent"
    static final int M_PIECES = 11;     // "The pieces are out there"
    static final int M_MISSION = 12;    // "YOUR MISSION"
    static final int M_SEARCH = 13;     // "Search every room"
    static final int M_PIECE = 14;      // "Piece the code together"
    static final int M_OPEN = 15;       // "Open the safe"
    static final int M_WALK = 16;       // "Walk out that door"
    static final int M_BEAT = 17;       // first heartbeat
    static final int M_BEATEND = 18;    // heartbeats stop
    static final int M_COUNT = 19;

    static final double STEP_PERIOD = 0.40;
    static final double BEAT_PERIOD = 1.05;
    static final double GIGGLE_GAP = 0.85;

    final double[] mark = new double[M_COUNT];
    final double[] sceneStart = new double[6];

    // ---------- Colours ----------
    private static final Color PARCHMENT = new Color(236, 220, 178);
    private static final Color GOLD = new Color(255, 215, 110);
    private static final Color SILHOUETTE = new Color(14, 10, 16);

    // ---------- Paper pieces (jagged quarters of one sheet) ----------
    private static final double PW = 124;
    private static final double PH = 84;
    private final Path2D.Double[] pieces = new Path2D.Double[4];

    private final double[] starX = new double[50];
    private final double[] starY = new double[50];
    private final double[] starPh = new double[50];

    IntroScenes() {
        Random r = new Random(11);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = r.nextDouble();
            starY[i] = r.nextDouble() * 0.7;
            starPh[i] = r.nextDouble() * Math.PI * 2;
        }
        buildPieces();
    }

    private static double jx(double y) {
        return Math.sin(y * 0.55) * 3.2 + Math.sin(y * 1.7) * 1.8;
    }

    private static double jy(double x) {
        return Math.sin(x * 0.5) * 3.0 + Math.sin(x * 1.9) * 1.6;
    }

    private void buildPieces() {
        int idx = 0;
        for (int sy = -1; sy <= 1; sy += 2) {
            for (int sx = -1; sx <= 1; sx += 2) {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(sx * PW / 2, sy * PH / 2);
                p.lineTo(jx(sy * PH / 2), sy * PH / 2);
                for (double y = sy * PH / 2; sy * y > 0.5; y -= sy * 4) {
                    p.lineTo(jx(y), y);
                }
                p.lineTo(0, 0);
                for (double x = 0; Math.abs(x) < PW / 2; x += sx * 4) {
                    p.lineTo(x, jy(x));
                }
                p.lineTo(sx * PW / 2, jy(sx * PW / 2));
                p.closePath();
                pieces[idx++] = p;
            }
        }
    }

    // =================================================
    // Entry point
    // =================================================

    void draw(Graphics2D g0, int scene, int w, int h, double t, double alpha) {
        if (alpha <= 0.002) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            if (alpha < 0.999) {
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, (float) alpha));
            }
            g.clipRect(0, 0, w, h);
            switch (scene) {
                case 0: scene0(g, w, h, t); break;
                case 1: scene1(g, w, h, t); break;
                case 2: scene2(g, w, h, t); break;
                case 3: scene3(g, w, h, t); break;
                case 4: scene4(g, w, h, t); break;
                default: scene5(g, w, h, t); break;
            }
        } finally {
            g.dispose();
        }
    }

    // =================================================
    // SCENE 0 - a quiet evening
    // =================================================

    private void scene0(Graphics2D g, int w, int h, double t) {
        double dusk = duskAt(t);
        roomBack(g, w, h, t, dusk);
        roomFront(g, w, h, t, dusk, 0);
        warmGrade(g, w, h, 0.10 * (1 - dusk));
    }

    // =================================================
    // SCENE 1 - giggling, a click, then silence
    // =================================================

    private void scene1(Graphics2D g, int w, int h, double t) {
        double dusk = duskAt(t);
        roomBack(g, w, h, t, dusk);
        roomFront(g, w, h, t, dusk, 1);
        playroomDoor(g, w, h, t);

        // Everything goes dim and still after "silence".
        double hush = smooth((t - mark[M_SILENCE]) / 1.2);
        if (hush > 0) {
            g.setColor(rgba(4, 4, 12, 0.55 * hush));
            g.fillRect(0, 0, w, h);
        }
    }

    private void playroomDoor(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        double fw = w * 0.13, fh = h * 0.46;
        double fx = w * 0.835, fy = floorY - fh;

        // 0 = shut, 1 = wide open. Ajar until it swings shut just before the click.
        double closeP = smooth((t - (mark[M_LOCK] - 0.55)) / 0.35);
        double open = 0.62 * (1 - closeP);
        double ang = Math.toRadians(70) * open;
        double leafW = fw * Math.cos(ang);
        double gap = fw - leafW;

        // frame
        g.setColor(new Color(62, 42, 28));
        g.fill(new Rectangle2D.Double(fx - 9, fy - 9, fw + 18, fh + 9));

        // warm light behind the gap
        if (gap > 0.5) {
            g.setPaint(new GradientPaint(0, (float) fy, new Color(255, 226, 150),
                    0, (float) (fy + fh), new Color(230, 160, 90)));
            g.fill(new Rectangle2D.Double(fx, fy, fw, fh));

            // light spilling across the floor
            Path2D.Double spill = new Path2D.Double();
            spill.moveTo(fx, floorY);
            spill.lineTo(fx + gap, floorY);
            spill.lineTo(fx + gap - 70 * u, h);
            spill.lineTo(fx - 170 * u, h);
            spill.closePath();
            double k = gap / (fw * 0.3);
            g.setPaint(new GradientPaint(0, floorY, rgba(255, 215, 140, 0.40 * Math.min(1, k)),
                    0, h, rgba(255, 215, 140, 0.05)));
            g.fill(spill);

            // the two kids peeking through the gap
            Shape oldClip = g.getClip();
            g.clipRect((int) fx, (int) fy, (int) fw, (int) fh);
            double env = giggleEnv(t);
            double gx = fx + gap / 2;
            kidPeek(g, gx, fy + fh * 0.40, 11 * u, Math.sin(t * 52) * 2.4 * env * u, env);
            kidPeek(g, gx + 2 * u, fy + fh * 0.62, 9 * u, Math.sin(t * 47 + 1) * 2.4 * env * u, env);
            g.setClip(oldClip);
        }

        // the door leaf, hinged on the right
        double cy = fy + fh / 2;
        double f = 1.0 / (1.0 + fw * Math.sin(ang) / (fw * 2.2));
        double xh = fx + fw;
        double xf = xh - leafW;
        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(xh, fy);
        leaf.lineTo(xf, cy - fh / 2 * f);
        leaf.lineTo(xf, cy + fh / 2 * f);
        leaf.lineTo(xh, fy + fh);
        leaf.closePath();
        g.setColor(new Color(118, 78, 48));
        g.fill(leaf);
        g.setColor(new Color(90, 58, 36));
        g.fill(inset(xh, fy, xf, cy - fh / 2 * f, xf, cy + fh / 2 * f, xh, fy + fh, 0.12, 0.08, 0.88, 0.44));
        g.fill(inset(xh, fy, xf, cy - fh / 2 * f, xf, cy + fh / 2 * f, xh, fy + fh, 0.12, 0.54, 0.88, 0.92));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(55, 36, 22));
        g.draw(leaf);
        g.setColor(new Color(214, 170, 70));
        g.fill(new Ellipse2D.Double(xf + 8 * u * f, cy + 6 * u, 7 * u, 7 * u));

        // hehe!
        String[] words = { "hehe", "hihi!", "tee-hee" };
        double[] starts = { mark[M_GIGGLE], mark[M_GIGGLE] + GIGGLE_GAP };
        g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, (int) Math.max(12, 18 * u)));
        for (int s = 0; s < starts.length; s++) {
            for (int k = 0; k < 3; k++) {
                double age = t - starts[s] - k * 0.14;
                if (age > 0 && age < 1.3) {
                    double a = Math.sin(Math.PI * age / 1.3);
                    g.setColor(rgba(255, 232, 170, 0.9 * a));
                    g.drawString(words[(k + s) % 3], (float) (fx - 4 + k * 30 * u),
                            (float) (fy - 22 * u - age * 28 * u - k * 14 * u));
                }
            }
        }

        // padlock slams onto the door when it clicks
        double lp = t - mark[M_LOCK];
        if (lp > 0) {
            double sc = easeOutBack(clamp(lp / 0.25, 0, 1));
            double dim = 1 - 0.6 * smooth((t - mark[M_SILENCE]) / 1.0);
            padlock(g, fx + fw / 2, cy + 4 * u, 17 * u * sc, dim, t);
            double ring = clamp(lp / 0.7, 0, 1);
            if (ring < 1) {
                g.setStroke(new BasicStroke(2.5f));
                g.setColor(rgba(255, 226, 140, 0.7 * (1 - ring)));
                double rr = (16 + 60 * ring) * u;
                g.draw(new Ellipse2D.Double(fx + fw / 2 - rr, cy + 4 * u - rr, 2 * rr, 2 * rr));
            }
        }
    }

    private void kidPeek(Graphics2D g, double x, double y, double r, double shake, double env) {
        g.setColor(SILHOUETTE);
        g.fill(new Ellipse2D.Double(x - r + shake, y - r, 2 * r, 2 * r));
        g.fill(new RoundRectangle2D.Double(x - r * 1.05 + shake, y + r * 0.7, r * 2.1, r * 5, r, r));
    }

    private double giggleEnv(double t) {
        double e = 0;
        double[] gs = { mark[M_GIGGLE], mark[M_GIGGLE] + GIGGLE_GAP };
        for (double s : gs) {
            double x = (t - s) / 0.8;
            if (x >= 0 && x <= 1) {
                e = Math.max(e, Math.sin(Math.PI * x));
            }
        }
        return e;
    }

    // =================================================
    // SCENE 2 - downstairs: the empty hook and the safe
    // =================================================

    private void scene2(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        int cx = w / 2;

        // cold hallway
        g.setPaint(new GradientPaint(0, 0, new Color(52, 44, 66), 0, floorY, new Color(30, 25, 40)));
        g.fillRect(0, 0, w, floorY);
        g.setColor(rgba(0, 0, 0, 0.08));
        for (int x = 0; x < w; x += 56) {
            g.fillRect(x, 0, 2, floorY);
        }
        g.setColor(new Color(40, 31, 44));
        g.fillRect(0, floorY - (int) (h * 0.14), w, (int) (h * 0.14));
        g.setColor(new Color(76, 58, 62));
        g.fillRect(0, floorY - (int) (h * 0.14) - 3, w, 4);
        g.setPaint(new GradientPaint(0, floorY, new Color(40, 30, 28), 0, h, new Color(14, 10, 12)));
        g.fillRect(0, floorY, w, h - floorY);
        g.setColor(rgba(0, 0, 0, 0.22));
        for (int k = -12; k <= 12; k++) {
            g.drawLine(cx + k * 40, floorY, cx + k * 130, h);
        }

        // a bare bulb hanging in the hall
        double flick = 0.88 + 0.12 * Math.sin(t * 9.1) * Math.sin(t * 3.7);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, h * 0.10), (float) (320 * u),
                new float[] { 0f, 1f },
                new Color[] { rgba(255, 190, 110, 0.20 * flick), rgba(255, 190, 110, 0) }));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(30, 24, 22));
        g.fillRect(cx - 1, 0, 2, (int) (h * 0.08));
        g.setColor(rgba(255, 226, 160, 0.95));
        g.fill(new Ellipse2D.Double(cx - 6 * u, h * 0.08, 12 * u, 15 * u));

        // front door, centre, softly lit
        double dw = w * 0.12, dh = h * 0.44;
        frontDoor(g, cx, floorY, dw, dh, 0, 0.35, t);

        stairsAndFigure(g, w, h, t);

        // key hook + safe on the right
        double sx = w * 0.895;
        hook(g, sx, h * 0.30, u, t);

        double sp = t - mark[M_SAFE];
        if (sp > 0) {
            double a = smooth(sp / 0.25);
            double sc = 0.6 + 0.4 * easeOutBack(clamp(sp / 0.45, 0, 1));
            double sw = w * 0.13, sh = h * 0.19;
            double sy = floorY - sh;
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp(a, 0, 1)));
            AffineTransform o = g.getTransform();
            g.translate(sx, floorY);
            g.scale(sc, sc);
            g.translate(-sx, -floorY);

            double[] lit = new double[4];
            for (int i = 0; i < 4; i++) {
                lit[i] = smooth((t - (mark[M_DIGITS] + i * 0.28)) / 0.15);
            }
            double keyGlow = 0.6 + 0.4 * Math.sin(t * 3);
            safe(g, sx - sw / 2, sy, sw, sh, lit, keyGlow, 0, t, u);
            g.setTransform(o);
            g.setComposite(oc);

            // clunk flash as it lands
            if (sp < 0.5) {
                g.setColor(rgba(255, 235, 170, 0.25 * (1 - sp / 0.5)));
                g.fill(new Ellipse2D.Double(sx - sw, sy - sh * 0.2, sw * 2, sh * 1.4));
            }
        }

        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0), (float) (Math.max(w, h) * 0.75),
                new float[] { 0.5f, 1f }, new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.5) }));
        g.fillRect(0, 0, w, h);
    }

    private void stairsAndFigure(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        double sh = 24 * u, sw = 24 * u;
        double x0 = w * 0.03;

        // light from upstairs
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x0, floorY - 6 * sh - 40 * u), (float) (230 * u),
                new float[] { 0f, 1f }, new Color[] { rgba(255, 200, 130, 0.22), rgba(255, 200, 130, 0) }));
        g.fillRect(0, 0, w, h);

        for (int k = 5; k >= 0; k--) {
            double ex = x0 + (6 - k) * sw;
            double top = floorY - (k + 1) * sh;
            g.setColor(new Color(58, 40, 30));
            g.fill(new Rectangle2D.Double(x0, top, ex - x0, floorY - top));
            g.setColor(new Color(104, 74, 50));
            g.fill(new Rectangle2D.Double(x0, top, ex - x0, 4 * u));
            g.setColor(rgba(0, 0, 0, 0.35));
            g.fill(new Rectangle2D.Double(ex - 3 * u, top + 4 * u, 3 * u, sh - 4 * u));
        }

        // banister
        g.setStroke(new BasicStroke((float) (3 * u)));
        g.setColor(new Color(92, 64, 44));
        g.draw(new Line2D.Double(x0 + sw * 0.5, floorY - 6 * sh - 34 * u,
                x0 + 7 * sw, floorY - sh - 34 * u));
        for (int k = 0; k < 6; k++) {
            double px = x0 + (6.5 - k) * sw - sw * 0.5;
            double py = floorY - (k + 1) * sh;
            g.draw(new Line2D.Double(px, py, px, py - 34 * u));
        }

        // the person coming down (q = step index, 5 at the top, -1 on the floor)
        double prog = clamp((t - mark[M_STAIRS]) / STEP_PERIOD, 0, 6);
        double q = 5 - prog;
        double fxp = x0 + (6 - q) * sw - sw / 2;
        double fyp = floorY - (q + 1) * sh;
        double frac = prog - Math.floor(prog);
        boolean walking = prog > 0 && prog < 6;
        double hop = walking ? Math.sin(Math.PI * frac) * 7 * u : 0;
        double idle = walking ? 0 : Math.sin(t * 1.6) * 1.0 * u;
        adult(g, fxp, fyp - hop, 1.05 * u, walking ? Math.sin(prog * Math.PI) : 0, idle);
    }

    private void adult(Graphics2D g, double x, double footY, double s, double stride, double bob) {
        g.setStroke(new BasicStroke((float) (6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color rim = rgba(255, 214, 160, 0.30);
        // legs
        for (int side = -1; side <= 1; side += 2) {
            double lx = x + side * 5 * s;
            double kick = side * stride * 9 * s;
            g.setColor(SILHOUETTE);
            g.draw(new Line2D.Double(lx, footY - 38 * s + bob, lx + kick, footY));
        }
        // arms
        g.setStroke(new BasicStroke((float) (5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int side = -1; side <= 1; side += 2) {
            double ax = x + side * 11 * s;
            g.draw(new Line2D.Double(ax, footY - 78 * s + bob, ax - side * stride * 6 * s, footY - 50 * s + bob));
        }
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(x - 12 * s, footY - 84 * s + bob, 24 * s, 50 * s, 12 * s, 12 * s);
        Ellipse2D.Double head = new Ellipse2D.Double(x - 9 * s, footY - 104 * s + bob, 18 * s, 19 * s);
        g.setStroke(new BasicStroke(2f));
        g.setColor(rim);
        g.draw(body);
        g.draw(head);
        g.setColor(SILHOUETTE);
        g.fill(body);
        g.fill(head);
    }

    private void hook(Graphics2D g, double x, double y, double u, double t) {

        // wall plate + hook
        g.setColor(new Color(92, 64, 44));
        g.fill(new RoundRectangle2D.Double(x - 22 * u, y - 10 * u, 44 * u, 14 * u, 6, 6));
        g.setColor(new Color(214, 170, 70));
        g.setStroke(new BasicStroke((float) (3 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2DHelper().hook(x, y + 3 * u, 8 * u));

        double since = t - mark[M_KEYGONE];
        double swing = since > 0 ? 0.55 * Math.exp(-since * 1.0) * Math.sin(since * 5.2) : 0;

        AffineTransform o = g.getTransform();
        g.rotate(swing, x, y + 3 * u);

        // empty key ring
        g.setColor(new Color(190, 150, 70));
        g.setStroke(new BasicStroke((float) (2.2 * u)));
        g.draw(new Ellipse2D.Double(x - 7 * u, y + 8 * u, 14 * u, 14 * u));

        // pale outline where the key should hang
        double gp = smooth(since / 0.6);
        if (gp > 0) {
            double pulse = 0.6 + 0.4 * Math.sin(t * 3.2);
            g.translate(x, y + 52 * u);
            g.rotate(Math.toRadians(90));
            g.scale(20 * u, 20 * u);
            g.setStroke(new BasicStroke(0.09f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[] { 0.22f, 0.2f }, 0f));
            g.setColor(rgba(255, 226, 150, 0.7 * gp * pulse));
            g.draw(keyArea());
        }
        g.setTransform(o);
    }

    // =================================================
    // SCENE 3 - the kids tear the code and hide the pieces
    // =================================================

    private void scene3(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        roomBack(g, w, h, t, 1.0);
        roomFront(g, w, h, t, 1.0, 2);

        double px = w * 0.5, py = h * 0.80;
        double ts = t - sceneStart[3];

        double drop = easeOutCubic(clamp(ts / 0.9, 0, 1));
        double pyNow = py - (1 - drop) * 90 * u + Math.sin(t * 2.0) * 1.5 * u;
        double a = smooth(ts / 0.6);

        double tear = t - mark[M_TORE];
        double sep = tear > 0 ? easeOutBack(clamp(tear / 0.35, 0, 1)) : 0;
        boolean torn = tear > 0;

        // the kids holding the sheet
        double run = clamp((t - mark[M_HID] - 0.35) / 1.0, 0, 1);
        double pull = 16 * u * sep;
        for (int side = -1; side <= 1; side += 2) {
            double kx = px + side * (PW / 2 * u + 40 * u + pull) + side * run * run * w * 0.6;
            double ky = py + 46 * u;
            double bob = run > 0 ? -Math.abs(Math.sin(t * 14)) * 5 * u : Math.sin(t * 2.1 + side) * 1.2 * u;
            double lean = giggleEnv3(t) * Math.sin(t * 40) * 1.6 * u;
            if (run < 1) {
                kidSilhouette(g, kx, ky, 1.0 * u, bob, lean, side, px + side * (PW / 2 * u + pull * 0.4), pyNow, a);
            }
        }

        // the sheet itself
        double jag = torn ? Math.sin(t * 60) * Math.exp(-tear * 6) * 2 * u : 0;
        for (int i = 0; i < 4; i++) {
            int sxq = (i % 2 == 0) ? -1 : 1;
            int syq = (i < 2) ? -1 : 1;
            double offX = sxq * 14 * u * sep + jag;
            double offY = syq * 7 * u * sep;
            double rot0 = torn ? sxq * syq * 0.10 * sep : 0;

            double p = clamp((t - (mark[M_HID] + i * 0.22)) / 1.1, 0, 1);
            double[] d = dest(i, w, h);
            double bx = px + offX, by = pyNow + offY;
            if (p > 0) {
                double e = smooth(p);
                double midX = (bx + d[0]) / 2, midY = Math.min(by, d[1]) - 150 * u;
                double om = 1 - e;
                double qx = om * om * bx + 2 * om * e * midX + e * e * d[0];
                double qy = om * om * by + 2 * om * e * midY + e * e * d[1];
                double sc = lerp(1, 0.30, e);
                double rot = rot0 + e * Math.PI * 4 * ((i % 2 == 0) ? 1 : -1);
                double fade = 1 - 0.65 * smooth((t - (mark[M_HID] + i * 0.22 + 1.1)) / 0.8);
                if (p < 1) {
                    for (int tr = 3; tr >= 1; tr--) {
                        double e2 = smooth(Math.max(0, p - tr * 0.05));
                        double o2 = 1 - e2;
                        double tx = o2 * o2 * bx + 2 * o2 * e2 * midX + e2 * e2 * d[0];
                        double ty = o2 * o2 * by + 2 * o2 * e2 * midY + e2 * e2 * d[1];
                        drawPiece(g, i, tx, ty, lerp(1, 0.30, e2), rot - tr * 0.2, 0.10 * a, true, u);
                    }
                }
                drawPiece(g, i, qx, qy, sc, rot, fade * a, true, u);
            } else {
                drawPiece(g, i, bx, by, 1.0, rot0, a, torn, u);
            }
        }

        // Soft shadow under the paper before it's torn
        if (!torn) {
            g.setColor(rgba(0, 0, 0, 0.22 * a));
            g.fill(new Ellipse2D.Double(px - PW * 0.5 * u, py + PH * 0.55 * u, PW * u, 10 * u));
        }

        // paper rips: little scraps flying
        if (tear > 0 && tear < 0.7) {
            Random r = new Random(5);
            for (int i = 0; i < 12; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double sp = 40 + r.nextDouble() * 90;
                double life = tear;
                double x = px + Math.cos(ang) * sp * life * u;
                double y = pyNow + Math.sin(ang) * sp * life * u + 120 * life * life * u;
                g.setColor(rgba(236, 220, 178, 0.9 * (1 - life / 0.7)));
                g.fill(new Rectangle2D.Double(x, y, 3 * u, 2 * u));
            }
        }

        moonMood(g, w, h, 1.0);
    }

    private double giggleEnv3(double t) {
        double x = (t - mark[M_HID]) / 0.9;
        return x >= 0 && x <= 1 ? Math.sin(Math.PI * x) : 0;
    }

    private void kidSilhouette(Graphics2D g, double x, double footY, double s, double bob,
                               double lean, int side, double armX, double armY, double alpha) {
        Composite oc = g.getComposite();
        g.setColor(new Color(24, 17, 24));
        g.setStroke(new BasicStroke((float) (5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - 4 * s, footY - 20 * s, x - 5 * s, footY));
        g.draw(new Line2D.Double(x + 4 * s, footY - 20 * s, x + 5 * s, footY));
        // arm reaching for the paper
        g.draw(new Line2D.Double(x - side * 3 * s, footY - 40 * s + bob, armX, armY + 2 * s));
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(x - 9 * s, footY - 44 * s + bob, 18 * s, 28 * s, 9 * s, 9 * s);
        Ellipse2D.Double head = new Ellipse2D.Double(x - 8.5 * s + lean, footY - 63 * s + bob, 17 * s, 17 * s);
        g.setStroke(new BasicStroke(2f));
        g.setColor(rgba(255, 214, 160, 0.26 * alpha));
        g.draw(body);
        g.draw(head);
        g.setColor(new Color(24, 17, 24));
        g.fill(body);
        g.fill(head);
        g.setComposite(oc);
    }

    private void drawPiece(Graphics2D g, int i, double x, double y, double sc, double rot,
                           double alpha, boolean torn, double u) {
        if (alpha <= 0.01) {
            return;
        }
        AffineTransform o = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        g.scale(sc * u, sc * u);
        Shape s = pieces[i];

        if (torn) {
            g.setColor(rgba(0, 0, 0, 0.28 * alpha));
            g.fill(AffineTransform.getTranslateInstance(2, 3).createTransformedShape(s));
        }
        g.setColor(rgba(236, 220, 178, alpha));
        g.fill(s);

        Shape oc = g.getClip();
        g.clip(s);
        g.setStroke(new BasicStroke(1f));
        g.setColor(rgba(120, 90, 60, 0.35 * alpha));
        for (double ly = -36; ly <= 38; ly += 9) {
            if (Math.abs(ly) > 20) {
                g.draw(new Line2D.Double(-54, ly, 54, ly));
            }
        }
        g.setFont(new Font("Serif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        for (int k = 0; k < 4; k++) {
            double bx = -45 + k * 30;
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(rgba(110, 70, 40, 0.85 * alpha));
            g.draw(new Rectangle2D.Double(bx - 12, -16, 24, 32));
            g.setColor(rgba(130, 60, 40, 0.9 * alpha));
            g.drawString("?", (float) (bx - fm.stringWidth("?") / 2.0), 8f);
        }
        g.setClip(oc);

        g.setStroke(new BasicStroke(1.3f));
        g.setColor(rgba(130, 98, 62, 0.9 * alpha));
        if (torn) {
            g.draw(s);
        } else {
            // Only the outside edge of the sheet before it tears.
            g.draw(new Rectangle2D.Double(-PW / 2, -PH / 2, PW, PH));
        }
        g.setTransform(o);
    }

    // Where each piece ends up hidden
    private double[] dest(int i, int w, int h) {
        switch (i) {
            case 0:  return new double[] { w * 0.082, h * 0.53 };   // behind the curtain
            case 1:  return new double[] { w * 0.905, h * 0.40 };   // in the bookshelf
            case 2:  return new double[] { w * 0.29, h * 0.88 };    // under the sofa
            default: return new double[] { w * 0.74, h * 0.94 };    // under the rug
        }
    }

    // =================================================
    // SCENE 4 - locked in, safe silent, pieces out there
    // =================================================

    private void scene4(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        double cx = w * 0.5;
        double dw = w * 0.12, dh = h * 0.44;

        roomBack(g, w, h, t, 1.0);

        // rattling front door with chain + padlock
        double since = t - mark[M_DOORLOCKED];
        double shake = (since > 0 && since < 1.0) ? Math.sin(since * 70) * 3.5 * Math.exp(-since * 4) * u : 0;
        AffineTransform o = g.getTransform();
        g.translate(shake, 0);
        frontDoor(g, (int) cx, floorY, dw, dh, 0, 0.0, t);
        chain(g, cx, floorY - dh / 2, dw, dh, u, t, since);
        g.setTransform(o);

        roomFront(g, w, h, t, 1.0, 2);

        // the safe: sits quietly under the bookshelf
        double sw = w * 0.13, sh = h * 0.19;
        double sx = w * 0.905;
        double silent = smooth((t - mark[M_SAFESILENT]) / 0.5);
        double[] lit = new double[4];
        double flickerOut = 1 - silent;
        for (int i = 0; i < 4; i++) {
            double fl = (t > mark[M_SAFESILENT] && t < mark[M_SAFESILENT] + 0.6)
                    ? (Math.sin(t * 40 + i) > 0 ? 1 : 0.2) : 1;
            lit[i] = flickerOut * fl * 0.9;
        }
        safe(g, sx - sw / 2, floorY - sh, sw, sh, lit, 0.35 * flickerOut, 0, t, u);

        // "hush" ripples around the dead safe
        double rp = t - mark[M_SAFESILENT];
        if (rp > 0 && rp < 1.6) {
            for (int k = 0; k < 2; k++) {
                double a = clamp((rp - k * 0.35) / 1.2, 0, 1);
                if (a > 0 && a < 1) {
                    g.setStroke(new BasicStroke(2f));
                    g.setColor(rgba(160, 170, 200, 0.4 * (1 - a)));
                    double rr = (24 + 70 * a) * u;
                    g.draw(new Ellipse2D.Double(sx - rr, floorY - sh / 2 - rr, rr * 2, rr * 2));
                }
            }
        }

        // pieces glinting where they were hidden
        double reveal = smooth((t - mark[M_PIECES]) / 0.6);
        for (int i = 0; i < 4; i++) {
            double[] d = dest(i, w, h);
            double tw = 0.5 + 0.5 * Math.sin(t * 3 + i * 1.7);
            double base = 0.25 + 0.15 * tw;
            peek(g, i, d[0], d[1], u, 0.35 + 0.65 * reveal);
            glint(g, d[0], d[1], (10 + 22 * reveal * (0.7 + 0.3 * tw)) * u, base + 0.65 * reveal * tw, t + i);
        }

        // heartbeat
        double pulse = heartPulse(t);
        if (pulse > 0) {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0),
                    (float) (Math.max(w, h) * 0.72), new float[] { 0.35f, 1f },
                    new Color[] { rgba(150, 10, 10, 0), rgba(150, 10, 10, 0.34 * pulse) }));
            g.fillRect(0, 0, w, h);
        }
        moonMood(g, w, h, 1.0);
    }

    private double heartPulse(double t) {
        if (t < mark[M_BEAT] || t > mark[M_BEATEND]) {
            return 0;
        }
        double p = (t - mark[M_BEAT]) % BEAT_PERIOD;
        double lub = Math.exp(-p * 9);
        double dub = p > 0.22 ? 0.7 * Math.exp(-(p - 0.22) * 10) : 0;
        return Math.max(lub, dub);
    }

    private void chain(Graphics2D g, double cx, double cy, double dw, double dh, double u, double t, double since) {
        g.setStroke(new BasicStroke((float) (2.2 * u)));
        for (int dir = -1; dir <= 1; dir += 2) {
            double x0 = cx - dw / 2 - 10 * u, y0 = cy - dir * dh * 0.28;
            double x1 = cx + dw / 2 + 10 * u, y1 = cy + dir * dh * 0.28;
            int n = 11;
            for (int i = 0; i <= n; i++) {
                double k = (double) i / n;
                double x = lerp(x0, x1, k), y = lerp(y0, y1, k) + Math.sin(k * Math.PI) * 6 * u;
                g.setColor(new Color(150, 150, 158));
                AffineTransform o = g.getTransform();
                g.translate(x, y);
                g.rotate(Math.atan2(y1 - y0, x1 - x0) + (i % 2 == 0 ? 0 : Math.PI / 2));
                g.draw(new Ellipse2D.Double(-6 * u, -3 * u, 12 * u, 6 * u));
                g.setTransform(o);
            }
        }
        double glow = since > 0 && since < 0.8 ? 1 - since / 0.8 : 0;
        padlock(g, cx, cy + 6 * u, 17 * u, 1.0, t);
        if (glow > 0) {
            g.setColor(rgba(255, 90, 70, 0.35 * glow));
            g.fill(new Ellipse2D.Double(cx - 30 * u, cy - 24 * u, 60 * u, 60 * u));
        }
    }

    // =================================================
    // SCENE 5 - the mission
    // =================================================

    private void scene5(Graphics2D g, int w, int h, double t) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        double cx = w * 0.5;
        double dw = w * 0.12, dh = h * 0.44;

        double warm = smooth((t - sceneStart[5]) / 1.5);
        double dusk = lerp(1.0, 0.55, warm);

        roomBack(g, w, h, t, dusk);

        // golden light behind the front door, then the door swings open
        double doorOpen = easeInOutCubic(clamp((t - mark[M_WALK]) / 1.8, 0, 1)) * 0.62;
        double seep = (0.45 + 0.25 * Math.sin(t * 2.4)) * warm;
        double cy = floorY - dh / 2;

        // rays from the door
        double rayA = warm * (0.07 + 0.10 * smooth((t - mark[M_WALK]) / 1.5));
        for (int i = 0; i < 9; i++) {
            double ang = -Math.PI / 2 + (i - 4) * 0.30 + Math.sin(t * 0.4 + i) * 0.04;
            double len = Math.max(w, h) * 1.2;
            Path2D.Double ray = new Path2D.Double();
            ray.moveTo(cx, cy);
            ray.lineTo(cx + Math.cos(ang - 0.05) * len, cy + Math.sin(ang - 0.05) * len);
            ray.lineTo(cx + Math.cos(ang + 0.05) * len, cy + Math.sin(ang + 0.05) * len);
            ray.closePath();
            g.setPaint(new GradientPaint((float) cx, (float) cy, rgba(255, 232, 160, rayA * 2.4),
                    (float) (cx + Math.cos(ang) * len), (float) (cy + Math.sin(ang) * len), rgba(255, 232, 160, 0)));
            g.fill(ray);
        }

        frontDoor(g, (int) cx, floorY, dw, dh, doorOpen, seep, t);

        roomFront(g, w, h, t, dusk, 2);

        // golden glow around the door
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) ((190 + 280 * doorOpen) * u),
                new float[] { 0f, 1f }, new Color[] { rgba(255, 220, 140, (0.32 + 0.4 * doorOpen) * warm), rgba(255, 220, 140, 0) }));
        g.fillRect(0, 0, w, h);

        // flashlight search
        double sEnv = smooth((t - (mark[M_SEARCH] - 0.3)) / 0.4) * (1 - smooth((t - (mark[M_PIECE] - 0.1)) / 0.5));
        if (sEnv > 0.01) {
            double s = t - mark[M_SEARCH];
            double fx = w * (0.5 + 0.34 * Math.sin(s * 1.7));
            double fy = h * (0.60 + 0.15 * Math.sin(s * 2.6 + 1));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(fx, fy), (float) (190 * u),
                    new float[] { 0f, 0.5f, 1f },
                    new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.55 * sEnv) }));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(fx, fy), (float) (140 * u),
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 240, 190, 0.18 * sEnv), rgba(255, 240, 190, 0) }));
            g.fillRect(0, 0, w, h);
        }

        // the code card reassembles on the left
        double cardX = w * 0.115, cardY = h * 0.72;
        double joined = 0;
        for (int i = 0; i < 4; i++) {
            double st = mark[M_PIECE] + i * 0.28;
            double p = clamp((t - st) / 1.0, 0, 1);
            double[] d = dest(i, w, h);
            if (t < mark[M_PIECE]) {
                double tw = 0.5 + 0.5 * Math.sin(t * 3 + i * 1.7);
                peek(g, i, d[0], d[1], u, 0.5);
                glint(g, d[0], d[1], (14 + 6 * tw) * u, 0.35 + 0.35 * tw, t + i);
            } else {
                double e = smooth(p);
                double midX = (d[0] + cardX) / 2, midY = Math.min(d[1], cardY) - 130 * u;
                double om = 1 - e;
                double qx = om * om * d[0] + 2 * om * e * midX + e * e * cardX;
                double qy = om * om * d[1] + 2 * om * e * midY + e * e * cardY;
                double sc = lerp(0.30, 0.85, e);
                double rot = (1 - e) * Math.PI * 3 * ((i % 2 == 0) ? 1 : -1);
                drawPiece(g, i, qx, qy, sc, rot, 1.0, true, u);
                if (p < 1) {
                    glint(g, qx, qy, 12 * u, 0.7, t + i);
                }
                if (p >= 1) {
                    joined++;
                }
            }
        }
        double doneT = mark[M_PIECE] + 3 * 0.28 + 1.0;
        if (t >= doneT) {
            double a = smooth((t - doneT) / 0.5);
            double pulse = 0.5 + 0.5 * Math.sin(t * 3.2);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cardX, cardY), (float) (110 * u),
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 220, 130, (0.28 + 0.12 * pulse) * a), rgba(255, 220, 130, 0) }));
            g.fillRect(0, 0, w, h);
            double ring = ((t - doneT) * 0.9) % 1.0;
            g.setStroke(new BasicStroke(2f));
            g.setColor(rgba(255, 226, 140, 0.5 * (1 - ring) * a));
            double rr = (60 + 50 * ring) * u;
            g.draw(new Ellipse2D.Double(cardX - rr, cardY - rr * 0.7, rr * 2, rr * 1.4));
            glint(g, cardX + 54 * u, cardY - 38 * u, 14 * u * (0.7 + 0.3 * pulse), a, t);
        }

        // the safe opens and the key floats out
        double sw = w * 0.13, sh = h * 0.19;
        double sx = w * 0.905;
        double open = easeInOutCubic(clamp((t - mark[M_OPEN]) / 0.9, 0, 1));
        double[] lit = { 0, 0, 0, 0 };
        if (open > 0) {
            double burst = smooth((t - mark[M_OPEN]) / 0.6);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, floorY - sh / 2), (float) (170 * u),
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 224, 140, 0.55 * burst), rgba(255, 224, 140, 0) }));
            g.fillRect(0, 0, w, h);
        }
        safe(g, sx - sw / 2, floorY - sh, sw, sh, lit, open > 0 ? 0 : 0.55, open, t, u);
        if (open > 0.15) {
            double rise = easeOutCubic(clamp((t - mark[M_OPEN] - 0.3) / 1.0, 0, 1));
            double ky = floorY - sh / 2 - rise * (sh * 0.95) + Math.sin(t * 2.6) * 3 * u * rise;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, ky), (float) (46 * u),
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 230, 150, 0.6 * rise), rgba(255, 230, 150, 0) }));
            g.fill(new Ellipse2D.Double(sx - 50 * u, ky - 50 * u, 100 * u, 100 * u));
            key(g, sx, ky, 20 * u, -0.55 + Math.sin(t * 2.0) * 0.12, GOLD, rise);
            if (rise > 0.5) {
                glint(g, sx + 26 * u, ky - 20 * u, 10 * u, 0.5 + 0.5 * Math.sin(t * 5), t);
            }
        }

        warmGrade(g, w, h, 0.12 * warm);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0), (float) (Math.max(w, h) * 0.78),
                new float[] { 0.5f, 1f }, new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.45) }));
        g.fillRect(0, 0, w, h);
    }

    // =================================================
    // The living room (scenes 0, 1, 3, 4, 5)
    // =================================================

    private double duskAt(double t) {
        double end = Math.max(1, mark[M_SILENCE]);
        return 0.30 + 0.55 * clamp(t / end, 0, 1);
    }

    private void roomBack(Graphics2D g, int w, int h, double t, double dusk) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);
        int cx = w / 2;

        // wall
        g.setPaint(new GradientPaint(0, 0, mix(new Color(98, 68, 50), new Color(36, 30, 48), dusk),
                0, floorY, mix(new Color(74, 52, 40), new Color(26, 22, 36), dusk)));
        g.fillRect(0, 0, w, floorY);
        g.setColor(rgba(0, 0, 0, 0.07));
        for (int x = 0; x < w; x += 56) {
            g.fillRect(x, 0, 2, floorY);
        }
        int wain = (int) (h * 0.14);
        g.setColor(mix(new Color(72, 50, 36), new Color(30, 24, 36), dusk));
        g.fillRect(0, floorY - wain, w, wain);
        g.setColor(mix(new Color(112, 80, 54), new Color(52, 40, 54), dusk));
        g.fillRect(0, floorY - wain - 3, w, 4);

        // floor
        g.setPaint(new GradientPaint(0, floorY, mix(new Color(86, 58, 40), new Color(30, 22, 28), dusk),
                0, h, mix(new Color(46, 32, 24), new Color(12, 9, 14), dusk)));
        g.fillRect(0, floorY, w, h - floorY);
        g.setColor(rgba(0, 0, 0, 0.2));
        for (int k = -12; k <= 12; k++) {
            g.drawLine(cx + k * 40, floorY, cx + k * 130, h);
        }

        // window
        int wx = (int) (w * 0.075), wy = (int) (h * 0.20), ww = (int) (w * 0.19), wh = (int) (h * 0.38);
        Color top = mix(new Color(90, 74, 150), new Color(10, 14, 38), dusk);
        Color hor = mix(new Color(255, 150, 80), new Color(36, 36, 80), dusk);
        g.setPaint(new GradientPaint(0, wy, top, 0, wy + wh, hor));
        g.fillRect(wx, wy, ww, wh);

        Shape oldClip = g.getClip();
        g.clipRect(wx, wy, ww, wh);

        // setting sun
        if (dusk < 0.92) {
            double sunY = wy + wh * (0.55 + 0.7 * dusk);
            double sr = 26 * u;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(wx + ww * 0.55, sunY), (float) (sr * 3),
                    new float[] { 0f, 1f },
                    new Color[] { rgba(255, 210, 130, 0.55 * (1 - dusk)), rgba(255, 210, 130, 0) }));
            g.fillRect(wx, wy, ww, wh);
            g.setColor(rgba(255, 226, 150, 1 - dusk));
            g.fill(new Ellipse2D.Double(wx + ww * 0.55 - sr, sunY - sr, sr * 2, sr * 2));
        }
        // stars + moon
        double night = clamp((dusk - 0.45) / 0.5, 0, 1);
        if (night > 0) {
            for (int i = 0; i < starX.length; i++) {
                double tw = 0.5 + 0.5 * Math.sin(t * 2 + starPh[i]);
                g.setColor(rgba(255, 250, 225, night * (0.3 + 0.6 * tw)));
                g.fill(new Rectangle2D.Double(wx + starX[i] * ww, wy + starY[i] * wh, 1.8, 1.8));
            }
            double mx = wx + ww * 0.68, my = wy + wh * 0.28, mr = 14 * u;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(mx, my), (float) (mr * 3),
                    new float[] { 0f, 1f }, new Color[] { rgba(210, 225, 255, 0.30 * night), rgba(210, 225, 255, 0) }));
            g.fillRect(wx, wy, ww, wh);
            g.setColor(rgba(238, 240, 220, night));
            g.fill(new Ellipse2D.Double(mx - mr, my - mr, mr * 2, mr * 2));
            g.setColor(mix(new Color(10, 14, 38), top, 0.5));
            g.fill(new Ellipse2D.Double(mx - mr + mr * 0.55, my - mr - mr * 0.1, mr * 2, mr * 2));
        }
        // hills
        Path2D.Double hills = new Path2D.Double();
        hills.moveTo(wx, wy + wh);
        for (int x = 0; x <= ww; x += 6) {
            hills.lineTo(wx + x, wy + wh * 0.80 - Math.sin(x * 0.035 + 1) * wh * 0.05 - Math.sin(x * 0.012) * wh * 0.05);
        }
        hills.lineTo(wx + ww, wy + wh);
        hills.closePath();
        g.setColor(mix(new Color(54, 40, 70), new Color(8, 10, 22), dusk));
        g.fill(hills);
        g.setClip(oldClip);

        // frame + panes
        g.setColor(new Color(60, 40, 28));
        g.setStroke(new BasicStroke(6f));
        g.draw(new Rectangle2D.Double(wx, wy, ww, wh));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Line2D.Double(wx + ww / 2.0, wy, wx + ww / 2.0, wy + wh));
        g.draw(new Line2D.Double(wx, wy + wh * 0.45, wx + ww, wy + wh * 0.45));

        // curtains
        Color cur = mix(new Color(138, 44, 52), new Color(58, 22, 34), dusk);
        Path2D.Double cl = new Path2D.Double();
        cl.moveTo(wx - 20, wy - 10);
        cl.lineTo(wx + ww * 0.16, wy - 10);
        for (int y = 0; y <= wh + 30; y += 10) {
            cl.lineTo(wx + ww * 0.12 + Math.sin(y * 0.09 + t * 0.4) * 4, wy + y);
        }
        cl.lineTo(wx - 20, wy + wh + 30);
        cl.closePath();
        g.setColor(cur);
        g.fill(cl);
        Path2D.Double cr = new Path2D.Double();
        cr.moveTo(wx + ww + 20, wy - 10);
        cr.lineTo(wx + ww * 0.84, wy - 10);
        for (int y = 0; y <= wh + 30; y += 10) {
            cr.lineTo(wx + ww * 0.88 + Math.sin(y * 0.09 + 1 + t * 0.4) * 4, wy + y);
        }
        cr.lineTo(wx + ww + 20, wy + wh + 30);
        cr.closePath();
        g.fill(cr);
        g.setColor(new Color(90, 64, 40));
        g.fill(new Rectangle2D.Double(wx - 26, wy - 16, ww + 52, 7));

        // light from the window on the floor
        double beam = dusk < 0.8 ? 0.16 * (1 - dusk) : 0.10 * clamp((dusk - 0.8) / 0.2, 0, 1);
        Color bc = dusk < 0.8 ? new Color(255, 190, 110) : new Color(170, 190, 255);
        Path2D.Double fl = new Path2D.Double();
        fl.moveTo(wx + 10, wy + wh);
        fl.lineTo(wx + ww - 10, wy + wh);
        fl.lineTo(wx + ww + 200 * u, h);
        fl.lineTo(wx + 40 * u, h);
        fl.closePath();
        g.setPaint(new GradientPaint(0, floorY - 60, rgba(bc.getRed(), bc.getGreen(), bc.getBlue(), beam),
                0, h, rgba(bc.getRed(), bc.getGreen(), bc.getBlue(), 0)));
        g.fill(fl);

        // rug
        g.setColor(mix(new Color(120, 52, 44), new Color(44, 24, 34), dusk));
        g.fill(new Ellipse2D.Double(w * 0.22, floorY + (h - floorY) * 0.45, w * 0.56, (h - floorY) * 0.42));
        g.setColor(mix(new Color(214, 170, 90), new Color(70, 56, 60), dusk));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(w * 0.24, floorY + (h - floorY) * 0.50, w * 0.52, (h - floorY) * 0.32));
    }

    /** Sofa + right-hand furniture. mode: 0 = lamp + table, 1 = playroom door area, 2 = bookshelf. */
    private void roomFront(Graphics2D g, int w, int h, double t, double dusk, int mode) {

        double u = unit(w, h);
        int floorY = (int) (h * 0.78);

        // sofa
        double sx = w * 0.23, sw = w * 0.40;
        Color sofa = mix(new Color(116, 50, 46), new Color(48, 26, 32), dusk);
        Color sofaDark = mix(new Color(88, 36, 34), new Color(36, 18, 26), dusk);
        g.setColor(sofaDark);
        g.fill(new RoundRectangle2D.Double(sx, floorY - 64 * u, sw, 72 * u, 26, 26));
        g.setColor(sofa);
        g.fill(new RoundRectangle2D.Double(sx - 10 * u, floorY - 24 * u, sw + 20 * u, 48 * u, 18, 18));
        g.fill(new RoundRectangle2D.Double(sx - 26 * u, floorY - 50 * u, 38 * u, 76 * u, 16, 16));
        g.fill(new RoundRectangle2D.Double(sx + sw - 12 * u, floorY - 50 * u, 38 * u, 76 * u, 16, 16));
        g.setColor(mix(new Color(140, 64, 56), new Color(58, 32, 40), dusk));
        g.fill(new RoundRectangle2D.Double(sx + 14 * u, floorY - 30 * u, sw / 2 - 20 * u, 30 * u, 12, 12));
        g.fill(new RoundRectangle2D.Double(sx + sw / 2 + 6 * u, floorY - 30 * u, sw / 2 - 20 * u, 30 * u, 12, 12));

        if (mode <= 1) {
            double secT = Math.min(t, mark[M_SILENCE]);
            clock(g, w * 0.895, h * 0.15, 30 * u, secT, u);
        }

        if (mode == 0) {
            lampAndMug(g, w, h, t, u, floorY);
        } else if (mode == 2) {
            bookshelf(g, w, h, dusk, u, floorY);
        }
    }

    private void lampAndMug(Graphics2D g, int w, int h, double t, double u, int floorY) {
        double lx = w * 0.915;
        double flick = 0.85 + 0.15 * Math.sin(t * 11.3) * Math.sin(t * 5.7);

        g.setPaint(new RadialGradientPaint(new Point2D.Double(lx, h * 0.40), (float) (300 * u),
                new float[] { 0f, 1f },
                new Color[] { rgba(255, 190, 110, 0.38 * flick), rgba(255, 190, 110, 0) }));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(44, 32, 26));
        g.fill(new Rectangle2D.Double(lx - 2 * u, h * 0.40, 4 * u, floorY - h * 0.40));
        g.fill(new Ellipse2D.Double(lx - 20 * u, floorY - 4 * u, 40 * u, 10 * u));
        Path2D.Double shade = new Path2D.Double();
        shade.moveTo(lx - 14 * u, h * 0.30);
        shade.lineTo(lx + 14 * u, h * 0.30);
        shade.lineTo(lx + 26 * u, h * 0.40);
        shade.lineTo(lx - 26 * u, h * 0.40);
        shade.closePath();
        g.setColor(rgba(255, 214, 150, 0.95));
        g.fill(shade);

        // side table + mug with steam
        double tx = w * 0.835;
        g.setColor(new Color(70, 48, 34));
        g.fill(new Ellipse2D.Double(tx - 34 * u, floorY - 46 * u, 68 * u, 12 * u));
        g.fill(new Rectangle2D.Double(tx - 3 * u, floorY - 40 * u, 6 * u, 40 * u));
        g.fill(new Ellipse2D.Double(tx - 22 * u, floorY - 4 * u, 44 * u, 8 * u));
        g.setColor(new Color(220, 210, 190));
        g.fill(new RoundRectangle2D.Double(tx - 9 * u, floorY - 66 * u, 18 * u, 22 * u, 5, 5));
        g.setStroke(new BasicStroke((float) (2.4 * u)));
        g.draw(new Arc2DHelper().handle(tx + 9 * u, floorY - 56 * u, 6 * u));
        g.setStroke(new BasicStroke((float) (2.2 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 3; k++) {
            double ph = (t * 0.5 + k * 0.33) % 1.0;
            Path2D.Double steam = new Path2D.Double();
            double sx0 = tx - 5 * u + k * 5 * u;
            steam.moveTo(sx0, floorY - 68 * u);
            for (int s = 1; s <= 8; s++) {
                double yy = floorY - 68 * u - s * 5 * u - ph * 8 * u;
                steam.lineTo(sx0 + Math.sin(s * 0.9 + t * 2 + k) * 4 * u, yy);
            }
            g.setColor(rgba(255, 255, 255, 0.28 * (1 - ph)));
            g.draw(steam);
        }
    }

    private void bookshelf(Graphics2D g, int w, int h, double dusk, double u, int floorY) {
        double bx = w * 0.835, bw = w * 0.14, by = h * 0.17, bh = h * 0.45;
        g.setColor(mix(new Color(86, 58, 38), new Color(36, 26, 30), dusk));
        g.fill(new Rectangle2D.Double(bx, by, bw, bh));
        Random r = new Random(7);
        int rows = 4;
        for (int row = 0; row < rows; row++) {
            double ry = by + 6 * u + row * (bh - 12 * u) / rows;
            double rh = (bh - 12 * u) / rows - 6 * u;
            double x = bx + 6 * u;
            while (x < bx + bw - 12 * u) {
                double bwid = (6 + r.nextInt(7)) * u;
                double bhh = rh * (0.72 + r.nextDouble() * 0.28);
                Color c = mix(new Color(70 + r.nextInt(130), 50 + r.nextInt(80), 40 + r.nextInt(90)),
                        new Color(24, 22, 34), dusk * 0.72);
                g.setColor(c);
                g.fill(new Rectangle2D.Double(x, ry + rh - bhh, bwid, bhh));
                x += bwid + 1.5 * u;
            }
            g.setColor(mix(new Color(60, 40, 28), new Color(26, 18, 24), dusk));
            g.fill(new Rectangle2D.Double(bx, ry + rh, bw, 5 * u));
        }
        g.setColor(mix(new Color(60, 40, 28), new Color(26, 18, 24), dusk));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Rectangle2D.Double(bx, by, bw, bh));
    }

    private void clock(Graphics2D g, double cx, double cy, double r, double secT, double u) {
        g.setColor(rgba(0, 0, 0, 0.35));
        g.fill(new Ellipse2D.Double(cx - r + 2, cy - r + 3, 2 * r, 2 * r));
        g.setColor(new Color(120, 84, 46));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(new Color(232, 218, 182));
        g.fill(new Ellipse2D.Double(cx - r + 4, cy - r + 4, 2 * r - 8, 2 * r - 8));
        g.setColor(new Color(60, 40, 24));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            double r1 = r - 7, r2 = r - (i % 3 == 0 ? 15 : 11);
            g.setStroke(new BasicStroke(i % 3 == 0 ? 2.2f : 1.2f));
            g.draw(new Line2D.Double(cx + Math.sin(a) * r1, cy - Math.cos(a) * r1,
                    cx + Math.sin(a) * r2, cy - Math.cos(a) * r2));
        }
        hand(g, cx, cy, Math.toRadians(230), r * 0.48, 3.6f, new Color(40, 28, 20));
        hand(g, cx, cy, Math.toRadians(240), r * 0.72, 2.6f, new Color(40, 28, 20));
        double whole = Math.floor(secT);
        double frac = secT - whole;
        double k = easeOutBack(clamp(frac / 0.18, 0, 1));
        double ang = Math.toRadians((whole - 1 + k) * 6);
        hand(g, cx, cy, ang, r * 0.80, 1.4f, new Color(190, 40, 30));
        g.setColor(new Color(190, 40, 30));
        g.fill(new Ellipse2D.Double(cx - 3, cy - 3, 6, 6));
    }

    private void hand(Graphics2D g, double cx, double cy, double ang, double len, float w, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - Math.sin(ang) * len * 0.12, cy + Math.cos(ang) * len * 0.12,
                cx + Math.sin(ang) * len, cy - Math.cos(ang) * len));
    }

    // =================================================
    // Props: front door, safe, padlock, key, glints
    // =================================================

    private void frontDoor(Graphics2D g, int cx, int floorY, double dw, double dh,
                           double open, double seep, double t) {

        double dl = cx - dw / 2, dt = floorY - dh;
        g.setColor(new Color(58, 38, 26));
        g.fill(new Rectangle2D.Double(dl - 12, dt - 12, dw + 24, dh + 12));
        g.setColor(new Color(40, 26, 18));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Rectangle2D.Double(dl - 12, dt - 12, dw + 24, dh + 12));

        if (open > 0.001) {
            g.setPaint(new GradientPaint(0, (float) dt, new Color(255, 246, 215), 0, (float) floorY, new Color(255, 214, 140)));
            g.fill(new Rectangle2D.Double(dl, dt, dw, dh));
            // light on the floor
            Path2D.Double shaft = new Path2D.Double();
            shaft.moveTo(dl + 6, floorY);
            shaft.lineTo(dl + dw - 6, floorY);
            shaft.lineTo(cx + dw * 1.2, floorY + dh * 0.6);
            shaft.lineTo(cx - dw * 1.2, floorY + dh * 0.6);
            shaft.closePath();
            g.setPaint(new GradientPaint(0, floorY, rgba(255, 244, 210, 0.55 * clamp(open / 0.3, 0, 1)),
                    0, (float) (floorY + dh * 0.6), rgba(255, 244, 210, 0)));
            g.fill(shaft);
        } else {
            g.setColor(new Color(18, 12, 10));
            g.fill(new Rectangle2D.Double(dl, dt, dw, dh));
        }

        double ang = Math.toRadians(70) * open / 0.62;
        double leafW = dw * Math.cos(Math.min(ang, Math.toRadians(80)));
        double f = 1.0 / (1.0 + dw * Math.sin(ang) / (dw * 2.4));
        double xh = dl, xf = dl + leafW;
        double cy = dt + dh / 2.0;
        double shade = 1 - 0.35 * open;

        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(xh, dt);
        leaf.lineTo(xf, cy - dh / 2 * f);
        leaf.lineTo(xf, cy + dh / 2 * f);
        leaf.lineTo(xh, dt + dh);
        leaf.closePath();
        g.setColor(shaded(122, 78, 46, shade));
        g.fill(leaf);
        g.setColor(shaded(96, 60, 36, shade));
        g.fill(inset(xh, dt, xf, cy - dh / 2 * f, xf, cy + dh / 2 * f, xh, dt + dh, 0.14, 0.07, 0.86, 0.45));
        g.fill(inset(xh, dt, xf, cy - dh / 2 * f, xf, cy + dh / 2 * f, xh, dt + dh, 0.14, 0.55, 0.86, 0.93));
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(shaded(56, 36, 22, shade));
        g.draw(leaf);

        double kx = lerp(xh, xf, 0.86);
        double ky = lerp(dt, cy - dh / 2 * f, 0.86) + (lerp(dt + dh, cy + dh / 2 * f, 0.86) - lerp(dt, cy - dh / 2 * f, 0.86)) * 0.52;
        g.setColor(new Color(214, 170, 70));
        g.fill(new Ellipse2D.Double(kx - 5, ky - 5, 10, 10));

        // golden light seeping through the keyhole + cracks
        if (seep > 0.01 && open < 0.02) {
            g.setColor(rgba(255, 226, 140, 0.75 * seep));
            g.fill(new Rectangle2D.Double(dl - 1, dt, 2, dh));
            g.fill(new Rectangle2D.Double(dl + dw - 1, dt, 2, dh));
            g.fill(new Rectangle2D.Double(dl, floorY - 2, dw, 2));
            g.fill(new Rectangle2D.Double(dl, dt, dw, 2));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(kx, ky + 8), 34f,
                    new float[] { 0f, 1f }, new Color[] { rgba(255, 230, 150, 0.8 * seep), rgba(255, 230, 150, 0) }));
            g.fill(new Ellipse2D.Double(kx - 40, ky - 30, 80, 80));
        }
    }

    private void safe(Graphics2D g, double x, double y, double sw, double sh,
                      double[] lit, double keyGlow, double open, double t, double u) {

        // back box
        g.setColor(new Color(22, 22, 28));
        g.fill(new RoundRectangle2D.Double(x, y, sw, sh, 10, 10));
        // feet
        g.setColor(new Color(20, 20, 24));
        g.fill(new Rectangle2D.Double(x + 8 * u, y + sh, 14 * u, 5 * u));
        g.fill(new Rectangle2D.Double(x + sw - 22 * u, y + sh, 14 * u, 5 * u));

        if (open > 0.01) {
            g.setPaint(new GradientPaint(0, (float) y, new Color(255, 236, 170), 0, (float) (y + sh), new Color(210, 150, 70)));
            g.fill(new RoundRectangle2D.Double(x + 6 * u, y + 6 * u, sw - 12 * u, sh - 12 * u, 6, 6));
        }

        double ang = Math.toRadians(80) * open;
        double sc = Math.max(0.05, Math.cos(ang));

        AffineTransform o = g.getTransform();
        g.translate(x, y);
        g.scale(sc, 1);

        // door face
        g.setPaint(new GradientPaint(0, 0, new Color(74, 80, 92), 0, (float) sh, new Color(34, 36, 44)));
        g.fill(new RoundRectangle2D.Double(0, 0, sw, sh, 10, 10));
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(214, 170, 70));
        g.draw(new RoundRectangle2D.Double(1.5, 1.5, sw - 3, sh - 3, 10, 10));
        g.setColor(rgba(255, 255, 255, 0.07));
        g.draw(new RoundRectangle2D.Double(8 * u, 8 * u, sw - 16 * u, sh - 16 * u, 6, 6));

        // porthole with the key inside
        double pr = sw * 0.15, pcx = sw * 0.30, pcy = sh * 0.36;
        g.setColor(new Color(10, 10, 14));
        g.fill(new Ellipse2D.Double(pcx - pr, pcy - pr, pr * 2, pr * 2));
        if (keyGlow > 0.01) {
            Shape oc = g.getClip();
            g.clip(new Ellipse2D.Double(pcx - pr, pcy - pr, pr * 2, pr * 2));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(pcx, pcy), (float) pr,
                    new float[] { 0f, 1f }, new Color[] { rgba(255, 215, 120, 0.7 * keyGlow), rgba(255, 215, 120, 0.05) }));
            g.fill(new Ellipse2D.Double(pcx - pr, pcy - pr, pr * 2, pr * 2));
            key(g, pcx, pcy, pr * 0.62, -0.5, GOLD, Math.min(1, keyGlow + 0.2));
            g.setClip(oc);
        }
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(214, 170, 70));
        g.draw(new Ellipse2D.Double(pcx - pr, pcy - pr, pr * 2, pr * 2));
        g.setColor(rgba(255, 255, 255, 0.25));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new java.awt.geom.Arc2D.Double(pcx - pr * 0.7, pcy - pr * 0.7, pr * 1.4, pr * 1.4, 110, 70, java.awt.geom.Arc2D.OPEN));

        // 4-digit keypad
        g.setFont(new Font("Serif", Font.BOLD, (int) Math.max(10, 14 * u)));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < 4; i++) {
            double kx = sw * 0.54 + i * sw * 0.105;
            double ky = sh * 0.17;
            double kw = sw * 0.085, kh = sh * 0.16;
            double l = lit[i];
            g.setColor(new Color(18, 18, 22));
            g.fill(new RoundRectangle2D.Double(kx, ky, kw, kh, 4, 4));
            if (l > 0.01) {
                g.setColor(rgba(255, 176, 60, 0.9 * l));
                g.fill(new RoundRectangle2D.Double(kx + 1.5, ky + 1.5, kw - 3, kh - 3, 3, 3));
                g.setColor(rgba(60, 30, 0, l));
                g.drawString("?", (float) (kx + (kw - fm.stringWidth("?")) / 2), (float) (ky + kh * 0.78));
            }
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(214, 170, 70, 160));
            g.draw(new RoundRectangle2D.Double(kx, ky, kw, kh, 4, 4));
        }

        // dial + handle
        double dr = sw * 0.17, dcx = sw * 0.68, dcy = sh * 0.66;
        g.setColor(new Color(24, 26, 32));
        g.fill(new Ellipse2D.Double(dcx - dr, dcy - dr, dr * 2, dr * 2));
        g.setStroke(new BasicStroke(2.6f));
        g.setColor(new Color(214, 170, 70));
        g.draw(new Ellipse2D.Double(dcx - dr, dcy - dr, dr * 2, dr * 2));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            g.draw(new Line2D.Double(dcx + Math.sin(a) * dr * 0.78, dcy - Math.cos(a) * dr * 0.78,
                    dcx + Math.sin(a) * dr * 0.95, dcy - Math.cos(a) * dr * 0.95));
        }
        double ha = open * 1.2;
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(dcx - Math.sin(ha) * dr * 0.6, dcy + Math.cos(ha) * dr * 0.6,
                dcx + Math.sin(ha) * dr * 0.6, dcy - Math.cos(ha) * dr * 0.6));
        g.draw(new Line2D.Double(dcx - Math.cos(ha) * dr * 0.6, dcy - Math.sin(ha) * dr * 0.6,
                dcx + Math.cos(ha) * dr * 0.6, dcy + Math.sin(ha) * dr * 0.6));
        g.setColor(new Color(214, 170, 70));
        g.fill(new Ellipse2D.Double(dcx - 3, dcy - 3, 6, 6));

        g.setTransform(o);
    }

    private void padlock(Graphics2D g, double cx, double cy, double s, double alpha, double t) {
        if (s <= 0.5) {
            return;
        }
        g.setStroke(new BasicStroke((float) (s * 0.28), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(rgba(190, 190, 200, alpha));
        g.draw(new java.awt.geom.Arc2D.Double(cx - s * 0.55, cy - s * 1.25, s * 1.1, s * 1.3, 0, 180, java.awt.geom.Arc2D.OPEN));
        g.setPaint(new GradientPaint(0, (float) (cy - s * 0.3), rgba(240, 200, 90, alpha),
                0, (float) (cy + s * 0.9), rgba(180, 130, 40, alpha)));
        g.fill(new RoundRectangle2D.Double(cx - s * 0.85, cy - s * 0.3, s * 1.7, s * 1.25, s * 0.3, s * 0.3));
        g.setColor(rgba(40, 26, 10, alpha));
        g.fill(new Ellipse2D.Double(cx - s * 0.16, cy + s * 0.05, s * 0.32, s * 0.32));
        g.fill(new Rectangle2D.Double(cx - s * 0.07, cy + s * 0.25, s * 0.14, s * 0.4));
    }

    private static Area keyArea() {
        Area a = new Area(new Ellipse2D.Double(-1.02, -0.32, 0.64, 0.64));
        a.subtract(new Area(new Ellipse2D.Double(-0.86, -0.16, 0.32, 0.32)));
        a.add(new Area(new Rectangle2D.Double(-0.4, -0.07, 1.3, 0.14)));
        a.add(new Area(new Rectangle2D.Double(0.55, 0.07, 0.12, 0.24)));
        a.add(new Area(new Rectangle2D.Double(0.75, 0.07, 0.12, 0.18)));
        return a;
    }

    private void key(Graphics2D g, double cx, double cy, double size, double rot, Color c, double alpha) {
        AffineTransform o = g.getTransform();
        g.translate(cx, cy);
        g.rotate(rot);
        g.scale(size, size);
        g.setColor(rgba(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        g.fill(keyArea());
        g.setTransform(o);
    }

    private void peek(Graphics2D g, int i, double x, double y, double u, double strength) {
        drawPiece(g, i, x, y, 0.30, i * 1.1 + 0.4, 0.55 * strength + 0.25, true, u);
    }

    private void glint(Graphics2D g, double x, double y, double size, double alpha, double phase) {
        if (alpha <= 0.01) {
            return;
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) Math.max(2, size * 1.5),
                new float[] { 0f, 1f }, new Color[] { rgba(255, 236, 170, 0.5 * alpha), rgba(255, 236, 170, 0) }));
        g.fill(new Ellipse2D.Double(x - size * 1.5, y - size * 1.5, size * 3, size * 3));
        AffineTransform o = g.getTransform();
        g.translate(x, y);
        g.rotate(phase * 0.4);
        Path2D.Double star = new Path2D.Double();
        double r1 = size, r2 = size * 0.14;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            double r = (i % 2 == 0) ? r1 : r2;
            double px = Math.sin(a) * r, py = -Math.cos(a) * r;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        g.setColor(rgba(255, 250, 220, clamp(alpha, 0, 1)));
        g.fill(star);
        g.setTransform(o);
    }

    private void moonMood(Graphics2D g, int w, int h, double k) {
        g.setColor(rgba(10, 14, 40, 0.18 * k));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0), (float) (Math.max(w, h) * 0.75),
                new float[] { 0.5f, 1f }, new Color[] { rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.5) }));
        g.fillRect(0, 0, w, h);
    }

    private void warmGrade(Graphics2D g, int w, int h, double a) {
        if (a > 0.005) {
            g.setColor(rgba(255, 170, 80, a));
            g.fillRect(0, 0, w, h);
        }
    }

    // small helper so hooks / handles can be drawn as open arcs
    private static final class Arc2DHelper {
        Shape hook(double x, double y, double r) {
            return new java.awt.geom.Arc2D.Double(x - r, y - r * 0.4, r * 2, r * 1.6, 200, 250, java.awt.geom.Arc2D.OPEN);
        }

        Shape handle(double x, double y, double r) {
            return new java.awt.geom.Arc2D.Double(x - r, y - r, r * 2, r * 2, -90, 180, java.awt.geom.Arc2D.OPEN);
        }
    }

    // =================================================
    // Math / colour helpers
    // =================================================

    /** Sub-quad of a four-corner leaf, in (u across, v down) fractions. */
    private static Path2D.Double inset(double x0, double t0, double x1, double t1,
                                       double x1b, double b1, double x0b, double b0,
                                       double u0, double v0, double u1, double v1) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(qx(x0, x1, u0), qy(t0, t1, b0, b1, u0, v0));
        p.lineTo(qx(x0, x1, u1), qy(t0, t1, b0, b1, u1, v0));
        p.lineTo(qx(x0, x1, u1), qy(t0, t1, b0, b1, u1, v1));
        p.lineTo(qx(x0, x1, u0), qy(t0, t1, b0, b1, u0, v1));
        p.closePath();
        return p;
    }

    private static double qx(double xh, double xf, double u) {
        return lerp(xh, xf, u);
    }

    private static double qy(double th, double tf, double bh, double bf, double u, double v) {
        double top = lerp(th, tf, u);
        double bot = lerp(bh, bf, u);
        return lerp(top, bot, v);
    }

    private static double unit(int w, int h) {
        return Math.min(w / 960.0, h / 640.0);
    }

    private static Color rgba(int r, int g, int b, double a) {
        return new Color(r, g, b, (int) clamp(a * 255, 0, 255));
    }

    private static Color mix(Color a, Color b, double k) {
        k = clamp(k, 0, 1);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    private static Color shaded(int r, int g, int b, double s) {
        return new Color((int) clamp(r * s, 0, 255), (int) clamp(g * s, 0, 255), (int) clamp(b * s, 0, 255));
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
}
