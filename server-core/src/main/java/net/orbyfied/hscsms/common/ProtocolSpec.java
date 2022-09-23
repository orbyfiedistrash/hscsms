package net.orbyfied.hscsms.common;

import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundClientKey;
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
        manager.compilePacketClass(PacketServerboundClientKey.class);
        manager.compilePacketClass(PacketUnboundHandshakeOk.class);

        // misc
        manager.compilePacketClass(PacketServerboundDisconnect.class);
        manager.compilePacketClass(PacketClientboundDisconnect.class);

    }

    /* ------------------- */

    public static final int G_KEY_LENGTH = 1024;
    public static final int S_KEY_LENGTH = 56;

    public static EncryptionProfile newBlankEncryptionProfile() {
        return new EncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", G_KEY_LENGTH);
    }

    public static EncryptionProfile newSymmetricSecretProfile() {
        return new EncryptionProfile("AES", "ECB", "NoPadding", "DES", S_KEY_LENGTH);
    }

    public static final EncryptionProfile EP_UTILITY = newBlankEncryptionProfile();
    public static final EncryptionProfile EP_SECRET  = newSymmetricSecretProfile();

}
