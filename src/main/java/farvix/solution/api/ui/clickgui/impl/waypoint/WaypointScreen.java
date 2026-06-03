package farvix.solution.api.ui.clickgui.impl.waypoint;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.WaypointManager;

import java.util.List;

public class WaypointScreen extends MenuScreen implements QuickImports {

    // ── Поля ввода ────────────────────────────────────────────────────────────
    private String inputX = "", inputY = "", inputZ = "", inputName = "";
    private int focused = -1; // 0=X, 1=Y, 2=Z, 3=Name

    // ── Анимации ──────────────────────────────────────────────────────────────
    private final Animation addHover = new Animation(Easing.EASE_IN_OUT_SINE, 150);

    // ── Размеры полей ─────────────────────────────────────────────────────────
    private static final float FIELD_H   = 18f;
    private static final float FIELD_R   = 5f;
    private static final float ROW_GAP   = 6f;
    private static final float ITEM_H    = 22f;

    @Override
    public void init() {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        farvix.solution.api.ui.clickgui.InterfaceScreen gui = getClickGUI();
        float alpha  = gui.getAlpha().getValue();
        float localAlpha = alpha * 0.9f;
        float guiX   = gui.getX();
        float guiY   = gui.getY();
        float guiW   = gui.getWidth();
        float sbW    = gui.getSidebar().getWidth();

        float sx = guiX + sbW + 12;
        float sy = guiY + 42;
        float cw = guiW - sbW - 20;

        // ── Заголовок удален ──────────────────────────────────────────────────

        // ── Строка X / Y / Z ──────────────────────────────────────────────────
        float coordW = (cw - ROW_GAP * 2) / 3f;

        renderCoordField(context, sx,                       sy, coordW, FIELD_H, "X", inputX, focused == 0, mouseX, mouseY, localAlpha);
        renderCoordField(context, sx + coordW + ROW_GAP,    sy, coordW, FIELD_H, "Y", inputY, focused == 1, mouseX, mouseY, localAlpha);
        renderCoordField(context, sx + (coordW + ROW_GAP)*2,sy, coordW, FIELD_H, "Z", inputZ, focused == 2, mouseX, mouseY, localAlpha);
        sy += FIELD_H + ROW_GAP;

        // ── Поле имени + кнопка Добавить ──────────────────────────────────────
        float nameW  = cw * 0.65f;
        float btnW   = cw - nameW - ROW_GAP;
        float btnX   = sx + nameW + ROW_GAP;

        renderNameField(context, sx, sy, nameW, FIELD_H, inputName, focused == 3, mouseX, mouseY, localAlpha);

        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW
                      && mouseY >= sy   && mouseY <= sy + FIELD_H;
        addHover.run(btnHov ? 1 : 0);

        int btnBg = TempColor.getClientColor().alpha(localAlpha * (0.15f + 0.2f * addHover.getValue())).getRGB();

        rectangle.render(ShapeProperties.create(context.getMatrices(), btnX, sy, btnW, FIELD_H)
                .round(FIELD_R)
                .color(btnBg)
                .build());
        Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), "Добавить",
                btnX + btnW / 2f, sy + FIELD_H / 2f - 3,
                new FixColor(255, 255, 255, (int)(255 * localAlpha)).getRGB());
        sy += FIELD_H + ROW_GAP + 4;

        // ── Разделитель ───────────────────────────────────────────────────────
        rectangle.render(ShapeProperties.create(context.getMatrices(), sx, sy, cw, 0.5f)
                .round(0.5f)
                .color(TempColor.getClientColor().alpha(alpha * 0.5f).getRGB()).build());
        sy += 8;

        // ── Список вейпоинтов ─────────────────────────────────────────────────
        List<WaypointManager.Waypoint> wps = WaypointManager.list();
        if (wps.isEmpty()) {
            Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), "Нет вейпоинтов",
                    sx + cw / 2f, sy + 12,
                    TempColor.getTextSecondary().alpha(alpha * 0.5f).getRGB());
        } else {
            for (int i = 0; i < wps.size(); i++) {
                renderWaypointItem(context, wps.get(i), i, sx, sy, cw, mouseX, mouseY, localAlpha);
                sy += ITEM_H + 4;
            }
        }
    }

    // ── Рендер поля координаты ────────────────────────────────────────────────
    private void renderCoordField(DrawContext ctx, float x, float y, float w, float h,
                                   String label, String value, boolean isFocused,
                                   int mx, int my, float alpha) {
        int bg  = isFocused
                ? TempColor.getModuleBackground().alpha(alpha).getRGB()
                : TempColor.getModuleBackground().alpha(alpha * 0.9f).getRGB();

        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, h)
                .round(FIELD_R).color(bg).build());

        // Метка слева
        float labelW = Fonts.DEFAULT.get(11).getStringWidth(label + ":");
        Fonts.DEFAULT.get(11).drawString(ctx.getMatrices(), label + ":",
                x + 4, y + h / 2f - 2,
                new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB());

        // Значение
        String display = value.isEmpty() && !isFocused ? "0" : value;
        int textColor  = value.isEmpty()
                ? new FixColor(255, 255, 255, (int)(128 * alpha)).getRGB()
                : new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();
        Fonts.DEFAULT.get(12).drawString(ctx.getMatrices(), display,
                x + labelW + 6, y + h / 2f - 2, textColor);

        // Курсор
        if (isFocused && System.currentTimeMillis() % 1000 < 500) {
            float cx = x + labelW + 6 + Fonts.DEFAULT.get(12).getStringWidth(value);
            rectangle.render(ShapeProperties.create(ctx.getMatrices(), cx, y + 3, 1, h - 6)
                    .round(0).color(TempColor.getClientColor().alpha(alpha).getRGB()).build());
        }
    }

    // ── Рендер поля имени ─────────────────────────────────────────────────────
    private void renderNameField(DrawContext ctx, float x, float y, float w, float h,
                                  String value, boolean isFocused,
                                  int mx, int my, float alpha) {
        int bg  = isFocused
                ? TempColor.getModuleBackground().alpha(alpha).getRGB()
                : TempColor.getModuleBackground().alpha(alpha * 0.9f).getRGB();

        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, h)
                .round(FIELD_R).color(bg).build());

        String display = value.isEmpty() && !isFocused ? "Название..." : value;
        int textColor  = value.isEmpty()
                ? new FixColor(255, 255, 255, (int)(128 * alpha)).getRGB()
                : new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();
        Fonts.DEFAULT.get(12).drawString(ctx.getMatrices(), display, x + 6, y + h / 2f - 2, textColor);

        if (isFocused && System.currentTimeMillis() % 1000 < 500) {
            float cx = x + 6 + Fonts.DEFAULT.get(12).getStringWidth(value);
            rectangle.render(ShapeProperties.create(ctx.getMatrices(), cx, y + 3, 1, h - 6)
                    .round(0).color(TempColor.getClientColor().alpha(alpha).getRGB()).build());
        }
    }

    // ── Рендер элемента вейпоинта ─────────────────────────────────────────────
    private void renderWaypointItem(DrawContext ctx, WaypointManager.Waypoint wp, int idx,
                                     float x, float y, float w,
                                     int mx, int my, float alpha) {
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, ITEM_H)
                .round(6)
                .color(TempColor.getModuleBackground().alpha(alpha * 0.7f).getRGB()).build());

        // Имя
        String name = wp.getName();
        Fonts.SEMIBOLD.get(12).drawString(ctx.getMatrices(), name,
                x + 8, y + ITEM_H / 2f - 3,
                TempColor.getTextPrimary().alpha(alpha).getRGB());

        // Координаты
        String coords = String.format("X:%.0f Y:%.0f Z:%.0f", wp.getX(), wp.getY(), wp.getZ());
        float coordsX = x + 8 + Fonts.SEMIBOLD.get(12).getStringWidth(name) + 8;
        Fonts.DEFAULT.get(10).drawString(ctx.getMatrices(), coords,
                coordsX, y + ITEM_H / 2f - 2,
                TempColor.getTextSecondary().alpha(alpha * 0.7f).getRGB());

        // Кнопка удалить
        float delW = 40f;
        float delX = x + w - delW - 4;
        boolean delHov = mx >= delX && mx <= delX + delW && my >= y + 3 && my <= y + ITEM_H - 3;
        int delBg = new FixColor(delHov ? 220 : 170, delHov ? 60 : 45, delHov ? 60 : 45,
                (int)(190 * alpha)).getRGB();
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), delX, y + 3, delW, ITEM_H - 6)
                .round(5).color(delBg).build());
        Fonts.DEFAULT.get(10).drawCenteredString(ctx.getMatrices(), "✕",
                delX + delW / 2f, y + ITEM_H / 2f - 2,
                new FixColor(255, 255, 255, (int)(230 * alpha)).getRGB());
    }

    // ── Ввод ──────────────────────────────────────────────────────────────────
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return;
        farvix.solution.api.ui.clickgui.InterfaceScreen gui = getClickGUI();
        float guiX = gui.getX(), guiY = gui.getY(), guiW = gui.getWidth();
        float sbW  = gui.getSidebar().getWidth();
        float sx   = guiX + sbW + 12;
        float sy   = guiY + 42; // без заголовка
        float cw   = guiW - sbW - 20;
        float coordW = (cw - ROW_GAP * 2) / 3f;

        // Поля X Y Z
        focused = -1;
        if (hitField(mx, my, sx,                        sy, coordW, FIELD_H)) { focused = 0; return; }
        if (hitField(mx, my, sx + coordW + ROW_GAP,     sy, coordW, FIELD_H)) { focused = 1; return; }
        if (hitField(mx, my, sx + (coordW+ROW_GAP)*2,   sy, coordW, FIELD_H)) { focused = 2; return; }
        sy += FIELD_H + ROW_GAP;

        // Поле имени
        float nameW = cw * 0.65f;
        if (hitField(mx, my, sx, sy, nameW, FIELD_H)) { focused = 3; return; }

        // Кнопка Добавить
        float btnX = sx + nameW + ROW_GAP;
        float btnW = cw - nameW - ROW_GAP;
        if (hitField(mx, my, btnX, sy, btnW, FIELD_H)) { tryAdd(); return; }

        // Кнопки удаления — используем ту же логику что в render
        float listY = getListStartY(guiY);
        List<WaypointManager.Waypoint> wps = WaypointManager.list();
        for (int i = 0; i < wps.size(); i++) {
            float iy   = listY + i * (ITEM_H + 4);
            float delW = 40f;
            float delX = sx + cw - delW - 4;
            if (mx >= delX && mx <= delX + delW && my >= iy + 3 && my <= iy + ITEM_H - 3) {
                WaypointManager.remove(i);
                return;
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused < 0) return;
        if (keyCode == 256) { focused = -1; return; } // ESC
        if (keyCode == 257) { tryAdd(); return; }      // Enter
        if (keyCode == 259) { // Backspace
            switch (focused) {
                case 0 -> { if (!inputX.isEmpty()) inputX = inputX.substring(0, inputX.length()-1); }
                case 1 -> { if (!inputY.isEmpty()) inputY = inputY.substring(0, inputY.length()-1); }
                case 2 -> { if (!inputZ.isEmpty()) inputZ = inputZ.substring(0, inputZ.length()-1); }
                case 3 -> { if (!inputName.isEmpty()) inputName = inputName.substring(0, inputName.length()-1); }
            }
        }
    }

    @Override
    public void charTyped(char c, int modifiers) {
        if (focused < 0) return;
        switch (focused) {
            case 0 -> { if (isCoordChar(c) && inputX.length() < 8)   inputX   += c; }
            case 1 -> { if (isCoordChar(c) && inputY.length() < 8)   inputY   += c; }
            case 2 -> { if (isCoordChar(c) && inputZ.length() < 8)   inputZ   += c; }
            case 3 -> { if (inputName.length() < 24)                  inputName += c; }
        }
    }

    /** Возвращает true если любое поле ввода в фокусе */
    public boolean isFocused() {
        return focused >= 0;
    }

    private boolean isCoordChar(char c) {
        return Character.isDigit(c) || c == '-' || c == '.';
    }

    /** Вычисляет Y-координату начала списка вейпоинтов */
    private float getListStartY(float guiY) {
        float sy = guiY + 42;
        sy += FIELD_H + ROW_GAP;
        sy += FIELD_H + ROW_GAP + 4;
        sy += 8;
        return sy;
    }

    private boolean hitField(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void tryAdd() {
        try {
            double x = inputX.isEmpty() ? 0 : Double.parseDouble(inputX);
            double y = inputY.isEmpty() ? 0 : Double.parseDouble(inputY);
            double z = inputZ.isEmpty() ? 0 : Double.parseDouble(inputZ);
            String name = inputName.isEmpty() ? "Waypoint" : inputName;
            WaypointManager.add(name, x, y, z);
            inputX = ""; inputY = ""; inputZ = ""; inputName = "";
            focused = -1;
        } catch (NumberFormatException ignored) {}
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
