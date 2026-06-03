package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;

import java.awt.*;

public class HotbarHUD extends HudElement {

    private final ThemeManager themeManager;

    // Visual constants similar to vanilla layout
    private final float barWidth = 180f;
    private final float barHeight = 20f;
    private final float slotSize = 20f;
    private final float radius = 2f;
    private float xpBarHeight = 6f; // thickness of XP bar

    // Reused colors to avoid per-frame allocations
    private static final Color BACKGROUND = new Color(30, 30, 30, 240);
    private static final Color HP_BG = new Color(0, 0, 0, 120);
    private static final Color HP_FG = new Color(220, 65, 65, 220);
    private static final Color ABSORB_FG = new Color(245, 198, 82, 225);
    private static final Color FOOD_BG = new Color(0, 0, 0, 120);
    private static final Color FOOD_FG = new Color(240, 180, 60, 220);
    private static final Color XP_BG = new Color(0, 0, 0, 120);
    private static final Color XP_FG = new Color(80, 200, 120, 220);
    private static final Color AIR_BG = new Color(0, 0, 0, 120);
    private static final Color AIR_FG = new Color(110, 190, 255, 220);

    // Animations
    private final InfinityAnimation hpAnimPx = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation absorptionAnimPx = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation foodAnimPx = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation xpAnimPx = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation airAnimPx = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation selAnimX = new InfinityAnimation(Easing.OUT_QUAD);
    private float lastSelX = -1f;

