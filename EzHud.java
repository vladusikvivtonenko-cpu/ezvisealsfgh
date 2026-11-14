package com.ezvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;

import java.util.*;

public class EzHud {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || mc.currentScreen instanceof EzScreen) return;
        EzConfig k = EzConfig.I;
        TextRenderer t = mc.textRenderer;
        int acc = EzConfig.accent();
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();

        if (!k.lowEnd && k.weather > 0) weather(c, w, h, k);
        if (!k.lowEnd && k.bloodVisuals) blood(c, w, h);
        if (k.itemGlow) itemGlow(c, mc, w, h);

        if (k.topBar) {
            renderWatermark(c, mc, t, acc);
        }

        int y = h / 2 - 40;
        if (k.armorHud) y = armor(c, mc, t, y, acc);
        if (k.totemHelper) y = totems(c, mc, t, y);
        if (k.cooldownsHud) cooldowns(c, mc, t, y + 6, acc);
        if (k.potionHud) potions(c, mc, t, w);
        if (k.nearbyPlayers) nearby(c, mc, t, acc);
        if (k.lookSlots && EzVisuals.lookSlotsEnabled && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity target) {
            lookSlots(c, mc, t, target, acc);
        }
        if (k.fullBridge && EzVisuals.bridgeEnabled) bridgeGuide(c, mc, t, w, h, acc);
        if (k.swimAnimation && mc.player.isSwimming()) swimIndicator(c, t, w, h, k.swimAmount, acc);

        if (System.currentTimeMillis() < EzVisuals.toastUntil) {
            int tw = t.getWidth(EzVisuals.toast);
            c.drawText(t, EzVisuals.toast, (w - tw) / 2, h - 60, 0xFFFF5555, true);
        }
    }

    private static void renderWatermark(DrawContext c, MinecraftClient mc, TextRenderer t, int acc) {
        int ping = getPing(mc);
        int fps = mc.getCurrentFps();

        String brand = "EzVisuals";
        String sep = "  /  ";
        String pingStr = ping + " ms";
        String fpsStr = fps + " FPS";

        int iconW = 14;
        int gap = 5;
        int padX = 7;
        int padY = 5;

        int textW = t.getWidth(brand) + t.getWidth(sep) * 2 + t.getWidth(pingStr) + t.getWidth(fpsStr);
        int boxW = padX * 2 + iconW + gap + textW;
        int boxH = padY * 2 + t.fontHeight;

        int x = 6;
        int yTop = 6;

        c.fill(x, yTop, x + boxW, yTop + boxH, 0xD80D0D14);
        c.fill(x, yTop, x + boxW, yTop + 1, 0x25FFFFFF);
        c.fill(x, yTop + boxH - 1, x + boxW, yTop + boxH, 0x60000000);

        int pulseColor = 0xFF5B5CE6;
        drawPulseIcon(c, x + padX, yTop + boxH / 2, pulseColor);

        int curX = x + padX + iconW + gap;
        int textY = yTop + padY + 1;

        c.drawText(t, brand, curX, textY, 0xFFFFFFFF, false);
        curX += t.getWidth(brand);

        c.drawText(t, sep, curX, textY, 0xFF6E6E7A, false);
        curX += t.getWidth(sep);

        c.drawText(t, pingStr, curX, textY, 0xFFE5E5EA, false);
        curX += t.getWidth(pingStr);

        c.drawText(t, sep, curX, textY, 0xFF6E6E7A, false);
        curX += t.getWidth(sep);

        c.drawText(t, fpsStr, curX, textY, 0xFFE5E5EA, false);

        if (EzConfig.I.pvpSafe && EzVisuals.inCombat()) {
            long left = 8000 - (System.currentTimeMillis() - EzVisuals.lastCombatMs);
            c.drawText(t, "PvP Safe: combat " + (left / 1000 + 1) + "s", x, yTop + boxH + 4, 0xFFFF5555, true);
        }
    }

    private static int getPing(MinecraftClient mc) {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return 0;
    }

    private static void drawPulseIcon(DrawContext c, int x, int midY, int col) {
        int glow = (col & 0x00FFFFFF) | 0x40000000;
        c.fill(x, midY - 1, x + 14, midY + 2, glow);
        c.fill(x, midY, x + 3, midY + 1, col);
        c.fill(x + 3, midY - 2, x + 4, midY, col);
        c.fill(x + 4, midY - 4, x + 5, midY - 2, col);
        c.fill(x + 5, midY - 3, x + 6, midY + 1, col);
        c.fill(x + 6, midY + 1, x + 7, midY + 4, col);
        c.fill(x + 7, midY + 1, x + 8, midY + 3, col);
        c.fill(x + 8, midY - 1, x + 9, midY + 1, col);
        c.fill(x + 9, midY, x + 14, midY + 1, col);
    }

    static int armorPercent(PlayerEntity p) {
        int tot = 0, max = 0;
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = p.getEquippedStack(s);
            if (!st.isEmpty() && st.isDamageable()) {
                tot += st.getMaxDamage() - st.getDamage();
                max += st.getMaxDamage();
            }
        }
        return max == 0 ? 0 : tot * 100 / max;
    }

    private static int pctColor(int pc) {
        return pc > 60 ? 0xFF55FF55 : pc > 30 ? 0xFFFFC83D : 0xFFFF5555;
    }

    private static int armor(DrawContext c, MinecraftClient mc, TextRenderer t, int y, int acc) {
        c.fill(4, y - 4, 70, y + 4 * 18 + 14, 0x90101018);
        c.fill(4, y - 4, 6, y + 4 * 18 + 14, acc);
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) {
                c.drawItem(st, 10, y);
                if (st.isDamageable()) {
                    int pc = (st.getMaxDamage() - st.getDamage()) * 100 / st.getMaxDamage();
                    c.drawText(t, pc + "%", 30, y + 4, pctColor(pc), true);
                }
            }
            y += 18;
        }
        c.drawText(t, "Armor " + armorPercent(mc.player) + "%", 10, y + 2, acc, true);
        return y + 18;
    }

    private static int totems(DrawContext c, MinecraftClient mc, TextRenderer t, int y) {
        PlayerInventory inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) n += 1;
        }
        boolean off = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        c.drawText(t, "Totems: " + n, 8, y, n > 0 ? 0xFFFFFFFF : 0xFFFF5555, true);
        if (n > 0 && !off) c.drawText(t, "Offhand: no totem!", 8, y + 10, 0xFFFF5555, true);
        return y + 24;
    }

    private static void cooldowns(DrawContext c, MinecraftClient mc, TextRenderer t, int y, int acc) {
        PlayerInventory inv = mc.player.getInventory();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !seen.add(s.getItem())) continue;
            float p = mc.player.getItemCooldownManager().getCooldownProgress(s, 0f);
            if (p <= 0f) continue;
            c.drawItem(s, 6, y);
            c.fill(26, y + 6, 66, y + 10, 0xFF2A2A35);
            c.fill(26, y + 6, 26 + (int) (p * 40), y + 10, acc);
            c.drawText(t, (int) (p * 100) + "%", 70, y + 4, 0xFFDDDDDD, true);
            y += 18;
        }
    }

    private static void potions(DrawContext c, MinecraftClient mc, TextRenderer t, int w) {
        int y = 40;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString();
            boolean beneficial = e.getEffectType().value().isBeneficial();
            if (EzConfig.I.potionBeneficialOnly && !beneficial) continue;
            String time;
            if (e.isInfinite()) time = "INF";
            else {
                int seconds = Math.max(0, e.getDuration() / 20);
                time = EzConfig.I.potionTimeFormat == 1 ? seconds + "s" : String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
            }
            String line = name + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "") + "  " + time;
            int tw = t.getWidth(line);
            if (EzConfig.I.potionBackground) c.fill(w - tw - 14, y - 2, w - 4, y + 11, 0x90101018);
            c.drawText(t, line, w - tw - 8, y + 1, beneficial ? 0xFF75F59A : 0xFFFF657A, true);
            y += 15;
        }
    }

    private static void nearby(DrawContext c, MinecraftClient mc, TextRenderer t, int acc) {
        List<PlayerEntity> list = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.distanceTo(mc.player) < 64) list.add(p);
        }
        list.sort(Comparator.comparingDouble(p -> p.distanceTo(mc.player)));
        int y = 40;
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            PlayerEntity p = list.get(i);
            String s = p.getName().getString() + "  " + (int) Math.ceil(p.getHealth()) + "HP  " + (EzConfig.I.armorNameTags ? armorPercent(p) + "% armor  " : "") + (int) p.distanceTo(mc.player) + "m";
            c.fill(6, y - 2, 6 + t.getWidth(s) + 8, y + 10, 0x90101018);
            c.drawText(t, s, 10, y, acc, true);
            y += 14;
        }
    }

    private static void itemGlow(DrawContext c, MinecraftClient mc, int w, int h) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            int col;
            if (s.isIn(ItemTags.SWORDS)) col = 0xFF4040;
            else if (s.isIn(ItemTags.PICKAXES)) col = 0x40E0FF;
