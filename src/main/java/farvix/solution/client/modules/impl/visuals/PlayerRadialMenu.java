package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.util.FriendManager;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Player Menu", category = ModuleCategory.VISUALS,
        description = "Меню при наведении на игрока")
public class PlayerRadialMenu extends Module implements QuickImports {

    private final Animation openAnim = new Animation(Easing.EASE_OUT_CUBIC, 250);

    private boolean menuOpen  = false;
    private boolean wasMiddle = false;
    private PlayerEntity target = null;

    // Мировые координаты точки где открылось меню
    private double worldX, worldY, worldZ;

    private float menuX, menuY;
    private float cursorX, cursorY;
    // Позиция мыши в момент открытия — для вычисления дельты
    private float openMouseX, openMouseY;

    // ── Публичные методы (для MouseMixin — больше не используются, но оставим) ─
    public boolean isMenuOpen() { return menuOpen; }
    public boolean hasAimedPlayer() {
        return getAimedPlayer() != null;
    }

    // Эти методы теперь не вызываются из MouseMixin — логика внутри onRender2D
    public void onMiddlePress() {}
    public void onMiddleRelease() {}

    // ── Пункты меню ───────────────────────────────────────────────────────────
    private static class MenuItem {
        String label;
        float angle;
        float dist;
        boolean hovered;
        MenuItem(String label, float angle, float dist) {
            this.label = label; this.angle = angle; this.dist = dist;
        }
    }

    private final List<MenuItem> items = new ArrayList<>();

    private static final float MENU_RADIUS = 55f;
    private static final float RING_THICK  = 3f;

    @Override
    public void onEnable() {
        super.onEnable();
        buildItems();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        menuOpen = false;
        wasMiddle = false;
    }

    private void buildItems() {
        items.clear();
        items.add(new MenuItem("Добавить в друзья", (float)(-Math.PI / 2), MENU_RADIUS));
        items.add(new MenuItem("Удалить из друзей", (float)( Math.PI / 2), MENU_RADIUS));
    }

