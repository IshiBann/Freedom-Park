package com.mygame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

/**
 * MenuScreen – 8-bit Filipino beat-em-up menu.
 *
 * Improvements over v1:
 *  - Pixel-art perspective grid with real vanishing-point lines
 *  - CRT scanline overlay (every 4 px, thicker than before)
 *  - Pulsing red horizon glow line
 *  - Star field that twinkles independently per star
 *  - Title screen: drawn pixel fighters (fallback when images missing)
 *  - Title glow uses a multi-layer red halo effect
 *  - Stage cards: pixel-art procedural backgrounds per stage
 *  - Card hover: top accent bar + gold number highlight
 *  - Lock icon drawn in pixel style (blocky, no curves)
 *  - Bottom bar: blinking CREDITS + ESC hint
 *  - All text exclusively Monospaced (Press Start 2P feel)
 */
public class MenuScreen extends JPanel {

    // ─── State ───────────────────────────────────────────────────────────────
    public enum MenuState { TITLE, STAGE_SELECT }
    private MenuState menuState = MenuState.TITLE;

    // ─── Assets ──────────────────────────────────────────────────────────────
    private BufferedImage bgImage, titleImage, pressImage, pickStageImage;
    private final BufferedImage[] stageBackgrounds = new BufferedImage[5];

    // ─── Interaction ─────────────────────────────────────────────────────────
    private int hoveredStageIndex = -1;
    private Runnable exitAction;
    private java.util.function.IntConsumer stageSelectedAction;

    // ─── Animation ───────────────────────────────────────────────────────────
    private long   tickCount    = 0;
    private float  titleGlow    = 0f;   // 0..1 pulsing
    private float  gridOffset   = 0f;   // perspective grid scroll
    private boolean pressVisible = true; // blinking "press any key"

    /** Each star: [x, y, size(1|2), twinkleCycle, twinkleOffset] */
    private final int[][] stars = new int[48][5];

    private final Timer animTimer;

    // ─── Palette ─────────────────────────────────────────────────────────────
    private static final Color BG_DEEP       = new Color(10, 5, 16);
    private static final Color BG_MID        = new Color(18, 8, 32);
    private static final Color RED_PRI       = new Color(220, 20, 20);
    private static final Color RED_GLOW1     = new Color(220, 20, 20, 90);
    private static final Color RED_GLOW2     = new Color(180, 10, 10, 30);
    private static final Color RED_DARK      = new Color(80,  0,  0);
    private static final Color GOLD          = new Color(255, 215,   0);
    private static final Color GOLD_DIM      = new Color(255, 215,   0, 110);
    private static final Color GOLD_BRIGHT   = new Color(255, 230,  60);
    private static final Color PANEL_BG      = new Color(8, 4, 18, 230);
    private static final Color PANEL_BORDER  = new Color(200, 20, 20, 80);
    private static final Color CARD_BG       = new Color(14, 10, 26);
    private static final Color CARD_HOVER_OV = new Color(160, 10, 10, 190);
    private static final Color SCANLINE      = new Color(0, 0, 0, 45);
    private static final Color LOCKED_BG     = new Color(20, 16, 12);
    private static final Color LOCKED_TEXT   = new Color(255, 255, 255, 45);

    // ─── Layout ──────────────────────────────────────────────────────────────
    private static final int CARD_COLS   = 3;
    private static final int CARD_GAP    = 12;
    private static final int PANEL_PAD   = 18;
    private static final int HEADER_H    = 34;
    private static final int CARD_R      = 2;   // corner radius (pixel style)
    private static final int LOCKED_IDX  = 3;

    // Stage names (index matches stageBackgrounds[])
    private static final String[] STAGE_NAMES = {
        "STAGE 1", "STAGE 2", "STAGE 3", "STAGE 4", "STAGE 5"
    };

    // ─── Fonts ───────────────────────────────────────────────────────────────
    private Font pixelFont;   // "Press Start 2P" vibe
    private Font titleFont;
    private Font smallFont;
    private Font tinyFont;

    // ═══════════════════════════════════════════════════════════════════════════
    // Constructor
    // ═══════════════════════════════════════════════════════════════════════════
    public MenuScreen() {
        setPreferredSize(new Dimension(1200, 800));
        setBackground(BG_DEEP);
        setFocusable(true);
        initFonts();
        loadImages();
        initStars();
        initInput();
        animTimer = new Timer(16, e -> tick());
        animTimer.start();
    }

