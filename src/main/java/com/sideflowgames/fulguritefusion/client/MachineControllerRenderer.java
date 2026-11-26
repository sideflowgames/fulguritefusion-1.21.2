package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.FulguriteFusion;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlock;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlockEntity;
import com.sideflowgames.fulguritefusion.machine.MachineShape;
import com.sideflowgames.fulguritefusion.machine.MachineType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Draws a formed processing machine as one open-fronted model (geo/block/<id>.geo.json, textures
// textures/block/machine/<id>_t<tier>.png + _glowmask), with its moving parts driven here so they stay in step
// with the item inside:
//   Press: the ram slams down and flattens the ingot on the bed.
//   Arc Furnace: the electrodes dip and arc into the molten crucible; the item spins in the heat.
//   Wire Extruder: the item sinks into the hopper, wire runs from the die onto the turning spool.
//   Kiln: flames flicker under the tray, the item bakes in the glow, smoke rises from the chimney.
//   Crusher: the item drops between the toothed rollers and bursts into crumbs.
//   Banbury Mixer: the rotors churn, the weight presses down on the rubber and it squishes and spreads.
// The trim colour follows the casing tier, and higher tiers work faster. Nothing is drawn until it's built.
public class MachineControllerRenderer extends LegacyGeoBlockRenderer<MachineControllerBlockEntity> {
    public MachineControllerRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new Model());
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    // ---------- Timing ----------

    static int tier(MachineControllerBlockEntity m) {
        return Math.max(1, Math.min(4, m.shownTier()));
    }

    static boolean working(MachineControllerBlockEntity m) {
        return m.getBlockState().getValue(MachineControllerBlock.WORKING);
    }

    static float time(MachineControllerBlockEntity m, float partialTick) {
        return m.getLevel() == null ? 0 : m.getLevel().getGameTime() + partialTick;
    }

    // Seconds per working cycle, before the tier speed-up.
    static float period(MachineType type) {
        return switch (type) {
            case PRESS -> 1.2f;
            case FURNACE -> 2.0f;
            case WIRE_EXTRUDER -> 1.6f;
            case KILN -> 3.0f;
            case CRUSHER -> 1.4f;
            case BANBURY_MIXER -> 1.8f;
            case LATEX_VULCANIZER -> 2.4f;
        };
    }

    static float rate(MachineControllerBlockEntity m) {
        return 1 + 0.5f * (tier(m) - 1);
    }

    // 0..1 through the current cycle, and which cycle it is.
    static double cycle(MachineControllerBlockEntity m, float time) {
        return time / 20.0 * rate(m) / period(m.type());
    }

    // Press ram: down (0..0.35), hold (..0.5), back up (..1).
    static float ramDown(float p) {
        if (p < 0.35f) return (float) Math.pow(p / 0.35f, 2);
        if (p < 0.5f) return 1;
        return 1 - (p - 0.5f) / 0.5f;
    }

    // ---------- Drawing ----------

    @Override
    protected boolean visible(MachineControllerBlockEntity machine) {
        return machine.isFormed();
    }

    // The controller is a solid block (always dark inside): light the machine from the air above it instead.
    @Override
    protected int light(MachineControllerBlockEntity machine, int packedLight) {
        Level level = machine.getLevel();
        return level == null ? packedLight : LevelRenderer.getLightCoords(level, machine.getBlockPos().above(machine.type().shape.size));
    }

    @Override
    protected void renderExtras(MachineControllerBlockEntity machine, float partialTick, PoseStack poseStack,
                                MultiBufferSource buffers, int light) {
        Level level = machine.getLevel();
        MachineShape shape = machine.type().shape;
        if (level == null) return;

        float time = time(machine, partialTick);
        boolean working = working(machine);
        double c = cycle(machine, time);
        float p = (float) (c - Math.floor(c));
        long cycleNo = (long) Math.floor(c);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(facing(machine))));
        poseStack.translate(-shape.centerRight(), 0, shape.centerBack());
        ItemStack in = machine.shownInput(), out = machine.shownOutput();
        Matrix4f m = poseStack.last().pose();
        VertexConsumer glow = buffers.getBuffer(RenderTypes.lightning());

        switch (machine.type()) {
            case PRESS -> {
                float down = working ? ramDown(p) : 0;
                float squash = working && p > 0.3f && p < 0.6f ? 1 : 0;
                ItemStack show = working ? in : out;
                item(poseStack, level, light, show, 0, 10.2f / 16, 0, 0.42f, 90, 0, 1 + 0.18f * squash, 1 - 0.55f * squash);
                if (working) cycleEvent(machine, cycleNo, p, 0.35f, () ->
                        level.playLocalSound(machine.getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25f, 0.6f, false));
                if (down > 0.95f && working && level.getRandom().nextFloat() < 0.3f) spark(level, machine, 0, 10.5f / 16, 0);
            }
            case FURNACE -> {
                ItemStack show = working ? in : out;
                float bob = working ? (float) Math.sin(time * 0.15) * 0.03f : 0;
                item(poseStack, level, 0xF000F0, show, 0, (working ? 13.5f : 11.6f) / 16 + bob, 0, 0.28f,
                        working ? 0 : 90, working ? time * 4 : 0, 1, 1);
                if (working) {
                    float drop = 1.5f + (float) Math.sin(time * 0.3) * 0.5f;
                    Random r = new Random((long) (time / 2) * 31);
                    for (int x : new int[]{-5, 5}) {
                        if (r.nextFloat() < 0.55f) {
                            float[] tip = {x / 16f, (14 - drop) / 16f, 0};
                            float[] pool = {(x * 0.5f + (r.nextFloat() - 0.5f) * 3) / 16f, 11 / 16f, (r.nextFloat() - 0.5f) * 6 / 16f};
                            ColliderRenderer.bolt(glow, m, tip, pool, r, 0.06f, 0.8f);
                        }
                    }
                }
            }
            case WIRE_EXTRUDER -> {
                if (working && !in.isEmpty()) {
                    float sink = p * 0.04f;
                    item(poseStack, level, light, in, 0, 36.5f / 16 - sink, 0, 0.32f, 0, time * 2, 1, 1);
                    // Wire from the die onto the top of the spool.
                    float[] die = {0, 16 / 16f, 1.5f / 16f}, spool = {0, 16 / 16f, -9 / 16f};
                    ColliderRenderer.ribbon(glow, m, die, spool, 0.012f, 220, 140, 70, 230);
                } else if (!out.isEmpty()) {
                    item(poseStack, level, light, out, 0, 4.2f / 16, -2.5f / 16, 0.3f, 90, 0, 1, 1);
                }
            }
            case KILN -> {
                ItemStack show = working ? in : out;
                item(poseStack, level, working ? 0xF000F0 : light, show, 0, 14.6f / 16, 0, 0.55f, 0, working ? time * 1.5f : 30, 1, 1);
                if (working && level.getRandom().nextFloat() < 0.08f) {
                    Vec3 top = world(machine, 0, 62 / 16f, 12 / 16f);
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.x, top.y, top.z, 0, 0.03, 0);
                }
                if (working && level.getRandom().nextFloat() < 0.01f) {
                    level.playLocalSound(machine.getBlockPos(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.4f, 0.8f, false);
                }
            }
            case CRUSHER -> {
                if (working && !in.isEmpty()) {
                    if (p < 0.6f) {
                        float f = p / 0.6f;
                        float y = (42 - 16 * f * f) / 16f;
                        float s = 0.24f * (1 - 0.5f * Math.max(0, (f - 0.7f) / 0.3f));
                        item(poseStack, level, light, in, 0, y, 0, s, 0, time * 6, 1, 1);
                    }
                    if (p > 0.55f && p < 0.85f && level.getRandom().nextFloat() < 0.5f) {
                        Vec3 gap = world(machine, 0, 23 / 16f, 0);
                        level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(in)), gap.x, gap.y, gap.z,
                                (level.getRandom().nextFloat() - 0.5f) * 0.1, -0.05, (level.getRandom().nextFloat() - 0.5f) * 0.1);
                    }
                    cycleEvent(machine, cycleNo, p, 0.6f, () ->
                            level.playLocalSound(machine.getBlockPos(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.5f, 0.7f, false));
                } else if (!out.isEmpty()) {
                    item(poseStack, level, light, out, 0, 9.2f / 16, -4 / 16f, 0.4f, 90, 20, 1, 1);
                }
            }
            case BANBURY_MIXER -> {
                if (working && !in.isEmpty()) {
                    // Pressed by the weight: spreads out and flattens while the ram is down, then springs back.
                    float press = ramDown(p);
                    float wobble = (float) Math.sin(time * 0.7) * 0.06f;
                    item(poseStack, level, light, in, 0, 18.0f / 16 - press * 0.6f / 16, 0, 0.22f, 90, time * 3,
                            1 + 0.35f * press + wobble, 1 - 0.5f * press);
                    cycleEvent(machine, cycleNo, p, 0.35f, () ->
                            level.playLocalSound(machine.getBlockPos(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.5f, 0.6f, false));
                    if (level.getRandom().nextFloat() < 0.04f) {
                        Vec3 top = world(machine, 0, 41 / 16f, 0);
                        level.addParticle(ParticleTypes.POOF, top.x, top.y, top.z, 0, 0.02, 0);
                    }
                } else if (!out.isEmpty()) {
                    item(poseStack, level, light, out, 0, 5.2f / 16, -11 / 16f, 0.32f, 90, 0, 1, 1);
                }
            }
            case LATEX_VULCANIZER -> {
                if (working) {
                    // Steam from the vent on top, and a hiss as each sheet comes off the drum.
                    if (level.getRandom().nextFloat() < 0.15f) {
                        Vec3 vent = world(machine, 0, 40 / 16f, 6 / 16f);
                        level.addParticle(ParticleTypes.CLOUD, vent.x, vent.y, vent.z, 0, 0.04, 0);
                    }
                    cycleEvent(machine, cycleNo, p, 0.8f, () ->
                            level.playLocalSound(machine.getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1.4f, false));
                }
                if (!out.isEmpty()) item(poseStack, level, light, out, 0, 5.2f / 16, -11.5f / 16, 0.32f, 90, 0, 1, 1);
            }
        }
        poseStack.popPose();
        ports(machine, poseStack, buffers, time);
    }

    // ---------- Port markers ----------
    // Glowing plates on the outside of the ports, so you can see where things go:
    //   Power port  (top, back):   yellow, filled centre. Put a Machine Connector on it.
    //   Input port  (top, front):  green, plus sign. A hopper above fills the machine.
    //   Output port (bottom, back): orange, minus sign, on the back face. A hopper below or a pipe beside empties it.
    private static void ports(MachineControllerBlockEntity m, PoseStack ps, MultiBufferSource buffers, float time) {
        Direction facing = facing(m);
        BlockPos base = m.getBlockPos();
        VertexConsumer c = buffers.getBuffer(RenderTypes.lightning());
        Matrix4f mat = ps.last().pose();
        int alpha = (int) (170 + 60 * Math.sin(time * 0.12));
        for (MachineShape.Part part : m.type().shape.parts(base, facing)) {
            BlockPos rel = part.pos().subtract(base);
            switch (part.role()) {
                case POWER -> marker(c, mat, rel, Direction.UP, 0.01f, 255, 210, 60, alpha, 0);
                case INPUT -> marker(c, mat, rel, Direction.UP, 0.01f, 90, 230, 110, alpha, 1);
                case OUTPUT -> {
                    // Stands off the back face, clear of the copper pipes there.
                    marker(c, mat, rel, facing.getOpposite(), 0.16f, 255, 140, 50, alpha, 2);
                    marker(c, mat, rel, Direction.DOWN, 0.01f, 255, 140, 50, alpha, 2);
                }
                default -> {}
            }
        }
    }

    // symbol: 0 = filled centre (power), 1 = plus (in), 2 = minus (out). Units are 16ths of the block face.
    private static void marker(VertexConsumer c, Matrix4f m, BlockPos rel, Direction face, float out,
                               int r, int g, int b, int a, int symbol) {
        // Frame
        rect(c, m, rel, face, out, 3, 3, 13, 4.5f, r, g, b, a);
        rect(c, m, rel, face, out, 3, 11.5f, 13, 13, r, g, b, a);
        rect(c, m, rel, face, out, 3, 4.5f, 4.5f, 11.5f, r, g, b, a);
        rect(c, m, rel, face, out, 11.5f, 4.5f, 13, 11.5f, r, g, b, a);
        switch (symbol) {
            case 0 -> rect(c, m, rel, face, out, 6.5f, 6.5f, 9.5f, 9.5f, r, g, b, a);
            case 1 -> {
                rect(c, m, rel, face, out, 7.25f, 6, 8.75f, 10, r, g, b, a);
                rect(c, m, rel, face, out, 6, 7.25f, 10, 8.75f, r, g, b, a);
            }
            default -> rect(c, m, rel, face, out, 6, 7.25f, 10, 8.75f, r, g, b, a);
        }
    }

    // A flat rectangle on one face of the block at 'rel', 'out' blocks proud of it, drawn from both sides.
    private static void rect(VertexConsumer c, Matrix4f m, BlockPos rel, Direction face, float out,
                             float u0, float v0, float u1, float v1, int r, int g, int b, int a) {
        float[][] p = {pt(rel, face, out, u0 / 16, v0 / 16), pt(rel, face, out, u1 / 16, v0 / 16),
                pt(rel, face, out, u1 / 16, v1 / 16), pt(rel, face, out, u0 / 16, v1 / 16)};
        for (int i = 0; i < 4; i++) c.addVertex(m, p[i][0], p[i][1], p[i][2]).setColor(r, g, b, a);
        for (int i = 3; i >= 0; i--) c.addVertex(m, p[i][0], p[i][1], p[i][2]).setColor(r, g, b, a);
    }

    private static float[] pt(BlockPos rel, Direction face, float out, float u, float v) {
        float x = rel.getX(), y = rel.getY(), z = rel.getZ();
        return switch (face) {
            case UP -> new float[]{x + u, y + 1 + out, z + v};
            case DOWN -> new float[]{x + u, y - out, z + v};
            case NORTH -> new float[]{x + u, y + v, z - out};
            case SOUTH -> new float[]{x + u, y + v, z + 1 + out};
            case WEST -> new float[]{x - out, y + v, z + u};
            case EAST -> new float[]{x + 1 + out, y + v, z + u};
        };
    }

    // Runs once per cycle, when the cycle passes 'at'.
    private static final Map<BlockPos, Long> LAST_EVENT = new HashMap<>();

    private static void cycleEvent(MachineControllerBlockEntity m, long cycleNo, float p, float at, Runnable r) {
        if (p < at) return;
        Long last = LAST_EVENT.get(m.getBlockPos());
        if (last != null && last == cycleNo) return;
        if (LAST_EVENT.size() > 256) LAST_EVENT.clear();
        LAST_EVENT.put(m.getBlockPos().immutable(), cycleNo);
        r.run();
    }

    private static void spark(Level level, MachineControllerBlockEntity m, float x, float y, float z) {
        Vec3 v = world(m, x, y, z);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, v.x, v.y, v.z, (level.getRandom().nextFloat() - 0.5f) * 0.2, 0.05, (level.getRandom().nextFloat() - 0.5f) * 0.2);
    }

    // An item in the machine's frame (front = -Z): position in blocks, size, tilt (x rotation), turn (y rotation),
    // and stretch across / squash through its thickness (for the press).
    private static void item(PoseStack ps, Level level, int light, ItemStack stack,
                             float x, float y, float z, float scale, float tilt, float turn, float stretch, float squash) {
        if (stack.isEmpty()) return;
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(turn));
        ps.mulPose(Axis.XP.rotationDegrees(tilt));
        ps.scale(scale * stretch, scale * stretch, scale * squash);
        LegacyRender.item(stack, ItemDisplayContext.FIXED, light, ps, level, 0);
        ps.popPose();
    }

    // A point in the machine's frame, in the world.
    static Vec3 world(MachineControllerBlockEntity m, float x, float y, float z) {
        MachineShape shape = m.type().shape;
        double lx = x - shape.centerRight(), lz = z + shape.centerBack();
        double a = Math.toRadians(degrees(facing(m)));
        double wx = lx * Math.cos(a) + lz * Math.sin(a), wz = -lx * Math.sin(a) + lz * Math.cos(a);
        BlockPos b = m.getBlockPos();
        return new Vec3(b.getX() + 0.5 + wx, b.getY() + y, b.getZ() + 0.5 + wz);
    }

    static Direction facing(MachineControllerBlockEntity m) {
        return m.getBlockState().getValue(MachineControllerBlock.FACING);
    }

    static float degrees(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case EAST -> -90f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    // Turn the model to the machine's facing, then slide it to the center of the structure.
    @Override
    protected void tryRotateByBlockstate(com.geckolib.renderer.base.RenderPassInfo<State<MachineControllerBlockEntity>> info, PoseStack poseStack) {
        MachineControllerBlockEntity animatable = info.renderState().blockEntity;
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees(facing(animatable))));
        MachineShape shape = animatable.type().shape;
        // In this turned frame the front is -Z, the back is +Z, and "right" (as you face the front) is -X.
        poseStack.translate(-shape.centerRight(), 0, shape.centerBack());
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    // The model is bigger than one block: keep drawing it while any part of the machine is on screen.
    @Override
    public AABB getRenderBoundingBox(MachineControllerBlockEntity machine) {
        return new AABB(machine.getBlockPos()).inflate(machine.type().shape.size + 1);
    }

    private static class Model extends GeoModel<MachineControllerBlockEntity> implements LegacyGeoBlockRenderer.Animated<MachineControllerBlockEntity> {
        private java.util.function.Function<String, com.geckolib.animation.state.BoneSnapshot> bones = n -> null;
        @Override
        public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            MachineControllerBlockEntity machine = LegacyGeoBlockRenderer.be(renderState);
            return rl("block/" + machine.type().id);
        }

        @Override
        public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
            MachineControllerBlockEntity machine = LegacyGeoBlockRenderer.be(renderState);
            return rl("textures/block/machine/" + machine.type().id + "_t" + tier(machine) + ".png");
        }

        @Override
        public Identifier getAnimationResource(MachineControllerBlockEntity machine) {
            return rl("block/" + machine.type().id);
        }

        // The moving parts, worked out from the game clock so they match the item drawn in render().
        @Override
        public void animate(MachineControllerBlockEntity machine, float partialTick, java.util.function.Function<String, com.geckolib.animation.state.BoneSnapshot> bonesIn) {
            this.bones = bonesIn;
            float time = time(machine, partialTick);
            boolean working = working(machine);
            double c = cycle(machine, time);
            float p = (float) (c - Math.floor(c));
            float rate = rate(machine);
            switch (machine.type()) {
                case PRESS -> bone("ram", b -> b.setTranslateY(working ? -2.6f * ramDown(p) : 0));
                case FURNACE -> bone("electrodes", b -> b.setTranslateY(working ? -(1.5f + (float) Math.sin(time * 0.3) * 0.5f) : 0));
                case WIRE_EXTRUDER -> bone("spool", b -> b.setRotX(working ? time * 0.12f * rate : 0));
                case KILN -> bone("flames", b -> {
                    b.skipRender(!working);
                    b.setScaleY(0.75f + 0.35f * (float) Math.abs(Math.sin(time * 0.9) * Math.sin(time * 0.37)));
                });
                case BANBURY_MIXER -> {
                    float a = working ? time * 0.14f * rate : 0;
                    bone("rotor_l", b -> b.setRotZ(a));
                    bone("rotor_r", b -> b.setRotZ(-a + (float) (Math.PI / 2)));
                    bone("ram", b -> b.setTranslateY(working ? -2.0f * ramDown(p) : 0));
                }
                case LATEX_VULCANIZER -> {
                    bone("drum", b -> b.setRotX(working ? time * 0.08f * rate : 0));
                    float fill = machine.shownLatex();
                    bone("latex", b -> {
                        b.skipRender(fill <= 0);
                        b.setScaleY(Math.max(0.01f, fill));
                    });
                }
                case CRUSHER -> {
                    float a = working ? time * 0.18f * rate : 0;
                    bone("roller_l", b -> b.setRotZ(a));
                    bone("roller_r", b -> b.setRotZ(-a + (float) (Math.PI / 4)));
                }
            }
        }

        private void bone(String name, java.util.function.Consumer<com.geckolib.animation.state.BoneSnapshot> f) {
            com.geckolib.animation.state.BoneSnapshot b = bones.apply(name);
            if (b != null) f.accept(b);
        }

        private static Identifier rl(String path) {
            return Identifier.fromNamespaceAndPath(FulguriteFusion.MODID, path);
        }
    }
}
