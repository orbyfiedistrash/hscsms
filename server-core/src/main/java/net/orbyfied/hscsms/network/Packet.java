package net.orbyfied.hscsms.network;

public abstract class Packet {

    // the packet type
    final PacketType<? extends Packet> type;

    public Packet(PacketType<? extends Packet> type) {
        this.type = type;
    }

    public PacketType<? extends Packet> getType() {
        return type;
    }

}
