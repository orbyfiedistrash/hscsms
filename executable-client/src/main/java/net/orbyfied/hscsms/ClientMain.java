package net.orbyfied.hscsms;

import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.server.ServerClient;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    public static final Logger LOGGER
            = Logging.getLogger("Client");

    ///////////////////////////////////////

    ClientMain() {

    }

    /**
     * Client network manager.
     */
    public final NetworkManager networkManager =
            new NetworkManager();

    /**
     * Client network handler.
     */
    public final SocketNetworkHandler networkHandler =
             new SocketNetworkHandler(networkManager, null);

    public void disconnect() {
        if (networkHandler.isOpen()) {
            networkHandler.sendSync(new PacketServerboundDisconnect());
            networkHandler.disconnect();
            networkHandler.stop();
        }
    }

    public void reconnect(Socket socket) {
        LOGGER.info("Reconnecting to [" + ServerClient.toStringAddress(socket) + "]");
        disconnect();
        networkHandler.connect(socket);
        networkHandler.start();
    }

    public void reconnect(InetSocketAddress address) {
        try {
            Socket socket = new Socket();
            socket.connect(address);
            reconnect(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------------------------- */

    private static ClientMain client;

    public static ClientMain getClient() {
        return client;
    }

    public static void main(String[] args) {
        // create client
        client = new ClientMain();

        // prepare input
        Scanner scanner = new Scanner(System.in);

        // input address
        System.out.print("Server Address: ");
        String[] addr = scanner.nextLine().split(":");
        String host = addr[0];
        int port = 42069;
        if (addr.length > 1)
            port = Integer.parseInt(addr[1]);

        // connect to server
        client.reconnect(new InetSocketAddress(host, port));
        System.out.println();

        // while socket is open
        Socket socket = client.networkHandler.getSocket();
        while (!socket.isClosed()) {
            // get line
            String line = scanner.nextLine();

            // client command
            if (line.startsWith("/")) {
                // get command
                String cmdStr = line.substring(1);
                if ("q".equals(cmdStr)) {
                    client.disconnect();
                    break;
                }
            } else {
                // send message to server

            }
        }

        // disconnect socket
        try {
            if (client.networkHandler.active()) {
                client.networkHandler.disconnect();
                client.networkHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
