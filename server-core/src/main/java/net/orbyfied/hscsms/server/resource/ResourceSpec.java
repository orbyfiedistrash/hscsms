package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResourceManager;

public class ResourceSpec {

    public static void loadSpec(ServerResourceManager manager) {
        // compile resource classes
        manager.compileResourceClass(ServerChannelMessage.class);
    }

}
