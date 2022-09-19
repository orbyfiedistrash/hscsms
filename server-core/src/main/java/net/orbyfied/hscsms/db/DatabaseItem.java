package net.orbyfied.hscsms.db;

import javax.xml.crypto.Data;

/**
 * Represents one item in a database.
 */
public abstract class DatabaseItem {

    // the database this item is contained in
    final Database database;

    public DatabaseItem(Database database) {
        this.database = database;
    }

    public Database database() {
        return database;
    }

    /**
     * Get the primary key of this database item,
     * this can be anything from a string to a
     * blob (depending on database implementation)
     * @return The key or null if absent.
     */
    public abstract Object key();

    /**
     * Set a value in this item by key, in, for example, MongoDB this
     * would be a key in a BSON document, where the value is serialized
     * to a JSON value, while in SQL this key might be a column name in
     * the target table and the value is serialized to an SQL value.
     * @param key The key.
     * @param val The value.
     */
    public abstract void set(String key, Object val);

    /**
     * Get a value from this item by key, in, for example, MongoDB this
     * would be a key in a BSON document, where the value is deserialized
     * from a JSON value, while in SQL this key might be a column name in
     * the target table and the value is deserialized from an SQL value.
     * @param key The key.
     * @param type The type to deserialize to.
     */
    public abstract <T> T get(String key, Class<T> type);

    /**
     * Will update the data in the item in the database,
     * like pushing the changes into the database, hence
     * the name. This is not required if the local copy
     * mirrors the database in real time.
     */
    public abstract void push();

    /**
     * Will update the local copy of the data from the
     * database, pulling the changes from the database.
     * This is not required if the local copy mirrors
     * the database in real time.
     */
    public abstract void pull();

}
