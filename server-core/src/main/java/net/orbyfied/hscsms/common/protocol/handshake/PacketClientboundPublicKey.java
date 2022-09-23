package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.common.ProtocolSpec;
import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.security.EncryptionProfile;
import static net.orbyfied.hscsms.common.ProtocolSpec.EP_UTILITY;

import java.security.PublicKey;

public class PacketClientboundPublicKey extends Packet {

    public static final PacketType<PacketClientboundPublicKey> TYPE =
            new PacketType<>(PacketClientboundPublicKey.class, "hscsms/handshake/clientbound/pubkey")
            .serializer((type, packet, stream) -> {
                // encode key and write
                String key = EP_UTILITY.encodePublicKey(packet.key);
                System.out.println("SERVER-PUB-KEY: " + key);
                stream.writeUTF(key);
            })
            .deserializer((type, stream) -> {
                // read and decode key
                String keyStr = stream.readUTF();
                System.out.println("SERVER-PUB-KEY: " + keyStr);
                PublicKey key = EP_UTILITY.decodePublicKey(keyStr);
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
