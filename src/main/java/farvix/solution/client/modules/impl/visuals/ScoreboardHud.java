package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.FontRenderer;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.ui.hud.HudElementRegistry;
import farvix.solution.api.ui.hud.IHudElement;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Scoreboard HUD", category = ModuleCategory.VISUALS,
        description = "Таблица очков на экране")
public class ScoreboardHud extends Module implements QuickImports, IHudElement {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 1.0f, 0.5f, 2.0f, 0.05f);

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.85f, 0f, 1f, 0.05f);

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = -1;
    public static float hudY = -1;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float PAD_X      = 10f;
    private static final float PAD_Y      = 4f;
    private static final float LINE_GAP   = 1f;
    private static final float VALUE_GAP  = 8f;
    private static final float TITLE_SIZE = 11;
    private static final float LINE_SIZE  = 10;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    // ── Cached dimensions ─────────────────────────────────────────────────────
    private float cachedW = 120f;
    private float cachedH = 40f;

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        ScoreboardObjective objective = getSidebarObjective();
        if (objective == null) return;

        float s = scale.getValue();
        float alpha = bgAlpha.getValue();

        // Шрифт масштабируется по размеру — чёткость при любом scale
        int titleFontSize = Math.max(8, Math.min(32, (int)(TITLE_SIZE * s)));
        int lineFontSize  = Math.max(7, Math.min(32, (int)(LINE_SIZE  * s)));

        FontRenderer titleFont = Fonts.SEMIBOLD.get(titleFontSize);
        FontRenderer lineFont  = Fonts.DEFAULT.get(lineFontSize);

        Scoreboard scoreboard = objective.getScoreboard();
        List<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective).stream()
                .filter(entry -> entry != null && !entry.hidden())
                .filter(entry -> entry.owner() != null && !entry.owner().isBlank()) // убираем пустые строки
                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                        .thenComparing(ScoreboardEntry::owner, String::compareToIgnoreCase))
                .limit(15)
                .toList();

        // ── Вычисляем размеры ─────────────────────────────────────────────────
        String titleStr = objective.getDisplayName().getString();
        float maxW = titleFont.getStringWidth(titleStr);

        for (ScoreboardEntry entry : entries) {
            String name  = resolveNameText(scoreboard, entry).getString();
            float rowW = lineFont.getStringWidth(name);
            if (rowW > maxW) maxW = rowW;
        }

        float titleH   = titleFont.getStringHeight(titleStr);
        float lineH2   = lineFontSize * 0.7f; // компактная высота строки
        float headerH  = titleH + PAD_Y * 2f;
        float lineH    = lineH2 + LINE_GAP;
        float totalW   = maxW + PAD_X * 2f + 8f;
        float totalH   = headerH + entries.size() * lineH + PAD_Y * 0.5f;

        cachedW = totalW;
        cachedH = totalH;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Авто-позиция справа
        if (hudX < 0 || hudY < 0) {
            hudX = (screenW - totalW - 10) / screenW;
            hudY = 40f / screenH;
        }

        float x = hudX * screenW;
        float y = hudY * screenH;

        var ms = e.getContext().getMatrices();

        // ── Фон ──────────────────────────────────────────────────────────────
        glass.render(ShapeProperties.create(ms, x, y, totalW, totalH)
                .round(7f).softness(1.5f).thickness(0).outlineColor(0)
                .color(TempColor.getGuiBackground().alpha(alpha).getRGB())
                .build());

        // ── Заголовок по центру с цветами ────────────────────────────────────
        float titleW = titleFont.getStringWidth(titleStr);
        titleFont.drawText(ms, objective.getDisplayName(),
                x + (totalW - titleW) / 2f,
                y + PAD_Y);

        // Разделитель под заголовком
        float sepY = y + headerH - 1f;
        rectangle.render(ShapeProperties.create(ms, x + PAD_X * 0.5f, sepY,
                totalW - PAD_X, 1f)
                .round(0.5f)
                .color(TempColor.getClientColor().alpha(0.3f).getRGB())
                .build());

        // ── Строки с цветами ──────────────────────────────────────────────────
        int nameColor  = TempColor.getTextPrimary().getRGB();

        float lineY = y + headerH;
        for (ScoreboardEntry entry : entries) {
            Text nameText = resolveNameText(scoreboard, entry);
            String nameStr = nameText.getString();
            if (nameStr.isBlank()) { lineY += lineH; continue; }

            float rowY = lineY + (lineH - lineH2) / 2f;

            // Рендерим с цветами из Text объекта
            lineFont.drawText(ms, nameText, x + PAD_X, rowY);

            lineY += lineH;
        }

        handleDrag(totalW, totalH);    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ScoreboardObjective getSidebarObjective() {
        if (mc.world == null) return null;
        return mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private Text resolveNameText(Scoreboard scoreboard, ScoreboardEntry entry) {
        Text display = entry.display();
        if (display != null) return display;
        String owner = entry.owner();
        if (owner == null) return Text.empty();
        Team team = scoreboard.getScoreHolderTeam(owner);
        Text base = Text.literal(owner);
        return team != null ? Team.decorateName(team, base) : base;
    }

    private Text resolveValueText(ScoreboardObjective objective, ScoreboardEntry entry) {
        Text formatted = entry.formatted(objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
        return formatted != null ? formatted : Text.literal(Integer.toString(entry.value()));
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    private void handleDrag(float w, float h) {
        // Блокируем перетаскивание если открыто контекстное меню
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
        
        // Запрет перетаскивания в контейнерах (сундуки, печки и т.д.), но разрешаем в инвентаре игрока
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen &&
            !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        net.minecraft.client.util.Window win = mc.getWindow();
        if (win == null) return;

        float screenW = win.getScaledWidth();
        float screenH = win.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        double mx = mc.mouse.getX() / win.getScaleFactor();
        double my = mc.mouse.getY() / win.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(win.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed) {
            // Проверяем, не внутри ли ClickGUI
            if (mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen) {
                farvix.solution.api.ui.clickgui.InterfaceScreen gui = 
                    (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
                float guiX = gui.getX();
                float guiY = gui.getY();
                float guiW = gui.getWidth();
                float guiH = gui.getHeight();
                
                // Если клик внутри ClickGUI, не начинаем перетаскивание HUD элемента
                if (mx >= guiX && mx <= guiX + guiW && my >= guiY && my <= guiY + guiH) {
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
            hudX = Math.max(0, Math.min((float)(mx - dragOffX), screenW - w)) / screenW;
            hudY = Math.max(0, Math.min((float)(my - dragOffY), screenH - h)) / screenH;
        }
        wasPressed = pressed;
    }

    // ── IHudElement ───────────────────────────────────────────────────────────

    @Override public float getHudX()      { return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteX(hudX); }
    @Override public float getHudY()      { return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteY(hudY); }
    @Override public float getHudWidth()  { return cachedW; }
    @Override public float getHudHeight() { return cachedH; }
    @Override public Module getModule()   { return this; }

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
}
