package net.orbyfied.hscsms.common;

import net.orbyfied.hscsms.common.protocol.PacketClientboundPublicKey;
import net.orbyfied.hscsms.net.NetworkManager;

public class ProtocolSpec {

    public static void loadProtocol(NetworkManager manager) {

        // load packets
        manager.compilePacketClass(PacketClientboundPublicKey.class);

    }

}