    public HotbarHUD() {
        super("Hotbar");
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (mc.player != null && mc.player.isSpectator()) return;

        var context = e.getContext();
        var matrices = context.getMatrices();
        float baseX = getX();
        float baseY = getY();

        // Watermark background color: same as in Watermark (30,30,30,240)
        Color background = BACKGROUND;
        // Selected color from current theme accent color
        Color selected = themeManager.getCurrentTheme().getAccentColor();

        // Update bounds lazily (safe after window is ready)
        setBounds(baseX, baseY, barWidth, barHeight);

        // Background bar
        Render2D.drawRoundedRect(
                matrices,
                getX(), getY(),
                barWidth, barHeight,
                radius,
                background
        );

        // --- Custom compact bars (HP, Hunger split side-by-side, XP below) ---
        if (mc.player != null && !mc.player.getAbilities().creativeMode) {
            float splitGap = 8f;     // gap between HP and Hunger halves
            float barH = 10.5f;      // thicker bars (+0.5)
            this.xpBarHeight = barH; // XP thickness matches HP

            float x = baseX;
            float w = barWidth;
            float halfW = (w - splitGap) / 2f;
            float rightX = x + halfW + splitGap;

            // Same Y for HP and Hunger (baseline for stacking extra bars)
            float barsY = baseY - (barH + 6f + xpBarHeight); // a bit above hotbar, with space for XP below

            float health = mc.player.getHealth();
            float maxHealth = mc.player.getMaxHealth();
            float absorption = mc.player.getAbsorptionAmount();
            boolean hasAbsorption = absorption > 0f;
            boolean hasAirBar = mc.player.isSubmergedInWater() || mc.player.getAir() < mc.player.getMaxAir();

            float stackTopLeft = barsY;

            if (hasAirBar) {
                float airGap = 6f;
                float airHeight = barH;
                float airWidth = halfW;
                float airY = barsY - (airHeight + airGap);
                float airX = rightX;
                int air = mc.player.getAir();
                int maxAir = mc.player.getMaxAir();
                float airPct = Math.max(0f, Math.min(1f, air / (float) Math.max(1, maxAir)));
                Render2D.drawRoundedRect(matrices, airX, airY, airWidth, airHeight, 2f, AIR_BG);
                float airTargetPx = airWidth * airPct;
                float airFill = Math.max(0f, Math.min(airWidth, airAnimPx.animate(airTargetPx, 90)));
                Render2D.drawRoundedRect(matrices, airX, airY, airFill, airHeight, 2f, AIR_FG);

                String airTxt = String.format("%.1fs", air / 20f);
                float airSize = 7f;
                var airFont = Fonts.REGULAR.getFont(airSize);
                float airTxtW = Fonts.REGULAR.getWidth(airTxt, airSize);
                float airTxtH = Fonts.REGULAR.getHeight(airSize);
                float airTxtX = airX + (airWidth - airTxtW) / 2f;
                float airTxtY = airY + (airHeight - airTxtH) / 2f + 0.3f;
                Render2D.drawFont(matrices, airFont, airTxt, airTxtX, airTxtY, new Color(220, 240, 255));
            } else {
                airAnimPx.animate(0f, 90);
            }

            if (hasAbsorption) {
                float absGap = 4f;
                float absorbY = stackTopLeft - (barH + absGap);
                float maxAbsorptionHalfHearts = 20f;
                float absorptionHalfHearts = Math.min(maxAbsorptionHalfHearts, Math.max(0f, absorption));
                float absorptionPct = Math.min(1f, Math.max(0f, absorptionHalfHearts / maxAbsorptionHalfHearts));
                Render2D.drawRoundedRect(matrices, x, absorbY, halfW, barH, 2f, HP_BG);
                float absorbTargetPx = halfW * absorptionPct;
                float absorbFill = Math.max(0f, Math.min(halfW, absorptionAnimPx.animate(absorbTargetPx, 60)));
                Render2D.drawRoundedRect(matrices, x, absorbY, absorbFill, barH, 2f, ABSORB_FG);

                int maxAbsorbHalfHeartsInt = (int) maxAbsorptionHalfHearts;
                int absorbHalfHearts = Math.min(maxAbsorbHalfHeartsInt, Math.max(1, Math.round(absorptionHalfHearts)));
                String absorbTxt = absorbHalfHearts + "/" + maxAbsorbHalfHeartsInt;
                float absorbSize = 8f;
                var absorbFont = Fonts.REGULAR.getFont(absorbSize);
                float absorbTxtW = Fonts.REGULAR.getWidth(absorbTxt, absorbSize);
                float absorbTxtH = Fonts.REGULAR.getHeight(absorbSize);
                float absorbTxtX = x + (halfW - absorbTxtW) / 2f;
                float absorbTxtY = absorbY + (barH - absorbTxtH) / 2f + 0.2f;
                Render2D.drawFont(matrices, absorbFont, absorbTxt, absorbTxtX, absorbTxtY, Color.WHITE);

                stackTopLeft = absorbY;
            } else {
                absorptionAnimPx.animate(0f, 60);
            }

            // Health progress (left half)
            float hpPct = Math.min(1f, Math.max(0f, health / Math.max(1f, maxHealth)));
            Render2D.drawRoundedRect(matrices, x, barsY, halfW, barH, 2f, HP_BG);
            float hpTargetPx = halfW * hpPct;
            float hpFill = Math.max(0f, Math.min(halfW, hpAnimPx.animate(hpTargetPx, 60)));
            Render2D.drawRoundedRect(matrices, x, barsY, hpFill, barH, 2f, HP_FG);
            int hpCur = (int) Math.floor(health);
            int hpMax = (int) Math.floor(maxHealth);
            String hpTxt = hpCur + "/" + hpMax;
            float hpTxtSize = 8.5f;
            var fontHp = Fonts.REGULAR.getFont(hpTxtSize);
            float hpTxtW = Fonts.REGULAR.getWidth(hpTxt, hpTxtSize);
            float hpTxtH = Fonts.REGULAR.getHeight(hpTxtSize);
            float hpTxtX = x + (halfW - hpTxtW) / 2f;
            float hpTxtY = barsY + (barH - hpTxtH) / 2f + 0.3f;
            Render2D.drawFont(matrices, fontHp, hpTxt, hpTxtX, hpTxtY, Color.WHITE);

            // Hunger progress (right half)
            float hunger = mc.player.getHungerManager().getFoodLevel(); // 0..20
            float foodPct = Math.min(1f, Math.max(0f, hunger / 20f));
            Render2D.drawRoundedRect(matrices, rightX, barsY, halfW, barH, 2f, FOOD_BG);
            float foodTargetPx = halfW * foodPct;
            float foodFill = Math.max(0f, Math.min(halfW, foodAnimPx.animate(foodTargetPx, 110)));
            float saturation = mc.player.getHungerManager().getSaturationLevel();
            Render2D.drawRoundedRect(matrices, rightX, barsY, foodFill, barH, 2f, FOOD_FG);
            int foodCur = (int) hunger;
            int foodMax = 20;
            String foodTxt = foodCur + "/" + foodMax;
            if (saturation > 0f) {
                foodTxt += String.format(" (%.1f)", saturation);
            }
            float foodTxtSize = 8.5f;
            var fontFood = Fonts.REGULAR.getFont(foodTxtSize);
            float foodTxtW = Fonts.REGULAR.getWidth(foodTxt, foodTxtSize);
            float foodTxtH = Fonts.REGULAR.getHeight(foodTxtSize);
            float foodTxtX = rightX + (halfW - foodTxtW) / 2f;
            float foodTxtY = barsY + (barH - foodTxtH) / 2f + 0.3f;
            Render2D.drawFont(matrices, fontFood, foodTxt, foodTxtX, foodTxtY, Color.WHITE);

            // XP progress + level text (below split bars)
            float xp = mc.player.experienceProgress; // 0..1
            int level = mc.player.experienceLevel;
            float xpY = barsY + barH + 4f;
            Render2D.drawRoundedRect(matrices, x, xpY, w, xpBarHeight, 2f, XP_BG);
            float xpTargetPx = w * Math.min(1f, Math.max(0f, xp));
            float xpFill = Math.max(0f, Math.min(w, xpAnimPx.animate(xpTargetPx, 110)));
            Render2D.drawRoundedRect(matrices, x, xpY, xpFill, xpBarHeight, 2f, XP_FG);
            String lvl = String.valueOf(level);
            float lvlSize = 8.5f;
            var fontLvl = Fonts.REGULAR.getFont(lvlSize);
            float lvlW = Fonts.REGULAR.getWidth(lvl, lvlSize);
            float lvlH = Fonts.REGULAR.getHeight(lvlSize);
            float lvlX = x + (w - lvlW) / 2f;
            float lvlY = xpY + (xpBarHeight - lvlH) / 2f + 0.3f;
            Render2D.drawFont(matrices, fontLvl, lvl, lvlX, lvlY, Color.WHITE);
        }

        // Selected slot highlight (uses player's current slot)
        if (mc.player != null) {
            var inv = mc.player.getInventory();
            int slot = inv.selectedSlot; // 0..8
            // Side gap so 9*20 fits inside barWidth
            float sideGap = (barWidth - slotSize * 9f) / 2f;
            // Make highlight larger: cover full slot area, aligned with inner grid
            float slotXTarget = baseX + sideGap + slot * slotSize;
            float slotY = baseY;
            float slotW = slotSize;
            float slotH = barHeight;

            // Use small uniform rounding to keep shape pleasant and avoid inner gaps
            float selRadius = 2f;
            // Height exactly as background
            float selY = slotY;
            float selH = slotH;
            // Default X spans the slot
            // Animate selection X between slots
            if (lastSelX < 0f) lastSelX = slotXTarget;
            float selX = selAnimX.animate(slotXTarget, 140);
            lastSelX = selX;
            float selW = (slot == 8) ? Math.max(slotW, getX() + barWidth - slotXTarget) : slotW;

            // Soft fill
            Render2D.drawRoundedRect(
                    matrices,
                    selX, selY,
                    selW, selH,
                    selRadius,
                    new Color(selected.getRed(), selected.getGreen(), selected.getBlue(), 160)
            );
            // No glow as requested

            boolean isMainHandLeft = mc.player.getMainArm().equals(net.minecraft.util.Arm.LEFT);
            float offhandX = isMainHandLeft ? baseX - slotSize - 4f : baseX + barWidth + 4f;

            var offhandStack = inv.getStack(40);
            context.drawItem(offhandStack, (int) offhandX, (int) baseY);
            if (!offhandStack.isEmpty()) {
                String offhandCnt = String.valueOf(offhandStack.getCount());
                float offhandCntSize = 7.5f;
                var offhandCntFont = Fonts.REGULAR.getFont(offhandCntSize);
                float offhandCntW = Fonts.REGULAR.getWidth(offhandCnt, offhandCntSize);
                float offhandCntH = Fonts.REGULAR.getHeight(offhandCntSize);
                float offhandCntX = offhandX + slotSize - 2f - offhandCntW;
                float offhandCntY = baseY + barHeight - 2f - offhandCntH + 0.5f;

                Render2D.drawRoundedRect(
                        matrices,
                        offhandX, baseY,
                        20, 20,
                        radius,
                        background
                );

                context.drawItem(offhandStack, (int) offhandX + 2, (int) baseY + 2);

                if(offhandStack.getCount() > 1) Render2D.drawFont(matrices, offhandCntFont, offhandCnt, offhandCntX, offhandCntY, Color.WHITE);
            }
            
            // Render items aligned to inner grid with side gap
            for (int i = 0; i <= 8; i++) {
                float slotBaseX = baseX + sideGap + i * slotSize;
                float slotBaseY = baseY;
                float ix = slotBaseX + 2f;    // inner margin similar to vanilla
                float iy = slotBaseY + 2f;
                var stack = inv.getStack(i);
                context.drawItem(stack, (int) ix, (int) iy);
                // Overlay: slot number (top-left) and item count (bottom-right)
                // Slot number 1..9
                String slotNum = String.valueOf(i + 1);
                float numSize = 6.0f;
                var numFont = Fonts.REGULAR.getFont(numSize);
                float slotLeftX = slotBaseX + 2f; // align with item inner margin
                float slotTopY = slotBaseY + 2f;
                Render2D.drawFont(matrices, numFont, slotNum, slotLeftX, slotTopY - 0.5f, Color.WHITE);

                // Item count if stack not empty
                if (!stack.isEmpty()) {
                    String cnt = String.valueOf(stack.getCount());
                    float cntSize = 7.5f;
                    var cntFont = Fonts.REGULAR.getFont(cntSize);
                    float cntW = Fonts.REGULAR.getWidth(cnt, cntSize);
                    float cntH = Fonts.REGULAR.getHeight(cntSize);
                    float slotRightX = slotBaseX + slotSize - 2f; // inner margin right
                    float slotBottomY = slotBaseY + barHeight - 2f; // inner margin bottom
                    float drawX = slotRightX - cntW;
                    float drawY = slotBottomY - cntH + 0.5f;
                    if(stack.getCount() > 1) Render2D.drawFont(matrices, cntFont, cnt, drawX, drawY, Color.WHITE);
                }

                // Draw cooldown overlay & highlight for special items
                if (!stack.isEmpty()) {
                    float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickDelta(true));
                    if (cooldown > 0f) {
                        float cooldownHeight = slotSize * cooldown;
                        Render2D.drawRoundedRect(
                                matrices,
                                slotBaseX,
                                slotBaseY,
                                slotSize,
                                cooldownHeight,
                                2f,
                                new Color(0, 0, 0, 130)
                        );
                        String cdText = Math.round(cooldown * 100f) + "%";
                        float cdSize = 6.5f;
                        var cdFont = Fonts.REGULAR.getFont(cdSize);
                        float cdW = Fonts.REGULAR.getWidth(cdText, cdSize);
                        Render2D.drawFont(
                                matrices,
                                cdFont,
                                cdText,
                                slotBaseX + (slotSize - cdW) / 2f,
                                slotBaseY + slotSize + 2.5f,
                                Color.WHITE
                        );
                    }
                }
            }

        }

        super.onRender2D(e);
    }
}
