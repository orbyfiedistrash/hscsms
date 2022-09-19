package net.orbyfied.hscsms.db;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Database {

    // the database name
    protected final String name;

    // the database manager dependency
    protected final DatabaseManager manager;

    // the database type
    protected final DatabaseType type;

    // the universal query pool
    public final QueryPool universalQueryPool;

    public Database(DatabaseManager manager,
                    String name,
                    DatabaseType type) {
        this.manager = manager;
        this.name    = name;
        this.type    = type;

        this.universalQueryPool = queryPool();
    }

    public DatabaseType type() {
        return type;
    }

    public DatabaseManager manager() {
        return manager;
    }

    public String name() {
        return name;
    }

    public void login(Login login) {
        type.login(this, login);
    }

    public void close() {
        type.close(this);
    }

    public QueryPool queryPool() {
        return new QueryPool(null).current(this);
    }

}
