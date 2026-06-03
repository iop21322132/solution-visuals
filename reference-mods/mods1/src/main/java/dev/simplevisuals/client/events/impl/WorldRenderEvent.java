package dev.simplevisuals.client.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import org.w3c.dom.events.Event;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor

public abstract class WorldRenderEvent implements Event {
    MatrixStack stack;
    float partialTicks;
}
