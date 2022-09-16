package net.orbyfied.hscsms.db;

@SuppressWarnings("unchecked")
public abstract class Database {

    // the database name
    protected final String name;

    // the database manager dependency
    protected final DatabaseManager manager;

    // the database type
    protected final DatabaseType    type;

    public Database(DatabaseManager manager,
                    String name,
                    DatabaseType type) {
        this.manager = manager;
        this.name    = name;
        this.type    = type;
    }

    public DatabaseType getType() {
        return type;
    }

    public DatabaseManager getManager() {
        return manager;
    }

    public String getName() {
        return name;
    }

    public void login(Login login) {
        type.login(this, login);
    }

    public void close() {
        type.close(this);
    }

    public QueryPool queryPool() {
        return new QueryPool(this, type);
    }

}
