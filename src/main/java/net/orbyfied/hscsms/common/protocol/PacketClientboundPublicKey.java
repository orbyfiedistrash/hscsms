package net.orbyfied.hscsms.common.protocol;

import net.orbyfied.hscsms.net.Packet;
import net.orbyfied.hscsms.net.PacketType;
import net.orbyfied.hscsms.security.RSAEncryption;

import java.security.PublicKey;

public class PacketClientboundPublicKey extends Packet {

    public static final PacketType<PacketClientboundPublicKey> TYPE =
            new PacketType<>(PacketClientboundPublicKey.class, "handshake:clientbound_pubkey")
            .serializer((type, packet, stream) -> {
                // encode key and write
                String key = RSAEncryption.UTILITY.encodePublicKey();
                stream.writeUTF(key);
            })
            .deserializer((type, stream) -> {
                // read and decode key
                String key = stream.readUTF();
                return new PacketClientboundPublicKey(RSAEncryption.UTILITY.decodePublicKey(key));
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
