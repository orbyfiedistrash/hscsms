package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

import javax.crypto.SecretKey;

import static net.orbyfied.hscsms.common.ProtocolSpec.EP_ASYMMETRIC;
import static net.orbyfied.hscsms.common.ProtocolSpec.EP_SYMMETRIC;

public class PacketServerboundClientKey extends Packet {

    public static final PacketType<PacketServerboundClientKey> TYPE =
            new PacketType<>(PacketServerboundClientKey.class, "hscsms/handshake/serverbound/clientkey")
                    .serializer((type, packet, stream) -> {
                        // encode key and write
                        String key = EP_SYMMETRIC.encodeKeyToBase64(packet.getKey());
                        stream.writeUTF(key);
                    })
                    .deserializer((type, stream) -> {
                        // read and decode key
                        String keyStr = stream.readUTF();
                        SecretKey key = EP_SYMMETRIC.decodeKeyFromBase64(SecretKey.class, keyStr);
                        return new PacketServerboundClientKey(key);
                    });

    SecretKey key;

    public PacketServerboundClientKey(SecretKey key) {
        super(TYPE);
        this.key = key;
    }

    public SecretKey getKey() {
        return key;
    }

}
