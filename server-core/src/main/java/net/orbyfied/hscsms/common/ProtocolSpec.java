package net.orbyfied.hscsms.common;

import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketUnboundHandshakeOk;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.security.AsymmetricEncryptionProfile;
import net.orbyfied.hscsms.security.LegacyEncryptionProfile;
import net.orbyfied.hscsms.security.SymmetricEncryptionProfile;

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
    public static final int S_KEY_LENGTH = 128;

    public static AsymmetricEncryptionProfile newAsymmetricEncryptionProfile() {
        return new AsymmetricEncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", G_KEY_LENGTH);
    }

    public static SymmetricEncryptionProfile newSymmetricEncryptionProfile() {
        return new SymmetricEncryptionProfile("AES", "ECB", "PKCS5Padding", "AES", S_KEY_LENGTH);
    }

    public static final SymmetricEncryptionProfile  EP_SYMMETRIC  = newSymmetricEncryptionProfile();
    public static final AsymmetricEncryptionProfile EP_ASYMMETRIC = newAsymmetricEncryptionProfile();

}
