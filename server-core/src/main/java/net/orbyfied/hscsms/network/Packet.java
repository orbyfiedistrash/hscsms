package net.orbyfied.hscsms.network;

@SuppressWarnings("rawtypes")
public abstract class Packet {

    // the packet type
    final PacketType<? extends Packet> type;

    public Packet(PacketType<? extends Packet> type) {
        this.type = type;
    }

    public PacketType type() {
        return type;
    }

}
