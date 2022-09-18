package net.orbyfied.hscsms.common;

import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.network.NetworkManager;

public class ProtocolSpec {

    public static void loadProtocol(NetworkManager manager) {

        // load packets
        manager.compilePacketClass(PacketClientboundPublicKey.class);
        manager.compilePacketClass(PacketClientboundDisconnect.class);
        manager.compilePacketClass(PacketServerboundDisconnect.class);

    }

}
