package net.orbyfied.hscsms.common.protocol;

import net.orbyfied.hscsms.net.Packet;
import net.orbyfied.hscsms.net.PacketType;
import net.orbyfied.hscsms.security.EncryptionProfile;

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
                // read and decode key
                String key = stream.readUTF();
                return new PacketClientboundPublicKey(EncryptionProfile.RSA_UTILITY_1024.decodePublicKey(key));
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
