package com.sideflowgames.fulguritefusion.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

// 26.1 port helper: tooltips are now handed out line by line through a Consumer instead of a List.
// This List passes every added line straight on, so the old tooltip code can keep calling lines.add(...).
public final class TooltipLines extends AbstractList<Component> {
    private final Consumer<Component> out;
    private final List<Component> seen = new ArrayList<>();

    public TooltipLines(Consumer<Component> out) {
        this.out = out;
    }

    @Override
    public boolean add(Component line) {
        seen.add(line);
        out.accept(line);
        return true;
    }

    @Override
    public void add(int index, Component line) {
        add(line);
    }

    @Override
    public Component get(int index) {
        return seen.get(index);
    }

    @Override
    public int size() {
        return seen.size();
    }
}
