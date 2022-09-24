package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.common.ProtocolSpec;
import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketClientboundPublicKey;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.common.protocol.handshake.PacketServerboundClientKey;
import net.orbyfied.hscsms.common.protocol.handshake.PacketUnboundHandshakeOk;
import net.orbyfied.hscsms.common.protocol.login.PacketServerboundCreateUser;
import net.orbyfied.hscsms.core.resource.ServerResourceHandle;
import net.orbyfied.hscsms.network.handler.ChainAction;
import net.orbyfied.hscsms.network.handler.HandlerNode;
import net.orbyfied.hscsms.network.handler.NodeAction;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.common.protocol.DisconnectReason;
import net.orbyfied.hscsms.security.SymmetricEncryptionProfile;
import net.orbyfied.hscsms.server.resource.User;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.logging.formatting.TextFormat;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
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
    SymmetricEncryptionProfile clientEncryptionProfile =
            ProtocolSpec.newSymmetricEncryptionProfile();

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
        networkHandler.withEncryptionProfile(server.topLevelEncryption);

        // add handshake handler to client
        networkHandler.node().childForType(PacketServerboundClientKey.TYPE)
                .<PacketServerboundClientKey>withHandler((handler, node, packet) -> {
                    // store key
                    clientEncryptionProfile.withKey("secret", packet.getKey());
                    networkHandler
                            .withEncryptionProfile(clientEncryptionProfile)
                            .autoEncrypt(true);

                    // generate message and send ok packet
                    Random random = new Random();
                    byte[] bytes = new byte[16];
                    for (int i = 0; i < bytes.length; i++)
                        bytes[i] = (byte)(random.nextInt(120) + 30);
                    okMessage.set(Base64.getEncoder().encodeToString(bytes));

                    networkHandler.sendSync(new PacketUnboundHandshakeOk(okMessage.get()));

                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

        // listen to verification
        networkHandler.node().childForType(PacketUnboundHandshakeOk.TYPE)
                .<PacketUnboundHandshakeOk>withHandler((handler, node, packet) -> {
                    if (!(okMessage.get() + "-modified").equals(packet.message)) {
                        LOGGER.err("AES encrypted handshake verification failed for {0}", this);
                        disconnect(DisconnectReason.KICK);
                    } else {
                        LOGGER.ok("Verified AES encrypted handshake for {0}", this);
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
        onEnterLoginState();
    }

    // called when we the client is allowed to
    // login or create a new user
    protected void onEnterLoginState() {
        // allow user creation
        networkHandler.node().childForType(PacketServerboundCreateUser.TYPE)
                .<PacketServerboundCreateUser>withHandler((handler, node, packet) -> {
                    // create new user
                    UserCreateResult result = createUser(packet.getUsername(), packet.getPassword());

                    // check result
                    if (result.success) {
                        //
                    } else {

                    }

                    // return
                    return new HandlerNode.Result(ChainAction.CONTINUE).nodeAction(NodeAction.REMOVE);
                });
    }

    /* ---------- User ----------- */

    public record UserAuthenticationResult(User user, boolean success, Object t) {
        public static UserAuthenticationResult ofSuccess(User user) { return new UserAuthenticationResult(user, true, null); }
        public static UserAuthenticationResult failPreFind(Object t) { return new UserAuthenticationResult(null, false, t); }
        public static UserAuthenticationResult failPostFind(User user, Object t) { return new UserAuthenticationResult(user, false, t); }
    }

    public record UserCreateResult(UUID uuid, boolean success, Object t) {
        public static UserCreateResult ofSuccess(UUID uuid) { return new UserCreateResult(uuid, true, null); }
        public static UserCreateResult fail(Object t) { return new UserCreateResult(null, false, t); }
    }

    protected boolean checkUserName(String username) {
        return true; // TODO
    }

    protected UserCreateResult createUser(String username,
                              String password) {
        try {
            // check credentials
            if (!checkUserName(username))
                return UserCreateResult.fail("invalid_username");

            // create user resource
            ServerResourceHandle<User> userHandle = server.resourceManager().createResource(User.TYPE);
            userHandle.useOrNot(user -> {
                user.setUsername(username);
                user.setPasswordLocal(password);
            });

            // log and return success
            LOGGER.info("{0} created user " + user.universalID() + "('" + username + "')");
            return UserCreateResult.ofSuccess(user.universalID());
        } catch (Exception e) {
            e.printStackTrace();
            return UserCreateResult.fail(e);
        }
    }

    protected UserAuthenticationResult authenticateAndLogin(String username,
                                                            String password) {
        // find user by username
        User user = server.resourceManager
                .loadDatabaseResourceFiltered(User.TYPE, new Values()
                        .set("username", username));

        // check if we found a user
        if (user == null) {
            return UserAuthenticationResult.failPreFind("unknown_user");
        }

        try {
            // digest password and compare
            byte[] digestedProvidedPassword = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));

            // compare
            boolean pwMatches = Arrays.equals(digestedProvidedPassword, user.getPasswordHash());
            if (!pwMatches) {
                return UserAuthenticationResult
                        .failPostFind(user, "invalid_password");
            }
        } catch (Exception e) {
            LOGGER.err("Error while checking login of client {0} to user {1}", this, user.localID());
            e.printStackTrace();
            return UserAuthenticationResult.failPostFind(user, e);
        }

        // log in client
        this.user = user;
        this.user.login(this);

        // return success
        return UserAuthenticationResult.ofSuccess(user);
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
