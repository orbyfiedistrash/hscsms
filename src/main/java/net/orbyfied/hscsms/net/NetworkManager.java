package net.orbyfied.hscsms.net;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.reflect.Reflector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkManager {

    // utilities
    private static final Reflector reflector = new Reflector("NetworkManager");
    private static final Logger LOGGER = Logging.getLogger("NetworkManager");

    // the packet types by id
    // the key is the hash of the string identifier
    Int2ObjectOpenHashMap<PacketType<? extends Packet>> packetTypesById = new Int2ObjectOpenHashMap<>();
    // the packet types
    ArrayList<PacketType<? extends Packet>> packetTypes = new ArrayList<>();

    public Logger getLogger() {
        return LOGGER;
    }

    public NetworkManager register(PacketType<? extends Packet> type) {
        packetTypesById.put(type.getIdentifier().hashCode(), type);
        packetTypes.add(type);
        return this;
    }

    public PacketType<? extends Packet> getByIdentifier(Identifier id) {
        return packetTypesById.get(id.hashCode());
    }

    public PacketType<? extends Packet> getByHash(int id) {
        return packetTypesById.get(id);
    }

    public List<PacketType<? extends Packet>> getPacketTypes() {
        return Collections.unmodifiableList(packetTypes);
    }

    public NetworkManager compilePacketClass(Class<? extends Packet> klass) {
        try {
            // get and register type
            Field        field = reflector.reflectDeclaredField(klass, "TYPE");
            PacketType<?> type = reflector.reflectGetField(field, null);
            register(type);

            // return
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

}
