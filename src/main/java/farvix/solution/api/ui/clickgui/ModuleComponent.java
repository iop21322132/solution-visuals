package farvix.solution.api.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.client.modules.impl.environment.ClickUI;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.*;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.ui.clickgui.impl.settings.*;
import farvix.solution.api.ui.clickgui.impl.settings.ItemListSettingComponent;
import farvix.solution.api.ui.clickgui.impl.settings.ColorPreviewComponent;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.managers.ThemeManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModuleComponent implements QuickImports {

    private final Module module;

    private float x, y, width, height;
    private boolean hovered = false;
    private boolean binding = false;

    private final List<SettingComponent> settings = new ArrayList<>();

    private final Animation toggleAnimation = new Animation(Easing.EASE_IN_OUT_QUINT, 750);
    private final Animation hoverAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 200);
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 250);
    private boolean expanded = false;

    // Чтобы при открытии ClickGUI не проигрывались анимации "включения/навода"
    // (даже если состояние уже было: мод включён / expanded уже true)
    private boolean justOpened = true;

    // Persist expanded state across GUI open/close
    private static final java.util.Map<String, Boolean> expandedState = new java.util.HashMap<>();

    public ModuleComponent(Module module) {
        this.module = module;
        this.justOpened = true;

        // Restore expanded state — по умолчанию всегда свёрнуто
        this.expanded = expandedState.getOrDefault(module.getName(), false);

        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                settings.add(new BooleanSettingComponent(setting, this));
            } else if (setting instanceof SliderSetting) {
                settings.add(new SliderSettingComponent(setting, this));
            } else if (setting instanceof ModeSetting) {
                settings.add(new ModeSettingComponent(setting, this));
            } else if (setting instanceof MultiModeSetting) {
                settings.add(new MultiModeSettingComponent(setting, this));
            }

            else if (setting instanceof BindSetting) {
                settings.add(new BindSettingComponent(setting, this));
            } else if (setting instanceof StringSetting) {
                settings.add(new StringSettingComponent(setting, this));
            } else if (setting instanceof ItemListSetting) {
                settings.add(new ItemListSettingComponent((ItemListSetting) setting, this));
            } else if (setting instanceof ColorSetting) {
                settings.add(new ColorSettingComponent((ColorSetting) setting, this));
            }
        }

        // ── Auto-detect RGB slider groups and insert color preview ────────────
        // Look for consecutive SliderSettings whose names end with .r / .g / .b
        // Optionally followed by .alpha for RGBA preview
        List<SettingComponent> withPreviews = new ArrayList<>();
        for (int i = 0; i < settings.size(); i++) {
            withPreviews.add(settings.get(i));
            // Check if this is the .b of a .r/.g/.b triplet
            if (i >= 2) {
                SettingComponent sc = settings.get(i);
                SettingComponent sg = settings.get(i - 1);
                SettingComponent sr = settings.get(i - 2);
                if (sc.getSetting() instanceof SliderSetting
                        && sg.getSetting() instanceof SliderSetting
                        && sr.getSetting() instanceof SliderSetting) {
                    String nb = sc.getSetting().getName();
                    String ng = sg.getSetting().getName();
                    String nr = sr.getSetting().getName();
                    if (nb.endsWith(".b") && ng.endsWith(".g") && nr.endsWith(".r")) {
                        // Check if next setting is .alpha
                        SliderSetting alphaSetting = null;
                        if (i + 1 < settings.size()) {
                            SettingComponent next = settings.get(i + 1);
                            if (next.getSetting() instanceof SliderSetting
                                    && next.getSetting().getName().endsWith(".alpha")) {
                                alphaSetting = (SliderSetting) next.getSetting();
                            }
                        }
                        DummySetting dummy = new DummySetting(nr.replace(".r", ".preview"), null);
                        withPreviews.add(new ColorPreviewComponent(
                                (SliderSetting) sr.getSetting(),
                                (SliderSetting) sg.getSetting(),
                                (SliderSetting) sc.getSetting(),
                                alphaSetting,
                                dummy, this));
                    }
                }
            }
        }
        settings.clear();
        settings.addAll(withPreviews);

        // ── Crosshair preview — вставляем первым если это модуль Crosshair ───
        if (module instanceof farvix.solution.client.modules.impl.visuals.Crosshair crosshairModule) {
            settings.add(0, new farvix.solution.api.ui.clickgui.impl.settings.CrosshairPreviewComponent(
                    crosshairModule, this));
        }
    }

    public void init() {
        this.width = 248f; // ширина под 2 колонки
        this.height = 40f;

        for (SettingComponent component : settings) {
            component.init();
        }
    }

    public void setWidth(float width) {
        this.width = width;
        for (SettingComponent component : settings) {
            component.setWidth(width);
        }
    }

    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        // Если модуль не должен отображаться на текущем сервере — отменяем рендер
        if (!ClickUI.shouldShowModule(module))
            return;

        this.x = x;
        this.y = y;

        // Сначала считаем полную высоту компонента
        float totalComponentHeight = getTotalHeight();

        float offset = getSettingsTopOffset();
        float settingsY = y + height + offset;
        float settingsHeight = 0;

        if (!settings.isEmpty()) {
            settingsHeight += offset + 3f;
            java.util.List<SettingComponent> visible = new java.util.ArrayList<>();
            for (SettingComponent setting : settings) {
                if (isSettingVisible(setting)) {
                    visible.add(setting);
                }
            }
            for (int i = 0; i < visible.size(); i++) {
                SettingComponent setting = visible.get(i);
                float extra = 0;
                if (setting instanceof ColorSettingComponent && i + 1 < visible.size()) {
                    SettingComponent next = visible.get(i + 1);
                    if (next instanceof SliderSettingComponent) {
                        extra = 6f; // 2x spacing from color to slider below
                    }
                }
                if (setting instanceof BooleanSettingComponent) {
                    settingsHeight -= 3f;
                    if (i == 0) {
                        settingsHeight += 2.5f;
                    }
                    boolean nextIsBoolean = false;
                    if (i + 1 < visible.size()) {
                        nextIsBoolean = (visible.get(i + 1) instanceof BooleanSettingComponent);
                    }
                    if (!nextIsBoolean) {
                        extra += 2f;
                    }
                }
                settingsHeight += setting.getHeight() + 2 + extra;
            }
        }

        // Проверяем hover для ВСЕГО компонента используя getTotalHeight()
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + totalComponentHeight;

        if (justOpened) {
            toggleAnimation.setValue(module.isEnabled() ? 1 : 0);
            hoverAnimation.setValue(hovered ? 1 : 0);
            expandAnimation.setValue(expanded ? 1f : 0f);
            justOpened = false;
        } else {
            toggleAnimation.run(module.isEnabled() ? 1 : 0);
            hoverAnimation.run(hovered ? 1 : 0);
            expandAnimation.run(expanded ? 1f : 0f);
        }

        float alpha = getClickGUI().getAlpha().getValue();
        float hoverValue = hoverAnimation.getValue();

        // ═══════════════════════════════════════════════════════════════════════
        // УЛУЧШЕННЫЕ ЦВЕТА И ЭФФЕКТЫ ДЛЯ 2-ОЙ ВЕРСИИ CLICKGUI (темный обсидиан)
        // ═══════════════════════════════════════════════════════════════════════
        int bgColor;
        int baseR = TempColor.getModuleBackground().getRed();
        int baseG = TempColor.getModuleBackground().getGreen();
        int baseB = TempColor.getModuleBackground().getBlue();
        
        if (module.isEnabled()) {
            // Смешиваем фон модуля с цветом темы акцента
            int acR = TempColor.getClientColor().getRed();
            int acG = TempColor.getClientColor().getGreen();
            int acB = TempColor.getClientColor().getBlue();
            
            // При черной теме активные кнопки должны становиться белее/светлее обычных
            if (ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("Black")) {
                acR = 180;
                acG = 180;
                acB = 180;
            }
            
            // Если включен, добавляем небольшой оттенок акцента темы (12% акцента, 88% базового фона)
            int r, g, b;
            if (ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("Custom")) {
                // Для кастомной темы делаем кнопку заметно светлее/белее её исходного цвета
                r = (int) (baseR + (255 - baseR) * 0.30f + 12 * hoverValue);
                g = (int) (baseG + (255 - baseG) * 0.30f + 12 * hoverValue);
                b = (int) (baseB + (255 - baseB) * 0.30f + 12 * hoverValue);
            } else {
                r = (int) (baseR * 0.88f + acR * 0.12f + 12 * hoverValue);
                g = (int) (baseG * 0.88f + acG * 0.12f + 12 * hoverValue);
                b = (int) (baseB * 0.88f + acB * 0.12f + 12 * hoverValue);
            }
            bgColor = new FixColor(r, g, b, (int) (211 * alpha)).getRGB();
        } else {
            int r = (int) (baseR + 8 * hoverValue);
            int g = (int) (baseG + 8 * hoverValue);
            int b = (int) (baseB + 8 * hoverValue);
            bgColor = new FixColor(r, g, b, (int) (211 * alpha)).getRGB();
        }

        // ═══════════════════════════════════════════════════════════════════════
        // РИСУЕМ ФОН МОДУЛЯ (скругление 6px)
        // ═══════════════════════════════════════════════════════════════════════
        glass.render(ShapeProperties.create(context.getMatrices(), x, y, width, totalComponentHeight)
                .round(6f)
                .softness(2f)
                .thickness(0.5f)
                .outlineColor(new FixColor(255, 255, 255, (int) (8 * alpha)).getRGB())
                .color(bgColor)
                .build());

        // ── Рисуем заголовок и описание ──
        String title = module.getName();
        String desc = module.getDescription();
        if (desc == null) desc = "";

        // Название модуля
        float nameX = x + 10f;
        float nameY;
        if (desc.isEmpty()) {
            // Если описания нет, центрируем название по вертикали
            nameY = y + (height - Fonts.SEMIBOLD.get(18).getStringHeight(title)) / 2f + 4f;
        } else {
            nameY = y + 10f;
        }

        int titleColor = module.isEnabled()
                ? new FixColor(255, 255, 255, (int) (240 * alpha)).getRGB()
                : new FixColor(165, 165, 165, (int) (190 * alpha)).getRGB();

        // Рисуем название
        Fonts.SEMIBOLD.get(18).drawString(context.getMatrices(), title, nameX, nameY, titleColor);

        // Рисуем описание (если оно есть)
        if (!desc.isEmpty()) {
            float descX = x + 10f;
            float descY = y + 22f;
            int descColor = module.isEnabled()
                    ? new FixColor(175, 175, 175, (int) (200 * alpha)).getRGB()
                    : new FixColor(115, 115, 115, (int) (160 * alpha)).getRGB();
            
            // Если описание слишком длинное, обрезаем
            float maxDescW = width - 35f; // оставляем место под шестеренку
            String displayDesc = desc;
            if (Fonts.SEMIBOLD.get(14).getStringWidth(displayDesc) > maxDescW) {
                while (displayDesc.length() > 0 && Fonts.SEMIBOLD.get(14).getStringWidth(displayDesc + "...") > maxDescW) {
                    displayDesc = displayDesc.substring(0, displayDesc.length() - 1);
                }
                displayDesc = displayDesc + "...";
            }
            Fonts.SEMIBOLD.get(14).drawBoldString(context.getMatrices(), displayDesc, descX, descY, descColor);
        }



        // ── Рендеринг настроек ──
        if (!settings.isEmpty()) {
            float expandValue = expandAnimation.getValue();

            if (expandValue > 0f && settingsHeight > 0) {
                float visibleHeight = settingsHeight * expandValue;
                float blurY = y + height;

                context.enableScissor((int) x, (int) blurY, (int) (x + width), (int) (blurY + visibleHeight));

                float currentSettingsY = settingsY;



                java.util.List<SettingComponent> visible = new java.util.ArrayList<>();
                for (SettingComponent setting : settings) {
                    if (isSettingVisible(setting)) {
                        visible.add(setting);
                    }
                }

                for (int i = 0; i < visible.size(); i++) {
                    SettingComponent setting = visible.get(i);
                    float extra = 0;
                    if (setting instanceof ColorSettingComponent && i + 1 < visible.size()) {
                        SettingComponent next = visible.get(i + 1);
                        if (next instanceof SliderSettingComponent) {
                            extra = 6f;
                        }
                    }
                    if (setting instanceof BooleanSettingComponent) {
                        currentSettingsY -= 3f;
                        if (i == 0) {
                            currentSettingsY += 2.5f;
                        }
                        boolean nextIsBoolean = false;
                        if (i + 1 < visible.size()) {
                            nextIsBoolean = (visible.get(i + 1) instanceof BooleanSettingComponent);
                        }
                        if (!nextIsBoolean) {
                            extra += 2f;
                        }
                    }
                    setting.render(context, x, currentSettingsY, mouseX, mouseY, delta);
                    currentSettingsY += setting.getHeight() + 2 + extra;
                }

                context.disableScissor();
            }
        }

    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        // Блокируем клики по скрытым модулям
        if (!ClickUI.shouldShowModule(module))
            return;

        // Клик по зоне настроек — только если они реально раскрыты
        boolean clickedOnSettings = false;
        if (!settings.isEmpty() && expanded && expandAnimation.getValue() > 0.01f) {
            float settingsStartY = y + height + getSettingsTopOffset();
            if (mouseX >= x && mouseX <= x + width
                    && mouseY >= settingsStartY && mouseY <= y + getTotalHeight()) {
                clickedOnSettings = true;
            }
        }

        // Клик по заголовку (только верхняя полоска height=28)
        boolean clickedOnHeader = mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;

        if (clickedOnHeader && !clickedOnSettings) {
            switch (button) {
                case 0 -> module.toggle(); // ЛКМ - включить/выключить
                case 1 -> {
                    // ПКМ - раскрываем/скрываем настройки
                    if (!settings.isEmpty()) {
                        expanded = !expanded;
                        expandedState.put(module.getName(), expanded);
                    }
                }
                case 2 -> binding = true; // СКМ - биндинг
            }
        }

        // Передаём клики в настройки всегда
        if (!settings.isEmpty()) {
            for (SettingComponent setting : settings) {
                if (!isSettingVisible(setting)) {
                    continue;
                }
                setting.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == 256 || keyCode == 261) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return; // don't propagate to settings while binding
        }

        // Propagate to all settings (e.g. ItemListSettingComponent needs Backspace)
        if (!settings.isEmpty()) {
            for (SettingComponent setting : settings) {
                if (!isSettingVisible(setting)) {
                    continue;
                }
                setting.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {

        if (!settings.isEmpty()) {
            for (SettingComponent setting : settings) {
                if (!isSettingVisible(setting)) {
                    continue;
                }
                setting.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void charTyped(char codePoint, int modifiers) {

        if (!settings.isEmpty()) {
            for (SettingComponent setting : settings) {
                if (!isSettingVisible(setting)) {
                    continue;
                }
                setting.charTyped(codePoint, modifiers);
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {

        if (!settings.isEmpty()) {
            for (SettingComponent setting : settings) {
                if (!isSettingVisible(setting)) {
                    continue;
                }
                if (setting instanceof farvix.solution.api.ui.clickgui.impl.settings.SliderSettingComponent) {
                    ((farvix.solution.api.ui.clickgui.impl.settings.SliderSettingComponent) setting)
                            .mouseDragged(mouseX, mouseY, button);
                } else if (setting instanceof farvix.solution.api.ui.clickgui.impl.settings.ColorSettingComponent) {
                    ((farvix.solution.api.ui.clickgui.impl.settings.ColorSettingComponent) setting).mouseDragged(mouseX,
                            mouseY, button);
                }
            }
        }
    }

    public boolean isSettingVisible(SettingComponent sc) {
        if (sc == null || sc.getSetting() == null) {
            return true;
        }

        // First check standard hide condition
        if (sc.getSetting().getHideCondition() != null && !sc.getSetting().getHideCondition().get()) {
            return false;
        }

        // If simplified mode is active (extendedMode == false)
        if (!farvix.solution.api.ui.clickgui.impl.sidebar.Sidebar.extendedMode) {
            String modName = module.getName();
            String setStr = sc.getSetting().getName();

            // 1) Armor HUD
            if (modName.equalsIgnoreCase("Armor HUD")) {
                if (setStr.equals("Ориентация")) {
                    return true;
                }
                if (sc.getSetting() instanceof SliderSetting slider) {
                    if (setStr.equals("Размер")) {
                        slider.setCurrentValue(0.5f);
                    } else if (setStr.equals("Прозрачность фона")) {
                        slider.setCurrentValue(1.0f);
                    }
                } else if (sc.getSetting() instanceof ModeSetting mode) {
                    if (setStr.equals("Прочность")) {
                        mode.setCurrentMode("Число");
                    }
                }
                return false;
            }

            // 2) FreeLook
            if (modName.equalsIgnoreCase("FreeLook")) {
                if (setStr.equals("Кнопка")) {
                    return true;
                }
                if (sc.getSetting() instanceof BooleanSetting bool) {
                    if (setStr.equals("Инверт X") || setStr.equals("Инверт Y")) {
                        bool.setValue(false);
                    }
                } else if (sc.getSetting() instanceof SliderSetting slider) {
                    if (setStr.equals("Дальность")) {
                        slider.setCurrentValue(4.5f);
                    } else if (setStr.equals("Скорость вращения")) {
                        slider.setCurrentValue(1.0f);
                    }
                }
                return false;
            }

            // 3) Hit Bubbles
            if (modName.equalsIgnoreCase("Hit Bubbles")) {
                if (setStr.equals("Цвет")) {
                    return true;
                }
                if (sc.getSetting() instanceof SliderSetting slider) {
                    if (setStr.equals("Размер")) {
                        slider.setCurrentValue(0.5f);
                    } else if (setStr.equals("Время жизни (сек)")) {
                        slider.setCurrentValue(0.5f);
                    }
                }
                return false;
            }

            // 4) Hit Effect
            if (modName.equalsIgnoreCase("Hit Effect")) {
                if (setStr.equals("Цель") || setStr.equals("Цвет")) {
                    return true;
                }
                if (sc.getSetting() instanceof SliderSetting slider) {
                    if (setStr.equals("Радиус")) {
                        slider.setCurrentValue(24.0f);
                    } else if (setStr.equals("Скорость")) {
                        slider.setCurrentValue(3.0f);
                    } else if (setStr.equals("Ширина волны")) {
                        slider.setCurrentValue(6.0f);
                    } else if (setStr.equals("Интенсивность свечения")) {
                        slider.setCurrentValue(0.9f);
                    } else if (setStr.equals("Толщина линий")) {
                        slider.setCurrentValue(2.5f);
                    }
                }
                return false;
            }

            // 5) Custom Hitbox
            if (modName.equalsIgnoreCase("Custom Hitbox")) {
                if (setStr.equals("Цвет") || setStr.equals("Заливка (Ниже настройка)") || setStr.equals("Цвет заливки 1")) {
                    return true;
                }
                return false;
            }

            // 6) Hit Particles
            if (modName.equalsIgnoreCase("Hit Particles")) {
                if (setStr.equals("hitparticles.target") || setStr.equals("hitparticles.effect") || setStr.equals("hitparticles.color") || setStr.equals("hitparticles.count")) {
                    return true;
                }
                return false;
            }

            // 7) HoldMyItems
            if (modName.equalsIgnoreCase("HoldMyItems")) {
                if (setStr.equals("Редактировать") || setStr.endsWith("(Правая)") || setStr.endsWith("(Левая)")) {
                    return true;
                }
                if (sc.getSetting() instanceof SliderSetting slider) {
                    if (setStr.equals("holdmyitems.swingSpeed")) {
                        slider.setCurrentValue(12.0f);
                    }
                } else if (sc.getSetting() instanceof BooleanSetting bool) {
                    if (setStr.equals("holdmyitems.swimmingAnimation") || setStr.equals("holdmyitems.climbAndCrawl")) {
                        bool.setValue(true);
                    }
                }
                return false;
            }

            // 8) Kill Effect
            if (modName.equalsIgnoreCase("Kill Effect")) {
                if (setStr.equals("Эффекты") || setStr.equals("Цель") || setStr.equals("Цвет") || setStr.equals("Гравитация")) {
                    return true;
                }
                return false;
            }

            // 9) World Particles
            if (modName.equalsIgnoreCase("World Particles")) {
                if (setStr.equals("worldparticles.type") || setStr.equals("worldparticles.color")) {
                    return true;
                }
                return false;
            }
        }

        return true;
    }

    public boolean isBinding() {
        return binding;
    }

    /** Возвращает true только если курсор на заголовке модуля (не на настройках) */
    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private float getSettingsTopOffset() {
        SettingComponent firstVisible = null;
        for (SettingComponent setting : settings) {
            if (isSettingVisible(setting)) {
                firstVisible = setting;
                break;
            }
        }
        if (firstVisible instanceof BooleanSettingComponent) {
            return -4.5f;
        }
        return 4f;
    }

    public float getTotalHeight() {
        // Модуль со "скрытым" статусом занимает 0 пикселей по высоте
        if (!ClickUI.shouldShowModule(module))
            return 0;

        float totalHeight = height;
        if (!settings.isEmpty()) {
            float offset = getSettingsTopOffset();
            float settingsHeight = offset + 3f;
            java.util.List<SettingComponent> visible = new java.util.ArrayList<>();
            for (SettingComponent setting : settings) {
                if (isSettingVisible(setting)) {
                    visible.add(setting);
                }
            }
            for (int i = 0; i < visible.size(); i++) {
                SettingComponent setting = visible.get(i);
                float extra = 0;
                if (setting instanceof ColorSettingComponent && i + 1 < visible.size()) {
                    SettingComponent next = visible.get(i + 1);
                    if (next instanceof SliderSettingComponent) {
                        extra = 6f;
                    }
                }
                if (setting instanceof BooleanSettingComponent) {
                    settingsHeight -= 3f;
                    if (i == 0) {
                        settingsHeight += 2.5f;
                    }
                    boolean nextIsBoolean = false;
                    if (i + 1 < visible.size()) {
                        nextIsBoolean = (visible.get(i + 1) instanceof BooleanSettingComponent);
                    }
                    if (!nextIsBoolean) {
                        extra += 2f;
                    }
                }
                settingsHeight += setting.getHeight() + 2 + extra;
            }
            totalHeight += settingsHeight * expandAnimation.getValue();
        }
        return totalHeight;
    }

    /** Полная высота при полностью раскрытых настройках (для расчёта скролла) */
    public float getTotalHeightExpanded() {
        if (!ClickUI.shouldShowModule(module))
            return 0;

        float totalHeight = height;
        if (!settings.isEmpty() && expanded) {
            float offset = getSettingsTopOffset();
            float settingsHeight = offset + 3f;
            java.util.List<SettingComponent> visible = new java.util.ArrayList<>();
            for (SettingComponent setting : settings) {
                if (isSettingVisible(setting)) {
                    visible.add(setting);
                }
            }
            for (int i = 0; i < visible.size(); i++) {
                SettingComponent setting = visible.get(i);
                float extra = 0;
                if (setting instanceof ColorSettingComponent && i + 1 < visible.size()) {
                    SettingComponent next = visible.get(i + 1);
                    if (next instanceof SliderSettingComponent) {
                        extra = 6f;
                    }
                }
                if (setting instanceof BooleanSettingComponent) {
                    settingsHeight -= 3f;
                    if (i == 0) {
                        settingsHeight += 2.5f;
                    }
                    boolean nextIsBoolean = false;
                    if (i + 1 < visible.size()) {
                        nextIsBoolean = (visible.get(i + 1) instanceof BooleanSettingComponent);
                    }
                    if (!nextIsBoolean) {
                        extra += 2f;
                    }
                }
                settingsHeight += setting.getHeight() + 2 + extra;
            }
            totalHeight += settingsHeight;
        }
        return totalHeight;
    }

    private boolean hasBooleanSettings() {
        for (SettingComponent setting : settings) {
            if (setting instanceof BooleanSettingComponent) {
                if (isSettingVisible(setting)) {
                    return true;
                }
            }
        }
        return false;
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == -1)
            return "NONE";

        switch (keyCode) {
            case 32:
                return "SPACE";
            case 257:
                return "ENTER";
            case 258:
                return "TAB";
            case 259:
                return "BACKSPACE";
            case 260:
                return "INSERT";
            case 261:
                return "DELETE";
            case 262:
                return "RIGHT";
            case 263:
                return "LEFT";
            case 264:
                return "DOWN";
            case 265:
                return "UP";
            case 266:
                return "PAGE_UP";
            case 267:
                return "PAGE_DOWN";
            case 268:
                return "HOME";
            case 269:
                return "END";
            case 280:
                return "CAPS_LOCK";
            case 281:
                return "SCROLL_LOCK";
            case 282:
                return "NUM_LOCK";
            case 283:
                return "PRINT_SCREEN";
            case 284:
                return "PAUSE";
            case 290:
                return "F1";
            case 291:
                return "F2";
            case 292:
                return "F3";
            case 293:
                return "F4";
            case 294:
                return "F5";
            case 295:
                return "F6";
            case 296:
                return "F7";
            case 297:
                return "F8";
            case 298:
                return "F9";
            case 299:
                return "F10";
            case 300:
                return "F11";
            case 301:
                return "F12";
            case 340:
                return "LEFT_SHIFT";
            case 341:
                return "LEFT_CONTROL";
            case 342:
                return "LEFT_ALT";
            case 343:
                return "LEFT_SUPER";
            case 344:
                return "RIGHT_SHIFT";
            case 345:
                return "RIGHT_CONTROL";
            case 346:
                return "RIGHT_ALT";
            case 347:
                return "RIGHT_SUPER";
            case 348:
                return "MENU";
            default: {
                if (keyCode >= 65 && keyCode <= 90) {
                    return String.valueOf((char) keyCode);
                } else if (keyCode >= 48 && keyCode <= 57) {
                    return String.valueOf((char) keyCode);
                } else {
                    return "KEY_" + keyCode;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // УТИЛИТЫ ДЛЯ РИСОВАНИЯ (как в reference mod)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Рисует градиентный прямоугольник (сверху вниз)
     */
    private void drawGradientBox(DrawContext context, float x, float y, float width, float height,
            int colorTop, int colorBottom) {
        // Используем blur с градиентом через два вызова
        blur.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(new Vector4f(8, 8, 0, 0)) // Скругление только сверху
                .softness(0)
                .thickness(0)
                .outlineColor(0)
                .color(colorTop)
                .build());
    }

    /**
     * Рисует тонкую рамку по краям прямоугольника
     */
    private void drawThinFrame(DrawContext context, float x, float y, float width, float height, int color) {
        float thickness = 0.5f;

        // Верх
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, thickness)
                .round(0)
                .color(color)
                .build());

        // Низ
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y + height - thickness, width, thickness)
                .round(0)
                .color(color)
                .build());

        // Лево
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, thickness, height)
                .round(0)
                .color(color)
                .build());

        // Право
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + width - thickness, y, thickness, height)
                .round(0)
                .color(color)
                .build());
    }

}
