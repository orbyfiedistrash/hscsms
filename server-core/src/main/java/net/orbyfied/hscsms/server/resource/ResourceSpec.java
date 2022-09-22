package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResourceManager;

public class ResourceSpec {

    // loads the resource specification
    public static void loadSpec(ServerResourceManager manager) {

        // compile resource classes
        manager.compileResourceClass(ServerMessageChannel.class);
        manager.compileResourceClass(ServerChannelMessage.class);

    }

}
