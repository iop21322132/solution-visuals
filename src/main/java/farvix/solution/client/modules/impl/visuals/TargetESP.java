package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.animations.Animation;
import farvix.solution.api.util.animations.Easing;
import farvix.solution.client.managers.ThemeManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import farvix.solution.api.util.color.FixColor;

@ModuleInfo(name = "Target ESP", category = ModuleCategory.PLAYER, description = "Эффект вокруг цели")
public class TargetESP extends Module implements ThemeManager.ThemeChangeListener {

    private final ModeSetting mode = new ModeSetting("targetesp.mode", this,
            "Marker", "Marker V2", "Soul", "Circle", "Crystals", "Chains");

    private final BooleanSetting changeColorOnDamage = new BooleanSetting("Изменение цвета при уроне", this);
    private final SliderSetting speed = new SliderSetting("Скорость вращения", this, 5f, 1f, 10f, 1f);
    private final SliderSetting size  = new SliderSetting("Размер",  this, 1.5f, 0.5f, 3.0f, 0.1f);
    private final SliderSetting ghostCount     = new SliderSetting("Количество",     this, 50f, 1f, 50f, 1f);
    private final SliderSetting ghostThickness = new SliderSetting("Толщина", this, 1.7f, 0.1f, 2.0f, 0.1f);
    private final SliderSetting crystalCountSetting = new SliderSetting("Количество кристаллов", this, 12f, 5f, 35f, 1f);
    private final SliderSetting lifetime = new SliderSetting("Время жизни (сек)", this, 5f, 1f, 25f, 1f);
    // Существующая настройка цвета, теперь используется как базовый цвет
    public final ColorSetting color = new ColorSetting("Цвет", this, new FixColor(255, 255, 255, 255).getRGB()); // Инициализация FixColor корректна
    // Новая настройка яркости
    public final SliderSetting brightness = new SliderSetting("Яркость", this, 1.0f, 0.1f, 2.0f, 0.1f); // Настройка яркости

    // animations — same constructor as reference: (duration, startValue, forward, easing)
    private final Animation animation     = new Animation(300L, 1.0f, false, Easing.BOTH_CUBIC);
    private final Animation moving        = new Animation(70L,  1.0f, false, Easing.LINEAR);
    private final Animation hurtAnimation = new Animation(200L, 1.0f, false, Easing.BOTH_CUBIC);
    private final ThemeManager themeManager;

    private LivingEntity prevTarget = null;
    private long lastAttackTime = 0L;
    private float chainRotationAngle = 0f;
    private float markerRotationAngle = 0f;
    private float movingValue = 0f;

