package net.orbyfied.hscsms;

import net.orbyfied.hscsms.server.Server;

import java.net.InetSocketAddress;

public class Main {

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
                .runMain();
    }

}
