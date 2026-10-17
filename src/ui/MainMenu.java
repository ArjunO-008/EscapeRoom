package ui;

import audio.SoundManager;
import audio.SoundManager.Sfx;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import save.SaveManager;

/**
 * Interactive main menu: NEW GAME / CONTINUE / EXIT.
 *
 * Usage from the host panel (the component that paints the menu):
 *   menu.setListener(...);
 *   menu.attachTo(this);          // once: installs mouse + keyboard + repaint timer
 *   menu.show();                  // whenever the menu becomes the active screen
 *   menu.draw(g2, getWidth(), getHeight());   // from paintComponent
 *
 * Input is fully self-contained: keyboard works even if the panel does not
 * have focus (as long as its window is active), and mouse coordinates are
 * automatically mapped if the menu is drawn at a size different from the
 * component's real size.
 */
public class MainMenu {

    public interface Listener {
        void onNewGame();
        void onContinue();
        void onExit();
    }

    /* ---------- Palette ---------- */
    private static final Color BG = new Color(25, 18, 14);
    private static final Color PARCHMENT = new Color(235, 218, 180);
    private static final Color WOOD = new Color(105, 65, 38);
    private static final Color WOOD_LIGHT = new Color(150, 98, 56);
    private static final Color WOOD_DARK = new Color(62, 38, 22);
    private static final Color LAMP = new Color(255, 190, 110);

    private static final int NEW_GAME = 0;
    private static final int CONTINUE = 1;
    private static final int EXIT = 2;
    private static final int ITEM_COUNT = 3;
    private static final String[] LABELS = {"NEW GAME", "CONTINUE", "EXIT"};

    private static final double FADE_IN_SECONDS = 0.6;
    private static final double FADE_OUT_SECONDS = 0.45;
    private static final double MESSAGE_SECONDS = 3.0;
    private static final int MOTE_COUNT = 36;

    private Listener listener;
    private boolean hasSave;

    private boolean active = true;
    private Component host;
    private KeyEventDispatcher keyDispatcher;
    private MouseAdapter mouseHandler;
    private Timer repaintTimer;

    /* ---------- Interaction state ---------- */
    private int selected = NEW_GAME;
    private int pressed = -1;
    private boolean confirmingExit;
    private int confirmSelected;      // 0 = STAY, 1 = EXIT
    private int confirmPressed = -1;
    private int pendingAction = -1;
    private boolean actionLaunched;
    private String message;

    /* ---------- Animation state ---------- */
    private final float[] glow = new float[ITEM_COUNT];
    private final float[] confirmGlow = new float[2];
    private float confirmFade;
    private double clock;
    private double introTime;
    private double leaveTime;
    private double messageAge;
    private double flicker = 1.0;
    private long lastNanos;

    /* ---------- Layout ---------- */
    private final Rectangle[] bounds = new Rectangle[ITEM_COUNT];
    private final Rectangle[] confirmBounds = new Rectangle[2];
    private int drawW;   // size used in the last draw(), for mouse mapping
    private int drawH;

    /* ---------- Dust motes ---------- */
    private final float[] moteX = new float[MOTE_COUNT];
    private final float[] moteY = new float[MOTE_COUNT];
    private final float[] moteSpeed = new float[MOTE_COUNT];
    private final float[] moteSize = new float[MOTE_COUNT];
    private final float[] motePhase = new float[MOTE_COUNT];

