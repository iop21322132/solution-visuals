package farvix.solution.api.events.impl.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import farvix.solution.api.events.Event;

public class EventRender2D extends Event {
    private final DrawContext context;
    private final RenderTickCounter tickCounter;

    public EventRender2D(DrawContext context, RenderTickCounter tickCounter) {
        this.context = context;
        this.tickCounter = tickCounter;
    }

    public DrawContext getContext() {
        return context;
    }

    public RenderTickCounter getTickCounter() {
        return tickCounter;
    }

    public float getTickDelta() {
        return tickCounter.getTickDelta(true);
    }
}
