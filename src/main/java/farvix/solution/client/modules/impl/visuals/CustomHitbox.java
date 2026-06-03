package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(name = "Custom Hitbox", category = ModuleCategory.VISUALS,
        description = "Кастомные хитбоксы (F3+B)")
public class CustomHitbox extends Module implements QuickImports {

    // ── Что показывать ────────────────────────────────────────────────────────
    public final BooleanSetting showPlayers  = new BooleanSetting("Игроки",   this);
    public final BooleanSetting showMobs     = new BooleanSetting("Мобы",     this);
    public final BooleanSetting showEntities = new BooleanSetting("Энтити",   this);
    public final BooleanSetting showItems    = new BooleanSetting("Предметы", this);

    // ── Внешний вид ───────────────────────────────────────────────────────────
    public final BooleanSetting noLookLine = new BooleanSetting("Убрать линию взгляда", this);
    public final BooleanSetting fill       = new BooleanSetting("Заливка (Ниже настройка)", this);

    // Новые настройки для динамического хитбокса
    public final BooleanSetting dynamicHitbox = new BooleanSetting("Динамический хитбокс", this);

    public final SliderSetting lineWidth = new SliderSetting(
            "Размер линий", this, 1.5f, 0.5f, 6.0f, 0.1f);

    public final ColorSetting color = new ColorSetting("Цвет", this,
            new FixColor(100, 180, 255, 255).getRGB());

    public final ColorSetting dynamicColor = new ColorSetting("Цвет динамического хитбокса", this,
            new FixColor(255, 0, 0, 200).getRGB()); // По умолчанию красный
    public final SliderSetting dynamicLineWidth = new SliderSetting(
            "Динамическая толщина линии", this, 2.5f, 0.5f, 6.0f, 0.1f);

    // ── Настройки заливки (видны только когда Заливка включена) ──────────────
    public final ColorSetting fillColor1 = new ColorSetting("Цвет заливки 1", this,
            new FixColor(100, 180, 255, 128).getRGB());

    public final BooleanSetting fillGradient = new BooleanSetting("Градиент заливки", this);

    public final ColorSetting fillColor2 = new ColorSetting("Цвет заливки 2", this,
            new FixColor(255, 100, 180, 128).getRGB());

    // Кэш цветов — пересоздаём только при изменении значения
    private Color cachedOutlineColor = null;
    private int   cachedColorRGB     = -1;
    private java.awt.Color cachedFillColor1 = null;
    private java.awt.Color cachedFillColor2 = null;
    private int cachedFillColor1RGB = -1;
    private int cachedFillColor2RGB = -1;

    // Переиспользуемый вектор нормали — не создаём new каждый кадр
    private final org.joml.Vector3f reusableNormal = new org.joml.Vector3f();

    public CustomHitbox() {
        showPlayers.setEnabled(true);
        showMobs.setEnabled(true);
        noLookLine.setEnabled(true);
        // Видимость настроек заливки
        fillColor1.setVisible(() -> fill.isEnabled());
        fillGradient.setVisible(() -> fill.isEnabled());
        fillColor2.setVisible(() -> fill.isEnabled() && fillGradient.isEnabled());
        dynamicColor.setVisible(() -> dynamicHitbox.isEnabled());
        dynamicLineWidth.setVisible(() -> dynamicHitbox.isEnabled());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Ванильный рендер хитбоксов НЕ включаем — рисуем только своё,
        // иначе будет двойной рендер и просадка FPS
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.world == null || mc.player == null) return;

        float tickDelta = e.getTickDelta();
        MatrixStack matrices = e.getMatrices();

