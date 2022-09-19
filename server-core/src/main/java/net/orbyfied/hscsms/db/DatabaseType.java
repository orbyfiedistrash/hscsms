package net.orbyfied.hscsms.db;

import net.orbyfied.hscsms.db.impl.MongoDatabaseType;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.registry.Identifier;

public abstract class DatabaseType<D extends Database> {

    protected final Identifier id;

    public DatabaseType(Identifier id) {
        this.id = id;
    }

    public Identifier getIdentifier() {
        return id;
    }

    /* Connections */

    protected abstract void login(D database, Login login);
    protected abstract void close(D database);

    /* Queries */

    protected abstract void putEnv(Database db, Values values);

    //////////////////////////////////

    public static final MongoDatabaseType MONGO_DB = new MongoDatabaseType();

}
