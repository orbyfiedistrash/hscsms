package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

public class PacketUnboundHandshakeOk extends Packet {

    public static final PacketType<PacketUnboundHandshakeOk> TYPE =
            new PacketType<>(PacketUnboundHandshakeOk.class, "hscsms/handshake/unbound/ok")
            .serializer((type, packet, stream) -> {
                stream.writeUTF(packet.message);
            })
            .deserializer((type, stream) -> {
                return new PacketUnboundHandshakeOk(stream.readUTF());
            });

    public final String message;

    public PacketUnboundHandshakeOk(String message) {
        super(TYPE);
        this.message = message;
    }

}
