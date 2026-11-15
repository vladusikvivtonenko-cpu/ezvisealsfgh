package com.ezvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/**
 * FunTime item zones: dust, divine aura, fire tornado (circles), trap and plast (boxes).
 * Drawn through the same screen projection as Pred.java, so it needs no render-system calls.
 * Trajectories (bow, crossbow, trident, pearl, potions) and the disorient / freeze / plast circles
 * already live in Pred.java + the "FT Helper" module.
 * EzPredict.init() is called from EzHud.init().
 */
public final class EzPredict {
    private EzPredict() {}
    private static boolean inited;

    public static final Module ZONES = new Module("zones", "Ability Zones", "Combat",
            "Circles and boxes for dust, divine aura, fire tornado, trap and plast items", true,
            Setting.bool("text", "Show radius text", true),
            Setting.num("dust", "Visible dust radius", 10, 1, 25, 0.5),
            Setting.num("aura", "Divine aura radius", 5, 1, 25, 0.5),
            Setting.num("tornado", "Fire tornado radius", 10, 1, 25, 0.5),
            Setting.bool("trap", "Trap box (3x4x3)", true),
            Setting.bool("plast", "Plast box (5x5)", true));

    public static void init() {
        if (inited) return;
        inited = true;
        Modules.ALL.add(ZONES);
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            try {
                render(ctx, tc.getTickDelta(false));
            } catch (Throwable ex) {
                Safe.err("ezpredict", ex);
            }
        });
    }

    private static boolean has(String n, String... keys) {
        for (String k : keys) if (n.contains(k)) return true;
        return false;
    }

    private static void render(DrawContext c, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!ZONES.enabled || mc.player == null || mc.world == null || mc.options.hudHidden) return;
        ClientPlayerEntity p = mc.player;
        ItemStack s = p.getMainHandStack();
        if (s.isEmpty()) s = p.getOffHandStack();
        if (s.isEmpty()) return;

        String n = s.getName().getString().toLowerCase(Locale.ROOT);
        Item it = s.getItem();
        Vec3d pos = p.getLerpedPos(td).add(0, 0.05, 0);

        if (has(n, "явная пыль", "явна пиль") || it == Items.SUGAR || it == Items.GLOWSTONE_DUST) {
            circle(c, mc, pos, ZONES.get("dust").d(), 0xFFFFFF55);
        } else if (has(n, "божья аура", "божа аура") || it == Items.PHANTOM_MEMBRANE || it == Items.GHAST_TEAR) {
            circle(c, mc, pos, ZONES.get("aura").d(), 0xFF55FFFF);
        } else if (has(n, "огненный смерч", "вогняний смерч") || it == Items.FIRE_CHARGE || it == Items.BLAZE_ROD) {
            circle(c, mc, pos, ZONES.get("tornado").d(), 0xFFFF5500);
        } else if (ZONES.get("trap").b() && (has(n, "трапка") || it == Items.NETHERITE_SCRAP)) {
            BlockPos b = p.getBlockPos();
            box(c, mc, b.getX() - 1, b.getY() - 1, b.getZ() - 1, b.getX() + 2, b.getY() + 3, b.getZ() + 2, 0xFFFF2244);
            label(c, mc, new Vec3d(b.getX() + 0.5, b.getY() + 3.3, b.getZ() + 0.5), "Trap 3x4x3", 0xFFFF2244);
        } else if (ZONES.get("plast").b() && (has(n, "пласт") || it == Items.DRIED_KELP)) {
            double[] q = plastBox(p);
            box(c, mc, q[0], q[1], q[2], q[3], q[4], q[5], 0xFF33AAFF);
        }
    }

    private static double[] plastBox(ClientPlayerEntity p) {
        Vec3d target = p.getEyePos().add(p.getRotationVector().multiply(3.5));
        BlockPos b = BlockPos.ofFloored(target);
        if (Math.abs(p.getPitch()) > 50f) {
            return new double[]{b.getX() - 2, b.getY(), b.getZ() - 2, b.getX() + 3, b.getY() + 1, b.getZ() + 3};
        }
        Direction d = p.getHorizontalFacing();
        if (d.getAxis() == Direction.Axis.X) {
            return new double[]{b.getX(), b.getY() - 2, b.getZ() - 2, b.getX() + 1, b.getY() + 3, b.getZ() + 3};
        }
        return new double[]{b.getX() - 2, b.getY() - 2, b.getZ(), b.getX() + 3, b.getY() + 3, b.getZ() + 1};
    }

    private static void circle(DrawContext c, MinecraftClient mc, Vec3d center, double r, int col) {
        Proj.circle(c, mc, center, r, Draw.alpha(col, 0xE6), 72);
        Proj.circle(c, mc, center, r * 0.97, Draw.alpha(col, 0x80), 72);
        if (ZONES.get("text").b()) {
            label(c, mc, center.add(0, 0.6, 0), "R " + String.format(Locale.ROOT, "%.1f", r), col);
        }
    }

    private static void label(DrawContext c, MinecraftClient mc, Vec3d at, String txt, int col) {
        int[] s = Proj.project(mc, at);
        if (s == null) return;
        Draw.rrect(c, s[0] - Draw.w(txt) / 2 - 3, s[1] - 2, Draw.w(txt) + 6, 12, 3, 0xC0000000);
        Draw.textC(c, txt, s[0], s[1], col);
    }

    private static void box(DrawContext c, MinecraftClient mc, double x1, double y1, double z1,
                            double x2, double y2, double z2, int col) {
        int[][] p = new int[8][];
        for (int i = 0; i < 8; i++) {
            p[i] = Proj.project(mc, new Vec3d((i & 1) == 0 ? x1 : x2, (i & 2) == 0 ? y1 : y2, (i & 4) == 0 ? z1 : z2));
        }
        int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            int[] a = p[e[0]], b = p[e[1]];
            if (a != null && b != null) Proj.line(c, a[0], a[1], b[0], b[1], Draw.alpha(col, 0xE0), 1);
        }
    }
}
