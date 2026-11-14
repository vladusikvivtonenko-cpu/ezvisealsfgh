package com.ezvisuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;

public class EzPredict {

    public static void renderWorld(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) stack = mc.player.getOffHandStack();
        if (stack.isEmpty()) return;

        String name = stack.getName().getString().toLowerCase();
        Item item = stack.getItem();

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Vec3d playerPos = mc.player.getPos();

        // 1. ДЕЗОРІЄНТАЦІЯ (Око Ендера / назва "дезориентация") - радіус 10 блоків
        if (name.contains("дезориентац") || name.contains("дезорієнтац") || item == Items.ENDER_EYE) {
            drawCircle(matrices, playerPos.add(0, 0.1, 0), 10.0, 0xFFAA00FF, 64);
        }
        // 2. ЯВНА ПИЛЬ (Цукор / Світлопил / назва "явная пыль") - радіус 10 блоків
        else if (name.contains("явная пыль") || name.contains("явна пиль") || item == Items.SUGAR || item == Items.GLOWSTONE_DUST) {
            drawCircle(matrices, playerPos.add(0, 0.1, 0), 10.0, 0xFFFFFF55, 64);
        }
        // 3. БОЖА АУРА (Фантомна мембрана / Сльоза гаста / назва "божья аура") - радіус 5 блоків
        else if (name.contains("божья аура") || name.contains("божа аура") || item == Items.PHANTOM_MEMBRANE || item == Items.GHAST_TEAR) {
            drawCircle(matrices, playerPos.add(0, 0.1, 0), 5.0, 0xFF55FFFF, 48);
        }
        // 4. ВОГНЯНИЙ СМЕРЧ (Вогняний заряд / Стрижень іфрита / назва "огненный смерч") - радіус 10 блоків
        else if (name.contains("огненный смерч") || name.contains("вогняний смерч") || item == Items.FIRE_CHARGE || item == Items.BLAZE_ROD) {
            drawCircle(matrices, playerPos.add(0, 0.1, 0), 10.0, 0xFFFF5500, 64);
        }
        // 5. ТРАПКА (Незеритовий лом / назва "трапка") - бокс 3x4x3 навколо гравця
        else if (name.contains("трапка") || item == Items.NETHERITE_SCRAP) {
            BlockPos bp = mc.player.getBlockPos();
            Box trapBox = new Box(bp.getX() - 1.0, bp.getY() - 1.0, bp.getZ() - 1.0,
                                  bp.getX() + 2.0, bp.getY() + 3.0, bp.getZ() + 2.0);
            drawBoxOutline(matrices, trapBox, 0xFFFF2244);
        }
        // 6. ПЛАСТ (Висушена ламінарія / Цегла / назва "пласт") - платформа 5x1x5 або стіна 5x5x1 по погляду
        else if (name.contains("пласт") || item == Items.DRIED_KELP) {
            Box plastBox = getPlastBox(mc.player);
            drawBoxOutline(matrices, plastBox, 0xFF33AAFF);
        }
        // 7. ТРИЗУБЕЦЬ - траєкторія кидка
        else if (item == Items.TRIDENT) {
            renderTrajectory(matrices, mc.player, 2.5, 0.05, 0.99, 0.0, 0xFF00E5FF);
        }
        // 8. АРБАЛЕТ КРУШИТЕЛЯ / ЗВИЧАЙНИЙ АРБАЛЕТ - траєкторія болта
        else if (item == Items.CROSSBOW) {
            double vel = CrossbowItem.isCharged(stack) ? 3.15 : 3.15;
            int col = name.contains("крушител") ? 0xFFFF0055 : 0xFFFFAA00;
            renderTrajectory(matrices, mc.player, vel, 0.05, 0.99, 0.0, col);
        }
        // 9. ЛУК - траєкторія стріли з урахуванням натягу
        else if (item == Items.BOW) {
            int useTicks = mc.player.isUsingItem() ? stack.getMaxUseTime(mc.player) - mc.player.getItemUseTimeLeft() : 20;
            float pull = BowItem.getPullProgress(useTicks);
            if (pull < 0.1f) pull = 1.0f;
            renderTrajectory(matrices, mc.player, pull * 3.0, 0.05, 0.99, 0.0, 0xFF55FF55);
        }
        // 10. ЕНДЕР-ПЕРЛ (ПЕРКА) - точна дуга телепортації
        else if (item == Items.ENDER_PEARL) {
            renderTrajectory(matrices, mc.player, 1.5, 0.03, 0.99, 0.0, 0xFFCC44FF);
        }
        // 11. ВИБУХОВІ ТА ОСІДАЮЧІ ЗІЛЛЯ - дуга + радіус сплешу 4.0 блоки в точці падіння
        else if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            renderTrajectory(matrices, mc.player, 0.5, 0.05, 0.99, 4.0, 0xFFFF66CC);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static Box getPlastBox(PlayerEntity p) {
        Vec3d look = p.getRotationVector();
        Vec3d target = p.getEyePos().add(look.multiply(3.5));
        BlockPos c = BlockPos.ofFloored(target);
        // Якщо дивимось вгору/вниз — горизонтальний пласт 5x1x5, інакше вертикальна стіна 5x5
        if (Math.abs(p.getPitch()) > 50.0f) {
            return new Box(c.getX() - 2, c.getY(), c.getZ() - 2, c.getX() + 3, c.getY() + 1, c.getZ() + 3);
        } else {
            Direction dir = p.getHorizontalFacing();
            if (dir.getAxis() == Direction.Axis.X) {
                return new Box(c.getX(), c.getY() - 2, c.getZ() - 2, c.getX() + 1, c.getY() + 3, c.getZ() + 3);
            } else {
                return new Box(c.getX() - 2, c.getY() - 2, c.getZ(), c.getX() + 3, c.getY() + 3, c.getZ() + 1);
            }
        }
    }

