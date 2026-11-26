package com.sideflowgames.fulguritefusion.titan;

import com.mojang.serialization.MapCodec;
import com.sideflowgames.fulguritefusion.ModEntities;
import com.sideflowgames.fulguritefusion.ModItems;
import com.sideflowgames.fulguritefusion.entity.boss.StormTitanEntity;
import com.sideflowgames.fulguritefusion.item.StormforgedArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Titan Altar: at the heart of each arena in the Titan Trials. Can't be broken.
//   - Right-click with a Storm Sigil to wake the Storm Titan (the sigil is used up; the Titan drops a new one).
//   - The Storm Eye, Molten Heart and Void Shard no longer go in directly: they're forged into the sigil.
//   - Before waking it you may offer one armor piece of Stormforged tier or better (it's used up). The Titan then
//     has a 90% chance to drop the Titan piece of that type.
//   - Right-click with an empty hand to see what's been set into it.
// Only one Titan can be awake within 128 blocks.
public class TitanAltarBlock extends Block {
    public static final MapCodec<TitanAltarBlock> CODEC = simpleCodec(TitanAltarBlock::new);
    public static final BooleanProperty EYE = BooleanProperty.create("eye");
    public static final BooleanProperty HEART = BooleanProperty.create("heart");
    public static final BooleanProperty SHARD = BooleanProperty.create("shard");
    public static final EnumProperty<Offering> OFFERING = EnumProperty.create("offering", Offering.class);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 12, 14), Block.box(0, 12, 0, 16, 16, 16));

    public enum Offering implements StringRepresentable {
        NONE("none", null), HEAD("head", EquipmentSlot.HEAD), CHEST("chest", EquipmentSlot.CHEST),
        LEGS("legs", EquipmentSlot.LEGS), FEET("feet", EquipmentSlot.FEET);

        private final String name;
        final EquipmentSlot slot;

        Offering(String name, EquipmentSlot slot) {
            this.name = name;
            this.slot = slot;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Offering of(EquipmentSlot slot) {
            for (Offering o : values()) if (o.slot == slot) return o;
            return NONE;
        }
    }

    public TitanAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EYE, false).setValue(HEART, false).setValue(SHARD, false)
                .setValue(OFFERING, Offering.NONE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EYE, HEART, SHARD, OFFERING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        Item item = stack.getItem();
        BooleanProperty socket = item == ModItems.STORM_EYE.get() ? EYE : item == ModItems.MOLTEN_HEART.get() ? HEART
                : item == ModItems.VOID_SHARD.get() ? SHARD : null;
        boolean sigil = item == ModItems.STORM_SIGIL.get();
        boolean offering = item instanceof StormforgedArmorItem;
        if (socket == null && !sigil && !offering) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!TitanTrials.is(level)) {
            hint(player, "The altar is dormant here. It only wakes in the Titan Trials.");
            return InteractionResult.FAIL;
        }
        if (socket != null) {
            hint(player, "Forge the Storm Eye, Molten Heart and Void Shard into a Storm Sigil to wake the Titan.");
            return InteractionResult.FAIL;
        }
        if (offering) {
            if (state.getValue(OFFERING) != Offering.NONE) {
                hint(player, "An offering already rests on the altar.");
                return InteractionResult.FAIL;
            }
            EquipmentSlot slot = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot();
            level.setBlockAndUpdate(pos, state.setValue(OFFERING, Offering.of(slot)));
            stack.consume(1, player);
            server.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6f, 0.6f);
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.5);
            hint(player, "The altar accepts your offering.");
            return InteractionResult.CONSUME;
        }
        if (titanAwake(server, pos)) {
            hint(player, "The Titan is already awake.");
            return InteractionResult.FAIL;
        }
        if (sigil) {
            stack.consume(1, player);
            summon(server, pos, state);
            return InteractionResult.CONSUME;
        }
        if (state.getValue(socket)) {
            hint(player, "That piece is already set into the altar.");
            return InteractionResult.FAIL;
        }
        stack.consume(1, player);
        state = state.setValue(socket, true);
        server.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (state.getValue(EYE) && state.getValue(HEART) && state.getValue(SHARD)) {
            summon(server, pos, state);
        } else {
            level.setBlockAndUpdate(pos, state);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        String pieces = "Use a Storm Sigil here to wake the Titan";
        Offering o = state.getValue(OFFERING);
        player.sendOverlayMessage(Component.literal(pieces + (o == Offering.NONE ? "." : ". Offering: " + o.getSerializedName()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.CONSUME;
    }

    private static boolean titanAwake(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(StormTitanEntity.class, new AABB(pos).inflate(128), e -> e.isAlive()).isEmpty();
    }

    private static void summon(ServerLevel level, BlockPos pos, BlockState state) {
        StormTitanEntity titan = ModEntities.STORM_TITAN.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (titan == null) return;
        Offering o = state.getValue(OFFERING);
        if (o != Offering.NONE) titan.setTargetSlot(o.slot);
        level.setBlockAndUpdate(pos, state.setValue(EYE, false).setValue(HEART, false).setValue(SHARD, false).setValue(OFFERING, Offering.NONE));
        titan.setHome(pos.below(2));
        titan.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        titan.setPersistenceRequired();
        level.addFreshEntity(titan);
        for (int i = 0; i < 6; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (bolt == null) continue;
            double a = Math.PI * 2 * i / 6;
            bolt.snapTo(pos.getX() + 0.5 + Math.cos(a) * 6, pos.getY() + 1, pos.getZ() + 0.5 + Math.sin(a) * 6);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.5f);
        for (Player p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 96 * 96) {
                p.sendOverlayMessage(Component.literal("The Storm Titan wakes!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
        }
    }

    private static void hint(Player player, String text) {
        player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int set = (state.getValue(EYE) ? 1 : 0) + (state.getValue(HEART) ? 1 : 0) + (state.getValue(SHARD) ? 1 : 0);
        if (random.nextInt(4 - set) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.1,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.05, 0);
        }
    }
}
