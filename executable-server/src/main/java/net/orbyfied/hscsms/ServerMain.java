package net.orbyfied.hscsms;

import net.orbyfied.hscsms.server.Server;

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

        // await a worker to block until the server stops
        server.serverSocketWorker.await();
    }

}