    public TargetESP() {
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
        changeColorOnDamage.setValue(true);
        ghostCount.setVisible(()     -> mode.getCurrentMode().equals("Soul"));
        ghostThickness.setVisible(() -> mode.getCurrentMode().equals("Soul"));
        crystalCountSetting.setVisible(() -> mode.getCurrentMode().equals("Crystals"));
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {}

    // ── color ────────────────────────────────────────────────────────────────

    private Color getTargetColor() {
        float hit = hurtAnimation.getValue();
        Color base = new Color(this.color.get(), true); // Используем настройку цвета модуля с поддержкой прозрачности
        if (changeColorOnDamage.getValue() && hit > 0) {
            // Смешиваем базовый цвет с красным в зависимости от анимации урона
            int r = clamp((int)(base.getRed() * (1 - hit) + 255 * hit));
            int g = clamp((int)(base.getGreen() * (1 - hit)));
            int b = clamp((int)(base.getBlue() * (1 - hit)));
            return new Color(r, g, b, base.getAlpha());
        }
        return base;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static int rgba(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(alpha)).getRGB();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Vec3d getRenderPos(LivingEntity target) {
        float td = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
        return new Vec3d(
            MathHelper.lerp(td, target.prevX, target.getX()),
            MathHelper.lerp(td, target.prevY, target.getY()),
            MathHelper.lerp(td, target.prevZ, target.getZ())
        );
    }

    private static float sin(double rad) { return (float) Math.sin(rad); }
    private static float cos(double rad) { return (float) Math.cos(rad); }

    // ── events ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttackEntity(EventAttackEntity e) {
        if (mc.player == null || mc.world == null) return;
        if (!(e.getTarget() instanceof LivingEntity living)) return;
        if (!living.isAlive()) return;
        
        // Проверяем невидимость
        if (living.hasStatusEffect(StatusEffects.INVISIBILITY) || living.isInvisible()) {
            // Если невидимый, проверяем есть ли хотя бы один элемент брони
            boolean hasAnyArmor = false;
            for (net.minecraft.item.ItemStack armorPiece : living.getArmorItems()) {
                if (!armorPiece.isEmpty()) {
                    hasAnyArmor = true;
                    break;
                }
            }
            // Если нет брони - не показываем ESP
            if (!hasAnyArmor) return;
        }
        
        prevTarget = living;
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = prevTarget;
        if (target != null && !target.isAlive()) {
            prevTarget = null;
            target = null;
        }
        
        // Проверяем невидимость цели перед рендером
        if (target != null && (target.hasStatusEffect(StatusEffects.INVISIBILITY) || target.isInvisible())) {
            // Проверяем есть ли хотя бы один элемент брони
            boolean hasAnyArmor = false;
            for (net.minecraft.item.ItemStack armorPiece : target.getArmorItems()) {
                if (!armorPiece.isEmpty()) {
                    hasAnyArmor = true;
                    break;
                }
            }
            // Если нет брони - не рендерим ESP
            if (!hasAnyArmor) {
                prevTarget = null;
                target = null;
            }
        }
        
        if (target != null && System.currentTimeMillis() - lastAttackTime > (long)(lifetime.getValue() * 1000f)) {
            prevTarget = null;
            target = null;
        }

        animation.update(target != null);

        // speed=1 → 0.1x, speed=10 → 1.0x (linear, each +1 adds 10% of max speed)
        float speedMul = speed.getValue() / 10f;
        movingValue         += 1.5f  * speedMul;
        markerRotationAngle += 0.125f * speedMul;
        chainRotationAngle  += 0.075f * speedMul;

        // Only show hurt color if we attacked recently (within 1s), prevents
        // false red flash when target takes damage from other sources
        boolean weJustAttacked = System.currentTimeMillis() - lastAttackTime < 1000L;
        if (target != null && weJustAttacked) {
            hurtAnimation.update(target.hurtTime > 0);
        } else {
            hurtAnimation.update(false);
        }

        if (target != null) prevTarget = target;

        float animVal = animation.getValue();
        if (prevTarget == null || animVal <= 0f) return;

        MatrixStack ms = e.getMatrices();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();

        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        switch (mode.getCurrentMode()) {
            case "Chains"     -> drawChains(ms, prevTarget);
            case "Circle"     -> drawCircles(ms, prevTarget);
            case "Marker" -> {
                if (mc.player != null) {
                    Vec3d pos = getRenderPos(prevTarget);
                    Vec3d markerPoint = new Vec3d(pos.x, pos.y + prevTarget.getHeight() / 2.0, pos.z);
                    Vec3d eyePos = mc.player.getEyePos();
                    var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                            eyePos, markerPoint,
                            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));
                    // Если между глазами и точкой маркера стоит блок — не рисуем "сквозь" стены
                    if (hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
                        RenderSystem.disableDepthTest();
                        drawMarker(ms, prevTarget, "textures/marker.png");
                        RenderSystem.enableDepthTest();
                    }
                }
            }
            case "Marker V2" -> {
                if (mc.player != null) {
                    Vec3d pos = getRenderPos(prevTarget);
                    Vec3d markerPoint = new Vec3d(pos.x, pos.y + prevTarget.getHeight() / 2.0, pos.z);
                    Vec3d eyePos = mc.player.getEyePos();
                    var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                            eyePos, markerPoint,
                            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));
                    if (hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
                        RenderSystem.disableDepthTest();
                        drawMarker(ms, prevTarget, "textures/markerv2.png");
                        RenderSystem.enableDepthTest();
                    }
                }
            }
            case "Crystals"   -> drawCrystals(ms, prevTarget);
            default           -> drawGhosts(ms, prevTarget);
        }
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        ms.pop();
    }

    // ── draw modes ───────────────────────────────────────────────────────────

    private void drawMarker(MatrixStack ms, LivingEntity target, String texturePath) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = getRenderPos(target);
        // MatrixStack already has camera.negate() applied — use world coords directly
        double entX = pos.x;
        double entY = pos.y + target.getHeight() / 2.0;
        double entZ = pos.z;

        float sz = 1.2f * size.getValue();
        Color baseColor = getTargetColor();
        int baseAlpha = baseColor.getAlpha();
        int alpha = (int)(animation.getValue() * baseAlpha * 0.7f);
 
        ms.push();
        ms.translate(entX, entY, entZ);
        ms.multiply(camera.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(markerRotationAngle));

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Identifier tex = Identifier.of("solution", texturePath);
        
        // Основной маркер (прозрачность независима от яркости)
        drawTexturedQuad(ms, tex, -sz / 2f, -sz / 2f, sz, sz, rgba(baseColor, alpha)); 

        // Свечение (активируется и усиливается ползунком яркости, при минимуме 0.1 свечения нет вовсе)
        float glowFactor = Math.max(0f, (brightness.getValue() - 0.1f) / 1.9f);
        if (glowFactor > 0f) {
            int glowAlpha = (int)(animation.getValue() * baseAlpha * 0.7f * 0.4f * glowFactor);
            float bFactor = Math.max(1.0f, brightness.getValue());
            Color glowColor = new Color(
                clamp((int)(baseColor.getRed() * bFactor)),
                clamp((int)(baseColor.getGreen() * bFactor)),
                clamp((int)(baseColor.getBlue() * bFactor)),
                baseColor.getAlpha()
            );
            drawTexturedQuad(ms, tex, -sz / 2f, -sz / 2f, sz, sz, rgba(glowColor, glowAlpha));
        }
        ms.pop();
    }

    private void drawChains(MatrixStack ms, LivingEntity target) {
        Vec3d pos = getRenderPos(target);
        double cx = pos.x;
        double cy = pos.y + target.getHeight() / 2.0;
        double cz = pos.z;
        renderChainCylinder(ms, target, cx, cy, cz, 0f);
        renderChainCylinder(ms, target, cx, cy, cz, 90f);
    }

    private void renderChainCylinder(MatrixStack ms, LivingEntity target, double x, double y, double z, float offsetAngle) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chainRotationAngle + offsetAngle));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chainRotationAngle + offsetAngle));

        float radius = target.getWidth() * 1.5f * size.getValue();
        int segments = 60;
        Color baseColor = getTargetColor();
        int baseAlpha = baseColor.getAlpha();
        int alpha = (int)(animation.getValue() * baseAlpha);
        
        float bFactor = Math.max(1.0f, brightness.getValue());
        Color chainColor = new Color(
            clamp((int)(baseColor.getRed() * bFactor)),
            clamp((int)(baseColor.getGreen() * bFactor)),
            clamp((int)(baseColor.getBlue() * bFactor)),
            baseColor.getAlpha()
        );
        int rgb = rgba(chainColor, alpha);

        Identifier chainTex = Identifier.of("solution", "textures/chain.png");
        RenderSystem.setShaderTexture(0, chainTex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float texRepeat = 4.0f;
        float chainHeight = target.getHeight() * 0.8f * size.getValue();
        float half = chainHeight / 2f;

        for (int i = 0; i < segments; i++) {
            float a1 = (float)(2 * Math.PI * i / segments);
            float a2 = (float)(2 * Math.PI * (i + 1) / segments);
            float x1 = cos(a1) * radius, z1 = sin(a1) * radius;
            float x2 = cos(a2) * radius, z2 = sin(a2) * radius;
            float u1 = (float) i / segments * texRepeat;
            float u2 = (float)(i + 1) / segments * texRepeat;
            buffer.vertex(matrix, x1, -half, z1).texture(u1, 1f).color(rgb);
            buffer.vertex(matrix, x2, -half, z2).texture(u2, 1f).color(rgb);
            buffer.vertex(matrix, x2,  half, z2).texture(u2, 0f).color(rgb);
            buffer.vertex(matrix, x1,  half, z1).texture(u1, 0f).color(rgb);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void drawCircles(MatrixStack ms, LivingEntity target) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = getRenderPos(target);
        double entX = pos.x;
        double entY = pos.y;
        double entZ = pos.z;

        float width   = target.getWidth() * 1.45f * size.getValue();
        float baseVal = Math.max(0.5f, 0.7f - 0.1f * hurtAnimation.getValue() + 0.1f - 0.1f * animation.getValue());
        Color baseColor   = getTargetColor();
        int baseAlpha = baseColor.getAlpha();
        int alpha     = (int)(animation.getValue() * baseAlpha);
        
        float bFactor = Math.max(1.0f, brightness.getValue());
        Color circleColor = new Color(
            clamp((int)(baseColor.getRed() * bFactor)),
            clamp((int)(baseColor.getGreen() * bFactor)),
            clamp((int)(baseColor.getBlue() * bFactor)),
            baseColor.getAlpha()
        );
        float dotSize = 0.4f * size.getValue();
        float bigSize = 0.8f * size.getValue();

        Identifier bloomTex = Identifier.of("solution", "textures/hud/bloom.png");
        RenderSystem.setShaderTexture(0, bloomTex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (int i = 0; i < 360; i += 3) {
            if ((i / 45) % 2 != 0) {
                double rad = Math.toRadians(i + movingValue);
                float sx = (float)(entX + Math.sin(rad) * width * baseVal);
                float sz = (float)(entZ + Math.cos(rad) * width * baseVal);
                float sy = (float)(entY + target.getHeight() * (float)((1.0 - Math.cos(Math.toRadians(movingValue))) / 2.0));

                ms.push();
                ms.translate(sx, sy, sz);
                ms.multiply(camera.getRotation());
                Matrix4f matrix = ms.peek().getPositionMatrix();

                BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                drawBillboard(matrix, buf, -bigSize / 2f, -bigSize / 2f, bigSize, rgba(circleColor, (int)(alpha * 0.1f * Math.max(0.1f, brightness.getValue())))); // Большое свечение
                BufferRenderer.drawWithGlobalProgram(buf.end());

                buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                drawBillboard(matrix, buf, -dotSize / 2f, -dotSize / 2f, dotSize, rgba(circleColor, alpha)); // Основная точка
                BufferRenderer.drawWithGlobalProgram(buf.end());

                ms.pop();
            }
        }
    }

    private void drawCrystals(MatrixStack ms, LivingEntity target) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = getRenderPos(target);
        double entX = pos.x;
        double entY = pos.y;
        double entZ = pos.z;

        Color baseColor = getTargetColor();
        int baseAlpha = baseColor.getAlpha();
        
        float bFactor = Math.max(1.0f, brightness.getValue());
        Color crystalColor = new Color(
            clamp((int)(baseColor.getRed() * bFactor)),
            clamp((int)(baseColor.getGreen() * bFactor)),
            clamp((int)(baseColor.getBlue() * bFactor)),
            baseColor.getAlpha()
        );

        // Size slider scales BOTH the orbit width and the crystal size
        float baseWidth = target.getWidth() * 1.3f * size.getValue();
        float sz2 = 0.12f * size.getValue(); // Crystal diamond size scales with slider

        ms.push();
        ms.translate(entX, entY, entZ);

        float crystalSpeed = movingValue * 0.0075f; // Swirling speed (20x slower)

        int crystalCount = (int) crystalCountSetting.getValue();
        float[] sxArray = new float[crystalCount];
        float[] syArray = new float[crystalCount];
        float[] szArray = new float[crystalCount];

        for (int idx = 0; idx < crystalCount; idx++) {
            // Even circular distribution around the player's circumference (closed ring)
            float angle = (float) (idx * 2 * Math.PI / crystalCount) + crystalSpeed;

            // Radius scales dynamically with a global breathe effect for fluid motion
            float rx = baseWidth * (0.94f + 0.06f * sin(crystalSpeed * 0.4f));

            float px = sin(angle) * rx;
            float pz = cos(angle) * rx;

            // Center waist height
            float yPos = target.getHeight() * 0.5f;

            // Smooth global vertical bobbing
            float bobbing = sin(crystalSpeed * 0.5f) * target.getHeight() * 0.05f;
            float syVal = yPos + bobbing;

            sxArray[idx] = px;
            syArray[idx] = syVal;
            szArray[idx] = pz;
        }

        // Draw crystals
        for (int idx = 0; idx < crystalCount; idx++) {
            float sx = sxArray[idx];
            float sy = syArray[idx];
            float sz = szArray[idx];

            ms.push();
            ms.translate(sx, sy, sz);

            // Double nested counter-rotating crystals
            // Outer shell (highly translucent, larger)
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(crystalSpeed * 120f + idx * 30f));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(crystalSpeed * 60f));
            int outerRgb = rgba(crystalColor, (int) (baseAlpha * animation.getValue() * 0.35f));
            drawDiamond(ms, sz2, outerRgb);
            ms.pop();

            // Inner core (more solid, smaller, counter-rotating)
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-crystalSpeed * 160f - idx * 45f));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-crystalSpeed * 80f));
            int innerRgb = rgba(crystalColor, (int) (baseAlpha * animation.getValue() * 0.9f));
            drawDiamond(ms, sz2 * 0.55f, innerRgb);
            ms.pop();

            ms.pop();
        }

        // bloom billboards
        Identifier bloomTex = Identifier.of("solution", "textures/hud/bloom.png");
        RenderSystem.setShaderTexture(0, bloomTex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float bloomSize = 0.8f * size.getValue(); // Bloom size scales with slider

        for (int idx = 0; idx < crystalCount; idx++) {
            float sx = sxArray[idx];
            float sy = syArray[idx];
            float sz = szArray[idx];

            ms.push();
            ms.translate(sx, sy, sz);
            ms.multiply(camera.getRotation());
            drawBillboard(ms.peek().getPositionMatrix(), buffer, -bloomSize / 2f, -bloomSize / 2f, bloomSize,
                rgba(crystalColor, (int)(baseAlpha * animation.getValue() * 0.25f * Math.max(0.1f, brightness.getValue()))));
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void drawGhosts(MatrixStack ms, LivingEntity target) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = getRenderPos(target);
        double entX = pos.x;
        double entY = pos.y;
        double entZ = pos.z;

        Color baseColor = getTargetColor();
        int baseAlpha = baseColor.getAlpha();
        
        float bFactor = Math.max(1.0f, brightness.getValue());
        Color ghostColor = new Color(
            clamp((int)(baseColor.getRed() * bFactor)),
            clamp((int)(baseColor.getGreen() * bFactor)),
            clamp((int)(baseColor.getBlue() * bFactor)),
            baseColor.getAlpha()
        );
        float width = target.getWidth() * 1.5f * size.getValue();
        float thicknessMul = ghostThickness.getValue();
        int step = Math.max(1, 360 / (int) ghostCount.getValue());

        Identifier bloomTex = Identifier.of("solution", "textures/hud/bloom.png");
        RenderSystem.setShaderTexture(0, bloomTex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        ms.push();
        ms.translate(entX, entY, entZ);

        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int wormTick = 0, wormCD = 0;
        boolean hasVertices = false;
        for (int i = 0; i < 360; i += step) {
            float sz    = (0.13f + 0.005f * wormTick) * thicknessMul;
            float bigSz = (0.7f  + 0.005f * wormTick) * thicknessMul;

            if (wormCD > 0) {
                wormCD -= step;
            } else {
                wormTick += step;
                if (wormTick > 50) {
                    wormCD = 100;
                    wormTick = 0;
                } else {
                    float val = Math.max(0.5f, 1.2f - 0.5f * animation.getValue());
                    float sx  = sin(Math.toRadians(i + movingValue)) * width * val;
                    float sz2 = cos(Math.toRadians(i + movingValue)) * width * val;
                    float sy  = target.getHeight() / 1.5f
                              + target.getHeight() / 3.0f * sin(Math.toRadians(i / 2.0 + movingValue / 5.0));

                    ms.push();
                    ms.translate(sx, sy, sz2);
                    ms.multiply(camera.getRotation());
                    Matrix4f matrix = ms.peek().getPositionMatrix();
                    drawBillboard(matrix, builder, -bigSz / 2f, -bigSz / 2f, bigSz, // Большое свечение
                        rgba(ghostColor, clamp((int)(baseAlpha * animation.getValue() * 0.05f * Math.max(0.1f, brightness.getValue())))));
                    drawBillboard(matrix, builder, -sz / 2f, -sz / 2f, sz, // Основной призрак
                        rgba(ghostColor, clamp((int)(baseAlpha * animation.getValue()))));
                    hasVertices = true;
                    ms.pop();
                }
            }
        }
        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(builder.end());
        }
        ms.pop();
    }

    // ── render primitives ────────────────────────────────────────────────────

    private static void drawBillboard(Matrix4f m, BufferBuilder buf, float x, float y, float sz, int rgb) {
        buf.vertex(m, x,      y,      0).texture(0f, 0f).color(rgb);
        buf.vertex(m, x,      y + sz, 0).texture(0f, 1f).color(rgb);
        buf.vertex(m, x + sz, y + sz, 0).texture(1f, 1f).color(rgb);
        buf.vertex(m, x + sz, y,      0).texture(1f, 0f).color(rgb);
    }

    private static void drawTexturedQuad(MatrixStack ms, Identifier tex, float x, float y, float w, float h, int rgb) {
        RenderSystem.setShaderTexture(0, tex);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, x,     y,     0).texture(0f, 0f).color(rgb);
        buf.vertex(m, x,     y + h, 0).texture(0f, 1f).color(rgb);
        buf.vertex(m, x + w, y + h, 0).texture(1f, 1f).color(rgb);
        buf.vertex(m, x + w, y,     0).texture(1f, 0f).color(rgb);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static int shadeColor(int rgb, float factor) {
        int a = (rgb >> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return ((a & 0xFF) << 24) |
               ((clamp((int)(r * factor)) & 0xFF) << 16) |
               ((clamp((int)(g * factor)) & 0xFF) << 8) |
               (clamp((int)(b * factor)) & 0xFF);
    }

    private static void drawDiamond(MatrixStack ms, float sz, int rgb) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        int rgb1 = shadeColor(rgb, 1.0f);
        int rgb2 = shadeColor(rgb, 0.82f);
        int rgb3 = shadeColor(rgb, 0.72f);
        int rgb4 = shadeColor(rgb, 0.9f);
        int rgb5 = shadeColor(rgb, 0.78f);
        int rgb6 = shadeColor(rgb, 0.65f);
        int rgb7 = shadeColor(rgb, 0.95f);
        int rgb8 = shadeColor(rgb, 0.85f);
        
        // top
        buf.vertex(m,  0,  sz,  0).color(rgb1); buf.vertex(m,  sz, 0,  0).color(rgb1); buf.vertex(m,  0,  0,  sz).color(rgb1);
        buf.vertex(m,  0,  sz,  0).color(rgb2); buf.vertex(m, -sz, 0,  0).color(rgb2); buf.vertex(m,  0,  0,  sz).color(rgb2);
        buf.vertex(m,  0,  sz,  0).color(rgb3); buf.vertex(m,  sz, 0,  0).color(rgb3); buf.vertex(m,  0,  0, -sz).color(rgb3);
        buf.vertex(m,  0,  sz,  0).color(rgb4); buf.vertex(m, -sz, 0,  0).color(rgb4); buf.vertex(m,  0,  0, -sz).color(rgb4);
        // bottom
        buf.vertex(m,  0, -sz,  0).color(rgb5); buf.vertex(m,  sz, 0,  0).color(rgb5); buf.vertex(m,  0,  0,  sz).color(rgb5);
        buf.vertex(m,  0, -sz,  0).color(rgb6); buf.vertex(m, -sz, 0,  0).color(rgb6); buf.vertex(m,  0,  0,  sz).color(rgb6);
        buf.vertex(m,  0, -sz,  0).color(rgb7); buf.vertex(m,  sz, 0,  0).color(rgb7); buf.vertex(m,  0,  0, -sz).color(rgb7);
        buf.vertex(m,  0, -sz,  0).color(rgb8); buf.vertex(m, -sz, 0,  0).color(rgb8); buf.vertex(m,  0,  0, -sz).color(rgb8);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
