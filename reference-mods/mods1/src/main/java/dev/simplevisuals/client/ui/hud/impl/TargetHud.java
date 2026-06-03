package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.util.Network.Server;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.render.DiffuseLighting;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.awt.*;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color headerTextColor;
    private Color lowDurabilityColor;
    private Color absorbColor;

    // Settings
    private final BooleanSetting displayAbsorption = new BooleanSetting("displayAbsorption", true);
    private final BooleanSetting displayHudParticles = new BooleanSetting("hudParticles", true);
    private final ListSetting style;

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        // Build ListSetting with internal options so individual booleans are not exposed as separate settings
        BooleanSetting optDefault = new BooleanSetting("targethud.style.default", true, () -> false);
        BooleanSetting optCard = new BooleanSetting("targethud.style.card", false, () -> false);
        this.style = new ListSetting("targethud.style", () -> true, true, optDefault, optCard).setSingleSelect(true);

        getSettings().add(displayAbsorption);
        getSettings().add(displayHudParticles);
        getSettings().add(style);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        // Match Potions background (30,30,30,240)
        this.bgColor = new Color(30, 30, 30, 255);

        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }

        this.headerTextColor = this.textColor;
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
        this.absorbColor = new Color(255, 190, 0, 255);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    // Animations
    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation scaleAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation slideAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation hpAnimPx = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation absAnimPx = new InfinityAnimation(Easing.BOTH_SINE);

    private LivingEntity lastTarget = null;
    private long lastSeenTime = 0L;
    private static final long HUD_DURATION = 2000;
    private boolean forceFade = false;
    private Vec3d lastKnownCenter = null;

    private float healthAnimation = 0f;
    private float hitFlash = 0f;
    private float lastHealth = -1f;
    private final List<HitParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrameTimeMs = System.currentTimeMillis();

    // Размеры из нового кода
    private float scaled(float f) { return f; } 
    private static final float HEAD_SIZE = 18f;
    private static final float PAD = 4f;
    private static final float BAR_H = 3f;
    private static final float GAP = 1f;

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr.getType() != HitResult.Type.MISS;
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTimeMs) / 1000f;
        lastFrameTimeMs = now;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);

                if (isInvisibleAndUnrevealed(living)) {
                } else if (isOccluded(mc.player.getCameraPosVec(e.getTickDelta()), center)) {
                    lastTarget = null;
                    forceFade = true;
                    lastKnownCenter = center;
                } else {
                    lastTarget = living;
                    lastSeenTime = now;
                    forceFade = false;
                    lastKnownCenter = center;
                }
            }
        }

        if (lastTarget != null && (!lastTarget.isAlive() || now - lastSeenTime > HUD_DURATION)) {
            lastTarget = null;
            forceFade = true;
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean previewMode = chatOpen && lastTarget == null;
        LivingEntity previewEntity = previewMode ? mc.player : null;

        boolean shouldShow = (lastTarget != null && lastTarget.isAlive() && !forceFade) || previewMode;
        fadeAnimation.animate(shouldShow ? 1.0f : 0.0f, 250);

        float anim = fadeAnimation.getValue();
        if (anim <= 0.01f) {
            if (forceFade) {
                forceFade = false;
                particles.clear();
                lastHealth = -1f;
            }
            return;
        }

        int fa = (int)(anim * 255f);
        
        LivingEntity target = lastTarget;
        if (!previewMode && target == null && lastKnownCenter == null) return;
        LivingEntity entity = previewMode ? previewEntity : target;

        float rawHp = MathUtils.round(Server.getHealth(entity, false));
        if (lastHealth > 0 && rawHp < lastHealth) {
            hitFlash = 1f;
            spawnHitParticles();
        }
        lastHealth = rawHp;
        if (hitFlash > 0) hitFlash = Math.max(0f, hitFlash - 0.06f * (dt * 60f));

        float maxHp = Math.max(1f, entity.getMaxHealth());
        float absorb = Math.max(0f, entity.getAbsorptionAmount());

        healthAnimation = MathHelper.clamp(MathUtils.interpolate(healthAnimation, rawHp / maxHp, 0.1f * (dt * 60f)), 0f, 1f);

        // Логика предметов
        List<ItemStack> allItems = new ArrayList<>();
        if (entity instanceof PlayerEntity player) {
            List<ItemStack> armor = player.getInventory().armor;
            for (int i = armor.size() - 1; i >= 0; i--) if (!armor.get(i).isEmpty()) allItems.add(armor.get(i));
            if (!player.getMainHandStack().isEmpty()) allItems.add(player.getMainHandStack());
            if (!player.getOffHandStack().isEmpty())  allItems.add(player.getOffHandStack());
        }

        float itemSize = scaled(8f);
        float itemGap = scaled(0.5f);
        float itemsRowW = allItems.isEmpty() ? 0f : allItems.size() * (itemSize + itemGap) - itemGap;

        String name = entity.getName().getString();
        float nameW = Fonts.BOLD.getWidth(name, scaled(6f));
        String hpPct = (int)(healthAnimation * 100) + "%";
        float hpPctW = Fonts.MEDIUM.getWidth(hpPct, scaled(5.5f));

        float contentW = Math.max(nameW + scaled(2f) + hpPctW, itemsRowW);
        float totalW = PAD + HEAD_SIZE + PAD + contentW + PAD;
        float innerH = Math.max(HEAD_SIZE, scaled(6f) + GAP + itemSize);
        float totalH = PAD + innerH + PAD + BAR_H + scaled(2f);

        float x = getX();
        float y = getY();
        setBounds(x, y, totalW, totalH);

        updateParticles(dt);
        renderParticles(e.getContext().getMatrices(), anim);

        e.getContext().getMatrices().push();
        // Настройки стиля теперь влияют на фон
        boolean isCard = style.getName("targethud.style.card").getValue();
        float rounding = isCard ? 2f : scaled(5f);

        Color accent = themeManager.getCurrentTheme().getAccentColor();
        Color background = new Color(
                Math.min(255, (int)(18 + accent.getRed()   * 0.10f)),
                Math.min(255, (int)(22 + accent.getGreen() * 0.10f)),
                Math.min(255, (int)(28 + accent.getBlue()  * 0.10f)),
                (int)(220 * anim)
        );

        Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, totalW, totalH, rounding, background);

        float headX = x + PAD;
        float headY = y + PAD + (innerH - HEAD_SIZE) / 2f;

        Color headTint;
        if (hitFlash > 0) {
            int red = (int)(255 * hitFlash);
            headTint = new Color(255, Math.max(0, 255 - red), Math.max(0, 255 - red), fa);
        } else {
            headTint = new Color(255, 255, 255, fa);
        }

        if (entity instanceof PlayerEntity player) {
            Render2D.drawTexture(e.getContext().getMatrices(), headX, headY, HEAD_SIZE, HEAD_SIZE, 3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) player).getSkinTextures().texture(), headTint);
        } else {
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(scaled(6f)), "?",
                    headX + HEAD_SIZE / 2f - Fonts.BOLD.getWidth("?", scaled(6f)) / 2f,
                    headY + HEAD_SIZE / 2f - Fonts.BOLD.getHeight(scaled(6f)) / 2f, headTint);
        }

        float cx = headX + HEAD_SIZE + PAD;
        float contentH = scaled(6f) + GAP + itemSize;
        float topY = y + PAD + (innerH - contentH) / 2f;

        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(scaled(6f)), name, cx, topY, new Color(255, 255, 255, fa));

        float hpPctX = x + totalW - PAD - hpPctW;
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(scaled(5.5f)), hpPct, hpPctX, topY + scaled(6f) / 2f - scaled(5.5f) / 2f,
                new Color(200, 200, 200, fa));

        float itemsY = topY + scaled(6f) + GAP;
        if (!allItems.isEmpty()) {
            float itemScale = itemSize / 16f;
            for (int i = 0; i < allItems.size(); i++) {
                float ix = cx + i * (itemSize + itemGap);
                e.getContext().getMatrices().push();
                e.getContext().getMatrices().translate(ix, itemsY, 150f);
                e.getContext().getMatrices().scale(itemScale, itemScale, 1f);
                DiffuseLighting.disableGuiDepthLighting();
                RenderSystem.setShaderColor(1f, 1f, 1f, anim);
                e.getContext().drawItem(allItems.get(i), 0, 0);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                e.getContext().getMatrices().pop();
            }
        }

        float barY = y + totalH - BAR_H - scaled(3f);
        float barX = x + scaled(3f);
        float barW = totalW - scaled(6f);

        Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barW, BAR_H, BAR_H / 2f, new Color(0, 0, 0, (int)(60 * anim)));
        
        float fillW = barW * healthAnimation;
        if (fillW > 0) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, fillW, BAR_H, BAR_H / 2f, accent);
        }

        // Поглощение (опционально)
        if (displayAbsorption.getValue() && absorb > 0) {
            float absW = barW * Math.min(1f, absorb / maxHp);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, absW, BAR_H, BAR_H / 2f, absorbColor);
        }

        e.getContext().getMatrices().pop();
        super.onRender2D(e);
    }

    private void spawnHitParticles() {
        if (!displayHudParticles.getValue()) return;
        float headX = getX() + PAD;
        float headY = getY() + PAD;
        for (int i = 0; i < 5; i++) {
            particles.add(new HitParticle(
                    headX + random.nextFloat() * HEAD_SIZE,
                    headY + random.nextFloat() * HEAD_SIZE));
        }
    }

    private void updateParticles(float dt) {
        particles.removeIf(p -> { p.update(dt); return p.isDead(); });
    }

    private void renderParticles(net.minecraft.client.util.math.MatrixStack ms, float anim) {
        if (!displayHudParticles.getValue()) return;
        for (HitParticle p : particles) p.render(ms, anim, themeManager.getCurrentTheme().getAccentColor());
    }

    private static class HitParticle {
        private float x, y;
        private float vx, vy;
        private float life;
        private final float maxLife;
        private final float size;

        HitParticle(float x, float y) {
            this.x = x; this.y = y;
            double a = Math.random() * Math.PI * 2;
            float spd = 0.1f + (float)Math.random() * 0.3f;
            vx = (float)Math.cos(a) * spd;
            vy = (float)Math.sin(a) * spd;
            maxLife = 60f + (float)Math.random() * 40f;
            life = maxLife;
            size = 1f + (float)Math.random() * 1.2f;
        }

        void update(float dt) {
            x += vx; y += vy;
            vx *= 0.95f; vy *= 0.95f;
            life -= (dt * 60f);
        }

        void render(net.minecraft.client.util.math.MatrixStack ms, float anim, Color primary) {
            float alpha = (life / maxLife) * anim * 0.8f;
            if (alpha <= 0) return;
            // Используем стандартный белый или акцент для частиц
            Color c = new Color(255, 255, 255, (int)(255 * alpha));
            Render2D.drawRoundedRect(ms, x - size / 2f, y - size / 2f, size, size, size / 2f, c);
            
            Color glow = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), (int)(50 * alpha));
            Render2D.drawRoundedRect(ms, x - size, y - size, size * 2f, size * 2f, size, glow);
        }
}