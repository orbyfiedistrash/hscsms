package net.orbyfied.hscsms.common.protocol.handshake;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

import javax.crypto.SecretKey;

import static net.orbyfied.hscsms.common.ProtocolSpec.EP_SECRET;
import static net.orbyfied.hscsms.common.ProtocolSpec.EP_UTILITY;

import java.security.PrivateKey;
import java.security.PublicKey;

public class PacketServerboundClientKey extends Packet {

    public static final PacketType<PacketServerboundClientKey> TYPE =
            new PacketType<>(PacketServerboundClientKey.class, "hscsms/handshake/serverbound/clientkey")
                    .serializer((type, packet, stream) -> {
                        // encode key and write
                        String key = EP_UTILITY.encodeSecretKey(packet.getKey());
                        System.out.println("SEC-KEY: " + key);
                        stream.writeUTF(key);
                    })
                    .deserializer((type, stream) -> {
                        // read and decode key
                        String keyStr = stream.readUTF();
                        System.out.println("SEC-KEY: " + keyStr);
                        SecretKey key = EP_SECRET.decodeSecretKey(keyStr);
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
