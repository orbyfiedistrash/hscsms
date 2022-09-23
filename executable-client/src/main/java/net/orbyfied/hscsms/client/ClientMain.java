package net.orbyfied.hscsms.client;

import net.orbyfied.hscsms.client.app.ClientApp;
import net.orbyfied.hscsms.client.app.ConnectScreenAC;
import net.orbyfied.hscsms.client.applib.impl.ExitAC;
import net.orbyfied.hscsms.common.ProtocolSpec;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketUnboundHandshakeOk;
import net.orbyfied.hscsms.libexec.ArgParseException;
import net.orbyfied.hscsms.libexec.ArgParser;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.handler.ChainAction;
import net.orbyfied.hscsms.network.handler.HandlerNode;
import net.orbyfied.hscsms.network.handler.NodeAction;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.security.LegacyEncryptionProfile;
import net.orbyfied.hscsms.server.ServerClient;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Base64;
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
    public SocketNetworkHandler networkHandler =
            new SocketNetworkHandler(networkManager, null);

    public final LegacyEncryptionProfile serverEncryptionProfile =
            ProtocolSpec.newBlankEncryptionProfile();
    private final LegacyEncryptionProfile clientEncryptionProfile =
            ProtocolSpec.newSymmetricSecretProfile();

    /**
     * The working directory.
     */
    public Path workDir;

    /**
     * The client application.
     */
    public ClientApp app;

    public void disconnect() {
        if (networkHandler.isOpen()) {
            networkHandler.sendSync(new PacketServerboundDisconnect());
            networkHandler.disconnect();
            networkHandler.stop();

            // create new network handler
            // to reset handlers and stuff
            networkHandler = new SocketNetworkHandler(networkManager, null);
        }
    }

    private void initHandshake() {

        HandlerNode node = networkHandler.node();
        node.childForType(PacketClientboundPublicKey.TYPE)
                .<PacketClientboundPublicKey>withHandler((handler, node1, packet) -> {
                    // set public key
                    serverEncryptionProfile.withPublicKey(packet.getKey());
                    networkHandler.withDecryptionProfile(serverEncryptionProfile);

                    // generate private key
                    clientEncryptionProfile.generateSymmetricKey();
                    
                    // send serverbound private key packet, encrypted
                    networkHandler.sendSyncEncrypted(
                            new PacketServerboundClientKey(clientEncryptionProfile.getSecretKey()),
                            serverEncryptionProfile
                    );

                    // use new decryption profile
                    networkHandler.withDecryptionProfile(clientEncryptionProfile);

                    // return and remove this node
                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

        node.childForType(PacketUnboundHandshakeOk.TYPE)
                .<PacketUnboundHandshakeOk>withHandler((handler, node1, packet) -> {
                    // modify message
                    String message = packet.message + "-modified";

                    // send new packet
                    networkHandler.sendSyncEncrypted(new PacketUnboundHandshakeOk(message),
                            clientEncryptionProfile);

                    // return and remove this node
                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

    }

    public void reconnect(Socket socket) {
        LOGGER.info("Reconnecting to [" + ServerClient.toStringAddress(socket) + "]");
        // disconnect from current server
        disconnect();

        // prepare handshake
        initHandshake();

        // connect
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

    public void runApp() {
        // create application
        app = new ClientApp(this);

        {
            app.appContextManager.addContext(ExitAC::new);
            app.appContextManager.addContext(ConnectScreenAC::new);
        }

        app.appContextManager.setCurrentContext("connect_screen");
    }

    public void run(String[] args) {
        // parse args
        Values argVals;
        try {
            argVals = new ArgParser()
                    .withArgument("work-dir", Path.class)
                    .withArgument("connect", String.class)
                    .parseConsoleArgs(args);
        } catch (ArgParseException e) {
            System.err.println("Error ArgParser: " + e.getMessage());
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            return;
        }

        // set properties
        workDir = argVals.getOrDefault("work-dir", Path.of("./hscsms-client"));

        // run app
//        runApp();

        // prepare protocol
        ProtocolSpec.loadProtocol(networkManager);

        // prepare input
        Scanner scanner = new Scanner(System.in);

        // input address
        String addrln;
        if (argVals.contains("connect")) {
            addrln = argVals.get("connect");
        } else {
            addrln = scanner.nextLine();
        }

        String[] addr = addrln.split(":");
        String host = addr[0];
        if (host.equals("localhost") || host.isBlank())
            host = "0.0.0.0";
        int port = 42069;
        if (addr.length > 1)
            port = Integer.parseInt(addr[1]);

        // connect to server
        reconnect(new InetSocketAddress(host, port));

        // while socket is open
        Socket socket = networkHandler.getSocket();
        while (!socket.isClosed()) {
            String s = scanner.nextLine();
            if (s.startsWith("/")) {
                String cmd = s.substring(1);
                switch (cmd) {
                    case "q" -> disconnect();
                }
            } else {

            }
        }

        // disconnect socket
        try {
            if (networkHandler.active()) {
                networkHandler.disconnect();
                networkHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // create client
        client = new ClientMain();
        client.run(args);
    }

}
