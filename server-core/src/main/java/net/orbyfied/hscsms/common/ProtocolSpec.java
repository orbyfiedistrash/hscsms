package net.orbyfied.hscsms.common;

import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundPrivateKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketUnboundHandshakeOk;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.security.EncryptionProfile;

public class ProtocolSpec {

    public static void loadProtocol(NetworkManager manager) {

        /*
            Packets
         */

        // handshake
        manager.compilePacketClass(PacketClientboundPublicKey.class);
        manager.compilePacketClass(PacketServerboundPrivateKey.class);
        manager.compilePacketClass(PacketUnboundHandshakeOk.class);

        // misc
        manager.compilePacketClass(PacketServerboundDisconnect.class);
        manager.compilePacketClass(PacketClientboundDisconnect.class);

    }

    /* ------------------- */

    public static EncryptionProfile newBlankEncryptionProfile() {
        return new EncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", 1024);
    }

}
