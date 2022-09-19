package net.orbyfied.hscsms.core.resource;

import java.util.UUID;

/**
 * A resource loaded in memory by a server.
 */
@SuppressWarnings("rawtypes")
public class ServerResource {

    /**
     * The universal resource unique identifier.
     */
    private final UUID uuid;

    /**
     * The resource type.
     */
    private final ServerResourceType type;

    /**
     * The local unique identifier, within
     * the type group.
     */
    private final UUID localId;

    public ServerResource(UUID uuid, ServerResourceType type, UUID localId) {
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
     * Get the type of this resource.
     * @return The type.
     */
    public ServerResourceType type() {
        return type;
    }

    /**
     * Get the resource' local ID within the type.
     * @return The UUID.
     */
    public UUID localID() {
        return localId;
    }

}
