package farvix.solution.api.events.impl.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.packet.Packet;
import farvix.solution.api.events.Event;

@Getter @AllArgsConstructor
public class EventReceivePacket extends Event {
    final Packet packet;
}
