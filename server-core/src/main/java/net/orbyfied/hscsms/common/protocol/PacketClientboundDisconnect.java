package net.orbyfied.hscsms.common.protocol;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.server.client.DisconnectReason;

public class PacketClientboundDisconnect extends Packet {

    public static final PacketType<PacketClientboundDisconnect> TYPE
            = new PacketType<>(PacketClientboundDisconnect.class,
                "hscsms/core/clientbound/disconnect")
            .serializer((type, packet, stream) -> {
                // write enum reason
                stream.writeInt(packet.reason.ordinal());
            })
            .deserializer((type, stream) -> {
                // read enum reason
                DisconnectReason reason =
                        DisconnectReason.values()[stream.readInt()];

                // construct packet
                return new PacketClientboundDisconnect(reason);
            });

    private DisconnectReason reason;

    public PacketClientboundDisconnect(DisconnectReason reason) {
        super(TYPE);
        this.reason = reason;
    }

    public DisconnectReason getReason() {
        return reason;
    }

}
