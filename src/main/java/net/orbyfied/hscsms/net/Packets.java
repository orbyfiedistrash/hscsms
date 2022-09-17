package net.orbyfied.hscsms.net;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Packets {

    public interface Serializer<P extends Packet> {
        void serialize(PacketType type, P packet,
                       DataOutputStream stream) throws Throwable;
    }

    public interface Deserializer<P extends Packet> {
        P deserialize(PacketType type, DataInputStream stream) throws Throwable;
    }

}
