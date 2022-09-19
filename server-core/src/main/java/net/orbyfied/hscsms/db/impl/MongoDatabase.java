package net.orbyfied.hscsms.db.impl;

import com.mongodb.client.MongoClient;
import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseManager;
import net.orbyfied.hscsms.db.DatabaseType;

public class MongoDatabase extends Database {

    public MongoDatabase(DatabaseManager manager, String name) {
        super(manager, name, DatabaseType.MONGO_DB);
    }

    // mongo client
    protected MongoClient client;
    // mongo database client
    protected com.mongodb.client.MongoDatabase db;

    public MongoClient getClient() {
        return client;
    }

    public com.mongodb.client.MongoDatabase getDatabaseClient() {
        return db;
    }

    @Override
    public boolean isOpen() {
        return db != null;
    }

}
