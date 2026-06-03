package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.events.impl.game.EventMessage;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.FontRenderer;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.ui.hud.HudElementRegistry;
import farvix.solution.api.ui.hud.IHudElement;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.NoteManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Note", category = ModuleCategory.VISUALS,
        description = "Заметки на экране, команды .note в чате")
public class Note extends Module implements QuickImports, IHudElement {

    public final SliderSetting width = new SliderSetting("note.width", this,
            180f, 80f, 400f, 5f);
    public final SliderSetting height = new SliderSetting("note.height", this,
            160f, 60f, 400f, 5f);
    public final SliderSetting bgAlpha = new SliderSetting("note.bgAlpha", this,
            0.85f, 0f, 1f, 0.05f);

    public static float hudX = 0.0104f;
    public static float hudY = 0.3703f;

    private static final float PAD = 8f;
    private static final float HEADER_H = 18f;
    private static final float NOTE_GAP = 1.75f;
    private static final String BULLET = "• ";
    private static final int BODY_FONT_SIZE = 10;
    private static final float LINE_H = 11f;

    private final Animation visibilityAnim = new Animation(Easing.EASE_OUT_CUBIC, 300);

    private boolean dragging;
    private double dragOffX, dragOffY;
    private boolean wasPressed;

    @EventHandler
    public void onMessage(EventMessage e) {
        if (NoteManager.getInstance() != null && NoteManager.getInstance().handleCommand(e.getMessage())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;

        List<String> notes = NoteManager.getInstance() != null
                ? NoteManager.getInstance().getNotesView()
                : List.of();

        boolean inGuiOrChat = mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen
                || mc.currentScreen instanceof ChatScreen;
        boolean shouldShow = (!notes.isEmpty() || inGuiOrChat)
                && (mc.currentScreen == null || inGuiOrChat);
        visibilityAnim.run(shouldShow ? 1f : 0f);
        float widgetAlpha = visibilityAnim.getValue();
        if (widgetAlpha <= 0.01f) return;

        float panelW = width.getValue();
        float panelH = height.getValue();
        float innerW = panelW - PAD * 2f;

        FontRenderer bodyFont = Fonts.DEFAULT.get(BODY_FONT_SIZE);
        FontRenderer titleFont = Fonts.SEMIBOLD.get(12);

        List<RenderedNote> rendered = buildRenderedNotes(notes, bodyFont, innerW);
        String contIndent = spacesForWidth(bodyFont, bodyFont.getStringWidth(BULLET));

        DrawContext ctx = e.getContext();
        FixColor bg = TempColor.getGuiBackground();

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        float textX = x + PAD;
        float contentY = y + PAD + HEADER_H;

        ctx.getMatrices().push();

        glass.render(ShapeProperties.create(ctx.getMatrices(), x, y, panelW, panelH)
                .round(10)
                .softness(1.5f)
                .thickness(0f)
                .outlineColor(0)
                .color(bg.alpha(bgAlpha.getValue() * widgetAlpha).getRGB())
                .build());

        float titleY = y + PAD + HEADER_H / 2f - 4f;
        titleFont.drawString(ctx.getMatrices(), "Note",
                textX, titleY,
                TempColor.getTextPrimary().alpha(widgetAlpha).getRGB());

        rectangle.render(ShapeProperties.create(ctx.getMatrices(),
                x + PAD * 0.5f, y + HEADER_H + PAD * 0.35f,
                panelW - PAD, 0.5f)
                .round(0)
                .color(TempColor.getSeparatorHorizontal().alpha(0.45f * widgetAlpha).getRGB())
                .build());

        float currentY = contentY;
        float clipBottom = y + panelH - PAD;

        if (notes.isEmpty() && inGuiOrChat) {
            int hintColor = TempColor.getTextSecondary().alpha(0.7f * widgetAlpha).getRGB();
            bodyFont.drawString(ctx.getMatrices(),
                    ".note add <текст>",
                    textX, currentY,
                    hintColor);
        } else {
            for (RenderedNote rn : rendered) {
                if (currentY > clipBottom - LINE_H) break;

                for (int i = 0; i < rn.lines.size(); i++) {
                    if (currentY > clipBottom - LINE_H) break;
                    String prefix = i == 0 ? BULLET : contIndent;
                    String line = prefix + rn.lines.get(i);
                    int lineColor = TempColor.getTextPrimary().alpha(widgetAlpha).getRGB();
                    bodyFont.drawString(ctx.getMatrices(), line, textX, currentY, lineColor);
                    currentY += LINE_H;
                }
                currentY += NOTE_GAP;
            }
        }

        ctx.getMatrices().pop();
        handleDrag(panelW, panelH);
    }

    private static String spacesForWidth(FontRenderer font, float targetWidth) {
        if (targetWidth <= 0f) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        while (font.getStringWidth(sb.toString()) < targetWidth - 0.5f && sb.length() < 24) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private List<RenderedNote> buildRenderedNotes(List<String> notes, FontRenderer font, float maxWidth) {
        List<RenderedNote> result = new ArrayList<>();
        float textMaxW = Math.max(8f, maxWidth - font.getStringWidth(BULLET));

        for (String note : notes) {
            List<String> wrapped = wrapText(note, font, textMaxW);
            if (wrapped.isEmpty()) {
                wrapped = new ArrayList<>();
                wrapped.add("");
            }
            result.add(new RenderedNote(wrapped));
        }
        return result;
    }

    private List<String> wrapText(String text, FontRenderer font, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        if (maxWidth <= 4f) {
            lines.add(breakLongToken(text, font, maxWidth));
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (font.getStringWidth(word) > maxWidth) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                lines.addAll(breakLongTokenLines(word, font, maxWidth));
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getStringWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String breakLongToken(String token, FontRenderer font, float maxWidth) {
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            chunk.append(token.charAt(i));
            if (font.getStringWidth(chunk.toString()) > maxWidth) {
                if (chunk.length() == 1) {
                    return chunk.toString();
                }
                chunk.deleteCharAt(chunk.length() - 1);
                return chunk.toString();
            }
        }
        return chunk.toString();
    }

    private static List<String> breakLongTokenLines(String token, FontRenderer font, float maxWidth) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < token.length()) {
            StringBuilder chunk = new StringBuilder();
            int i = start;
            for (; i < token.length(); i++) {
                chunk.append(token.charAt(i));
                if (font.getStringWidth(chunk.toString()) > maxWidth) {
                    if (chunk.length() == 1) {
                        parts.add(chunk.toString());
                        start = i + 1;
                        chunk = new StringBuilder();
                        break;
                    }
                    chunk.deleteCharAt(chunk.length() - 1);
                    parts.add(chunk.toString());
                    start = i;
                    chunk = new StringBuilder();
                    break;
                }
            }
            if (i >= token.length() && !chunk.isEmpty()) {
                parts.add(chunk.toString());
                break;
            }
            if (i >= token.length()) {
                break;
            }
        }
        return parts.isEmpty() ? List.of("") : parts;
    }

    private void handleDrag(float w, float h) {
        if (farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance().hasOpenMenu()) {
            dragging = false;
            wasPressed = false;
            return;
        }
        if (mc.currentScreen == null) {
            dragging = false;
            wasPressed = false;
            return;
        }
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen
                && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
            dragging = false;
            wasPressed = false;
            return;
        }

        var window = mc.getWindow();
        if (window == null) return;

        float screenW = window.getScaledWidth();
        float screenH = window.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        double mx = mc.mouse.getX() / window.getScaleFactor();
        double my = mc.mouse.getY() / window.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed) {
            if (mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen gui) {
                if (mx >= gui.getX() && mx <= gui.getX() + gui.getWidth()
                        && my >= gui.getY() && my <= gui.getY() + gui.getHeight()) {
                    return;
                }
            }
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
                dragging = true;
                dragOffX = mx - x;
                dragOffY = my - y;
            }
        }
        if (!pressed) dragging = false;
        if (dragging && pressed) {
            hudX = Math.max(0, Math.min((float) (mx - dragOffX), screenW - w)) / screenW;
            hudY = Math.max(0, Math.min((float) (my - dragOffY), screenH - h)) / screenH;
        }
        wasPressed = pressed;
    }

    @Override
    public float getHudX() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteX(hudX);
    }

    @Override
    public float getHudY() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteY(hudY);
    }

    @Override
    public float getHudWidth() {
        return width.getValue();
    }

    @Override
    public float getHudHeight() {
        return height.getValue();
    }

    @Override
    public Module getModule() {
        return this;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float x = getHudX();
        float y = getHudY();
        return mouseX >= x && mouseX <= x + getHudWidth()
                && mouseY >= y && mouseY <= y + getHudHeight();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        HudElementRegistry.getInstance().register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HudElementRegistry.getInstance().unregister(this);
    }

    private static final class RenderedNote {
        final List<String> lines;

        RenderedNote(List<String> lines) {
            this.lines = lines;
        }
    }
}