    private static void renderTrajectory(MatrixStack matrices, PlayerEntity p, double velocity, double gravity, double drag, double splashRadius, int color) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float yaw = p.getYaw();
        float pitch = p.getPitch();

        double x = p.getX() - Math.cos(Math.toRadians(yaw)) * 0.16;
        double y = p.getEyeY() - 0.1;
        double z = p.getZ() - Math.sin(Math.toRadians(yaw)) * 0.16;

        double vx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * velocity;
        double vy = -Math.sin(Math.toRadians(pitch)) * velocity;
        double vz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * velocity;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        Vec3d hitPos = null;

        for (int i = 0; i < 150; i++) {
            buf.vertex(mat, (float) x, (float) y, (float) z).color(r, g, b, a);

            Vec3d curr = new Vec3d(x, y, z);
            Vec3d next = new Vec3d(x + vx, y + vy, z + vz);

            var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                    curr, next,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    p
            ));

            if (hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
                hitPos = hit.getPos();
                buf.vertex(mat, (float) hitPos.x, (float) hitPos.y, (float) hitPos.z).color(r, g, b, a);
                break;
            }

            x += vx;
            y += vy;
            z += vz;
            vx *= drag;
            vy = vy * drag - gravity;
            vz *= drag;
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        if (hitPos != null) {
            Box marker = new Box(hitPos.x - 0.25, hitPos.y - 0.05, hitPos.z - 0.25,
                                 hitPos.x + 0.25, hitPos.y + 0.05, hitPos.z + 0.25);
            drawBoxOutline(matrices, marker, color);
            if (splashRadius > 0) {
                drawCircle(matrices, hitPos.add(0, 0.05, 0), splashRadius, color, 48);
            }
        }
    }

    private static void drawCircle(MatrixStack matrices, Vec3d center, double radius, int color, int segments) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float px = (float) (center.x + Math.cos(angle) * radius);
            float pz = (float) (center.z + Math.sin(angle) * radius);
            buf.vertex(mat, px, (float) center.y, pz).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void drawBoxOutline(MatrixStack matrices, Box box, int color) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Нижній контур
