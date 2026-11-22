package com.sideflowgames.fulguritefusion.client;

import com.sideflowgames.fulguritefusion.ModBlocks;
import com.sideflowgames.fulguritefusion.block.MachineControllerBlock;
import com.sideflowgames.fulguritefusion.machine.MachineShape;
import com.sideflowgames.fulguritefusion.machine.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// The multiblocks the guide can show in 3D, layer by layer. Built from the same layouts as the ghost outlines
// (GhostBuild) and the machines' own shape checks, so the guide always matches what the game expects.
// Ids for guide.json "build": fusion_reactor, reactor_console, mini_collider, storm_collider, storm_forge,
// press, arc_furnace, wire_extruder, kiln, crusher.
public final class GuideBuilds {
    private GuideBuilds() {}

    public record Placed(BlockPos pos, BlockState state) {}

    // One step of a build: what to place, and what to say about it.
    public record Step(Component caption, ItemStack icon, List<Placed> blocks) {}

    public static final class Build {
        public final List<Placed> blocks;
        public final int[] layers; // the y levels, bottom to top
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final List<Step> steps = new ArrayList<>();

        Build(List<Placed> blocks) {
            this.blocks = blocks;
            TreeSet<Integer> ys = new TreeSet<>();
            int x0 = Integer.MAX_VALUE, y0 = x0, z0 = x0, x1 = Integer.MIN_VALUE, y1 = x1, z1 = x1;
            for (Placed p : blocks) {
                BlockPos b = p.pos();
                ys.add(b.getY());
                x0 = Math.min(x0, b.getX()); y0 = Math.min(y0, b.getY()); z0 = Math.min(z0, b.getZ());
                x1 = Math.max(x1, b.getX()); y1 = Math.max(y1, b.getY()); z1 = Math.max(z1, b.getZ());
            }
            layers = ys.stream().mapToInt(Integer::intValue).toArray();
            minX = x0; minY = y0; minZ = z0; maxX = x1; maxY = y1; maxZ = z1;
            makeSteps();
        }

        // Step by step, the way you'd build it: the core block first (the ghost outline grows from it), then
        // bottom to top, one kind of block at a time.
        private void makeSteps() {
            if (blocks.isEmpty()) return;
            Placed core = null;
            for (Placed p : blocks) if (p.pos().equals(BlockPos.ZERO)) core = p;
            if (core != null) {
                steps.add(new Step(Component.literal("Place the ").append(core.state().getBlock().getName()),
                        new ItemStack(core.state().getBlock()), List.of(core)));
            }
            for (int li = 0; li < layers.length; li++) {
                int y = layers[li];
                Map<Block, List<Placed>> groups = new LinkedHashMap<>();
                for (Placed p : blocks) {
                    if (p == core || p.pos().getY() != y) continue;
                    groups.computeIfAbsent(p.state().getBlock(), k -> new ArrayList<>()).add(p);
                }
                for (var e : groups.entrySet()) {
                    // Nearest the middle first, so each step grows outward.
                    List<Placed> list = new ArrayList<>(e.getValue());
                    list.sort(java.util.Comparator.comparingDouble(p -> p.pos().distSqr(new BlockPos(0, y, 0))));
                    Component caption = layers.length > 1
                            ? Component.literal("Layer " + (li + 1) + " of " + layers.length + ": place " + list.size() + " \u00D7 ").append(e.getKey().getName())
                            : Component.literal("Place " + list.size() + " \u00D7 ").append(e.getKey().getName());
                    steps.add(new Step(caption, new ItemStack(e.getKey()), list));
                }
            }
        }

        // layer < 0: everything.
        public boolean shown(Placed p, int layer) {
            return layer < 0 || layer >= layers.length || p.pos().getY() <= layers[layer];
        }

        // What a layer (or the whole build, layer < 0) is made of, as item stacks with counts.
        public List<ItemStack> materials(int layer) {
            Map<Block, Integer> counts = new LinkedHashMap<>();
            for (Placed p : blocks) {
                if (layer >= 0 && layer < layers.length && p.pos().getY() != layers[layer]) continue;
                counts.merge(p.state().getBlock(), 1, Integer::sum);
            }
            List<ItemStack> out = new ArrayList<>();
            counts.forEach((b, n) -> {
                if (b.asItem() != net.minecraft.world.item.Items.AIR) out.add(new ItemStack(b.asItem(), n));
            });
            return out;
        }
    }

    private static final Map<String, Build> CACHE = new HashMap<>();

    public static Build get(String id) {
        return CACHE.computeIfAbsent(id, GuideBuilds::make);
    }

    private static Build make(String id) {
        List<Placed> out = new ArrayList<>();
        var empty = EmptyBlockGetter.INSTANCE;
        BlockPos zero = BlockPos.ZERO;
        switch (id) {
            case "fusion_reactor" -> {
                add(out, GhostBuild.fusionReactor(empty, zero));
                out.add(new Placed(zero, ModBlocks.FUSION_COLLIDER.get().defaultBlockState()));
            }
            case "reactor_console" -> {
                add(out, GhostBuild.console(empty, zero));
                out.add(new Placed(zero, ModBlocks.REACTOR_CONSOLE.get().defaultBlockState()));
            }
            case "mini_collider" -> {
                add(out, GhostBuild.colliderRing(empty, zero));
                out.add(new Placed(zero, ModBlocks.MINI_COLLIDER.get().defaultBlockState()));
            }
            case "storm_collider" -> {
                add(out, GhostBuild.colliderRing(empty, zero));
                out.add(new Placed(zero, ModBlocks.STORM_COLLIDER.get().defaultBlockState()));
            }
            case "storm_forge" -> {
                add(out, GhostBuild.stormForge(empty, zero));
                out.add(new Placed(zero, ModBlocks.STORM_FORGE.get().defaultBlockState()));
            }
            default -> {
                MachineType type = switch (id) {
                    case "press" -> MachineType.PRESS;
                    case "arc_furnace" -> MachineType.FURNACE;
                    case "wire_extruder" -> MachineType.WIRE_EXTRUDER;
                    case "kiln" -> MachineType.KILN;
                    case "crusher" -> MachineType.CRUSHER;
                    case "banbury_mixer" -> MachineType.BANBURY_MIXER;
                    case "latex_vulcanizer" -> MachineType.LATEX_VULCANIZER;
                    default -> null;
                };
                if (type != null) {
                    Block controller = switch (type) {
                        case PRESS -> ModBlocks.PRESS.get();
                        case FURNACE -> ModBlocks.ARC_FURNACE.get();
                        case WIRE_EXTRUDER -> ModBlocks.WIRE_EXTRUDER.get();
                        case KILN -> ModBlocks.KILN.get();
                        case CRUSHER -> ModBlocks.CRUSHER.get();
                        case BANBURY_MIXER -> ModBlocks.BANBURY_MIXER.get();
                        case LATEX_VULCANIZER -> ModBlocks.LATEX_VULCANIZER.get();
                    };
                    BlockState casing = ModBlocks.CRUDE_ARC_CASING.get().defaultBlockState();
                    for (MachineShape.Part part : type.shape.parts(zero, Direction.NORTH)) out.add(new Placed(part.pos(), casing));
                    out.add(new Placed(zero, controller.defaultBlockState().setValue(MachineControllerBlock.FACING, Direction.NORTH)));
                }
            }
        }
        return new Build(out);
    }

    private static void add(List<Placed> out, List<GhostBuild.Ghost> ghosts) {
        for (GhostBuild.Ghost g : ghosts) if (g.want() != null) out.add(new Placed(g.pos(), g.want()));
    }
}
