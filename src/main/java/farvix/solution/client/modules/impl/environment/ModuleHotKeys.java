package farvix.solution.client.modules.impl.environment;

import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

@ModuleInfo(name = "HotKeys", category = ModuleCategory.VISUALS,
        description = "Список активных модулей")
public class ModuleHotKeys extends Module implements QuickImports, farvix.solution.api.ui.hud.IHudElement {

    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 1.0f, 0.1f, 2.0f, 0.05f);

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.92f, 0f, 1f, 0.05f);

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = -1; // -1 = авто (правый верхний угол)
    public static float hudY = 0.0185f;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final float FIXED_WIDTH = 95f;  // Ширина как в reference

    // ── Animated size ─────────────────────────────────────────────────────────
    private float currentH = 40f;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    // ── Animation ─────────────────────────────────────────────────────────────
    private final Map<String, Float> moduleAnimations = new HashMap<>();
    private final Map<String, Long> moduleEnableTime = new HashMap<>();

    // Кэш для активных модулей
    private List<Module> cachedActiveModules = new ArrayList<>();
    private long lastModuleUpdate = 0;
    private static final long MODULE_CACHE_TIME = 250; // обновляем каждые 250мс для оптимизации

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;

        // Постоянная проверка текущего сервера для обновления ограничений функций
        ClickUI.isToolrise();

        float s = scale.getValue();
        DrawContext ctx = e.getContext();

        // Обновляем кэш забинженных модулей только раз в 250мс
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastModuleUpdate > MODULE_CACHE_TIME) {
            cachedActiveModules = Client.getInstance().getModuleManager().getModules()
                    .stream()
                    .filter(m -> getModuleBindKey(m) != -1 && m != this
                            && !(m instanceof farvix.solution.client.modules.impl.visuals.PlayerRadialMenu)
                            && !(m instanceof farvix.solution.client.modules.impl.environment.DiscordRPC)
                            && !(m instanceof farvix.solution.client.modules.impl.visuals.ContextMenuModule))
                    .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                    .collect(Collectors.toList());
            lastModuleUpdate = currentTime;
        }

        List<Module> active = cachedActiveModules;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();

        // Авто-позиция: правый верхний угол
        if (hudX < 0) {
            hudX = (screenW - FIXED_WIDTH * s - 5) / screenW;
        }

        int moduleCount = active.size();

        // Высота
        float titleH    = 21f; // Уменьшено на 3 пикселя для удаления пустого места сверху
        float rowH      = 13f;
        float spacing   = 1.5f;
        float titleSpacing = 2.0f;
        float contentH  = moduleCount > 0 ? moduleCount * (rowH + spacing) - spacing : 0;
        float targetH   = moduleCount > 0 ? titleH + titleSpacing + contentH + 4f : titleH + 5f;

        currentH = MathHelper.lerp(0.05f, currentH, targetH);

        float x = hudX * screenW;
        float y = hudY * screenH;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(s, s, 1f);

        // ── Единый фон панели ──────────────────────────────────────────────────
        glass.render(ShapeProperties.create(ctx.getMatrices(), 0, 0, FIXED_WIDTH, currentH)
                .round(6f)
                .softness(1.5f)
                .color(TempColor.getGuiBackground().alpha(bgAlpha.getValue()).getRGB())
                .build());

        // Определяем контрастные цвета по фактическому фону
        FixColor bg = TempColor.getGuiBackground();
        float bgBr = (bg.getRed() * 0.299f + bg.getGreen() * 0.587f + bg.getBlue() * 0.114f) / 255f;
        boolean isLight = bgBr > 0.5f;
        int textColor = isLight
                ? new farvix.solution.api.util.color.FixColor(20, 20, 20, 230).getRGB()
                : TempColor.getTextPrimary().alpha(0.9f).getRGB();
        int accentColor = isLight
                ? new farvix.solution.api.util.color.FixColor(0, 80, 180, 255).getRGB()
                : TempColor.getClientColor().getRGB();

        // ── Отрисовка минималистичной иконки клавиатуры 8x8 ─────────────────
        float iconX = 9f;
        float iconY = 7f; // Поднято на 3 пикселя вверх вместе с шапкой (было 10f)

        int iconColor = isLight
                ? new farvix.solution.api.util.color.FixColor(20, 20, 20, 230).getRGB()
                : new farvix.solution.api.util.color.FixColor(255, 255, 255, 230).getRGB();

        // Корпус клавиатуры (8x6) - только рамка толщиной 3.0f с прозрачной заливкой и мягкостью 1.0f
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX, iconY + 1f, 8f, 6f)
                .round(1f)
                .thickness(3f)
                .softness(1f)
                .outlineColor(iconColor)
                .color(0) // прозрачный внутри
                .build());

        // Ряд клавиш 1 (3 штуки) - сплошной белый цвет
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 1.5f, iconY + 2.4f, 1f, 1f).round(0.2f).color(iconColor).build());
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 3.5f, iconY + 2.4f, 1f, 1f).round(0.2f).color(iconColor).build());
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 5.5f, iconY + 2.4f, 1f, 1f).round(0.2f).color(iconColor).build());

        // Ряд клавиш 2 (пробел) - сплошной белый цвет
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 2f, iconY + 4.2f, 4f, 1f).round(0.2f).color(iconColor).build());

        // Название модуля без градиента (поднято на 3 пикселя вверх, теперь Y = 10f)
        Fonts.SEMIBOLD.get(12).drawString(ctx.getMatrices(), "HotKeys",
                22f, 10f, textColor);

        // ── Контент ───────────────────────────────────────────────────────────
        float currentY = titleH + titleSpacing;
        for (Module m : active) {
            // Анимация состояния (вкл/выкл)
            String keyNameStr = m.getName();
            boolean activeState = isModuleActive(m);
            float anim = moduleAnimations.getOrDefault(keyNameStr, activeState ? 1f : 0f);
            float target = activeState ? 1f : 0f;
            anim = MathHelper.lerp(0.1f, anim, target);
            moduleAnimations.put(keyNameStr, anim);

            int key = getModuleBindKey(m);
            String keyName = key != -1 ? getKeyName(key) : "";
            drawModuleRow(ctx.getMatrices(), currentY, rowH, m.getName(), keyName, anim, accentColor, textColor, isLight);
            currentY += rowH + spacing;
        }

        ctx.getMatrices().pop();
        handleDrag(FIXED_WIDTH * s, currentH * s);
    }

    private void drawModuleRow(net.minecraft.client.util.math.MatrixStack matrix,
                                float y, float rowH,
                                String name, String keyName, float anim,
                                int accentColor, int textColor, boolean isLight) {
        // Название модуля: плавная прозрачность
        float textAlpha = MathHelper.lerp(anim, 0.4f, 0.9f);
        int nameColor = TempColor.getTextPrimary().alpha(textAlpha).getRGB();
        Fonts.SEMIBOLD.get(11).drawString(matrix, name, 8f, y + 2f, nameColor);

        // Бинд справа - отображается чистым текстом без рамок и заливок
        if (!keyName.isEmpty()) {
            float keyW = Fonts.DEFAULT.get(10).getStringWidth(keyName);
            float textX = FIXED_WIDTH - 8f - keyW;

            // Цвета для клавиши: неактивный цвет (затененный серый/белый) перетекает в активный textColor (черный в светлой теме, белый в темной)
            int cDisable = isLight
                    ? new farvix.solution.api.util.color.FixColor(20, 20, 20, 115).getRGB() // ~45% непрозрачности черный
                    : new farvix.solution.api.util.color.FixColor(230, 230, 230, 115).getRGB(); // ~45% непрозрачности белый
            int cEnable = textColor; // Активный цвет темы (черный/белый)

            int finalKeyColor = farvix.solution.api.util.color.FixColor.interpolateColor(cDisable, cEnable, anim);

            // Отрисовка символа клавиши
            Fonts.DEFAULT.get(10).drawString(matrix, keyName, textX, y + 2.5f, finalKeyColor);
        }
    }

    // ── Получение имени клавиши — всегда английские буквы ────────────────────
    private String getKeyName(int keyCode) {
        if (keyCode == -1) return "";
        // Буквы A-Z — по GLFW коду, всегда английские
        if (keyCode >= 65 && keyCode <= 90) return String.valueOf((char) keyCode);
        // Цифры 0-9
        if (keyCode >= 48 && keyCode <= 57) return String.valueOf((char) keyCode);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACK";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            default -> "K" + keyCode;
        };
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
        return FIXED_WIDTH * scale.getValue();
    }

    @Override
    public float getHudHeight() {
        return currentH * scale.getValue();
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

    // ── Отслеживание включения модулей ────────────────────────────────────────
    @Override
    public void onEnable() {
        super.onEnable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().register(this);
        // Инициализация времени включения для всех активных модулей
        Client.getInstance().getModuleManager().getModules().forEach(m -> {
            if (m.isEnabled() && m != this) {
                moduleEnableTime.put(m.getName(), System.currentTimeMillis());
            }
        });
    }

    @Override
    public void onDisable() {
        super.onDisable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().unregister(this);
    }

    private int getModuleBindKey(Module m) {
        if (m.getKey() != -1 && m.getKey() != 0) {
            return m.getKey();
        }
        for (farvix.solution.api.settings.Setting s : m.getSettings()) {
            if (s instanceof farvix.solution.api.settings.impl.BindSetting) {
                int key = ((farvix.solution.api.settings.impl.BindSetting) s).getKey();
                if (key != -1 && key != 0) {
                    return key;
                }
            }
        }
        return -1;
    }

    private boolean isModuleActive(Module m) {
        if (m instanceof farvix.solution.client.modules.impl.visuals.FreeLook) {
            return farvix.solution.client.modules.impl.visuals.FreeLook.active;
        }
        if (m instanceof farvix.solution.client.modules.impl.visuals.NoChat) {
            return ((farvix.solution.client.modules.impl.visuals.NoChat) m).isChatHidden();
        }
        if (m instanceof farvix.solution.client.modules.impl.visuals.Zoom) {
            return farvix.solution.client.modules.impl.visuals.Zoom.isZoomKeyHeld((farvix.solution.client.modules.impl.visuals.Zoom) m);
        }
        return m.isEnabled();
    }
}
