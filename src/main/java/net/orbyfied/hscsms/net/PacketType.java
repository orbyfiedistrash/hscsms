package net.orbyfied.hscsms.net;

import net.orbyfied.j8.registry.Identifier;

public class PacketType<P extends Packet> {

    // the packet class
    final Class<? extends P> type;

    // the identifier of this type
    final Identifier id;

    // serialization handlers
    Packets.Serializer<P>   serializer;
    Packets.Deserializer<P> deserializer;

    public PacketType(Class<? extends P> type,
                      Identifier id) {
        this.type = type;
        this.id   = id;
    }

    public PacketType(Class<? extends P> type,
                      String id) {
        this.type = type;
        this.id   = Identifier.of(id);
    }

    public Identifier getIdentifier() {
        return id;
    }

    public Class<? extends Packet> getPacketClass() {
        return type;
    }

    public PacketType<P> serializer(Packets.Serializer<P> serializer) {
        this.serializer = serializer;
        return this;
    }

    public PacketType<P> deserializer(Packets.Deserializer<P> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

}