    // ─── Public API ──────────────────────────────────────────────────────────
    public void setExitAction(Runnable r)                          { this.exitAction = r; }
    public void setStageSelectedAction(java.util.function.IntConsumer c) { this.stageSelectedAction = c; }

    // ═══════════════════════════════════════════════════════════════════════════
    // Init helpers
    // ═══════════════════════════════════════════════════════════════════════════
    private void initFonts() {
        Font mono = new Font(Font.MONOSPACED, Font.BOLD, 12);
        tinyFont  = mono.deriveFont(Font.BOLD,  7f);
        smallFont = mono.deriveFont(Font.BOLD,  9f);
        pixelFont = mono.deriveFont(Font.BOLD, 11f);
        titleFont = mono.deriveFont(Font.BOLD, 26f);
    }

    private void loadImages() {
        try {
            String base = Paths.get(System.getProperty("user.dir"), "src", "assets", "menu").toString();
            bgImage        = safeRead(base, "Menu.png");
            titleImage     = safeRead(base, "Game Title.png");
            pressImage     = safeRead(base, "Press any key to enter.png");
            for (int i = 0; i < stageBackgrounds.length; i++)
                stageBackgrounds[i] = loadImage("src", "assets", "stage " + (i+1), "Background.png");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private BufferedImage safeRead(String dir, String name) {
        try { return ImageIO.read(new File(Paths.get(dir, name).toString())); }
        catch (Exception e) { return null; }
    }
    private BufferedImage loadImage(String... parts) {
        try { return ImageIO.read(new File(Paths.get(System.getProperty("user.dir"), parts).toString())); }
        catch (Exception e) { return null; }
    }

    private void initStars() {
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = rng.nextInt(1200);          // x
            stars[i][1] = rng.nextInt(300);           // y (upper half)
            stars[i][2] = rng.nextBoolean() ? 2 : 1; // size
            stars[i][3] = 60 + rng.nextInt(120);      // twinkle period (ticks)
            stars[i][4] = rng.nextInt(180);            // twinkle offset
        }
    }

    private void initInput() {
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (menuState == MenuState.STAGE_SELECT) { menuState = MenuState.TITLE; repaint(); }
                    else if (exitAction != null) exitAction.run();
                    return;
                }
                handleKeyPressed(e);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleMouseClick(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e)  { resetHover(); }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = stageCardAt(e.getPoint());
                boolean changed = idx != hoveredStageIndex;
                hoveredStageIndex = idx;
                setCursor(idx >= 0 && idx != LOCKED_IDX
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
                if (changed) repaint();
            }
        });
    }

    private void resetHover() {
        hoveredStageIndex = -1;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Animation tick
    // ═══════════════════════════════════════════════════════════════════════════
    private void tick() {
        tickCount++;
        titleGlow   = (float)(Math.sin(tickCount * 0.038) * 0.5 + 0.5);
        gridOffset  = (gridOffset + 0.5f) % 40f;
        if (tickCount % 55 == 0) pressVisible = !pressVisible;
        repaint();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Input
    // ═══════════════════════════════════════════════════════════════════════════
    private void handleKeyPressed(KeyEvent e) {
        if (menuState == MenuState.TITLE) { menuState = MenuState.STAGE_SELECT; repaint(); }
    }

    private void handleMouseClick(Point p) {
        if (menuState == MenuState.TITLE) { menuState = MenuState.STAGE_SELECT; repaint(); return; }
        if (menuState == MenuState.STAGE_SELECT) {
            int idx = stageCardAt(p);
            if (idx >= 0 && idx != LOCKED_IDX && stageSelectedAction != null)
                stageSelectedAction.accept(idx);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Hit-testing & card geometry
    // ═══════════════════════════════════════════════════════════════════════════
    private int stageCardAt(Point p) {
        if (menuState != MenuState.STAGE_SELECT) return -1;
        Rectangle[] cards = getCardRects();
        for (int i = 0; i < cards.length; i++) if (cards[i].contains(p)) return i;
        return -1;
    }

    private Rectangle getPanelRect() {
        int W = getWidth(), H = getHeight();
        return new Rectangle(22, 100, W - 44, H - 120);
    }

    private Rectangle[] getCardRects() {
        Rectangle panel = getPanelRect();
        int cx = panel.x + PANEL_PAD;
        int cy = panel.y + PANEL_PAD + HEADER_H;
        int cw = panel.width  - PANEL_PAD * 2;
        int ch = panel.height - PANEL_PAD * 2 - HEADER_H;
        int rows   = (int) Math.ceil((double) stageBackgrounds.length / CARD_COLS);
        int cardW  = (cw - CARD_GAP * (CARD_COLS - 1)) / CARD_COLS;
        int cardH  = (ch - CARD_GAP * (rows - 1)) / rows;
        Rectangle[] rects = new Rectangle[stageBackgrounds.length];
        for (int i = 0; i < stageBackgrounds.length; i++) {
            int col = i % CARD_COLS, row = i / CARD_COLS;
            rects[i] = new Rectangle(cx + col*(cardW+CARD_GAP), cy + row*(cardH+CARD_GAP), cardW, cardH);
        }
        return rects;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAINT
    // ═══════════════════════════════════════════════════════════════════════════
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int W = getWidth(), H = getHeight();

        drawBackground(g2, W, H);

        if (menuState == MenuState.TITLE) {
            drawTitleScreen(g2, W, H);
        } else {
            drawStars(g2, W);
            drawPerspectiveGrid(g2, W, H);
            drawHorizonGlow(g2, W, H);
            drawStageSelect(g2, W, H);
            drawScanlines(g2, W, H);
        }
    }

    // ── Background ───────────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g2, int W, int H) {
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, W, H, null);
            g2.setPaint(new GradientPaint(0, 0, new Color(10, 5, 16, 210), W/2f, H/2f, new Color(10, 5, 16, 70)));
            g2.fillRect(0, 0, W, H);
        } else {
            float[] fr = {0f, 0.55f, 1f};
            Color[] co = {new Color(24, 8, 42), new Color(14, 6, 28), BG_DEEP};
            g2.setPaint(new RadialGradientPaint(W/2f, H*0.3f, H*0.85f, fr, co));
            g2.fillRect(0, 0, W, H);
        }
    }

    // ── Star field ───────────────────────────────────────────────────────────
    private void drawStars(Graphics2D g2, int W) {
        for (int[] s : stars) {
            // Per-star twinkle: sine based on tickCount + individual offset/period
            double phase = ((tickCount + s[4]) % s[3]) / (double) s[3] * Math.PI * 2;
            float alpha  = (float)(Math.sin(phase) * 0.4 + 0.6);
            int a = Math.max(0, Math.min(255, (int)(alpha * 220)));
            g2.setColor(new Color(255, 255, 255, a));
            g2.fillRect(s[0] % W, s[1], s[2], s[2]);
        }
    }

    // ── Perspective grid ─────────────────────────────────────────────────────
    private void drawPerspectiveGrid(Graphics2D g2, int W, int H) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(0.8f));

        int vp  = H / 2;           // vanishing-point Y
        int vpX = W / 2;           // vanishing-point X (centre)
        int gridH = H - vp;        // grid covers bottom half

        // Horizontal lines (evenly spaced but scroll down for motion)
        int n = 10;
        for (int i = 0; i <= n; i++) {
            float t   = (float) i / n;
            float off = (gridOffset / 40f) * (1f / n);
            float tt  = (t + off) % 1.0f;
            // Exponential distribution → perspective spacing
            float y   = vp + gridH * tt * tt;
            int   a   = (int)(180 * tt * tt);
            g2.setColor(new Color(180, 0, 0, Math.min(a, 160)));
            g2.drawLine(0, (int) y, W, (int) y);
        }

        // Vertical perspective lines radiating from vanishing point
        int vertLines = 14;
        for (int i = 0; i <= vertLines; i++) {
            float bx  = (float) i / vertLines * W;        // spread at bottom
            float t   = (float) i / vertLines;
            float sym = Math.abs(t - 0.5f) * 2f;         // 0 at centre, 1 at edges
            int   a   = (int)(140 * sym * sym);
            g2.setColor(new Color(180, 0, 0, Math.min(a, 120)));
            g2.drawLine(vpX, vp, (int) bx, H);
        }

        g2.setStroke(old);
    }

    // ── Horizon glow ─────────────────────────────────────────────────────────
    private void drawHorizonGlow(Graphics2D g2, int W, int H) {
        int hy = H / 2;
        // Wide soft glow band
        float[] fr = {0f, 0.5f, 1f};
        Color[] co = {new Color(220, 20, 20, (int)(60 + titleGlow*50)),
                      new Color(180,  0,  0, (int)(20 + titleGlow*20)),
                      new Color(0, 0, 0, 0)};
        g2.setPaint(new RadialGradientPaint(W/2f, hy, W*0.6f, fr, co));
        g2.fillRect(0, hy - 80, W, 160);

        // Sharp 2-px horizon line
        int lineAlpha = (int)(180 + titleGlow * 75);
        g2.setColor(new Color(220, 20, 20, lineAlpha));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(0, hy, W, hy);
        // Glow beneath the line
        g2.setColor(new Color(220, 20, 20, 60));
        g2.setStroke(new BasicStroke(6f));
        g2.drawLine(0, hy + 3, W, hy + 3);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── CRT scanlines ────────────────────────────────────────────────────────
    private void drawScanlines(Graphics2D g2, int W, int H) {
        g2.setColor(SCANLINE);
        for (int y = 0; y < H; y += 4) g2.fillRect(0, y, W, 2);
    }

    // ── Corner brackets ──────────────────────────────────────────────────────
    private void drawCorners(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(220, 20, 20, 180));
        g2.setStroke(new BasicStroke(2f));
        int s = 18;
        // TL
        g2.drawLine(x,   y,   x+s, y);   g2.drawLine(x, y,   x, y+s);
        // TR
        g2.drawLine(x+w, y,   x+w-s, y); g2.drawLine(x+w, y, x+w, y+s);
        // BL
        g2.drawLine(x,   y+h, x+s, y+h); g2.drawLine(x, y+h, x, y+h-s);
        // BR
        g2.drawLine(x+w, y+h, x+w-s, y+h); g2.drawLine(x+w, y+h, x+w, y+h-s);
        g2.setStroke(new BasicStroke(1f));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TITLE SCREEN
    // ═══════════════════════════════════════════════════════════════════════════
    private void drawTitleScreen(Graphics2D g2, int W, int H) {
        // Title image or drawn text with multi-layer glow
        if (titleImage != null) {
            int tw = Math.min(titleImage.getWidth(), W - 80);
            int th = (int)(tw * (double) titleImage.getHeight() / titleImage.getWidth());
            float ga = 0.9f + titleGlow * 0.1f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ga));
            g2.drawImage(titleImage, (W - tw)/2, H/4 - th/2, tw, th, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else {
            drawPixelTitle(g2, W, H);
        }

        // "PRESS ANY KEY" blink
        if (pressVisible) {
            g2.setFont(pixelFont.deriveFont(Font.BOLD, 20f));
            g2.setColor(GOLD);
            String msg = "\u25B6 PRESS ANY KEY \u25C0";
            FontMetrics fm = g2.getFontMetrics();
            int tx = (W - fm.stringWidth(msg)) / 2;
            // Glow behind text
            g2.setColor(new Color(255, 215, 0, 40));
            for (int d = 3; d >= 1; d--)
                g2.drawString(msg, tx + d, H*2/3 + d);
            g2.setColor(GOLD);
            g2.drawString(msg, tx, H*2/3);
        }

        // Credits line at bottom
        g2.setFont(tinyFont);
        g2.setColor(new Color(255, 215, 0, 80));
        String cr = "\u25B7 CREDITS: 99    INSERT COIN    1 OR 2 PLAYERS \u25C1";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(cr, (W - fm.stringWidth(cr))/2, H - 16);
    }

    /** Drawn title with multi-layer pixel glow */
    private void drawPixelTitle(Graphics2D g2, int W, int H) {
        String line1 = "KALYE";
        String line2 = "KOMBAT";
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        int y1 = H/4 - 10;
        int y2 = y1 + fm.getHeight() + 4;

        for (String[] pair : new String[][]{{line1, String.valueOf((W - fm.stringWidth(line1))/2)},
                                             {line2, String.valueOf((W - fm.stringWidth(line2))/2)}}) {
            String text = pair[0];
            int x = Integer.parseInt(pair[1]);
            int yy = text.equals(line1) ? y1 : y2;

            // Shadow layers (pixel offset)
            g2.setColor(new Color(80, 0, 0));
            g2.drawString(text, x + 6, yy + 6);
            g2.setColor(new Color(40, 0, 0));
            g2.drawString(text, x + 10, yy + 10);

            // Glow passes
            int glowA = (int)(60 + titleGlow * 80);
            for (int pass = 5; pass >= 1; pass--) {
                g2.setColor(new Color(220, 20, 20, Math.min(255, glowA / pass)));
                for (int dx = -pass; dx <= pass; dx += pass)
                    for (int dy = -pass; dy <= pass; dy += pass)
                        if (dx != 0 || dy != 0) g2.drawString(text, x + dx, yy + dy);
            }

            // Main text
            g2.setColor(RED_PRI);
            g2.drawString(text, x, yy);
        }

        // Subtitle
        g2.setFont(tinyFont);
        String sub = "\u25B8 PHILIPPINE STREETS \u25C2";
        g2.setColor(GOLD_DIM);
        fm = g2.getFontMetrics();
        g2.drawString(sub, (W - fm.stringWidth(sub))/2, y2 + 30);
    }

    /** Minimal pixel-art stick fighters on the title screen */
    
    /**
     * Block-pixel fighter (6x1 pixel grid, each "pixel" is 6px on screen).
     * @param flipped true = facing left
     */

    private void drawSeparator(Graphics2D g2, int cx, int y, int halfW) {
        GradientPaint gp;
        gp = new GradientPaint(cx - halfW, y, new Color(0,0,0,0), cx - halfW/3, y, GOLD_DIM);
        g2.setPaint(gp); g2.fillRect(cx - halfW, y, halfW/2, 1);
        gp = new GradientPaint(cx - halfW/3, y, GOLD_DIM, cx, y, RED_PRI);
        g2.setPaint(gp); g2.fillRect(cx - halfW/3, y, halfW/3, 1);
        gp = new GradientPaint(cx, y, RED_PRI, cx + halfW/3, y, GOLD_DIM);
        g2.setPaint(gp); g2.fillRect(cx, y, halfW/3, 1);
        gp = new GradientPaint(cx + halfW/3, y, GOLD_DIM, cx + halfW, y, new Color(0,0,0,0));
        g2.setPaint(gp); g2.fillRect(cx + halfW/3, y, halfW/2, 1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STAGE SELECT
    // ═══════════════════════════════════════════════════════════════════════════
    private void drawStageSelect(Graphics2D g2, int W, int H) {
        Rectangle panel = getPanelRect();

        drawPanel(g2, panel);
        drawPanelHeader(g2, panel);
        drawCorners(g2, panel.x, panel.y, panel.width, panel.height);

        Rectangle[] cards = getCardRects();
        for (int i = 0; i < cards.length; i++) drawStageCard(g2, i, cards[i]);

        drawPickStageTitle(g2, W);
        drawBottomBar(g2, W, H);
    }

    private void drawPickStageTitle(Graphics2D g2, int W) {
        if (pickStageImage != null) {
            int pw = Math.min(pickStageImage.getWidth(), 580);
            int ph = (int)(pw * (double) pickStageImage.getHeight() / pickStageImage.getWidth());
            g2.drawImage(pickStageImage, (W - pw)/2, 12, pw, ph, null);
        } else {
            g2.setFont(pixelFont.deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            String t = "PICK A STAGE";
            int tx = (W - fm.stringWidth(t)) / 2;
            // Glow
            g2.setColor(new Color(220, 20, 20, (int)(50 + titleGlow*60)));
            for (int d = 3; d >= 1; d--)
                g2.drawString(t, tx + d, 68 + d);
            // Shadow
            g2.setColor(RED_DARK);
            g2.drawString(t, tx + 4, 72);
            // Text
            g2.setColor(RED_PRI);
            g2.drawString(t, tx, 68);
        }
    }

    private void drawPanel(Graphics2D g2, Rectangle r) {
        // Fill
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, CARD_R*3, CARD_R*3);
        // Border
        g2.setColor(PANEL_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, CARD_R*3, CARD_R*3);
        // Inner top glow
        float[] f = {0f, 1f};
        Color[] c = {new Color(200, 20, 20, 14), new Color(0,0,0,0)};
        g2.setPaint(new RadialGradientPaint(r.x + r.width/2f, (float)r.y, r.width*0.55f, f, c));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, CARD_R*3, CARD_R*3);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawPanelHeader(Graphics2D g2, Rectangle panel) {
        int hx = panel.x + PANEL_PAD;
        int hy = panel.y + PANEL_PAD;
        int hw = panel.width - PANEL_PAD * 2;

        // Separator line
        g2.setColor(PANEL_BORDER);
        g2.drawLine(hx, hy + HEADER_H - 4, hx + hw, hy + HEADER_H - 4);

        // Pixel diamond
        int dx = hx + 4, dy = hy + HEADER_H/2 - 5;
        g2.setColor(RED_PRI);
        int[] xs = {dx+5, dx+10, dx+5, dx};
        int[] ys = {dy,   dy+5,  dy+10, dy+5};
        g2.fillPolygon(xs, ys, 4);
        g2.setColor(new Color(220, 30, 30, 55));
        g2.fillOval(dx - 4, dy - 3, 19, 19);

        // "SINGLE PLAYER" label
        g2.setFont(tinyFont);
        g2.setColor(RED_PRI);
        g2.drawString("SINGLE PLAYER", hx + 20, hy + HEADER_H/2 + 4);

        // "SELECT STAGE" right-aligned
        g2.setFont(tinyFont);
        g2.setColor(GOLD_DIM);
        FontMetrics fm = g2.getFontMetrics();
        String right = "SELECT STAGE";
        g2.drawString(right, hx + hw - fm.stringWidth(right), hy + HEADER_H/2 + 4);
    }

    private void drawStageCard(Graphics2D g2, int idx, Rectangle r) {
        boolean locked  = (idx == LOCKED_IDX);
        boolean hovered = (idx == hoveredStageIndex) && !locked;

        // Clip to card shape
        Shape oldClip = g2.getClip();
        g2.setClip(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, CARD_R, CARD_R));

        // ── Stage background ──
        BufferedImage img = (idx < stageBackgrounds.length) ? stageBackgrounds[idx] : null;
        if (img != null) {
            if (locked) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g2.drawImage(img, r.x, r.y, r.width, r.height, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else {
            // Procedural pixel-art pattern fallback
            drawCardPixelPattern(g2, idx, r, locked);
        }

        // ── Gradient overlay ──
        if (!locked) {
            Color bot = hovered ? new Color(155, 8, 8, 210) : new Color(0, 0, 0, 195);
            Color top = hovered ? new Color(80, 4, 4, 60)  : new Color(0, 0, 0, 50);
            g2.setPaint(new GradientPaint(r.x, r.y + r.height, bot, r.x, r.y + r.height/2, top));
            g2.fillRect(r.x, r.y, r.width, r.height);
        } else {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(r.x, r.y, r.width, r.height);
        }

        g2.setClip(oldClip);

        // ── Border ──
        Color border = locked   ? new Color(80, 50, 50, 90)
                     : hovered  ? RED_PRI
                     : new Color(200, 20, 20, 55);
        g2.setColor(border);
        g2.setStroke(hovered ? new BasicStroke(1.5f) : new BasicStroke(1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, CARD_R, CARD_R);
        g2.setStroke(new BasicStroke(1f));

        // ── Top accent line on hover ──
        if (hovered) {
            g2.setPaint(new GradientPaint(r.x, r.y, new Color(0,0,0,0), r.x+r.width/2, r.y, RED_PRI));
            g2.fillRect(r.x, r.y, r.width/2, 2);
            g2.setPaint(new GradientPaint(r.x+r.width/2, r.y, RED_PRI, r.x+r.width, r.y, new Color(0,0,0,0)));
            g2.fillRect(r.x+r.width/2, r.y, r.width/2, 2);
        }

        // ── Content ──
        if (locked) drawLockedCard(g2, r);
        else        drawCardLabels(g2, idx, r, hovered);
    }

    /** Simple procedural pixel pattern per stage (fallback when no BG image) */
    private void drawCardPixelPattern(Graphics2D g2, int idx, Rectangle r, boolean locked) {
        Color base, accent;
        switch (idx) {
            case 0: base = new Color(18, 36, 14); accent = new Color(0, 80, 0, 50);  break; // park
            case 1: base = new Color(36, 24, 8);  accent = new Color(120, 80, 20, 40); break; // grandstand
            case 2: base = new Color(8, 18, 36);  accent = new Color(0, 50, 120, 50); break; // uni
            case 3: base = LOCKED_BG;             accent = new Color(60, 40, 20, 30); break;
            default:base = new Color(18, 8, 30);  accent = new Color(100, 0, 160, 40); break; // BGC
        }
        g2.setColor(locked ? LOCKED_BG : base);
        g2.fillRect(r.x, r.y, r.width, r.height);

        // Simple pixel grid texture
        g2.setColor(accent);
        for (int ty = r.y; ty < r.y + r.height; ty += 8)
            for (int tx = r.x; tx < r.x + r.width; tx += 8)
                if ((tx / 8 + ty / 8) % 3 == 0)
                    g2.fillRect(tx, ty, 4, 4);
    }

    private void drawLockedCard(Graphics2D g2, Rectangle r) {
        int cx = r.x + r.width/2, cy = r.y + r.height/2;

        // Pixel-art padlock (blocky squares, no curves)
        int px = 3;
        g2.setColor(new Color(255, 255, 255, 50));
        // Shackle top
        g2.fillRect(cx - 3*px, cy - 7*px, px, 4*px);
        g2.fillRect(cx + 2*px, cy - 7*px, px, 4*px);
        g2.fillRect(cx - 3*px, cy - 7*px, 6*px, px);
        // Body
        g2.fillRect(cx - 4*px, cy - 3*px, 8*px, 6*px);
        // Keyhole
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(cx - px,  cy - px,  2*px, 2*px);
        g2.fillRect(cx - px,  cy + px,  2*px, 3*px);

        // "LOCKED" label
        g2.setFont(tinyFont);
        g2.setColor(LOCKED_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        String lk = "LOCKED";
        g2.drawString(lk, cx - fm.stringWidth(lk)/2, cy + 5*px + 14);

        // Stage number dim
        g2.setFont(tinyFont);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.drawString("0" + (LOCKED_IDX + 1), r.x + r.width - 20, r.y + 14);
    }

    private void drawCardLabels(Graphics2D g2, int idx, Rectangle r, boolean hovered) {
        // Stage number top-right
        g2.setFont(tinyFont);
        g2.setColor(hovered ? GOLD : GOLD_DIM);
        String num = String.format("%02d", idx + 1);
        g2.drawString(num, r.x + r.width - 20, r.y + 14);

        // Stage name bottom-left with pixel shadow
        g2.setFont(tinyFont.deriveFont(Font.BOLD, 8f));
        String name = (idx < STAGE_NAMES.length) ? STAGE_NAMES[idx] : ("STAGE " + (idx+1));
        int nx = r.x + 8;
        int ny = r.y + r.height - 10;
        g2.setColor(new Color(0, 0, 0, 190));
        g2.drawString(name, nx + 2, ny + 2);
        g2.setColor(hovered ? Color.WHITE : new Color(230, 230, 230, 200));
        g2.drawString(name, nx, ny);

        // "▶ SELECT" on hover, bottom-right
        if (hovered) {
            g2.setFont(tinyFont);
            g2.setColor(GOLD);
            String sel = "\u25B6 SELECT";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sel, r.x + r.width - fm.stringWidth(sel) - 6, ny);
        }
    }

    private void drawBottomBar(Graphics2D g2, int W, int H) {
        int y = H - 14;
        g2.setFont(tinyFont);

        // Credits blink
        if (pressVisible) {
            g2.setColor(GOLD_DIM);
            g2.drawString("CREDITS: 99", 40, y);
        }

        // Nav hint right
        g2.setColor(new Color(255, 255, 255, 65));
        String hint = "CLICK TO SELECT  |  ESC = BACK";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, W - 40 - fm.stringWidth(hint), y);
    }
}