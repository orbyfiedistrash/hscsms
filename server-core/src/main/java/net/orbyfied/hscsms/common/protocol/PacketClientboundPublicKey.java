package net.orbyfied.hscsms.common.protocol;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.security.EncryptionProfile;
import net.orbyfied.hscsms.server.Server;

import java.security.PublicKey;

public class PacketClientboundPublicKey extends Packet {

    public static final PacketType<PacketClientboundPublicKey> TYPE =
            new PacketType<>(PacketClientboundPublicKey.class, "handshake:clientbound_pubkey")
            .serializer((type, packet, stream) -> {
                // encode key and write
                String key = EncryptionProfile.RSA_UTILITY_1024.encodePublicKey();
                stream.writeUTF(key);
            })
            .deserializer((type, stream) -> {
                throw new IllegalStateException("packet type " + type.getIdentifier() + " was received by the server, " +
                        "but it is clientbound");
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
