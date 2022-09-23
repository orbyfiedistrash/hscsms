package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.common.ProtocolSpec;
import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketUnboundHandshakeOk;
import net.orbyfied.hscsms.network.handler.ChainAction;
import net.orbyfied.hscsms.network.handler.HandlerNode;
import net.orbyfied.hscsms.network.handler.NodeAction;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.common.protocol.DisconnectReason;
import net.orbyfied.hscsms.security.EncryptionProfile;
import net.orbyfied.hscsms.server.resource.User;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.logging.formatting.TextFormat;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class ServerClient {

    public static String toStringAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    static final Logger LOGGER = Logging.getLogger("ServerClient");

    /////////////////////////////////////////

    // the server
    final Server server;

    // the network handler
    SocketNetworkHandler networkHandler;
    // the client encryption profile
    EncryptionProfile clientEncryptionProfile =
            ProtocolSpec.newSymmetricSecretProfile();

    // the user this client has authenticated as
    // this is null at first
    User user;

    // last disconnect reason
    private DisconnectReason lastDisconnectReason;

    public ServerClient(Server server, Socket socket) {
        this.server = server;
        networkHandler = new SocketNetworkHandler(
                server.networkManager(),
                server.utilityNetworkHandler()
        )
                .owned(this)
                .withDisconnectHandler(this::onDisconnect)
                .connect(socket);
    }

    // disconnect handler
    private void onDisconnect(Throwable t) {
        // check error
        if (t == null) {
            if (lastDisconnectReason != DisconnectReason.CLOSE) {
                LOGGER.info("{0} disconnected", this);
            }
        } else {
            LOGGER.err("{0} disconnected with error", this);
            LOGGER.err(TextFormat.RED_FG + t.toString());
        }

        // destroy client
        this.destroy();
    }

    /**
     * Destroys this client, disconnecting it
     * and removing it from the server list.
     */
    public void destroy() {
        // disconnect client
        disconnect(DisconnectReason.DESTROY);

        // remove client
        server.clients.remove(this);
    }

    /**
     * Disconnects the client from the
     * server.
     */
    public void disconnect(DisconnectReason reason) {
        // close socket
        if (!networkHandler.getSocket().isClosed()) {
            try {
                try {
                    // send disconnect packet
                    if (reason != null) {
                        PacketClientboundDisconnect packet =
                                new PacketClientboundDisconnect(reason);
                        networkHandler.sendSync(packet);
                    }
                } catch (Exception e) {
                    LOGGER.err("Error while disconnecting {0}: Send disconnect packet", this);
                }

                // store reason
                this.lastDisconnectReason = reason;

                // disconnect socket
                networkHandler.getSocket().close();
            } catch (Exception e) {
                LOGGER.err("Error while disconnecting {0}", this);
            }
        }
    }

    public ServerClient stop() {
        // stop network handler
        networkHandler.stop();

        // return
        return this;
    }

    public ServerClient start() {
        // start network handler
        networkHandler.start();

        // add disconnect handler
        networkHandler.node()
                .childForType(PacketServerboundDisconnect.TYPE)
                .withHandler((handler, node, packet) -> {
                    networkHandler.disconnect();
                    return new HandlerNode.Result(ChainAction.HALT);
                });

        // return
        return this;
    }

    public ServerClient readyTopLevelEncryption() {
        // ok message
        AtomicReference<String> okMessage = new AtomicReference<>();

        // initialize decryption before client encryption
        networkHandler.withDecryptionProfile(server.topLevelEncryption);

        // add handshake handler to client
        networkHandler.node().childForType(PacketServerboundClientKey.TYPE)
                .<PacketServerboundClientKey>withHandler((handler, node, packet) -> {
                    // store key
                    clientEncryptionProfile.withSecretKey(packet.getKey());
                    networkHandler.withDecryptionProfile(clientEncryptionProfile);

                    // generate message and send ok packet
                    Random random = new Random();
                    byte[] bytes = new byte[256];
                    for (int i = 0; i < bytes.length; i++)
                        bytes[i] = (byte)(random.nextInt(120) + 30);
                    okMessage.set(new String(bytes, StandardCharsets.UTF_8));

                    networkHandler.sendSyncEncrypted(new PacketUnboundHandshakeOk(okMessage.get()), clientEncryptionProfile);

                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

        // listen to verification
        networkHandler.node().childForType(PacketUnboundHandshakeOk.TYPE)
                .<PacketUnboundHandshakeOk>withHandler((handler, node, packet) -> {
                    if (!(okMessage.get() + "-modified").equals(packet.message)) {
                        LOGGER.err("RSA encrypted handshake verification failed for {0}", this);
                        disconnect(DisconnectReason.KICK);
                    } else {
                        LOGGER.ok("Verified RSA encrypted handshake for {0}", this);
                    }

                    // finish encryption
                    onEncryptionReady();

                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

        // send public key
        networkHandler.sendSync(new PacketClientboundPublicKey(server.topLevelEncryption.getPublicKey()));

        // return
        return this;
    }

    // called when a secure, encrypted
    // connection has been established
    protected void onEncryptionReady() {
        // allow login to a user now
        authenticateAndLogin("orbyfied", "hhr3");
    }

    /* ---------- User ----------- */

    protected record AuthenticationResult(User user, boolean success, Object t) {
        public static AuthenticationResult ofSuccess(User user) {
            return new AuthenticationResult(user, true, null);
        }

        public static AuthenticationResult failPreFind(Object t) {
            return new AuthenticationResult(null, false, t);
        }

        public static AuthenticationResult failPostFind(User user, Object t) {
            return new AuthenticationResult(user, false, t);
        }
    }

    protected AuthenticationResult authenticateAndLogin(String username,
                                                        String password) {
        // find user by username
        User user = server.resourceManager
                .loadDatabaseResourceFiltered(User.TYPE, new Values()
                        .set("username", username));

        // check if we found a user
        if (user == null) {
            return AuthenticationResult.failPreFind("unknown_user");
        }

        try {
            // digest password and compare
            byte[] digestedProvidedPassword = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));

            // compare
            boolean pwMatches = Arrays.equals(digestedProvidedPassword, user.getPasswordHash());
            if (!pwMatches) {
                return AuthenticationResult
                        .failPostFind(user, "invalid_password");
            }
        } catch (Exception e) {
            LOGGER.err("Error while checking login of client {0} to user {1}", this, user.localID());
            e.printStackTrace();
            return AuthenticationResult.failPostFind(user, e);
        }

        // log in client
        this.user = user;
        this.user.login(this);

        // return success
        return AuthenticationResult.ofSuccess(user);
    }

    protected void logOut() {
        // log out
        user.logout(this);
        // dispose of user resource
        server.resourceManager().unloadResource(user);
    }

    /* ---------- Other ------------ */

    @Override
    public String toString() {
        return "ServerClient[" + toStringAddress() + "]";
    }

    public String toStringAddress() {
        return toStringAddress(networkHandler.getSocket());
    }

}
