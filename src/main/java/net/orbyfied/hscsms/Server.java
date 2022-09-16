package net.orbyfied.hscsms;

import net.orbyfied.hscsms.core.ServiceManager;
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

    // the service manager
    ServiceManager serviceManager;

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

    /**
     * Get the core service manager.
     * @return The service manager.
     */
    public ServiceManager services() {
        return serviceManager;
    }

}
