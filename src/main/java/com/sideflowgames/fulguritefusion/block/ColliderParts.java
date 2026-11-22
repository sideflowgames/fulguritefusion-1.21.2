package com.sideflowgames.fulguritefusion.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

// Block state properties of a collider ring once it has formed: the ring blocks (Fulgurite Ring, Collider
// Input, Collider Injectors) hide (formed = true) and the center block draws the whole ring as one model,
// turned so its input station faces the Collider Input (input = that side).
public final class ColliderParts {
    private ColliderParts() {}

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty INPUT = DirectionProperty.create("input", Direction.Plane.HORIZONTAL);
}
