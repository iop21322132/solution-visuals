package farvix.solution.client.modules.impl.environment.interfaces;

import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.client.modules.impl.environment.interfaces.api.UIElement;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkUI implements UIElement, QuickImports {

    // ── Per-element positions ─────────────────────────────────────────────────
    public static float solutionX = 0.0052f, solutionY = 0.0093f;
    public static float timeX  = -1, timeY  = 0.0093f;
    public static float fpsX   = 0.0052f,  fpsY   = 0.0389f;
    public static float coordsX = 0.0052f, coordsY = 0.0685f;

    // ── Drag state for each element ───────────────────────────────────────────
    private boolean solutionDragging = false;
    private double  solutionDragOffX, solutionDragOffY;
    private float   solutionW = 80, solutionH = 15;

    private boolean timeDragging = false;
    private double  timeDragOffX, timeDragOffY;
    private float   timeW = 60, timeH = 15;

    private boolean fpsDragging = false;
    private double  fpsDragOffX, fpsDragOffY;
    private float   fpsW = 50, fpsH = 15;

    private boolean coordsDragging = false;
    private double  coordsDragOffX, coordsDragOffY;
    private float   coordsW = 80, coordsH = 15;

    private boolean dragEnabled = false;
    private boolean wasPressed  = false;

    public void setDragEnabled(boolean enabled) {
        this.dragEnabled = enabled;
    }

    @Override
    public void render(EventRender2D e) {
        render(e, true, true, true, true, true, 1.0f);
    }

    public void render(EventRender2D e, boolean showWatermark, boolean showTime, boolean showFps, boolean showCoords, boolean merged, float bgAlpha) {
        Color bgColor      = TempColor.getModuleBackground().alpha(bgAlpha).getColor();
        Color outlineColor = new java.awt.Color(0, 0, 0, 0); // без рамки

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        if (merged) {
            renderMerged(e, showWatermark, showTime, showFps, showCoords, bgColor, outlineColor);
        } else {
            if (showWatermark) {
                boolean isWhiteTheme = farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("White");
                Color textColor = isWhiteTheme ? new Color(20, 20, 20) : Color.WHITE;
                float x = solutionX * screenW;
                float y = solutionY * screenH;
                solutionW = drawIslandSolidColor(e, x, y, bgColor, outlineColor, "Solution Visual", textColor);
            }

            if (showTime) {
                String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                String timeText    = "%s PM".formatted(currentTime);
                if (timeX < 0) {
                    float solXAbs = solutionX * screenW;
                    timeX = (solXAbs + solutionW + 1) / screenW;
                }
                float x = timeX * screenW;
                float y = timeY * screenH;
                timeW = drawIslandWithPartialColor(e, x, y, bgColor, outlineColor, timeText, "PM", TempColor.getTextSecondary());
            }

            if (showFps) {
                String fpsText = "%s FPS".formatted(mc.getCurrentFps());
                float x = fpsX * screenW;
                float y = fpsY * screenH;
                fpsW = drawIslandWithPartialColor(e, x, y, bgColor, outlineColor, fpsText, "FPS", TempColor.getTextSecondary());
            }

            if (showCoords && mc.player != null) {
                int px = (int) mc.player.getX();
                int py = (int) mc.player.getY();
                int pz = (int) mc.player.getZ();
                String coordText = px + " " + py + " " + pz + " XYZ";
                float x = coordsX * screenW;
                float y = coordsY * screenH;
                coordsW = drawIslandWithPartialColor(e, x, y, bgColor, outlineColor, coordText, "XYZ", TempColor.getTextSecondary());
            }
        }

        handleDrag(showWatermark, showTime, showFps);
    }

    private void renderMerged(EventRender2D e, boolean showWatermark, boolean showTime, boolean showFps, boolean showCoords,
                               Color bgColor, Color outlineColor) {
        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Build combined text parts
        float x = solutionX * screenW, y = solutionY * screenH;
        float pad = 10f, gap = 8f;
        float totalW = pad;

        // Measure widths
        float wW = showWatermark ? Fonts.DEFAULT.get(14).getStringWidth("Solution Visual") + gap : 0;
        float wT = 0;
        String timeText = "";
        if (showTime) {
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            timeText = currentTime + " PM";
            wT = Fonts.DEFAULT.get(14).getStringWidth(timeText) + gap;
        }
        float wF = 0;
        String fpsText = "";
        if (showFps) {
            fpsText = mc.getCurrentFps() + " FPS";
            wF = Fonts.DEFAULT.get(14).getStringWidth(fpsText) + gap;
        }
        float wC = 0;
        String coordsText = "";
        if (showCoords && mc.player != null) {
            int cx2 = (int) mc.player.getX();
            int cy2 = (int) mc.player.getY();
            int cz2 = (int) mc.player.getZ();
            coordsText = cx2 + " " + cy2 + " " + cz2 + " XYZ";
            wC = Fonts.DEFAULT.get(14).getStringWidth(coordsText) + gap;
        }

        // Count visible separators
        int parts = (showWatermark ? 1 : 0) + (showTime ? 1 : 0) + (showFps ? 1 : 0) + (showCoords && mc.player != null ? 1 : 0);
        if (parts == 0) return;

        float sepW = parts > 1 ? (parts - 1) * 1f : 0;
        totalW = pad + wW + wT + wF + wC + sepW + pad;

        float height = 15f;

        // Draw single background
        glass.render(ShapeProperties.create(e.getContext().getMatrices(), x, y, totalW, height)
                .round(6.5f).softness(1.5f).thickness(0)
                .outlineColor(outlineColor.getRGB()).color(bgColor.getRGB()).build());

        // Draw content
        float cx = x + pad;

        if (showWatermark) {
            boolean isWhiteTheme = farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("White");
            Color textColor = isWhiteTheme ? new Color(20, 20, 20) : Color.WHITE;
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), "Solution Visual", cx, y + 6, textColor.getRGB());
            cx += wW;

            if (showTime || showFps) {
                // Separator
                rectangle.render(ShapeProperties.create(e.getContext().getMatrices(), cx, y + 2, 1, height - 4)
                        .round(0).color(TempColor.getSeparatorVertical().alpha(0.5f).getRGB()).build());
                cx += 1 + gap / 2f;
            }
        }

        if (showTime) {
            String timePart = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            float tw = Fonts.DEFAULT.get(14).getStringWidth(timePart + " ");
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), timePart + " ", cx, y + 6,
                    TempColor.getTextPrimary().getRGB());
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), "PM", cx + tw, y + 6,
                    TempColor.getTextSecondary().getRGB());
            cx += wT;

            if (showFps || (showCoords && mc.player != null)) {
                rectangle.render(ShapeProperties.create(e.getContext().getMatrices(), cx, y + 2, 1, height - 4)
                        .round(0).color(TempColor.getSeparatorVertical().alpha(0.5f).getRGB()).build());
                cx += 1 + gap / 2f;
            }
        }

        if (showFps) {
            String fpsPart = String.valueOf(mc.getCurrentFps());
            float fw = Fonts.DEFAULT.get(14).getStringWidth(fpsPart + " ");
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), fpsPart + " ", cx, y + 6,
                    TempColor.getTextPrimary().getRGB());
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), "FPS", cx + fw, y + 6,
                    TempColor.getTextSecondary().getRGB());
            cx += wF;

            if (showCoords && mc.player != null) {
                rectangle.render(ShapeProperties.create(e.getContext().getMatrices(), cx, y + 2, 1, height - 4)
                        .round(0).color(TempColor.getSeparatorVertical().alpha(0.5f).getRGB()).build());
                cx += 1 + gap / 2f;
            }
        }

        if (showCoords && mc.player != null) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String coordStr = px + " " + py + " " + pz;
            float cw2 = Fonts.DEFAULT.get(14).getStringWidth(coordStr + " ");
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), coordStr + " ", cx, y + 6,
                    TempColor.getTextPrimary().getRGB());
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), "XYZ", cx + cw2, y + 6,
                    TempColor.getTextSecondary().getRGB());
        }

        solutionW = totalW;
    }

    // ── Drag logic ────────────────────────────────────────────────────────────

    private void handleDrag(boolean showWatermark, boolean showTime, boolean showFps) {
        // Блокируем перетаскивание если открыто контекстное меню
        if (farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance().hasOpenMenu()) {
            solutionDragging = timeDragging = fpsDragging = coordsDragging = false;
            wasPressed = false;
            return;
        }
        
        if (!dragEnabled) {
            solutionDragging = timeDragging = fpsDragging = coordsDragging = false;
            wasPressed = false;
            return;
        }

        net.minecraft.client.util.Window currentWindow = mc.getWindow();
        if (currentWindow == null) return;
        
        float screenW = currentWindow.getScaledWidth();
        float screenH = currentWindow.getScaledHeight();

        double mx      = mc.mouse.getX() / currentWindow.getScaleFactor();
        double my      = mc.mouse.getY() / currentWindow.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(currentWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        float solXAbs = solutionX * screenW;
        float solYAbs = solutionY * screenH;
        float timeXAbs = timeX * screenW;
        float timeYAbs = timeY * screenH;
        float fpsXAbs = fpsX * screenW;
        float fpsYAbs = fpsY * screenH;
        float coordsXAbs = coordsX * screenW;
        float coordsYAbs = coordsY * screenH;

        if (pressed && !wasPressed) {
            if (showFps && hits(mx, my, fpsXAbs, fpsYAbs, fpsW, fpsH)) {
                fpsDragging    = true;
                fpsDragOffX    = mx - fpsXAbs;
                fpsDragOffY    = my - fpsYAbs;
            } else if (showTime && hits(mx, my, timeXAbs, timeYAbs, timeW, timeH)) {
                timeDragging   = true;
                timeDragOffX   = mx - timeXAbs;
                timeDragOffY   = my - timeYAbs;
            } else if (hits(mx, my, coordsXAbs, coordsYAbs, coordsW, coordsH)) {
                // Координаты всегда можно перетаскивать
                coordsDragging  = true;
                coordsDragOffX  = mx - coordsXAbs;
                coordsDragOffY  = my - coordsYAbs;
            } else if (showWatermark && hits(mx, my, solXAbs, solYAbs, solutionW, solutionH)) {
                solutionDragging  = true;
                solutionDragOffX  = mx - solXAbs;
                solutionDragOffY  = my - solYAbs;
            }
        }

        if (!pressed) {
            solutionDragging = timeDragging = fpsDragging = coordsDragging = false;
        }

        if (pressed) {
            if (solutionDragging) {
                solutionX = clamp((float)(mx - solutionDragOffX), screenW - solutionW) / screenW;
                solutionY = clamp((float)(my - solutionDragOffY), screenH - solutionH) / screenH;
            }
            if (timeDragging) {
                timeX = clamp((float)(mx - timeDragOffX), screenW - timeW) / screenW;
                timeY = clamp((float)(my - timeDragOffY), screenH - timeH) / screenH;
            }
            if (fpsDragging) {
                fpsX = clamp((float)(mx - fpsDragOffX), screenW - fpsW) / screenW;
                fpsY = clamp((float)(my - fpsDragOffY), screenH - fpsH) / screenH;
            }
            if (coordsDragging) {
                coordsX = clamp((float)(mx - coordsDragOffX), screenW - coordsW) / screenW;
                coordsY = clamp((float)(my - coordsDragOffY), screenH - coordsH) / screenH;
            }
        }

        wasPressed = pressed;
    }

    private boolean hits(double mx, double my, float ex, float ey, float ew, float eh) {
        return mx >= ex && mx <= ex + ew && my >= ey && my <= ey + eh;
    }

    private float clamp(float v, float max) {
        return Math.max(0, Math.min(v, max));
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    public float drawIslandPartialGradient(EventRender2D e, float x, float y, Color bg, Color outline, String text, String gradientPart) {
        float fullTextWidth = Fonts.DEFAULT.get(14).getStringWidth(text);
        float width  = fullTextWidth + 10f;
        float textX  = x + (width - fullTextWidth) / 2f;

        glass.render(ShapeProperties.create(e.getContext().getMatrices(), x, y, width, 15)
                .round(6.5f).softness(1.5f).thickness(0)
                .outlineColor(outline.getRGB()).color(bg.getRGB()).build());

        int idx = text.indexOf(gradientPart);
        if (idx != -1) {
            String before  = text.substring(0, idx);
            String after   = text.substring(idx + gradientPart.length());
            float  beforeW = Fonts.DEFAULT.get(14).getStringWidth(before);
            float  gradW   = Fonts.DEFAULT.get(14).getStringWidth(gradientPart);

            if (!before.isEmpty())
                Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), before, textX, y + 6, TempColor.getTextPrimary().getRGB());
            Fonts.DEFAULT.get(14).drawGradientString(e.getContext().getMatrices(), gradientPart, textX + beforeW, y + 6,
                    TempColor.getClientColor().getRGB(), TempColor.getTextPrimary().getRGB());
            if (!after.isEmpty())
                Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), after, textX + beforeW + gradW, y + 6, TempColor.getTextPrimary().getRGB());
        } else {
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), text, textX, y + 6, TempColor.getTextPrimary().getRGB());
        }
        return width;
    }

    public float drawIslandSolidColor(EventRender2D e, float x, float y, Color bg, Color outline, String text, Color textColor) {
        float textWidth = Fonts.DEFAULT.get(14).getStringWidth(text);
        float width  = textWidth + 10f;
        float textX  = x + (width - textWidth) / 2f;

        glass.render(ShapeProperties.create(e.getContext().getMatrices(), x, y, width, 15)
                .round(6.5f).softness(1.5f).thickness(0)
                .outlineColor(outline.getRGB()).color(bg.getRGB()).build());

        Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), text, textX, y + 6, textColor.getRGB());
        return width;
    }

    public float drawIsland(EventRender2D e, float x, float y, Color bg, Color outline, String text, boolean gradient) {
        float textWidth = Fonts.DEFAULT.get(14).getStringWidth(text);
        float width  = textWidth + 10f;
        float textX  = x + (width - textWidth) / 2f;

        glass.render(ShapeProperties.create(e.getContext().getMatrices(), x, y, width, 15)
                .round(6.5f).softness(1.5f).thickness(0)
                .outlineColor(outline.getRGB()).color(bg.getRGB()).build());

        if (gradient) {
            Fonts.DEFAULT.get(14).drawGradientString(e.getContext().getMatrices(), text, textX, y + 6,
                    TempColor.getClientColor().getRGB(), TempColor.getTextPrimary().getRGB());
        } else {
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), text, textX, y + 6, TempColor.getTextPrimary().getRGB());
        }
        return width;
    }

    public float drawIslandWithPartialColor(EventRender2D e, float x, float y, Color bg, Color outline, String text, String coloredPart, Color color) {
        float fullTextWidth = Fonts.DEFAULT.get(14).getStringWidth(text);
        float width  = fullTextWidth + 10f;
        float textX  = x + (width - fullTextWidth) / 2f;

        glass.render(ShapeProperties.create(e.getContext().getMatrices(), x, y, width, 15)
                .round(6.5f).softness(1.5f).thickness(0)
                .outlineColor(outline.getRGB()).color(bg.getRGB()).build());

        int coloredIndex = text.indexOf(coloredPart);
        if (coloredIndex != -1) {
            String before      = text.substring(0, coloredIndex);
            float  beforeWidth = Fonts.DEFAULT.get(14).getStringWidth(before);
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), before, textX, y + 6, TempColor.getTextPrimary().getRGB());
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), coloredPart, textX + beforeWidth, y + 6, color.getRGB());
        } else {
            Fonts.DEFAULT.get(14).drawString(e.getContext().getMatrices(), text, textX, y + 6, TempColor.getTextPrimary().getRGB());
        }
        return width;
    }

    // ── Getters for IHudElement ───────────────────────────────────────────────
    public float getX() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteX(solutionX);
    }

    public float getY() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteY(solutionY);
    }

    public float getWidth() {
        return solutionW;
    }

    public float getHeight() {
        return solutionH;
    }
}