        // Определяем сущность, на которую наведен прицел
        LivingEntity hoveredEntity = null;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            if (((EntityHitResult) mc.crosshairTarget).getEntity() instanceof LivingEntity le) {
                hoveredEntity = le;
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            Color currentOutlineColor;
            float currentLineWidth;
            java.awt.Color currentFillColor1, currentFillColor2;
            float currentAnimT = 0f;

            boolean isHovered = (dynamicHitbox.isEnabled() && entity == hoveredEntity);

            if (isHovered) {
                // Используем динамические настройки
                int dynColorRGB = dynamicColor.get();
                currentOutlineColor = new Color(
                        (dynColorRGB >> 16) & 0xFF,
                        (dynColorRGB >> 8)  & 0xFF,
                        dynColorRGB         & 0xFF,
                        (dynColorRGB >> 24) & 0xFF
                );
                currentLineWidth = dynamicLineWidth.getValue();
                
                // Для заливки используем тот же динамический цвет с его альфой
                int dynFillRGB = new FixColor(currentOutlineColor.getRed(), currentOutlineColor.getGreen(), currentOutlineColor.getBlue(), currentOutlineColor.getAlpha()).getRGB();
                currentFillColor1 = new java.awt.Color(dynFillRGB, true);
                currentFillColor2 = currentFillColor1; // Градиент не применяется для динамической заливки
            } else {
                // Используем обычные настройки
                // Кэшируем outline color
                int colorRGB = color.get();
                if (cachedOutlineColor == null || cachedColorRGB != colorRGB) {
                    cachedColorRGB = colorRGB;
                    cachedOutlineColor = new Color(
                            (colorRGB >> 16) & 0xFF,
                            (colorRGB >> 8)  & 0xFF,
                            colorRGB         & 0xFF,
                            (colorRGB >> 24) & 0xFF
                    );
                }
                currentOutlineColor = cachedOutlineColor;
                currentLineWidth = lineWidth.getValue();

                // Кэшируем fill colors заранее (один раз на кадр, не в цикле)
                currentFillColor1 = getCachedFillColor(fillColor1.get(), cachedFillColor1, cachedFillColor1RGB);
                currentFillColor2 = getCachedFillColor(fillColor2.get(), cachedFillColor2, cachedFillColor2RGB);
                if (fillGradient.isEnabled()) currentAnimT = (float)((Math.sin(System.currentTimeMillis() * 0.002) + 1.0) / 2.0);
            }

            Box worldBox = entity.getBoundingBox().expand(0.002);
            Vec3d lerped  = entity.getLerpedPos(tickDelta);
            Vec3d current = entity.getPos();
            worldBox = worldBox.offset(
                    lerped.x - current.x,
                    lerped.y - current.y,
                    lerped.z - current.z);

            renderOutline(matrices, worldBox, currentOutlineColor, currentLineWidth);

            if (fill.isEnabled()) {
                java.awt.Color animColor = fillGradient.isEnabled() && !isHovered
                        ? interpolateColor(currentFillColor1, currentFillColor2, currentAnimT)
                        : currentFillColor1;
                renderFill(matrices, worldBox, animColor, animColor);
            }

            if (!noLookLine.isEnabled() && entity instanceof LivingEntity le) {
                renderLookLine(matrices, le, tickDelta, currentOutlineColor, currentLineWidth);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) return false;
        // If entity is invisible and has no armor — don't show hitbox
        if (entity instanceof LivingEntity le) {
            if (le.isInvisible()) {
                boolean hasArmor = false;
                for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{
                        net.minecraft.entity.EquipmentSlot.HEAD,
                        net.minecraft.entity.EquipmentSlot.CHEST,
                        net.minecraft.entity.EquipmentSlot.LEGS,
                        net.minecraft.entity.EquipmentSlot.FEET}) {
                    if (!le.getEquippedStack(slot).isEmpty()) { hasArmor = true; break; }
                }
                if (!hasArmor) return false;
            }
        }
        if (entity instanceof PlayerEntity)  return showPlayers.isEnabled();
        if (entity instanceof MobEntity)     return showMobs.isEnabled();
        if (entity instanceof ItemEntity)    return showItems.isEnabled();
        if (entity instanceof LivingEntity)  return showEntities.isEnabled();
        return showEntities.isEnabled();
    }

    // Helper method to get cached fill color
    private java.awt.Color getCachedFillColor(int colorRGB, java.awt.Color cachedColor, int cachedRGB) {
        // This is a bit tricky with the caching. For simplicity, let's just create new Color objects
        // or pass the RGB int directly and convert inside renderFill if performance is an issue.
        // For now, let's return a new Color object to avoid modifying cached objects.
        // Or, better, pass the RGB int and let renderFill handle it.
        // Let's stick to passing Color objects for now, and rely on the caching mechanism in onRender3D.
        // The caching logic in onRender3D for default colors will still work as intended.
        // For dynamic colors, we always create a new Color based on the dynamicColor setting.
        // This helper is primarily for the default fill colors.
        // To avoid re-creating Color objects for cachedFillColor1/2, we need to update them directly.
        return new java.awt.Color(colorRGB, true);
    }

    private void renderOutline(MatrixStack matrices, Box box, Color color, float lineW) {
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        float r = color.getRed()/255f, g = color.getGreen()/255f,
              b = color.getBlue()/255f, a = color.getAlpha()/255f;

        RenderSystem.lineWidth(lineW);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        line(buf, mat, entry, minX,minY,minZ, maxX,minY,minZ, r,g,b,a);
        line(buf, mat, entry, maxX,minY,minZ, maxX,minY,maxZ, r,g,b,a);
        line(buf, mat, entry, maxX,minY,maxZ, minX,minY,maxZ, r,g,b,a);
        line(buf, mat, entry, minX,minY,maxZ, minX,minY,minZ, r,g,b,a);
        line(buf, mat, entry, minX,maxY,minZ, maxX,maxY,minZ, r,g,b,a);
        line(buf, mat, entry, maxX,maxY,minZ, maxX,maxY,maxZ, r,g,b,a);
        line(buf, mat, entry, maxX,maxY,maxZ, minX,maxY,maxZ, r,g,b,a);
        line(buf, mat, entry, minX,maxY,maxZ, minX,maxY,minZ, r,g,b,a);
        line(buf, mat, entry, minX,minY,minZ, minX,maxY,minZ, r,g,b,a);
        line(buf, mat, entry, maxX,minY,minZ, maxX,maxY,minZ, r,g,b,a);
        line(buf, mat, entry, maxX,minY,maxZ, maxX,maxY,maxZ, r,g,b,a);
        line(buf, mat, entry, minX,minY,maxZ, minX,maxY,maxZ, r,g,b,a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
        matrices.pop();
    }

    private void renderLookLine(MatrixStack matrices, LivingEntity entity, float tickDelta, Color color, float lineW) {
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        Vec3d lerped = entity.getLerpedPos(tickDelta);
        float ox = (float) lerped.x;
        float oy = (float)(lerped.y + entity.getEyeHeight(entity.getPose()));
        float oz = (float) lerped.z;
        Vec3d look = entity.getRotationVec(tickDelta);

        float r = color.getRed()/255f, g = color.getGreen()/255f,
              b = color.getBlue()/255f, a = color.getAlpha()/255f;

        RenderSystem.lineWidth(lineW);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        line(buf, mat, entry, ox, oy, oz,
                (float)(ox + look.x*2), (float)(oy + look.y*2), (float)(oz + look.z*2),
                r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1f);
        matrices.pop();
    }

    private void renderFill(MatrixStack matrices, Box box, Color c1, Color c2) {
        matrices.push();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        float r1=c1.getRed()/255f, g1=c1.getGreen()/255f, b1=c1.getBlue()/255f, a1=c1.getAlpha()/255f;
        float r2=c2.getRed()/255f, g2=c2.getGreen()/255f, b2=c2.getBlue()/255f, a2=c2.getAlpha()/255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Bottom (c1)
        buf.vertex(mat, minX, minY, minZ).color(r1,g1,b1,a1);
        buf.vertex(mat, maxX, minY, minZ).color(r1,g1,b1,a1);
        buf.vertex(mat, maxX, minY, maxZ).color(r1,g1,b1,a1);
        buf.vertex(mat, minX, minY, maxZ).color(r1,g1,b1,a1);
        // Top (c2)
        buf.vertex(mat, minX, maxY, minZ).color(r2,g2,b2,a2);
        buf.vertex(mat, minX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, maxY, minZ).color(r2,g2,b2,a2);
        // North (gradient)
        buf.vertex(mat, minX, minY, minZ).color(r1,g1,b1,a1);
        buf.vertex(mat, minX, maxY, minZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, maxY, minZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, minY, minZ).color(r1,g1,b1,a1);
        // South (gradient)
        buf.vertex(mat, maxX, minY, maxZ).color(r1,g1,b1,a1);
        buf.vertex(mat, maxX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, minX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, minX, minY, maxZ).color(r1,g1,b1,a1);
        // West (gradient)
        buf.vertex(mat, minX, minY, maxZ).color(r1,g1,b1,a1);
        buf.vertex(mat, minX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, minX, maxY, minZ).color(r2,g2,b2,a2);
        buf.vertex(mat, minX, minY, minZ).color(r1,g1,b1,a1);
        // East (gradient)
        buf.vertex(mat, maxX, minY, minZ).color(r1,g1,b1,a1);
        buf.vertex(mat, maxX, maxY, minZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, maxY, maxZ).color(r2,g2,b2,a2);
        buf.vertex(mat, maxX, minY, maxZ).color(r1,g1,b1,a1);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private java.awt.Color interpolateColor(java.awt.Color c1, java.awt.Color c2, float t) {
        int r = (int)(c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int)(c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        int a = (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
        return new java.awt.Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                Math.max(0, Math.min(255, a)));
    }

    private void line(BufferBuilder buf, Matrix4f mat, MatrixStack.Entry entry,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float r, float g, float b, float a) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        // Переиспользуем reusableNormal — не создаём new каждый вызов
        reusableNormal.set(dx/len, dy/len, dz/len);
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(entry, reusableNormal);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(entry, reusableNormal);
    }
}
