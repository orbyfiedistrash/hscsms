package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceHandle;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.core.resource.ServerResourceType;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.j8.registry.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ServerMessageChannel extends ServerResource {

    public static final ServerResourceType<ServerMessageChannel> TYPE = ServerResourceType.ofChronoIds(
            ServerMessageChannel.class, "message_channel",
            (manager, databaseItem, serverMessageChannel) -> {
                return ServerResourceType.ResourceSaveResult.ofSuccess();
            },
            (manager, databaseItem, serverMessageChannel) -> {
                return ServerResourceType.ResourceLoadResult.ofSuccess();
            }
    );

    /////////////////////////////////////////////////s

    public ServerMessageChannel(UUID uuid, UUID localId) {
        super(uuid, TYPE, localId);
    }

}
