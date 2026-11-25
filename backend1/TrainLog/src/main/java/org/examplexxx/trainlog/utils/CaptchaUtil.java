package org.examplexxx.trainlog.utils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图形验证码绘制工具，只负责把字符画成图片，不保存任何状态。
 */
public final class CaptchaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaUtil() {
    }

    public static String render(String code, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(249, 251, 255));
            g.fillRect(0, 0, width, height);

            int noise = Math.max(40, width * height / 22);
            for (int i = 0; i < noise; i++) {
                g.setColor(randomColor(120, 230));
                g.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
            }

            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 3; i++) {
                g.setColor(randomColor(110, 200));
                int y1 = RANDOM.nextInt(height);
                int y2 = RANDOM.nextInt(height);
                QuadCurve2D curve = new QuadCurve2D.Float(0, y1, width / 2f,
                        RANDOM.nextInt(height), width, y2);
                g.draw(curve);
            }

            int fontSize = (int) Math.round(height * 0.62);
            Font baseFont = new Font("Arial", Font.BOLD, fontSize);
            int slot = width / code.length();
            for (int i = 0; i < code.length(); i++) {
                AffineTransform previous = g.getTransform();
                int centerX = slot * i + slot / 2;
                int centerY = height / 2;
                double angle = Math.toRadians(RANDOM.nextInt(19) - 9);
                Font font = baseFont.deriveFont(Font.BOLD, fontSize + RANDOM.nextInt(3) - 1);
                g.rotate(angle, centerX, centerY);
                g.setFont(font);
                FontMetrics metrics = g.getFontMetrics(font);
                String text = String.valueOf(code.charAt(i));
                int x = centerX - metrics.stringWidth(text) / 2;
                int y = centerY + metrics.getAscent() / 2 - 1;
                g.setColor(randomColor(20, 120));
                g.drawString(text, x, y);
                g.setTransform(previous);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("验证码图片生成失败", e);
        }
    }

    private static Color randomColor(int min, int max) {
        int range = Math.max(1, max - min);
        return new Color(min + RANDOM.nextInt(range), min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range));
    }
}
