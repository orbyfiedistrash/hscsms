package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.core.ServiceManager;
import net.orbyfied.hscsms.net.NetworkManager;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.net.ServerSocket;
import java.net.SocketAddress;

public class Server {

    // the logger
    Logger logger = Logging.getLogger("Server");

    // the servers socket address
    SocketAddress address;
    // the server socket
    ServerSocket socket;

    /* ------ Top-Level Services ------ */

    // the service manager
    ServiceManager serviceManager;
    // the network manager
    NetworkManager networkManager;

    public Server() { }

    /**
     * Bind and open the server on the provided
     * socket address.
     * @param address The socket address.
     * @return This.
     */
    public Server open(SocketAddress address) {
        // set address
        this.address = address;

        try {
            // create and bind socket
            socket = new ServerSocket();
            socket.bind(address);

            logger.ok("Connected server on " + address);
        } catch (Exception e) {
            logger.err("Failed to connect server on " + address);
            e.printStackTrace();
        }

        // return
        return this;
    }

    /* ------ Top-Level Services ------ */

    /**
     * Get the core service manager.
     * @return The service manager.
     */
    public ServiceManager services() {
        return serviceManager;
    }

    /**
     * Get the core network manager.
     * @return The network manager.
     */
    public NetworkManager networkManager() {
        return networkManager;
    }

}