    // ── Главный рендер-цикл ───────────────────────────────────────────────────
    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.getWindow() == null) return;

        net.minecraft.client.util.Window win = mc.getWindow();
        boolean middleNow = GLFW.glfwGetMouseButton(win.getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        // Лог каждые 20 кадров чтобы не спамить — убран

        // ── Нажали СКМ ────────────────────────────────────────────────────────
        if (middleNow && !wasMiddle) {
            PlayerEntity aimed = getAimedPlayer();
            if (aimed != null) {
                target = aimed;
                menuOpen = true;
                updateItemLabels();
                // Сохраняем мировые координаты игрока в момент открытия
                worldX = aimed.getX();
                worldY = aimed.getY() + aimed.getHeight() / 2.0;
                worldZ = aimed.getZ();
                // Начальная экранная позиция
                Vec3d sp = projectToScreen(worldX, worldY, worldZ);
                if (sp != null && sp.z > 0 && sp.z < 1) {
                    menuX = (float) sp.x;
                    menuY = (float) sp.y;
                } else {
                    menuX = win.getScaledWidth()  / 2f;
                    menuY = win.getScaledHeight() / 2f;
                }
                cursorX = menuX;
                cursorY = menuY + 8f; // начальная позиция под ником
                // Запоминаем позицию мыши при открытии
                openMouseX = (float)(mc.mouse.getX() / win.getScaleFactor());
                openMouseY = (float)(mc.mouse.getY() / win.getScaleFactor());
            }
        }

        // ── Отпустили СКМ ─────────────────────────────────────────────────────
        if (!middleNow && wasMiddle && menuOpen) {
            checkHover();
            handleClick();
            menuOpen = false;
        }

        wasMiddle = middleNow;

        // ── Каждый кадр пересчитываем экранную позицию из мировых координат ──
        if (menuOpen) {
            Vec3d sp = projectToScreen(worldX, worldY, worldZ);
            if (sp != null && sp.z > 0 && sp.z < 1) {
                menuX = (float) sp.x;
                menuY = (float) sp.y;
            }
        }

        // ── Обновляем курсор — следует за дельтой мыши от точки открытия ─────
        if (menuOpen) {
            float mx = (float)(mc.mouse.getX() / win.getScaleFactor());
            float my = (float)(mc.mouse.getY() / win.getScaleFactor());
            // Курсор = начальная позиция (под ником) + дельта движения мыши
            cursorX = menuX + (mx - openMouseX);
            cursorY = menuY + 8f + (my - openMouseY);
        }

        // ── Анимация ──────────────────────────────────────────────────────────
        openAnim.run(menuOpen ? 1f : 0f);
        float anim = openAnim.getValue();
        if (anim <= 0.01f) return;

        var ms = e.getContext().getMatrices();
        renderMenu(ms, anim);
        if (menuOpen) {
            checkHover();
            renderCursor(ms, anim);
        }
    }

    // ── Рендер ────────────────────────────────────────────────────────────────

    private void renderMenu(MatrixStack ms, float anim) {
        float ease = 1f - (float) Math.pow(1 - anim, 3);

        // Setup glass framebuffer before rendering
        glass.setup();

        // Получаем акцентный цвет темы
        java.awt.Color accentColor = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();

        drawRing(ms, menuX, menuY, MENU_RADIUS * ease,
                new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(60 * anim)).getRGB(), RING_THICK);

        glass.render(ShapeProperties.create(ms,
                menuX - MENU_RADIUS * ease, menuY - MENU_RADIUS * ease,
                MENU_RADIUS * 2 * ease, MENU_RADIUS * 2 * ease)
                .round(MENU_RADIUS * ease).softness(18f)
                .color(new FixColor(12, 14, 20, (int)(120 * anim)).getRGB())
                .build());

        for (MenuItem item : items) {
            float ix = menuX + (float)(Math.cos(item.angle) * item.dist * ease);
            float iy = menuY + (float)(Math.sin(item.angle) * item.dist * ease);

            // Получаем цвет фона темы
            java.awt.Color themeBg = farvix.solution.client.managers.ThemeManager.getInstance()
                    .getCurrentTheme().getBackgroundColor();
            
            int bg = item.hovered
                    ? new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(180 * anim)).getRGB()
                    : new FixColor(themeBg.getRed(), themeBg.getGreen(), themeBg.getBlue(), (int)(150 * anim)).getRGB();
            int border = item.hovered
                    ? new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(200 * anim)).getRGB()
                    : new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(120 * anim)).getRGB();

            float btnW = 70f, btnH = 26f;
            glass.render(ShapeProperties.create(ms,
                    ix - btnW / 2f, iy - btnH / 2f, btnW, btnH)
                    .round(btnH / 2f).softness(15f).thickness(1.8f)
                    .outlineColor(border).color(bg).build());

            boolean isAdd = item.label.startsWith("Добавить") || item.label.startsWith("Уже");
            String label = (isAdd ? "+ " : "− ") + item.label;
            
            // Инвертированный цвет текста для максимальной контрастности
            int textColor;
            if (item.hovered) {
                // При наведении - инвертируем акцентный цвет
                textColor = new FixColor(
                    255 - accentColor.getRed(),
                    255 - accentColor.getGreen(),
                    255 - accentColor.getBlue(),
                    (int)(255 * anim)
                ).getRGB();
            } else {
                // Для неактивных кнопок - инвертируем цвет фона темы (используем уже объявленную переменную)
                textColor = new FixColor(
                    255 - themeBg.getRed(),
                    255 - themeBg.getGreen(),
                    255 - themeBg.getBlue(),
                    (int)(220 * anim)
                ).getRGB();
            }
            
            float tw = Fonts.SEMIBOLD.get(10).getStringWidth(label);
            Fonts.SEMIBOLD.get(10).drawString(ms, label, ix - tw / 2f, iy - 4f, textColor);
        }

        if (target != null) { 
            String name = target.getName().getString();
            boolean isFriend = FriendManager.isFriend(name);
            int nc = isFriend
                    ? new FixColor(80, 220, 100, (int)(255 * anim)).getRGB()
                    : new FixColor(255, 255, 255, (int)(255 * anim)).getRGB();
            float nw = Fonts.SEMIBOLD.get(12).getStringWidth(name);
            Fonts.SEMIBOLD.get(12).drawString(ms, name, menuX - nw / 2f, menuY - 5f, nc);
        }
    }

    private void renderCursor(MatrixStack ms, float anim) {
        // Используем акцентный цвет темы для курсора
        java.awt.Color accentColor = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        
        drawRing(ms, cursorX, cursorY, 5f,
                new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(200 * anim)).getRGB(), 1.5f);
        blur.render(ShapeProperties.create(ms, cursorX - 2, cursorY - 2, 4, 4)
                .round(2f).softness(0.5f)
                .color(new FixColor(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int)(255 * anim)).getRGB())
                .build());
    }

    private void checkHover() {
        float btnW = 70f, btnH = 26f;
        float crosshairX = 0f;
        float crosshairY = 0f;
        if (mc.getWindow() != null) {
            crosshairX = mc.getWindow().getScaledWidth() / 2f;
            crosshairY = mc.getWindow().getScaledHeight() / 2f;
        }
        for (MenuItem item : items) {
            float ix = menuX + (float)(Math.cos(item.angle) * item.dist);
            float iy = menuY + (float)(Math.sin(item.angle) * item.dist);
            
            boolean hoveredByCursor = Math.abs(cursorX - ix) < btnW / 2f
                                   && Math.abs(cursorY - iy) < btnH / 2f;
                                   
            boolean hoveredByCrosshair = Math.abs(crosshairX - ix) < btnW / 2f
                                      && Math.abs(crosshairY - iy) < btnH / 2f;
                                      
            item.hovered = hoveredByCursor || hoveredByCrosshair;
        }
    }

    private void handleClick() {
        if (target == null || mc.player == null) return;
        String name = target.getName().getString();
        for (MenuItem item : items) {
            if (!item.hovered) continue;
            if (item.label.startsWith("Добавить") || item.label.startsWith("Уже")) {
                FriendManager.addFriend(name);
                mc.player.sendMessage(
                        net.minecraft.text.Text.literal("§a+ Добавлен в друзья: §f" + name), false);
            } else {
                FriendManager.removeFriend(name);
                mc.player.sendMessage(
                        net.minecraft.text.Text.literal("§c- Удалён из друзей: §f" + name), false);
            }
            updateItemLabels();
            break;
        }
    }

    private void updateItemLabels() {
        if (target == null) return;
        boolean isFriend = FriendManager.isFriend(target.getName().getString());
        if (items.size() >= 2) {
            items.get(0).label = isFriend ? "Уже в друзьях"    : "Добавить в друзья";
            items.get(1).label = isFriend ? "Удалить из друзей" : "Не в друзьях";
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlayerEntity getAimedPlayer() {
        if (mc.crosshairTarget == null) return null;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return null;
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return null;
        if (!(ehr.getEntity() instanceof PlayerEntity p)) return null;
        if (p == mc.player) return null;
        return p;
    }

    private Vec3d projectToScreen(PlayerEntity player) {
        try {
            return farvix.solution.api.util.render.ProjectionUtility
                    .worldSpaceToScreenSpace(player.getPos().add(0, player.getHeight() / 2.0, 0));
        } catch (Throwable t) {
            return null;
        }
    }

    private Vec3d projectToScreen(double wx, double wy, double wz) {
        try {
            return farvix.solution.api.util.render.ProjectionUtility
                    .worldSpaceToScreenSpace(new Vec3d(wx, wy, wz));
        } catch (Throwable t) {
            return null;
        }
    }

    private void drawRing(MatrixStack ms, float cx, float cy, float r, int color, float thick) {
        int segments = 48;
        float outerR = r + thick / 2f;
        float innerR = Math.max(0, r - thick / 2f);
        float[] c = rgba(color);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            buf.vertex(m, cx + (float)(Math.cos(a0) * innerR), cy + (float)(Math.sin(a0) * innerR), 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(m, cx + (float)(Math.cos(a0) * outerR), cy + (float)(Math.sin(a0) * outerR), 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(m, cx + (float)(Math.cos(a1) * outerR), cy + (float)(Math.sin(a1) * outerR), 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(m, cx + (float)(Math.cos(a1) * innerR), cy + (float)(Math.sin(a1) * innerR), 0).color(c[0], c[1], c[2], c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private float[] rgba(int argb) {
        return new float[]{
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >>  8) & 0xFF) / 255f,
            ( argb        & 0xFF) / 255f,
            ((argb >> 24) & 0xFF) / 255f
        };
    }
}
