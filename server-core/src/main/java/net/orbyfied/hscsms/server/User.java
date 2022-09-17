package net.orbyfied.hscsms.server;

import java.util.UUID;

public class User {

    // the client logged into this user
    Client client;

    // the uuid and username
    final UUID uuid;
    String username;

    // the MD1 password hash
    byte[] pwdHash;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public void setCredentials(String username,
                               byte[] pwdHash) {
        this.username = username;
        this.pwdHash  = pwdHash;
    }

    public void login(Client client) {
        this.client = client;
    }

}
