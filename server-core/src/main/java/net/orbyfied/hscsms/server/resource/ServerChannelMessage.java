package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.ServerResource;
import net.orbyfied.hscsms.core.ServerResourceManager;
import net.orbyfied.hscsms.core.ServerResourceType;
import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.j8.registry.Identifier;
import org.bson.Document;

import java.util.UUID;

public class ServerChannelMessage extends ServerResource {

    public static final Type TYPE = new Type();

    public static class Type extends ServerResourceType<ServerChannelMessage> {

        public Type() {
            super(Identifier.of("channel_message"), ServerChannelMessage.class);
        }

        @Override
        public ResourceSaveResult saveResource(ServerResourceManager manager, DatabaseItem dbItem, ServerChannelMessage resource) {
            dbItem.set("content_raw", resource.getContentRaw());
            return ResourceSaveResult.ofSuccess();
        }

        @Override
        public ResourceLoadResult loadResource(ServerResourceManager manager, DatabaseItem dbItem, ServerChannelMessage resource) {
            return ResourceLoadResult.ofSuccess();
        }

    }

    ///////////////////////////////////////////

    public ServerChannelMessage(UUID uuid, int type, UUID localId) {
        super(uuid, type, localId);
    }

    // the raw message content
    String contentRaw;

    public ServerChannelMessage setContentRaw(String contentRaw) {
        this.contentRaw = contentRaw;
        return this;
    }

    public String getContentRaw() {
        return contentRaw;
    }

}
