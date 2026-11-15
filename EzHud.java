package com.ezvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Extra HUD widgets: Nearby Players, Blood Vision, Swim Indicator, Bridge Guide.
 * They are normal modules (shown in the menu, saved in the config).
 * Call EzHud.init() once in EzVisualsPremium.onInitializeClient(), BEFORE Config.load("main").
 */
public final class EzHud {
    private EzHud() {}
    private static boolean inited;

    public static final Module NEARBY = new Module("nearby", "Nearby Players", "HUD",
            "List of players around you: HP, armor percent, distance", true,
            Setting.num("max", "Max players", 5, 1, 10, 1),
            Setting.num("dist", "Max distance", 64, 8, 128, 4),
            Setting.bool("hp", "Show HP", true),
            Setting.bool("armor", "Show armor %", true));

    public static final Module BLOOD = new Module("bloodvision", "Blood Vision", "Visuals",
            "Red screen edges when your HP is low or when you get hit", false,
            Setting.num("low", "Low HP at", 8, 2, 19, 1),
            Setting.num("strength", "Strength", 0.6, 0.2, 1.0, 0.1),
            Setting.bool("hurt", "Flash when hit", true));

    public static final Module SWIM = new Module("swimind", "Swim Indicator", "HUD",
            "Wave indicator with your speed while you swim", true,
            Setting.num("amount", "Wave speed", 1.0, 0.5, 2.0, 0.1));

    public static final Module BRIDGEGUIDE = new Module("bridgeguide", "Bridge Guide", "HUD",
            "Label under the crosshair while Full Bridge is on", true);

