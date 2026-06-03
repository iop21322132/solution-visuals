package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.util.color.FixColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@Getter
@ModuleInfo(name = "Inventory", category = ModuleCategory.VISUALS, description = "Показывает инвентарь на экране")
public class InventoryHUD extends Module implements QuickImports, farvix.solution.api.ui.hud.IHudElement {

    public final SliderSetting scale = new SliderSetting("Размер", this, 1.0f, 0.5f, 2.0f, 0.05f);
    public final SliderSetting bgAlpha = new SliderSetting("Прозрачность", this, 0.85f, 0f, 1f, 0.05f);

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = 0.0104f;
    public static float hudY = 0.0185f;

    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    // Base layout (at scale 1.0) — slots are 20×20 with 3px gap
    private static final int   COLS      = 9;
    private static final int   ROWS      = 3;
    private static final float SLOT_SIZE = 20f;
    private static final float SLOT_GAP  = 3f;
    private static final float PADDING   = 10f;
    private static final float HEADER_H  = 20f;

    // Fixed dark colours — never affected by theme
    private static final int COUNT_BG    = new FixColor(0,   0,   0,   200).getRGB();
    private static final int COUNT_FG    = new FixColor(255, 255, 255, 255).getRGB();

    // Кэш для инвентаря
    private DefaultedList<ItemStack> cachedInventory = null;
    private long lastInventoryUpdate = 0;
    private static final long INVENTORY_CACHE_TIME = 50; // обновляем каждые 50мс

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || mc.player == null) return;

        // Кэшируем инвентарь
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInventoryUpdate > INVENTORY_CACHE_TIME || cachedInventory == null) {
            cachedInventory = DefaultedList.copyOf(ItemStack.EMPTY, mc.player.getInventory().main.toArray(new ItemStack[0]));
            lastInventoryUpdate = currentTime;
        }

        float s = scale.getValue();

        float innerW = COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        float innerH = ROWS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        float totalW = (innerW + PADDING * 2) * s;
        float totalH = (PADDING + innerH + PADDING) * s;  // без HEADER_H

        DrawContext ctx = e.getContext();
        int accent = TempColor.getClientColor().alpha(1).getRGB();

        // ── Outer panel ───────────────────────────────────────────────────────
        int bgColor    = TempColor.getGuiBackground().alpha(bgAlpha.getValue()).getRGB();
        int bgBorder   = TempColor.getGuiBorder().alpha(bgAlpha.getValue()).getRGB();
        int slotColor  = TempColor.getModuleBackground().alpha(0.7f * bgAlpha.getValue()).getRGB();
        int slotBorder = TempColor.getModuleBorder().alpha(0.5f * bgAlpha.getValue()).getRGB();

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        glass.render(ShapeProperties.create(ctx.getMatrices(), x, y, totalW, totalH)
                .round(12 * s).softness(1.5f).thickness(0f)
                .outlineColor(0).color(bgColor).build());

        // ── Grid (без заголовка) ──────────────────────────────────────────────
        DefaultedList<ItemStack> inv = cachedInventory;
        float gridX0 = x + PADDING * s;
        float gridY0 = y + PADDING * s;  // сразу от верхнего края

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotIdx = 9 + row * COLS + col;

                float sx = gridX0 + col * (SLOT_SIZE + SLOT_GAP) * s;
                float sy = gridY0 + row * (SLOT_SIZE + SLOT_GAP) * s;
                float sw = SLOT_SIZE * s;
                float sh = SLOT_SIZE * s;

                // Slot background
                blur.render(ShapeProperties.create(ctx.getMatrices(), sx, sy, sw, sh)
                        .round(5 * s).softness(1f).thickness(0f)
                        .outlineColor(0).color(slotColor).build());

                if (slotIdx >= inv.size()) continue;
                ItemStack stack = inv.get(slotIdx);
                if (stack.isEmpty()) continue;

                // ── Pass 1: item icon ─────────────────────────────────────────
                float iconScale = sw / 16f;
                float centerX   = sx + sw / 2f;
                float centerY   = sy + sh / 2f;

                ctx.getMatrices().push();
                ctx.getMatrices().translate(centerX, centerY, 0);
                ctx.getMatrices().scale(iconScale, iconScale, 1f);
                ctx.getMatrices().translate(-8, -8, 0);
                ctx.drawItem(stack, 0, 0);
                ctx.getMatrices().pop();
            }
        }

        // ── Pass 2: count badges — drawn AFTER all items so z is on top ──────
        // drawItem pushes z to ~150; we translate z above that to stay on top.
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200); // above item z (150) + gui layers

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotIdx = 9 + row * COLS + col;
                if (slotIdx >= inv.size()) continue;
                ItemStack stack = inv.get(slotIdx);
                if (stack.isEmpty() || stack.getCount() <= 1) continue;

                float sx = gridX0 + col * (SLOT_SIZE + SLOT_GAP) * s;
                float sy = gridY0 + row * (SLOT_SIZE + SLOT_GAP) * s;
                float sw = SLOT_SIZE * s;
                float sh = SLOT_SIZE * s;

                String countStr = String.valueOf(stack.getCount());

                // Pill size: fixed fraction of slot, never bigger than slot
                float pillW = Math.min(sw * 0.75f, sw - 2);
                float pillH = Math.min(sh * 0.38f, sh - 2);
                float pillX = sx + (sw - pillW) / 2f;   // horizontally centred in slot
                float pillY = sy + sh - pillH - 1;       // bottom of slot

                // Pick largest font that fits inside pill width
                int countFont = Math.max(6, (int)(9 * s));
                while (countFont > 6) {
                    if (Fonts.SEMIBOLD.get(countFont).getStringWidth(countStr) <= pillW - 2) break;
                    countFont--;
                }

                float tw = Fonts.SEMIBOLD.get(countFont).getStringWidth(countStr);
                float th = countFont * 0.75f; // visual cap-height approx

                // Dark pill
                blur.render(ShapeProperties.create(ctx.getMatrices(), pillX, pillY, pillW, pillH)
                        .round(3 * s).color(COUNT_BG).build());

                // Text centred on pill + 5px down, larger semibold font
                int boldFont = Math.max(6, countFont + 2);
                // re-check it still fits
                while (boldFont > 6 && Fonts.SEMIBOLD.get(boldFont).getStringWidth(countStr) > pillW - 2) {
                    boldFont--;
                }
                float boldTw = Fonts.SEMIBOLD.get(boldFont).getStringWidth(countStr);
                float boldTh = boldFont * 0.75f;
                float textX = pillX + (pillW - boldTw) / 2f;
                float textY = pillY + (pillH - boldTh) / 2f + 3;
                Fonts.SEMIBOLD.get(boldFont).drawString(
                        ctx.getMatrices(), countStr, textX, textY, COUNT_FG);
            }
        }

        ctx.getMatrices().pop();

        handleDrag(totalW, totalH);
    }

    private void handleDrag(float w, float h) {
        // Блокируем перетаскивание если открыто контекстное меню
        if (farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance().hasOpenMenu()) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        // Перетаскивание только когда открыт экран (GUI или чат)
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
        
        net.minecraft.client.util.Window currentWindow = mc.getWindow();
        if (currentWindow == null) return;
        
        float screenW = currentWindow.getScaledWidth();
        float screenH = currentWindow.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;
        
        double mx = mc.mouse.getX() / currentWindow.getScaleFactor();
        double my = mc.mouse.getY() / currentWindow.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(currentWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

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

    // ── IHudElement implementation ────────────────────────────────────────────
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
        float s = scale.getValue();
        float innerW = COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        return (innerW + PADDING * 2) * s;
    }

    @Override
    public float getHudHeight() {
        float s = scale.getValue();
        float innerH = ROWS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        return (PADDING + innerH + PADDING) * s;
    }

    @Override
    public Module getModule() {
        return this;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float x = getHudX();
        float y = getHudY();
        float w = getHudWidth();
        float h = getHudHeight();
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().unregister(this);
    }
}
