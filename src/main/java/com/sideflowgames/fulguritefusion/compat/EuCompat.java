package com.sideflowgames.fulguritefusion.compat;

import java.lang.reflect.Method;

import com.sideflowgames.fulguritefusion.Config;
import com.sideflowgames.fulguritefusion.FulguriteFusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;

// Talks to other mods' EU systems without needing their code at compile time.
//
// NeoForge capabilities are found by name, so asking for "gtceu:energy_container" or
// "modern_industrialization:sided_mi_energy_storage" gives us the very same capability those mods use.
// We then call their methods by reflection. If a mod isn't installed, its adapter simply stays off.
//
// Supported: GregTech CEu Modern, Modern Industrialization.
public final class EuCompat {
    public enum Source { NONE, GREGTECH, MODERN_INDUSTRIALIZATION }

    public static final String[] TIER_NAMES = {"LV", "MV", "HV", "EV"};

    // EU per tick for each tier (LV, MV, HV, EV), from the config.
    public static long tierEu(int tier) {
        return switch (tier) {
            case 0 -> Config.EU_LV.get();
            case 1 -> Config.EU_MV.get();
            case 2 -> Config.EU_HV.get();
            default -> Config.EU_EV.get();
        };
    }

    // What the converter found on its output face.
    public record Target(Source source, int tier, long voltage, Object handler) {
        public static final Target NONE = new Target(Source.NONE, -1, 0, null);
    }

    private static boolean initialised;

    private static BlockCapability<Object, Direction> gtCap;
    private static Method gtInputsEnergy, gtGetInputVoltage, gtGetInputAmperage, gtAcceptEnergy;

    private static BlockCapability<Object, Direction> miCap;
    private static Method miCanConnect, miCanReceive, miReceive;

    private EuCompat() {}

    @SuppressWarnings("unchecked")
    private static BlockCapability<Object, Direction> capability(String id, Class<?> type) {
        return (BlockCapability<Object, Direction>) (BlockCapability<?, ?>) BlockCapability.createSided(Identifier.parse(id), type);
    }

    private static void init() {
        if (initialised) return;
        initialised = true;
        if (ModList.get().isLoaded("gtceu")) {
            try {
                Class<?> type = Class.forName("com.gregtechceu.gtceu.api.capability.IEnergyContainer");
                gtInputsEnergy = type.getMethod("inputsEnergy", Direction.class);
                gtGetInputVoltage = type.getMethod("getInputVoltage");
                gtGetInputAmperage = type.getMethod("getInputAmperage");
                gtAcceptEnergy = type.getMethod("acceptEnergyFromNetwork", Direction.class, long.class, long.class);
                gtCap = capability("gtceu:energy_container", type);
                FulguriteFusion.LOGGER.info("EU Converter: GregTech CEu support enabled");
            } catch (Throwable t) {
                gtCap = null;
                FulguriteFusion.LOGGER.warn("EU Converter: could not hook into GregTech CEu", t);
            }
        }
        if (ModList.get().isLoaded("modern_industrialization")) {
            try {
                Class<?> type = Class.forName("aztech.modern_industrialization.api.energy.MIEnergyStorage");
                miCanConnect = type.getMethod("canConnect", String.class);
                miCanReceive = type.getMethod("canReceive");
                miReceive = type.getMethod("receive", long.class, boolean.class);
                miCap = capability("modern_industrialization:sided_mi_energy_storage", type);
                FulguriteFusion.LOGGER.info("EU Converter: Modern Industrialization support enabled");
            } catch (Throwable t) {
                miCap = null;
                FulguriteFusion.LOGGER.warn("EU Converter: could not hook into Modern Industrialization", t);
            }
        }
    }

    // Look at the block on 'side' of the converter and work out what EU system and tier it uses.
    public static Target detect(Level level, BlockPos converterPos, Direction side) {
        init();
        BlockPos pos = converterPos.relative(side);
        Direction face = side.getOpposite();
        try {
            if (gtCap != null) {
                Object h = level.getCapability(gtCap, pos, face);
                if (h != null && (boolean) gtInputsEnergy.invoke(h, face)) {
                    long voltage = (long) gtGetInputVoltage.invoke(h);
                    int tier = tierForVoltage(voltage);
                    if (voltage > 0) return new Target(Source.GREGTECH, tier, voltage, h);
                }
            }
            if (miCap != null) {
                Object h = level.getCapability(miCap, pos, face);
                if (h != null && (boolean) miCanReceive.invoke(h)) {
                    for (int tier = 0; tier < TIER_NAMES.length; tier++) {
                        if ((boolean) miCanConnect.invoke(h, TIER_NAMES[tier].toLowerCase())) {
                            return new Target(Source.MODERN_INDUSTRIALIZATION, tier, tierEu(tier), h);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            FulguriteFusion.LOGGER.debug("EU Converter: error while detecting target", t);
        }
        return Target.NONE;
    }

    // Send energy to the target. 'available' = EU we can afford. Returns EU actually delivered.
    // Only whole packets at the target's own voltage are sent, so machines are never over-volted.
    public static long send(Target target, Direction side, long available) {
        if (target.source() == Source.NONE || available < target.voltage()) return 0;
        Direction face = side.getOpposite();
        try {
            if (target.source() == Source.GREGTECH) {
                long maxAmps = (long) gtGetInputAmperage.invoke(target.handler());
                long amps = Math.min(Math.max(1, maxAmps), available / target.voltage());
                long accepted = (long) gtAcceptEnergy.invoke(target.handler(), face, target.voltage(), amps);
                return accepted * target.voltage();
            }
            if (target.source() == Source.MODERN_INDUSTRIALIZATION) {
                return (long) miReceive.invoke(target.handler(), target.voltage(), false);
            }
        } catch (Throwable t) {
            FulguriteFusion.LOGGER.debug("EU Converter: error while sending energy", t);
        }
        return 0;
    }

    public static int tierForVoltage(long voltage) {
        for (int i = 0; i < TIER_NAMES.length; i++) {
            if (voltage <= tierEu(i)) return i;
        }
        return TIER_NAMES.length - 1;
    }

    public static String sourceName(Source source) {
        return switch (source) {
            case GREGTECH -> "GregTech";
            case MODERN_INDUSTRIALIZATION -> "Modern Ind.";
            default -> "None";
        };
    }
}
