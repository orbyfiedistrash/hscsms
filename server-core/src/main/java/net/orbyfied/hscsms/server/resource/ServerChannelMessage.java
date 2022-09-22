package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceType;

import java.util.UUID;

public class ServerChannelMessage extends ServerResource {

    public static final ServerResourceType<ServerMessageChannel> TYPE = ServerResourceType.ofChronoIds(
            ServerMessageChannel.class, "message",
            (manager, databaseItem, serverMessageChannel) -> {
                return ServerResourceType.ResourceSaveResult.ofSuccess();
            },
            (manager, databaseItem, serverMessageChannel) -> {
                return ServerResourceType.ResourceLoadResult.ofSuccess();
            }
    );

    ///////////////////////////////////////////

    public ServerChannelMessage(UUID uuid, UUID localId) {
        super(uuid, TYPE, localId);
    }

    // the raw message content
    String contentRaw;

    // the channel this message is in
    ServerMessageChannel channel;

    public ServerChannelMessage setContentRaw(String contentRaw) {
        this.contentRaw = contentRaw;
        return this;
    }

    public String getContentRaw() {
        return contentRaw;
    }

    public ServerMessageChannel getChannel() {
        return channel;
    }

}