    public MainMenu() {
        Random rnd = new Random(42);
        for (int i = 0; i < MOTE_COUNT; i++) {
            moteX[i] = rnd.nextFloat();
            moteY[i] = rnd.nextFloat();
            moteSpeed[i] = 0.008f + rnd.nextFloat() * 0.022f;
            moteSize[i] = 1f + rnd.nextFloat() * 2.2f;
            motePhase[i] = rnd.nextFloat() * (float) (Math.PI * 2);
        }
        hasSave = SaveManager.hasSave();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /* ====================================================================
     *  Self-installing input
     * ==================================================================== */

    public void attachTo(final Component component) {
        detach();
        host = component;
        component.setFocusable(true);
        // Without this, TAB is swallowed by Swing focus traversal.
        component.setFocusTraversalKeysEnabled(false);

        // Keyboard: a dispatcher on the focus manager, so menu keys work even
        // when another component (or nothing) technically owns the focus.
        keyDispatcher = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!active || host == null || e.getID() != KeyEvent.KEY_PRESSED
                        || !host.isShowing()) {
                    return false;
                }
                Window w = SwingUtilities.getWindowAncestor(host);
                if (w == null || !w.isActive()) {
                    return false;
                }
                if (handleKeyPressed(e)) {
                    e.consume();
                    host.repaint();
                    return true;
                }
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(keyDispatcher);

        // Mouse
        mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!active) {
                    return;
                }
                int x = mapX(e.getX());
                int y = mapY(e.getY());
                handleMouseMoved(x, y);
                component.setCursor(Cursor.getPredefinedCursor(
                        isOverButton(x, y) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (active) {
                    component.requestFocusInWindow();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                component.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (active && SwingUtilities.isLeftMouseButton(e)) {
                    component.requestFocusInWindow();
                    handleMousePressed(mapX(e.getX()), mapY(e.getY()));
                    component.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (active && SwingUtilities.isLeftMouseButton(e)) {
                    handleMouseReleased(mapX(e.getX()), mapY(e.getY()));
                    component.repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (active && pendingAction == -1 && !confirmingExit) {
                    moveSelection(e.getWheelRotation() > 0 ? 1 : -1);
                    component.repaint();
                }
            }
        };
        component.addMouseListener(mouseHandler);
        component.addMouseMotionListener(mouseHandler);
        component.addMouseWheelListener(mouseHandler);

        repaintTimer = new Timer(16, e -> {
            if (active && host != null) {
                host.repaint();
            }
        });
        repaintTimer.start();

        SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    public void detach() {
        if (repaintTimer != null) {
            repaintTimer.stop();
            repaintTimer = null;
        }
        if (keyDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .removeKeyEventDispatcher(keyDispatcher);
            keyDispatcher = null;
        }
        if (host != null) {
            if (mouseHandler != null) {
                host.removeMouseListener(mouseHandler);
                host.removeMouseMotionListener(mouseHandler);
                host.removeMouseWheelListener(mouseHandler);
            }
            host.setCursor(Cursor.getDefaultCursor());
        }
        host = null;
        mouseHandler = null;
    }

    public boolean isActive() {
        return active;
    }

    private void deactivate() {
        active = false;
        if (host != null) {
            host.setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Maps component pixels to the coordinate space used by draw(). */
    private int mapX(int x) {
        if (host == null || host.getWidth() <= 0 || drawW <= 0) {
            return x;
        }
        return Math.round(x * (float) drawW / host.getWidth());
    }

    private int mapY(int y) {
        if (host == null || host.getHeight() <= 0 || drawH <= 0) {
            return y;
        }
        return Math.round(y * (float) drawH / host.getHeight());
    }

    /* ====================================================================
     *  Lifecycle / actions
     * ==================================================================== */

    public void show() {
        active = true;
        if (host != null) {
            final Component c = host;
            SwingUtilities.invokeLater(c::requestFocusInWindow);
        }
        hasSave = SaveManager.hasSave();
        selected = NEW_GAME;
        pressed = -1;
        confirmingExit = false;
        confirmSelected = 0;
        confirmPressed = -1;
        confirmFade = 0f;
        pendingAction = -1;
        actionLaunched = false;
        leaveTime = 0;
        introTime = 0;
        message = null;
        lastNanos = 0;
        java.util.Arrays.fill(glow, 0f);
    }

    public void startNewGame() {
        SaveManager.createNewSave();
        if (listener != null) {
            listener.onNewGame();
        } else {
            System.out.println("Main menu: no Listener set, new game not started.");
        }
    }

    public void continueGame() {
        doContinue();
    }

    private boolean doContinue() {
        if (!SaveManager.hasSave()) {
            hasSave = false;
            showMessage("No saved game found.");
            return false;
        }
        save.SaveData data = null;
        try {
            data = SaveManager.load();
        } catch (RuntimeException ex) {
            data = null;
        }
        // SaveManager.load() returns null (it does not throw) when the file
        // is corrupt or unreadable.
        if (data == null) {
            hasSave = SaveManager.hasSave();
            showMessage("The saved game could not be loaded.");
            return false;
        }
        if (listener != null) {
            listener.onContinue();
        } else {
            System.out.println("Main menu: no Listener set, game not resumed.");
        }
        return true;
    }

    private void exitGame() {
        deactivate();
        if (listener != null) {
            listener.onExit();
        } else {
            System.exit(0);
        }
    }

    private void beginLeave(int action) {
        pendingAction = action;
        leaveTime = 0;
        pressed = -1;
    }

    private void runPendingAction() {
        int action = pendingAction;
        actionLaunched = true;
        if (action == NEW_GAME) {
            startNewGame();
            deactivate();
        } else if (action == CONTINUE) {
            if (doContinue()) {
                deactivate();
            } else {
                actionLaunched = false;
                pendingAction = -1;
                leaveTime = 0;
            }
        }
    }

    private void activate(int item) {
        switch (item) {
            case NEW_GAME:
                SoundManager.play(Sfx.CLICK);
                beginLeave(NEW_GAME);
                break;
            case CONTINUE:
                if (SaveManager.hasSave()) {
                    hasSave = true;
                    SoundManager.play(Sfx.CLICK);
                    beginLeave(CONTINUE);
                } else {
                    hasSave = false;
                    SoundManager.play(Sfx.ERROR);
                    showMessage("No saved game found.");
                }
                break;
            case EXIT:
                SoundManager.play(Sfx.CLICK);
                openExitConfirm();
                break;
            default:
                break;
        }
    }

    private void openExitConfirm() {
        confirmingExit = true;
        confirmSelected = 0;
        confirmPressed = -1;
        pressed = -1;
    }

    private void resolveConfirm(int choice) {
        SoundManager.play(Sfx.CLICK);
        if (choice == 1) {
            exitGame();
        } else {
            confirmingExit = false;
        }
    }

    private void showMessage(String text) {
        message = text;
        messageAge = 0;
    }

    private boolean isEnabled(int item) {
        return item != CONTINUE || hasSave;
    }

    private void moveSelection(int dir) {
        int i = selected;
        for (int n = 0; n < ITEM_COUNT; n++) {
            i = (i + dir + ITEM_COUNT) % ITEM_COUNT;
            if (isEnabled(i)) {
                if (i != selected) {
                    SoundManager.play(Sfx.HOVER);
                }
                selected = i;
                return;
            }
        }
    }

    /* ====================================================================
     *  Input handlers (coordinates are in draw() space)
     * ==================================================================== */

    /** @return true if the key was used by the menu. */
    public boolean handleKeyPressed(KeyEvent e) {
        if (pendingAction != -1) {
            return true;
        }
        int key = e.getKeyCode();

        if (confirmingExit) {
            switch (key) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_W:
                case KeyEvent.VK_A:
                case KeyEvent.VK_S:
                case KeyEvent.VK_D:
                case KeyEvent.VK_TAB:
                    confirmSelected = 1 - confirmSelected;
                    SoundManager.play(Sfx.HOVER);
                    return true;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    resolveConfirm(confirmSelected);
                    return true;
                case KeyEvent.VK_Y:
                    resolveConfirm(1);
                    return true;
                case KeyEvent.VK_N:
                case KeyEvent.VK_ESCAPE:
                    resolveConfirm(0);
                    return true;
                default:
                    return false;
            }
        }

        switch (key) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                moveSelection(-1);
                return true;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
            case KeyEvent.VK_TAB:
                moveSelection(1);
                return true;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                activate(selected);
                return true;
            case KeyEvent.VK_ESCAPE:
                openExitConfirm();
                return true;
            default:
                return false;
        }
    }

    public void handleMouseMoved(int x, int y) {
        if (pendingAction != -1) {
            return;
        }
        if (confirmingExit) {
            int h = hitConfirm(x, y);
            if (h >= 0) {
                if (h != confirmSelected) {
                    SoundManager.play(Sfx.HOVER);
                }
                confirmSelected = h;
            }
            return;
        }
        int h = hitMenu(x, y);
        if (h >= 0 && isEnabled(h)) {
            if (h != selected) {
                SoundManager.play(Sfx.HOVER);
            }
            selected = h;
        }
    }

    public void handleMousePressed(int x, int y) {
        if (pendingAction != -1) {
            return;
        }
        if (confirmingExit) {
            confirmPressed = hitConfirm(x, y);
        } else {
            pressed = hitMenu(x, y);
        }
    }

    /** A click counts when press and release happen on the same button. */
    public void handleMouseReleased(int x, int y) {
        if (pendingAction != -1) {
            pressed = -1;
            confirmPressed = -1;
            return;
        }
        if (confirmingExit) {
            int h = hitConfirm(x, y);
            int p = confirmPressed;
            confirmPressed = -1;
            if (h >= 0 && h == p) {
                confirmSelected = h;
                resolveConfirm(h);
            }
            return;
        }
        int h = hitMenu(x, y);
        int p = pressed;
        pressed = -1;
        if (h >= 0 && h == p) {
            if (isEnabled(h)) {
                selected = h;
            }
            activate(h);
        }
    }

    /** True when the point is over a clickable button (for hand cursor). */
    public boolean isOverButton(int x, int y) {
        if (pendingAction != -1) {
            return false;
        }
        if (confirmingExit) {
            return hitConfirm(x, y) >= 0;
        }
        int h = hitMenu(x, y);
        return h >= 0;   // CONTINUE is clickable even when empty (shows a message)
    }

    private int hitMenu(int x, int y) {
        for (int i = 0; i < ITEM_COUNT; i++) {
            if (bounds[i] != null && bounds[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private int hitConfirm(int x, int y) {
        for (int i = 0; i < 2; i++) {
            if (confirmBounds[i] != null && confirmBounds[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    /* ====================================================================
     *  Animation clock
     * ==================================================================== */

    private double tick() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : (now - lastNanos) / 1e9;
        lastNanos = now;
        dt = Math.min(dt, 0.1);

        clock += dt;
        introTime += dt;
        if (pendingAction != -1) {
            leaveTime += dt;
        }
        if (message != null) {
            messageAge += dt;
            if (messageAge > MESSAGE_SECONDS) {
                message = null;
            }
        }

        flicker = 0.85 + 0.10 * Math.sin(clock * 3.1) + 0.05 * Math.sin(clock * 7.3);

        float k = (float) Math.min(1.0, dt * 12.0);
        for (int i = 0; i < ITEM_COUNT; i++) {
            float target = (i == selected && isEnabled(i)) ? 1f : 0f;
            glow[i] += (target - glow[i]) * k;
        }
        for (int i = 0; i < 2; i++) {
            float target = (confirmingExit && i == confirmSelected) ? 1f : 0f;
            confirmGlow[i] += (target - confirmGlow[i]) * k;
        }
        float fadeTarget = confirmingExit ? 1f : 0f;
        confirmFade += (fadeTarget - confirmFade) * (float) Math.min(1.0, dt * 10.0);
        return dt;
    }

    /* ====================================================================
     *  Rendering
     * ==================================================================== */

    public void draw(Graphics2D graphics, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        drawW = width;
        drawH = height;
        tick();

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float s = Math.max(0.6f, Math.min(Math.min(width / 800f, height / 600f), 2f));
            int cx = width / 2;

            layout(width, height, s);

            drawBackground(g, width, height, s);
            drawMotes(g, width, height, s);
            drawHeader(g, cx, height, s);

            for (int i = 0; i < ITEM_COUNT; i++) {
                boolean enabled = isEnabled(i);
                drawButton(g, bounds[i], LABELS[i], enabled, glow[i],
                        pressed == i && enabled, s, true);
            }

            drawFooter(g, width, height, s);

            if (confirmFade > 0.01f) {
                drawExitConfirm(g, width, height, s);
            }

            double fade = Math.max(
                    Math.max(0.0, 1.0 - introTime / FADE_IN_SECONDS),
                    pendingAction != -1 ? Math.min(1.0, leaveTime / FADE_OUT_SECONDS) : 0.0);
            if (fade > 0.002) {
                g.setColor(new Color(0, 0, 0, (int) Math.round(255 * Math.min(1.0, fade))));
                g.fillRect(0, 0, width, height);
            }
        } finally {
            g.dispose();
        }

        if (pendingAction != -1 && !actionLaunched && leaveTime >= FADE_OUT_SECONDS) {
            runPendingAction();
        }
    }

    private void layout(int width, int height, float s) {
        int cx = width / 2;
        int bw = Math.round(280 * s);
        int bh = Math.round(56 * s);
        int gap = Math.round(18 * s);
        int firstCenterY = Math.round(height * 0.52f);
        for (int i = 0; i < ITEM_COUNT; i++) {
            int cy = firstCenterY + i * (bh + gap);
            bounds[i] = new Rectangle(cx - bw / 2, cy - bh / 2, bw, bh);
        }

        int pw = Math.round(440 * s);
        int ph = Math.round(190 * s);
        int py = (height - ph) / 2;
        int cbw = Math.round(150 * s);
        int cbh = Math.round(44 * s);
        int cgap = Math.round(24 * s);
        int by = py + ph - cbh - Math.round(26 * s);
        confirmBounds[0] = new Rectangle(cx - cgap / 2 - cbw, by, cbw, cbh);
        confirmBounds[1] = new Rectangle(cx + cgap / 2, by, cbw, cbh);
    }

    private void drawBackground(Graphics2D g, int w, int h, float s) {
        g.setColor(BG);
        g.fillRect(0, 0, w, h);

        int plank = Math.max(20, Math.round(90 * s));
        for (int x = 0; x < w; x += plank) {
            g.setColor(new Color(0, 0, 0, 45));
            g.drawLine(x, 0, x, h);
            g.setColor(new Color(235, 218, 180, 8));
            g.drawLine(x + 1, 0, x + 1, h);
        }

        float radius = Math.max(w, h) * 0.75f;
        Point2D lamp = new Point2D.Float(w / 2f, h * 0.22f);
        g.setPaint(new RadialGradientPaint(lamp, radius,
                new float[] {0f, 1f},
                new Color[] {alpha(LAMP, 80 * flicker), alpha(LAMP, 0)}));
        g.fillRect(0, 0, w, h);

        Point2D mid = new Point2D.Float(w / 2f, h / 2f);
        g.setPaint(new RadialGradientPaint(mid, Math.max(w, h) * 0.75f,
                new float[] {0.35f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 190)}));
        g.fillRect(0, 0, w, h);
    }

    private void drawMotes(Graphics2D g, int w, int h, float s) {
        for (int i = 0; i < MOTE_COUNT; i++) {
            double yy = ((moteY[i] - clock * moteSpeed[i]) % 1.0 + 1.0) % 1.0;
            double xx = moteX[i] + Math.sin(clock * 0.4 + motePhase[i]) * 0.015;
            double twinkle = 0.5 + 0.5 * Math.sin(clock * 1.3 + motePhase[i] * 3);
            g.setColor(alpha(LAMP, 20 + 55 * twinkle));
            int size = Math.max(1, Math.round(moteSize[i] * s));
            g.fillOval((int) (xx * w), (int) (yy * h), size, size);
        }
    }

    private void drawHeader(Graphics2D g, int cx, int height, float s) {
        int titleY = Math.round(height * 0.27f);

        g.setFont(new Font("Serif", Font.BOLD, Math.round(56 * s)));
        String title = "ESCAPE ROOM";
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(title) / 2;

        int spread = Math.max(1, Math.round(2 * s));
        g.setColor(alpha(LAMP, 18 * flicker));
        for (int dx = -spread; dx <= spread; dx += spread) {
            for (int dy = -spread; dy <= spread; dy += spread) {
                g.drawString(title, tx + dx, titleY + dy);
            }
        }
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(title, tx + Math.round(3 * s), titleY + Math.round(3 * s));
        g.setColor(PARCHMENT);
        g.drawString(title, tx, titleY);

        g.setFont(new Font("Serif", Font.ITALIC, Math.round(20 * s)));
        g.setColor(alpha(PARCHMENT, 210));
        drawCentered(g, "A Mystery Awaits...", cx, titleY + Math.round(42 * s));

        int dy = titleY + Math.round(68 * s);
        int half = Math.round(110 * s);
        int d = Math.round(6 * s);
        g.setColor(alpha(PARCHMENT, 120));
        g.drawLine(cx - half, dy, cx - 2 * d, dy);
        g.drawLine(cx + 2 * d, dy, cx + half, dy);
        g.setColor(alpha(PARCHMENT, 200));
        g.fill(diamond(cx, dy, d));
    }

    private void drawFooter(Graphics2D g, int w, int h, float s) {
        int cx = w / 2;
        if (message != null) {
            double a = 255;
            double remaining = MESSAGE_SECONDS - messageAge;
            if (remaining < 0.5) {
                a = 255 * (remaining / 0.5);
            }
            g.setFont(new Font("Serif", Font.BOLD, Math.round(17 * s)));
            g.setColor(alpha(new Color(220, 130, 95), a));
            drawCentered(g, message, cx, bounds[ITEM_COUNT - 1].y
                    + bounds[ITEM_COUNT - 1].height + Math.round(34 * s));
        }

        g.setFont(new Font("Serif", Font.PLAIN, Math.round(15 * s)));
        g.setColor(alpha(PARCHMENT, 140));
        drawCentered(g, "UP / DOWN or W / S  select      ENTER or CLICK  confirm      ESC  exit",
                cx, h - Math.round(36 * s));
    }

    private void drawExitConfirm(Graphics2D g, int w, int h, float s) {
        g.setColor(new Color(0, 0, 0, Math.round(160 * confirmFade)));
        g.fillRect(0, 0, w, h);

        int pw = Math.round(440 * s);
        int ph = Math.round(190 * s);
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;
        int cx = w / 2;

        java.awt.Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, confirmFade)));

        g.setColor(new Color(35, 24, 18));
        g.fillRect(px, py, pw, ph);
        g.setStroke(new BasicStroke(2f * s));
        g.setColor(PARCHMENT);
        g.drawRect(px, py, pw, ph);
        g.setStroke(new BasicStroke(1f));
        g.setColor(alpha(PARCHMENT, 70));
        g.drawRect(px + 6, py + 6, pw - 12, ph - 12);

        g.setFont(new Font("Serif", Font.BOLD, Math.round(26 * s)));
        g.setColor(PARCHMENT);
        drawCentered(g, "LEAVE THE ROOM?", cx, py + Math.round(52 * s));

        g.setFont(new Font("Serif", Font.PLAIN, Math.round(16 * s)));
        g.setColor(alpha(PARCHMENT, 210));
        drawCentered(g, "Are you sure you want to exit the game?",
                cx, py + Math.round(84 * s));

        drawButton(g, confirmBounds[0], "STAY", true, confirmGlow[0],
                confirmPressed == 0, s, false);
        drawButton(g, confirmBounds[1], "EXIT", true, confirmGlow[1],
                confirmPressed == 1, s, false);

        g.setComposite(old);
    }

    private void drawButton(Graphics2D g, Rectangle r, String label, boolean enabled,
                            float glowAmount, boolean down, float s, boolean markers) {
        int off = down ? Math.max(1, Math.round(2 * s)) : 0;
        int x = r.x;
        int y = r.y + off;
        int w = r.width;
        int h = r.height;

        if (enabled && glowAmount > 0.01f) {
            for (int k = 4; k >= 1; k--) {
                int e = Math.round(k * 3 * s);
                g.setColor(alpha(LAMP, glowAmount * flicker * 9));
                g.fillRect(x - e, y - e, w + 2 * e, h + 2 * e);
            }
        }

        Color base;
        if (!enabled) {
            base = new Color(46, 34, 27);
        } else if (down) {
            base = mix(WOOD, WOOD_DARK, 0.6f);
        } else {
            base = mix(WOOD, WOOD_LIGHT, glowAmount);
        }
        g.setColor(base);
        g.fillRect(x, y, w, h);
        g.setPaint(new GradientPaint(x, y, new Color(255, 235, 200, enabled ? 38 : 12),
                x, y + h, new Color(0, 0, 0, 70)));
        g.fillRect(x, y, w, h);

        Color border = enabled
                ? mix(new Color(190, 172, 135), PARCHMENT, glowAmount)
                : new Color(110, 98, 80);
        g.setColor(border);
        g.setStroke(new BasicStroke((1.5f + 1.5f * glowAmount) * s));
        g.drawRect(x, y, w, h);
        g.setStroke(new BasicStroke(1f));
        g.setColor(alpha(PARCHMENT, enabled ? 55 : 25));
        g.drawRect(x + 4, y + 4, w - 8, h - 8);

        if (markers && enabled && glowAmount > 0.02f) {
            int d = Math.round(6 * s);
            int slide = Math.round((14 + (1f - glowAmount) * 10) * s);
            g.setColor(alpha(PARCHMENT, 255 * glowAmount));
            g.fill(diamond(x - slide, y + h / 2, d));
            g.fill(diamond(x + w + slide, y + h / 2, d));
        }

        g.setFont(new Font("Serif", Font.BOLD, Math.round(20 * s)));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(new Color(0, 0, 0, enabled ? 150 : 90));
        g.drawString(label, tx + 1, ty + 1);
        g.setColor(enabled ? PARCHMENT : new Color(130, 115, 95));
        g.drawString(label, tx, ty);
    }

    /* ====================================================================
     *  Helpers
     * ==================================================================== */

    private static void drawCentered(Graphics2D g, String text, int cx, int baselineY) {
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - tw / 2, baselineY);
    }

    private static Polygon diamond(int cx, int cy, int r) {
        return new Polygon(new int[] {cx, cx + r, cx, cx - r},
                new int[] {cy - r, cy, cy + r, cy}, 4);
    }

    private static Color alpha(Color c, double a) {
        int v = (int) Math.max(0, Math.min(255, Math.round(a)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), v);
    }

    private static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
