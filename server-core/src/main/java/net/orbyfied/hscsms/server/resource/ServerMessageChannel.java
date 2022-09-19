package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.core.resource.ServerResourceType;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.j8.registry.Identifier;

import java.util.Random;
import java.util.UUID;

public class ServerMessageChannel extends ServerResource {

    public static final Type TYPE = new Type();

    public static class Type extends ServerResourceType<ServerMessageChannel> {

        public Type() {
            super(Identifier.of("message_channel"), ServerMessageChannel.class);
        }

        @Override
        public UUID createLocalID() {
            return new UUID(System.currentTimeMillis(), RANDOM.nextLong());
        }

        @Override
        public ResourceSaveResult saveResource(ServerResourceManager manager, DatabaseItem dbItem, ServerMessageChannel resource) {
            return ResourceSaveResult.ofSuccess();
        }

        @Override
        public ResourceLoadResult loadResource(ServerResourceManager manager, DatabaseItem dbItem, ServerMessageChannel resource) {
            return ResourceLoadResult.ofSuccess();
        }
    }

    /////////////////////////////////////////////////

    public ServerMessageChannel(UUID uuid, UUID localId) {
        super(uuid, TYPE, localId);
    }

}
