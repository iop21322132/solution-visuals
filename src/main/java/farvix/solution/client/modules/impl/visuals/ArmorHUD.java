package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Armor HUD", category = ModuleCategory.PLAYER,
        description = "Показывает броню и прочность")
public class ArmorHUD extends Module implements QuickImports, farvix.solution.api.ui.hud.IHudElement {

    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 1.0f, 0.5f, 2.0f, 0.05f);

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.85f, 0f, 1f, 0.05f);

    public final ModeSetting durabilityMode = new ModeSetting(
            "Прочность", this,
            "Ползунок",
            "Число"
    );

    public final ModeSetting orientation = new ModeSetting(
            "Ориентация", this,
            "Горизонтально",
            "Вертикально"
    );

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = -1; // -1 = авто-позиция правее хотбара
    public static float hudY = -1;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final float SLOT_SIZE  = 20f; // размер иконки брони
    private static final float SLOT_GAP   = 6f;  // отступ между слотами
    private static final float BAR_H      = 3f;  // высота полоски прочности
    private static final float BAR_OFFSET = 2f;  // отступ полоски от иконки
    private static final float PAD_X      = 4f;
    private static final float PAD_Y      = 4f;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        float s = scale.getValue();

        // Броня: порядок как на скрине — шлем, нагрудник, поножи, ботинки
        // Индексы в инвентаре: 39=шлем, 38=нагрудник, 37=поножи, 36=ботинки
        ItemStack[] armor = new ItemStack[]{
                mc.player.getInventory().getArmorStack(3), // шлем
                mc.player.getInventory().getArmorStack(2), // нагрудник
                mc.player.getInventory().getArmorStack(1), // поножи
                mc.player.getInventory().getArmorStack(0)  // ботинки
        };

        // Считаем сколько слотов не пустые
        int count = 0;
        for (ItemStack stack : armor) if (!stack.isEmpty()) count++;
        if (count == 0) return;

        float slotStep = (SLOT_SIZE + SLOT_GAP) * s;
        boolean vertical = orientation.is("Вертикально");

        // In vertical mode each slot = icon + bar + gap
        float vertSlotStep = (SLOT_SIZE + BAR_OFFSET + BAR_H + SLOT_GAP) * s;

        float totalW, totalH;
        if (vertical) {
            totalW = (SLOT_SIZE + PAD_X * 2) * s;
            totalH = count * vertSlotStep - SLOT_GAP * s + PAD_Y * 2 * s;
        } else {
            totalW = count * slotStep - SLOT_GAP * s + PAD_X * 2 * s;
            totalH = (SLOT_SIZE + BAR_OFFSET + BAR_H + PAD_Y * 2) * s;
        }

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Авто-позиция: правее хотбара снизу экрана
        if (hudX < 0 || hudY < 0) {
            // Хотбар: ширина ~182px, центрирован, высота ~22px, отступ снизу ~2px
            float hotbarW = 182f;
            float hotbarX = (screenW - hotbarW) / 2f;
            hudX = (hotbarX + hotbarW + 8) / screenW;
            hudY = (screenH - totalH - 2) / screenH;
        }

        float x = hudX * screenW;
        float y = hudY * screenH;

        DrawContext ctx = e.getContext();
        MatrixStack ms  = ctx.getMatrices();

        // ── Фон панели ────────────────────────────────────────────────────────
        glass.render(ShapeProperties.create(ms, x, y, totalW, totalH)
                .round(8 * s)
                .softness(1.5f)
                .thickness(0f)
                .outlineColor(0)
                .color(TempColor.getGuiBackground().alpha(bgAlpha.getValue()).getRGB())
                .build());

        // ── Слоты брони ───────────────────────────────────────────────────────
        float slotX = x + PAD_X * s;
        float slotY = y + PAD_Y * s;

        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;

            float iconSize = SLOT_SIZE * s;

            // Иконка предмета
            ctx.getMatrices().push();
            ctx.getMatrices().translate(slotX + iconSize / 2f, slotY + iconSize / 2f, 0);
            ctx.getMatrices().scale(s, s, 1f);
            ctx.getMatrices().translate(-8, -8, 0);
            ctx.drawItem(stack, 0, 0);
            ctx.getMatrices().pop();

            // Прочность
            int maxDur = stack.getMaxDamage();
            int curDur = maxDur - stack.getDamage();
            float pct  = maxDur > 0 ? MathHelper.clamp((float) curDur / maxDur, 0f, 1f) : 1f;
            int barColor = getDurabilityColor(pct);

            if (vertical) {
                // Вертикальный: полоска/число ПОД иконкой (как горизонтальный, но слоты идут вниз)
                float barY = slotY + iconSize + BAR_OFFSET * s;
                float barW = iconSize;

                if (durabilityMode.is("Ползунок")) {
                    blur.render(ShapeProperties.create(ms, slotX, barY, barW, BAR_H * s)
                            .round(BAR_H * s / 2f)
                            .color(new FixColor(255, 255, 255, 40).getRGB())
                            .build());
                    float fillW = Math.max(BAR_H * s, barW * pct);
                    blur.render(ShapeProperties.create(ms, slotX, barY, fillW, BAR_H * s)
                            .round(BAR_H * s / 2f)
                            .color(barColor)
                            .build());
                } else {
                    String durStr = maxDur > 0 ? String.valueOf(curDur) : "∞";
                    int fontSize = Math.max(8, (int)(11 * s));
                    float tw = Fonts.SEMIBOLD.get(fontSize).getStringWidth(durStr);
                    Fonts.SEMIBOLD.get(fontSize).drawString(ms, durStr,
                            slotX + (iconSize - tw) / 2f, barY, barColor);
                }
                slotY += vertSlotStep;
            } else {
                // Горизонтальный: полоска/число под иконкой
                float barY = slotY + iconSize + BAR_OFFSET * s;
                float barW = iconSize;

                if (durabilityMode.is("Ползунок")) {
                    blur.render(ShapeProperties.create(ms, slotX, barY, barW, BAR_H * s)
                            .round(BAR_H * s / 2f)
                            .color(new FixColor(255, 255, 255, 40).getRGB())
                            .build());
                    float fillW = Math.max(BAR_H * s, barW * pct);
                    blur.render(ShapeProperties.create(ms, slotX, barY, fillW, BAR_H * s)
                            .round(BAR_H * s / 2f)
                            .color(barColor)
                            .build());
                } else {
                    String durStr = maxDur > 0 ? String.valueOf(curDur) : "∞";
                    int fontSize = Math.max(8, (int)(11 * s));
                    float tw = Fonts.SEMIBOLD.get(fontSize).getStringWidth(durStr);
                    Fonts.SEMIBOLD.get(fontSize).drawString(ms, durStr,
                            slotX + (iconSize - tw) / 2f, barY, barColor);
                }
                slotX += slotStep;
            }
        }

        handleDrag(totalW, totalH);
    }

    // ── Цвет прочности: зелёный → жёлтый → красный ───────────────────────────
    private int getDurabilityColor(float pct) {
        if (pct > 0.5f) {
            // зелёный → жёлтый
            float t = (pct - 0.5f) * 2f;
            int r = (int)(255 * (1f - t));
            return new FixColor(r, 210, 0, 255).getRGB();
        } else {
            // жёлтый → красный
            float t = pct * 2f;
            int g = (int)(210 * t);
            return new FixColor(220, g, 0, 255).getRGB();
        }
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
        
        net.minecraft.client.util.Window currentWindow = mc.getWindow();
        if (currentWindow == null) return;
        
        float screenW = currentWindow.getScaledWidth();
        float screenH = currentWindow.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;
        
        double mx = mc.mouse.getX() / currentWindow.getScaleFactor();
        double my = mc.mouse.getY() / currentWindow.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(
                currentWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

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
                dragging  = true;
                dragOffX  = mx - x;
                dragOffY  = my - y;
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
        boolean vertical = orientation.is("Вертикально");
        int count = 0;
        if (mc.player != null) {
            ItemStack[] armor = new ItemStack[]{
                mc.player.getInventory().getArmorStack(3),
                mc.player.getInventory().getArmorStack(2),
                mc.player.getInventory().getArmorStack(1),
                mc.player.getInventory().getArmorStack(0)
            };
            for (ItemStack stack : armor) if (!stack.isEmpty()) count++;
        }
        if (count == 0) return 0;
        
        float s = scale.getValue();
        if (vertical) {
            return (SLOT_SIZE + PAD_X * 2) * s;
        } else {
            float slotStep = (SLOT_SIZE + SLOT_GAP) * s;
            return count * slotStep - SLOT_GAP * s + PAD_X * 2 * s;
        }
    }

    @Override
    public float getHudHeight() {
        boolean vertical = orientation.is("Вертикально");
        int count = 0;
        if (mc.player != null) {
            ItemStack[] armor = new ItemStack[]{
                mc.player.getInventory().getArmorStack(3),
                mc.player.getInventory().getArmorStack(2),
                mc.player.getInventory().getArmorStack(1),
                mc.player.getInventory().getArmorStack(0)
            };
            for (ItemStack stack : armor) if (!stack.isEmpty()) count++;
        }
        if (count == 0) return 0;
        
        float s = scale.getValue();
        if (vertical) {
            float vertSlotStep = (SLOT_SIZE + BAR_OFFSET + BAR_H + SLOT_GAP) * s;
            return count * vertSlotStep - SLOT_GAP * s + PAD_Y * 2 * s;
        } else {
            return (SLOT_SIZE + BAR_OFFSET + BAR_H + PAD_Y * 2) * s;
        }
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
