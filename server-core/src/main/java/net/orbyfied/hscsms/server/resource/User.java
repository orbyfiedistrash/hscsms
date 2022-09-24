package net.orbyfied.hscsms.server.resource;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.core.resource.ServerResourceType;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.hscsms.server.ServerClient;
import net.orbyfied.j8.registry.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class User extends ServerResource {

    public static final ServerResourceType<User> TYPE = new ServerResourceType<>(
            Identifier.of("user"), User.class
    ) {
        @Override
        public UUID createLocalID() {
            return new UUID(System.currentTimeMillis(), RANDOM.nextLong());
        }

        @Override
        public ResourceSaveResult saveResource(ServerResourceManager manager, DatabaseItem dbItem, User user) {
            dbItem.set("password", user.passwordHash);
            dbItem.set("username", user.username);
            return ResourceSaveResult.ofSuccess();
        }

        @Override
        public ResourceLoadResult loadResource(ServerResourceManager manager, DatabaseItem dbItem, User user) {
            user.passwordHash = dbItem.get("password", byte[].class);
            user.username = dbItem.get("username", String.class);
            return ResourceLoadResult.ofSuccess();
        }
    };

    // the SHA256 digest instance
    private static MessageDigest SHA256;

    static {
        try {
            SHA256 = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////

    // the client logged into this user
    ServerClient client;

    // the username
    String username;

    // the SHA256 password hash
    byte[] passwordHash;

    public User(UUID uuid, UUID localId) {
        super(uuid, TYPE, localId);
    }

    public User setCredentials(String username,
                               byte[] pwdHash) {
        this.username     = username;
        this.passwordHash = pwdHash;
        return this;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public User setPasswordLocal(String name) {
        // hash password
        this.passwordHash = SHA256.digest(name.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public User login(ServerClient client) {
        this.client = client;
        return this;
    }

    public User logout(ServerClient client) {
        this.client = null;
        return this;
    }

}
