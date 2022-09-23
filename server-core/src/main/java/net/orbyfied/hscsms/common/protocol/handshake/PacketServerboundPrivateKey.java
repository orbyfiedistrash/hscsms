package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.security.EncryptionProfile;

import java.security.PrivateKey;

public class PacketServerboundPrivateKey extends Packet {

    public static final PacketType<PacketServerboundPrivateKey> TYPE =
            new PacketType<>(PacketServerboundPrivateKey.class, "hscsms/handshake/serverbound/privatekey")
                    .serializer((type, packet, stream) -> {
                        // encode key and write
                        String key = EncryptionProfile.RSA_UTILITY_1024.encodePrivateKey(packet.key);
                        stream.writeUTF(key);
                    })
                    .deserializer((type, stream) -> {
                        // read and decode key
                        String keyStr = stream.readUTF();
                        PrivateKey key = EncryptionProfile.RSA_UTILITY_1024.decodePrivateKey(keyStr);
                        return new PacketServerboundPrivateKey(key);
                    });

    PrivateKey key;

    public PacketServerboundPrivateKey(PrivateKey key) {
        super(TYPE);
        this.key = key;
    }

    public PrivateKey getKey() {
        return key;
    }

}
