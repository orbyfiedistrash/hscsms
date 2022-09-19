package net.orbyfied.hscsms;

import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.server.resource.ServerChannelMessage;
import net.orbyfied.hscsms.service.Logging;

import java.net.InetSocketAddress;

public class ServerMain {

    // server instance
    public static Server server;

    /* --------------------------- */

    public static void main(String[] args) {
        // create server
        server = new Server();
        server.open(new InetSocketAddress(42069));

        // run server on main thread
        server
                .setActive(true)
                .setup()
                .start();

        // create resource
        ServerResourceManager resourceManager = server.resourceManager();
        ServerChannelMessage message = resourceManager.createResource(ServerChannelMessage.TYPE);
        System.out.println(message);
        resourceManager.saveResourceAsync(message).whenComplete((__, t) -> {
            if (t != null) {
                t.printStackTrace(Logging.ERR);
            }

            // unload resource
            resourceManager.unloadResource(message);
        });

        // await a worker to block until the server stops
        server.serverSocketWorker.await();
    }

}
