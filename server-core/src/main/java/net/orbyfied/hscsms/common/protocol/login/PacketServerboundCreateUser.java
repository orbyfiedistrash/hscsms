package net.orbyfied.hscsms.common.protocol.login;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

public class PacketServerboundCreateUser extends Packet {

    public static final PacketType<PacketServerboundCreateUser> TYPE =
            new PacketType<>(PacketServerboundCreateUser.class, "hscsms/login/serverbound/createuser")
            .serializer((type, packet, stream) -> {
                stream.writeUTF(packet.username);
                stream.writeUTF(packet.password);
            })
            .deserializer((type, stream) -> {
                String username = stream.readUTF();
                String password = stream.readUTF();
                return new PacketServerboundCreateUser(username, password);
            });

    /////////////////////////////////////////////////

    private String username;
    private String password;

    public PacketServerboundCreateUser(String username, String password) {
        super(TYPE);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
