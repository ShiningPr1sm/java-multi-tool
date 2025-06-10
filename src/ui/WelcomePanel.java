package ui;

import db.DB;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WelcomePanel extends JPanel {
    private String greeting;
    private final String suffix;
    private int pos;
    private int phase;
    private int delayCounter;

    private int lastWidth = -1;
    private int lastHeight = -1;

    private int circleRadius = 140;
    private int circleCenterX = 0;
    private int circleCenterY = 0;

    private Font[] fonts;
    private Font greetingFont;
    private final Font suffixFont = new Font("SansSerif", Font.PLAIN, 24);

    private static final int DOT_COUNT = 35;
    private static final int DOT_RADIUS = 4;
    private static final int CONNECT_DISTANCE = 100;
    private final List<Point> dotPositions = new ArrayList<>();
    private final List<Point> dotVelocities = new ArrayList<>();
    private final double[] dotPulsePhase = new double[DOT_COUNT];

    private int typingDelay = 0;

    private double caretAlpha = 0.0;
    private double caretDir = 1.0;
    private final double fadeSpeed;

    private final Timer timer;
    private final Random rnd = new Random();
    private boolean dotsInitialized = false;

    public WelcomePanel(String login) {
        this(login, 0.5);
    }

    public WelcomePanel(String login, double initialFadeSpeed) {
        suffix = ", " + DB.getNickname(login) + "!";
        setBackground(new Color(25, 25, 25));
        this.fadeSpeed = Math.max(0.0, Math.min(1.0, initialFadeSpeed));

        loadFonts();
        resetAnimation();

        timer = new Timer(40, e -> {
            stepAnimation();
            updateCaretAlpha();
            updateDots();
            repaint();
        });

        SwingUtilities.invokeLater(() -> {
            if (!dotsInitialized && getWidth() > 0 && getHeight() > 0) {
                initDots();
            }
        });

        timer.start();
    }

    private void updateDots() {
        if (dotPositions.size() != DOT_COUNT) return;

        for (int i = 0; i < DOT_COUNT; i++) {
            Point p = dotPositions.get(i);
            Point v = dotVelocities.get(i);

            p.translate(v.x, v.y);

            double dx = p.x - circleCenterX;
            double dy = p.y - circleCenterY;
            double distSq = dx * dx + dy * dy;
            double radiusSq = circleRadius * circleRadius;

            if (distSq > radiusSq) {
                double dist = Math.sqrt(distSq);

                // Нормализация вектора и возвращение на границу круга
                double nx = dx / dist;
                double ny = dy / dist;

                p.x = (int) (circleCenterX + nx * (circleRadius - 1));
                p.y = (int) (circleCenterY + ny * (circleRadius - 1));

                // Переопределение направления внутрь
                int dxNew, dyNew;
                do {
                    dxNew = rnd.nextInt(3) - 1;
                    dyNew = rnd.nextInt(3) - 1;
                } while (dxNew == 0 && dyNew == 0);

                dotVelocities.get(i).setLocation(dxNew, dyNew);
            }
        }

        // Обновление фазы пульсации
        for (int i = 0; i < DOT_COUNT; i++) {
            dotPulsePhase[i] += 0.15;
            if (dotPulsePhase[i] > Math.PI * 2) {
                dotPulsePhase[i] -= Math.PI * 2;
            }
        }
    }


    private void loadFonts() {
        fonts = new Font[]{
                new Font("SansSerif", Font.BOLD, 24),
                new Font("Serif", Font.ITALIC, 24),
                new Font("Monospaced", Font.PLAIN, 24),
                new Font("Dialog", Font.PLAIN, 24)
        };
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/minecraft.ttf");
            if (is != null) {
                Font custom = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(24f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(custom);
                Font[] tmp = new Font[fonts.length + 1];
                System.arraycopy(fonts, 0, tmp, 0, fonts.length);
                tmp[fonts.length] = custom;
                fonts = tmp;
            }
        } catch (Exception ignored) {
        }
    }

    private Font selectBestFontForGreeting(String currentGreeting) {
        List<Font> candidates = new ArrayList<>();
        for (Font font : fonts) {
            if (font.canDisplayUpTo(currentGreeting) == -1) {
                candidates.add(font.deriveFont(24f));
            }
        }
        return candidates.isEmpty() ? new Font("Dialog", Font.PLAIN, 24) : candidates.get(rnd.nextInt(candidates.size()));
    }

    private void resetAnimation() {
        greeting = randomGreeting();
        greetingFont = selectBestFontForGreeting(greeting);
        pos = 0;
        phase = 0;
        delayCounter = 0;
        caretAlpha = 0.0;
        caretDir = 1.0;
    }

    private String randomGreeting() {
        String[] opts = {"Hello", "Привет", "Hola", "Bonjour", "こんにちは",
                "Hallo", "Olá", "Hei", "Salut", "Ahoj", "Sveiki",
                "안녕하세요", "Kamusta", "Aloha", "Jambo"};
        return opts[rnd.nextInt(opts.length)];
    }

    private void stepAnimation() {
        switch (phase) {
            case 0:
                if (typingDelay > 0) typingDelay--;
                else if (pos < greeting.length()) {
                    pos++;
                    typingDelay = 2;
                } else {
                    phase = 1;
                    delayCounter = 25;
                }
                break;
            case 1:
                if (--delayCounter <= 0) phase = 2;
                break;
            case 2:
                if (pos > 0) pos--;
                else {
                    phase = 3;
                    delayCounter = 15;
                }
                break;
            case 3:
                if (--delayCounter <= 0) resetAnimation();
                break;
        }
    }

    private void updateCaretAlpha() {
        double currentAlphaStep = (this.fadeSpeed * 0.05) + 0.02;
        caretAlpha += caretDir * currentAlphaStep;
        if (caretAlpha >= 1.0) {
            caretAlpha = 1.0;
            caretDir = -1.0;
        } else if (caretAlpha <= 0.0) {
            caretAlpha = 0.0;
            caretDir = 1.0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int currentWidth = getWidth();
        int currentHeight = getHeight();

        if (currentWidth > 0 && currentHeight > 0 &&
                (circleCenterX == 0 || circleCenterY == 0 ||
                        currentWidth != lastWidth || currentHeight != lastHeight)) {
            lastWidth = currentWidth;
            lastHeight = currentHeight;
            circleCenterX = currentWidth / 2;
            circleCenterY = currentHeight - 200;
            dotsInitialized = false;
        }

        if (!dotsInitialized && currentWidth > 0 && currentHeight > 0) {
            initDots();
        }

        String shown = greeting.substring(0, pos);
        FontMetrics fmG = g2.getFontMetrics(greetingFont);
        FontMetrics fmS = g2.getFontMetrics(suffixFont);

        int caretWidth = fmS.stringWidth("|");
        int textWidth = fmG.stringWidth(shown) + caretWidth + fmS.stringWidth(suffix);

        int x = (getWidth() - textWidth) / 2;
        int y = 50 + Math.max(fmG.getAscent(), fmS.getAscent());

        g2.setFont(greetingFont);
        g2.setColor(Color.WHITE);
        g2.drawString(shown, x, y);
        x += fmG.stringWidth(shown);

        g2.setFont(suffixFont);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) caretAlpha));
        g2.drawString("|", x, y);
        x += caretWidth;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.drawString(suffix, x, y);

        for (int i = 0; i < DOT_COUNT; i++) {
            Point pi = dotPositions.get(i);

            double pulse = 1.5 + Math.sin(dotPulsePhase[i]) * 1.5;
            int size = (int) (DOT_RADIUS + pulse);
            int drawX = pi.x - size / 2;
            int drawY = pi.y - size / 2;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX, drawY, size, size);

            for (int j = i + 1; j < DOT_COUNT; j++) {
                Point pj = dotPositions.get(j);
                double dist = pi.distance(pj);
                if (dist < CONNECT_DISTANCE) {
                    float alpha = (float) (1.0 - dist / CONNECT_DISTANCE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(Color.GRAY);
                    g2.drawLine(pi.x, pi.y, pj.x, pj.y);
                }
            }
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.dispose();
    }

    private void initDots() {
        dotPositions.clear();
        dotVelocities.clear();

        for (int i = 0; i < DOT_COUNT; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double dist = rnd.nextDouble() * circleRadius;
            int x = (int) (circleCenterX + dist * Math.cos(angle));
            int y = (int) (circleCenterY + dist * Math.sin(angle));
            dotPositions.add(new Point(x, y));

            int dx, dy;
            do {
                dx = rnd.nextInt(3) - 1;
                dy = rnd.nextInt(3) - 1;
            } while (dx == 0 && dy == 0);
            dotVelocities.add(new Point(dx, dy));

            dotPulsePhase[i] = rnd.nextDouble() * Math.PI * 2;
        }

        dotsInitialized = true;
    }
}