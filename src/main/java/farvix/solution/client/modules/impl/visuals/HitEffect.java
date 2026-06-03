package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.ColorUtility;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(name = "Hit Effect", category = ModuleCategory.PLAYER, description = "Волновой эффект при ударе")
public class HitEffect extends Module implements QuickImports {

    // ── Settings ──────────────────────────────────────────────────────────────
    private final ModeSetting target = new ModeSetting("Цель", this,
            "Все",
            "Игроки",
            "Мобы"
    );

    private final ColorSetting color = new ColorSetting(
            "Цвет", this, new FixColor(100, 180, 255, 255).getRGB());

    private final SliderSetting radius = new SliderSetting(
            "Радиус", this, 12f, 4f, 24f, 1f);

    private final SliderSetting speed = new SliderSetting(
            "Скорость", this, 1.0f, 0.3f, 3.0f, 0.1f);

    private final SliderSetting waveWidth = new SliderSetting(
            "Ширина волны", this, 5.0f, 0.5f, 12f, 0.5f);

    private final SliderSetting glowIntensity = new SliderSetting(
            "Интенсивность свечения", this, 0.3f, 0.0f, 5.0f, 0.1f);
    
    private final SliderSetting lineWidth = new SliderSetting(
            "Толщина линий", this, 2.5f, 1.0f, 10.0f, 0.5f);

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<WaveEffect> waveEffects = new ArrayList<>();

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (e.getTarget() == null) return;

        String t = target.getCurrentMode();
        boolean isPlayer = e.getTarget() instanceof PlayerEntity;
        boolean isMob    = e.getTarget() instanceof MobEntity && !isPlayer;

        if (t.equals("Игроки") && !isPlayer) return;
        if (t.equals("Мобы")   && !isMob)    return;