    public static void init() {
        if (inited) return;
        inited = true;
        Modules.ALL.add(NEARBY);
        Modules.ALL.add(BLOOD);
        Modules.ALL.add(SWIM);
        Modules.ALL.add(BRIDGEGUIDE);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            try {
                render(ctx);
            } catch (Throwable ex) {
                Safe.err("ezhud", ex);
            }
        });
    }

    private static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        boolean lite = Modules.AUTO_Q.enabled && mc.getCurrentFps() < Modules.AUTO_Q.get("fps").i();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        try { if (BLOOD.enabled && !lite) blood(c, mc, sw, sh); } catch (Throwable ex) { Safe.err("blood", ex); }
        try { if (NEARBY.enabled && !lite) nearby(c, mc, sw); } catch (Throwable ex) { Safe.err("nearby", ex); }
        try { if (SWIM.enabled) swim(c, mc, sw, sh); } catch (Throwable ex) { Safe.err("swim", ex); }
        try { if (BRIDGEGUIDE.enabled) bridge(c, mc, sw, sh); } catch (Throwable ex) { Safe.err("bridgeguide", ex); }
    }

    // ---------------- blood vision ----------------
    private static void blood(DrawContext c, MinecraftClient mc, int sw, int sh) {
        ClientPlayerEntity p = mc.player;
        float hp = p.getHealth();
        double low = BLOOD.get("low").d();
        float k = 0f;
        boolean lowHp = hp <= low;
        if (lowHp) k = (float) Math.min(1.0, (low - hp) / low * 0.8 + 0.25);
        if (BLOOD.get("hurt").b() && p.hurtTime > 0) k = Math.max(k, Math.min(1f, p.hurtTime / 10f * 0.8f));
        if (k <= 0f) return;
        if (lowHp) k *= 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 180.0);
        float st = BLOOD.get("strength").f();
        int layers = 12;
        int th = Math.max(2, Math.min(sw, sh) / 28);
        for (int i = 0; i < layers; i++) {
            float f = 1f - i / (float) layers;
            int a = (int) (170 * st * k * f * f);
            if (a <= 0) continue;
            int col = (Math.min(255, a) << 24) | 0xB00000;
            int o = i * th;
            c.fill(o, o, sw - o, o + th, col);
            c.fill(o, sh - o - th, sw - o, sh - o, col);
            c.fill(o, o + th, o + th, sh - o - th, col);
            c.fill(sw - o - th, o + th, sw - o, sh - o - th, col);
        }
    }

    // ---------------- nearby players ----------------
    private static void nearby(DrawContext c, MinecraftClient mc, int sw) {
        double maxD = NEARBY.get("dist").d();
        int max = NEARBY.get("max").i();
        boolean hpOn = NEARBY.get("hp").b(), armOn = NEARBY.get("armor").b();
        List<PlayerEntity> list = new ArrayList<>();
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator()) continue;
            if (pl.distanceTo(mc.player) <= maxD) list.add(pl);
        }
        if (list.isEmpty()) return;
        list.sort(Comparator.comparingDouble(o -> o.distanceTo(mc.player)));
        int n = Math.min(max, list.size());

        String[] names = new String[n], info = new String[n];
        int[] cols = new int[n];
        int w = 90;
        for (int i = 0; i < n; i++) {
            PlayerEntity pl = list.get(i);
            names[i] = pl.getName().getString();
            float hv = pl.getHealth() + pl.getAbsorptionAmount();
            int ap = U.armorPct(pl);
            StringBuilder sb = new StringBuilder();
            if (hpOn) sb.append(String.format(Locale.ROOT, "%.1f", hv)).append("  ");
            if (armOn && ap >= 0) sb.append(ap).append("%  ");
            sb.append((int) pl.distanceTo(mc.player)).append("m");
            info[i] = sb.toString();
            cols[i] = hpOn ? (hv > 14 ? 0xFF5CFF7A : hv > 7 ? 0xFFFFD34D : 0xFFFF5555) : Draw.SUB;
            w = Math.max(w, Draw.w(names[i]) + Draw.w(info[i]) + 28);
        }
        Theme t = Theme.cur();
        int h = 16 + n * 12 + 2;
        int x = sw - w - 6, y = 6;
        Draw.rrect(c, x, y, w, h, 4, 0xD00E0E14);
        Draw.hgrad(c, x + 3, y, w - 6, 1, t.accent, t.accent2);
        Draw.shadow(c, "Nearby", x + 7, y + 4, 0xFFFFFFFF);
        Draw.textR(c, String.valueOf(list.size()), x + w - 7, y + 4, Draw.SUB);
        for (int i = 0; i < n; i++) {
            int ry = y + 16 + i * 12;
            Draw.text(c, names[i], x + 7, ry, 0xFFE6E6F0);
            Draw.textR(c, info[i], x + w - 7, ry, cols[i]);
        }
    }

    // ---------------- swim indicator ----------------
    private static void swim(DrawContext c, MinecraftClient mc, int sw, int sh) {
        ClientPlayerEntity p = mc.player;
        if (!p.isSwimming()) return;
        Theme t = Theme.cur();
        float amt = SWIM.get("amount").f();
        Vec3d v = p.getVelocity();
        double sp = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
        String s = String.format(Locale.ROOT, "SWIM  %.1f b/s", sp);
        int w = Math.max(96, Draw.w(s) + 16), h = 26;
        int x = sw / 2 - w / 2, y = sh / 2 + 40;
        Draw.rrect(c, x, y, w, h, 4, 0xD00E0E14);
        Draw.hgrad(c, x + 3, y, w - 6, 1, t.accent, t.accent2);
        Draw.textC(c, s, sw / 2, y + 4, t.accent);
        double tm = System.currentTimeMillis() / 1000.0 * 3.0 * amt;
        int n = (w - 16) / 2;
        for (int i = 0; i < n; i++) {
            int yy = y + 17 + (int) Math.round(Math.sin(tm + i * 0.45) * 3);
            c.fill(x + 8 + i * 2, yy, x + 10 + i * 2, yy + 2, Draw.alpha(t.accent, 0xC0));
        }
    }

    // ---------------- bridge guide ----------------
    private static void bridge(DrawContext c, MinecraftClient mc, int sw, int sh) {
        if (!Modules.BRIDGE.enabled) return;
        boolean blocks = mc.player.getMainHandStack().getItem() instanceof BlockItem;
        String s = blocks ? "BRIDGE ON" : "BRIDGE: hold blocks";
        Draw.textC(c, s, sw / 2, sh / 2 + 14, blocks ? Theme.cur().accent : 0xFFFF5555);
    }
}
