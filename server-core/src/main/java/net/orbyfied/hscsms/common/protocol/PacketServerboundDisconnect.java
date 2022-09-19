package net.orbyfied.hscsms.common.protocol;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

public class PacketServerboundDisconnect extends Packet {

    public static final PacketType<PacketServerboundDisconnect> TYPE
            = new PacketType<>(PacketServerboundDisconnect.class,
            "hscsms/core/serverbound/disconnect")
            .serializer((type, packet, stream) -> {

            })
            .deserializer((type, stream) -> {
                return new PacketServerboundDisconnect();
            });

    public PacketServerboundDisconnect() {
        super(TYPE);
    }

}