        Vec3d pos = e.getTarget().getPos();
        BlockPos basePos = BlockPos.ofFloored(pos.x, pos.y - 0.1, pos.z);
        waveEffects.add(new WaveEffect(basePos, System.currentTimeMillis()));
    }

    @EventHandler
    public void onWorldRender(EventRender3D.Game e) {
        if (waveEffects.isEmpty() || mc.world == null) return;

        Iterator<WaveEffect> iterator = waveEffects.iterator();
        while (iterator.hasNext()) {
            WaveEffect wave = iterator.next();
            if (wave.isExpired()) {
                iterator.remove();
                continue;
            }
            wave.render(e);
        }
    }

    // ── WaveEffect ────────────────────────────────────────────────────────────

    private class WaveEffect {
        private final BlockPos centerPos;
        private final long startTime;
        private final long totalLifetimeMs;

        WaveEffect(BlockPos centerPos, long startTime) {
            this.centerPos = centerPos;
            this.startTime = startTime;
            
            float maxLoopRad = radius.getValue();
            float baseSpeed = 8.0f;
            float speedVal = speed.getValue();
            this.totalLifetimeMs = (long) (maxLoopRad / (baseSpeed * speedVal) * 1000L);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - startTime > totalLifetimeMs;
        }

        void render(EventRender3D.Game e) {
            if (mc.world == null) return;

            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / totalLifetimeMs;
            progress = Math.min(progress, 1f);

            float maxLoopRad = radius.getValue();
            int maxRadInt = (int) maxLoopRad;
            float currentRadius = progress * maxLoopRad;
            float waveW = waveWidth.getValue();
            float coreHalfW = waveW / 2.0f;
            float fadeW = 1.5f;

            java.awt.Color c = new java.awt.Color(color.get(), true);
            float colorAlpha = c.getAlpha() / 255f;
            
            // Улучшенное fade-in и fade-out
            float fadeIn = Math.min(progress * 4f, 1f);
            float fadeOut = (float) Math.pow(1.0f - progress, 1.2);
            float globalAlpha = fadeIn * fadeOut * colorAlpha;

            float minCoreRad = Math.max(0f, currentRadius - coreHalfW);
            float maxCoreRad = currentRadius + coreHalfW;

            MatrixStack ms = e.getMatrices();

            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest(); // Включаем depth test - не видно через сущности
            com.mojang.blaze3d.systems.RenderSystem.disableCull();

            int rendered = 0;
            int maxPerFrame = 2000;

            // ═══════════════════════════════════════════════════════════════════
            // PASS 1: Glow / Fill Layer (красивое заливание цвета в волну)
            // ═══════════════════════════════════════════════════════════════════
            float glowStr = glowIntensity.getValue();
            if (glowStr > 0.05f) {
                com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
                BufferBuilder glowBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                for (int x = -maxRadInt; x <= maxRadInt; x++) {
                    for (int z = -maxRadInt; z <= maxRadInt; z++) {
                        if (rendered >= maxPerFrame) break;

                        float distSq = x * x + z * z;
                        float distance = (float) Math.sqrt(distSq);
                        if (distance < minCoreRad || distance > maxCoreRad) continue;

                        BlockPos checkPos = centerPos.add(x, 0, z);
                        BlockPos renderPos = findSurface(checkPos);
                        if (renderPos == null) continue;

                        BlockState state = mc.world.getBlockState(renderPos);
                        if (state.isAir()) continue;

                        float localAlpha = globalAlpha;
                        if (localAlpha <= 0.05f) continue;

                        rendered++;

                        // Однородная полупрозрачная заливка поверхности блока (в 3 раза ярче)
                        int alphaVal = Math.max(0, Math.min(255, (int)(localAlpha * 195f * glowStr)));
                        int fillColor = ColorUtility.getColor(c.getRed(), c.getGreen(), c.getBlue(), alphaVal);

                        ms.push();
                        ms.translate(renderPos.getX(), renderPos.getY() + 1.002, renderPos.getZ());
                        Matrix4f mat = ms.peek().getPositionMatrix();

                        glowBuf.vertex(mat, 0, 0, 0).color(fillColor);
                        glowBuf.vertex(mat, 0, 0, 1).color(fillColor);
                        glowBuf.vertex(mat, 1, 0, 1).color(fillColor);
                        glowBuf.vertex(mat, 1, 0, 0).color(fillColor);

                        ms.pop();
                    }
                }

                BuiltBuffer glowBuilt = glowBuf.endNullable();
                if (glowBuilt != null) BufferRenderer.drawWithGlobalProgram(glowBuilt);
            }

            // ═══════════════════════════════════════════════════════════════════
            // PASS 2: Main Layer (четкие линии контуров в виде полигонов)
            // ═══════════════════════════════════════════════════════════════════
            rendered = 0;

            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder lineBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            float thickness = lineWidth.getValue() * 0.015f;

            for (int x = -maxRadInt; x <= maxRadInt; x++) {
                for (int z = -maxRadInt; z <= maxRadInt; z++) {
                    if (rendered >= maxPerFrame) break;

                    float distSq = x * x + z * z;
                    float distance = (float) Math.sqrt(distSq);
                    if (distance < minCoreRad || distance > maxCoreRad) continue;

                    BlockPos checkPos = centerPos.add(x, 0, z);
                    BlockPos renderPos = findSurface(checkPos);
                    if (renderPos == null) continue;

                    BlockState state = mc.world.getBlockState(renderPos);
                    if (state.isAir()) continue;

                    float localAlpha = globalAlpha;
                    if (localAlpha <= 0.05f) continue;

                    rendered++;
                    
                    // Более яркий и насыщенный цвет
                    int finalColor = ColorUtility.getColor(c.getRed(), c.getGreen(), c.getBlue(), (int)(localAlpha * 255));

                    Matrix4f mat = ms.peek().getPositionMatrix();
                    float rx = renderPos.getX();
                    float ry = renderPos.getY();
                    float rz = renderPos.getZ();

                    boolean drawNorth = true;
                    boolean drawEast = true;
                    boolean drawSouth = true;
                    boolean drawWest = true;

                    // Check neighbors
                    boolean nActive = false;
                    boolean eActive = false;
                    boolean sActive = false;
                    boolean wActive = false;
                    
                    boolean neActive = false;
                    boolean seActive = false;
                    boolean swActive = false;
                    boolean nwActive = false;

                    // North
                    float nDist = (float) Math.sqrt(x * x + (z - 1) * (z - 1));
                    if (nDist >= minCoreRad && nDist <= maxCoreRad) {
                        BlockPos nPos = findSurface(centerPos.add(x, 0, z - 1));
                        if (nPos != null && nPos.getY() == renderPos.getY()) {
                            drawNorth = false;
                            nActive = true;
                        }
                    }
                    // East
                    float eDist = (float) Math.sqrt((x + 1) * (x + 1) + z * z);
                    if (eDist >= minCoreRad && eDist <= maxCoreRad) {
                        BlockPos ePos = findSurface(centerPos.add(x + 1, 0, z));
                        if (ePos != null && ePos.getY() == renderPos.getY()) {
                            drawEast = false;
                            eActive = true;
                        }
                    }
                    // South
                    float sDist = (float) Math.sqrt(x * x + (z + 1) * (z + 1));
                    if (sDist >= minCoreRad && sDist <= maxCoreRad) {
                        BlockPos sPos = findSurface(centerPos.add(x, 0, z + 1));
                        if (sPos != null && sPos.getY() == renderPos.getY()) {
                            drawSouth = false;
                            sActive = true;
                        }
                    }
                    // West
                    float wDist = (float) Math.sqrt((x - 1) * (x - 1) + z * z);
                    if (wDist >= minCoreRad && wDist <= maxCoreRad) {
                        BlockPos wPos = findSurface(centerPos.add(x - 1, 0, z));
                        if (wPos != null && wPos.getY() == renderPos.getY()) {
                            drawWest = false;
                            wActive = true;
                        }
                    }

                    // Diagonal neighbors
                    // North-East
                    float neDist = (float) Math.sqrt((x + 1) * (x + 1) + (z - 1) * (z - 1));
                    if (neDist >= minCoreRad && neDist <= maxCoreRad) {
                        BlockPos nePos = findSurface(centerPos.add(x + 1, 0, z - 1));
                        if (nePos != null && nePos.getY() == renderPos.getY()) {
                            neActive = true;
                        }
                    }
                    // South-East
                    float seDist = (float) Math.sqrt((x + 1) * (x + 1) + (z + 1) * (z + 1));
                    if (seDist >= minCoreRad && seDist <= maxCoreRad) {
                        BlockPos sePos = findSurface(centerPos.add(x + 1, 0, z + 1));
                        if (sePos != null && sePos.getY() == renderPos.getY()) {
                            seActive = true;
                        }
                    }
                    // South-West
                    float swDist = (float) Math.sqrt((x - 1) * (x - 1) + (z + 1) * (z + 1));
                    if (swDist >= minCoreRad && swDist <= maxCoreRad) {
                        BlockPos swPos = findSurface(centerPos.add(x - 1, 0, z + 1));
                        if (swPos != null && swPos.getY() == renderPos.getY()) {
                            swActive = true;
                        }
                    }
                    // North-West
                    float nwDist = (float) Math.sqrt((x - 1) * (x - 1) + (z - 1) * (z - 1));
                    if (nwDist >= minCoreRad && nwDist <= maxCoreRad) {
                        BlockPos nwPos = findSurface(centerPos.add(x - 1, 0, z - 1));
                        if (nwPos != null && nwPos.getY() == renderPos.getY()) {
                            nwActive = true;
                        }
                    }

                    // North border
                    if (drawNorth) {
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz).color(finalColor);
                    }
                    
                    // East border
                    if (drawEast) {
                        lineBuf.vertex(mat, rx + 1f - thickness, ry + 1.003f, rz).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f - thickness, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz).color(finalColor);
                    }
                    
                    // South border
                    if (drawSouth) {
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                    }
                    
                    // West border
                    if (drawWest) {
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + thickness, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + thickness, ry + 1.003f, rz).color(finalColor);
                    }

                    // Fill diagonal gaps precisely
                    // SE diagonal
                    if (seActive && !sActive && !eActive) {
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f + thickness, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f + thickness, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                    }
                    // SW diagonal
                    if (swActive && !sActive && !wActive) {
                        lineBuf.vertex(mat, rx - thickness, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                        lineBuf.vertex(mat, rx - thickness, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + 1f).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + 1f - thickness).color(finalColor);
                    }
                    // NE diagonal
                    if (neActive && !nActive && !eActive) {
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f + thickness, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx + 1f + thickness, ry + 1.003f, rz).color(finalColor);
                    }
                    // NW diagonal
                    if (nwActive && !nActive && !wActive) {
                        lineBuf.vertex(mat, rx - thickness, ry + 1.003f, rz).color(finalColor);
                        lineBuf.vertex(mat, rx - thickness, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz + thickness).color(finalColor);
                        lineBuf.vertex(mat, rx, ry + 1.003f, rz).color(finalColor);
                    }
                }
            }

            BuiltBuffer lineBuilt = lineBuf.endNullable();
            if (lineBuilt != null) BufferRenderer.drawWithGlobalProgram(lineBuilt);

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest(); // Уже включен
            com.mojang.blaze3d.systems.RenderSystem.enableCull();
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.lineWidth(1f);
        }

        private BlockPos findSurface(BlockPos pos) {
            if (mc.world == null) return null;
            for (int y = 2; y >= -4; y--) {
                BlockPos p = pos.up(y);
                if (!mc.world.getBlockState(p).isAir() && mc.world.getBlockState(p.up()).isAir()) {
                    return p;
                }
            }
            return null;
        }

        private boolean isNeighborVisible(float nDist, float currentRadius, float coreHalfW, float fadeW, float globalAlpha) {
            float localAlpha = 0.0f;
            float diff = Math.abs(nDist - currentRadius);
            if (diff <= coreHalfW) {
                localAlpha = 1.0f;
            } else if (diff <= coreHalfW + fadeW) {
                float excess = diff - coreHalfW;
                localAlpha = 1.0f - (excess / fadeW);
            }
            localAlpha = Math.max(0, Math.min(1, localAlpha)) * globalAlpha;
            return localAlpha > 0.05f;
        }
    }
}
