package net.orbyfied.hscsms.core;

import java.util.UUID;

/**
 * A resource loaded in memory by a server.
 */
public class ServerResource {

    /**
     * The universal resource unique identifier.
     */
    private final UUID uuid;

    /**
     * The resource type.
     */
    private final int type;

    /**
     * The local unique identifier, within
     * the type group.
     */
    private final UUID localId;

    public ServerResource(UUID uuid, int type, UUID localId) {
        this.uuid    = uuid;
        this.localId = localId;
        this.type    = type;
    }

    /**
     * Get the resource' universal unique identifier.
     * @return The UUID.
     */
    public UUID universalID() {
        return uuid;
    }

    /**
     * Get the type hash (identifier hash) of this resource.
     * @return The type identifier hash.
     */
    public int getTypeHash() {
        return type;
    }

    /**
     * Get the type by type hash.
     * @param manager The manager for indexing the type.
     * @return The type or null if not found.
     */
    public ServerResourceType<?> getType(ServerResourceManager manager) {
        return manager.getType(type);
    }

    /**
     * Get the resource' local ID within the type.
     * @return The UUID.
     */
    public UUID localID() {
        return localId;
    }

}
