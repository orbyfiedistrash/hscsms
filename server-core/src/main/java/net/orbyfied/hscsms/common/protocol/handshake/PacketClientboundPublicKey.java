package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

import static net.orbyfied.hscsms.common.ProtocolSpec.EP_ASYMMETRIC;
import static net.orbyfied.hscsms.common.ProtocolSpec.EP_SYMMETRIC;

import java.security.PublicKey;

public class PacketClientboundPublicKey extends Packet {

    public static final PacketType<PacketClientboundPublicKey> TYPE =
            new PacketType<>(PacketClientboundPublicKey.class, "hscsms/handshake/clientbound/pubkey")
            .serializer((type, packet, stream) -> {
                // encode key and write
                String key = EP_ASYMMETRIC.encodeKeyToBase64(packet.key);
                stream.writeUTF(key);
            })
            .deserializer((type, stream) -> {
                // read and decode key
                String keyStr = stream.readUTF();
                PublicKey key = EP_ASYMMETRIC.decodeKeyFromBase64(PublicKey.class, keyStr);
                return new PacketClientboundPublicKey(key);
            });

    PublicKey key;

    public PacketClientboundPublicKey(PublicKey key) {
        super(TYPE);
        this.key = key;
    }

    public PublicKey getKey() {
        return key;
    }

}
