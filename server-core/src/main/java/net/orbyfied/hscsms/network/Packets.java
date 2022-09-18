package net.orbyfied.hscsms.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Packets {

    public interface Serializer<P extends Packet> {
        void serialize(PacketType type, Packet packet,
                       DataOutputStream stream) throws Throwable;
    }

    public interface Deserializer<P extends Packet> {
        P deserialize(PacketType type, DataInputStream stream) throws Throwable;
    }

}
