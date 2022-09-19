package net.orbyfied.hscsms.db.impl;

import com.mongodb.client.MongoCollection;
import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseItem;
import org.bson.BsonInt32;
import org.bson.Document;

public class MongoDatabaseItem extends DatabaseItem {

    /**
     * The document it will be wrapping.
     */
    Document document;

    /**
     * The collection this item is stored in.
     */
    MongoCollection<Document> collection;

    /**
     * The primary key value name.
     */
    String keyName;

    public MongoDatabaseItem(Database database,
                             String keyName,
                             MongoCollection<Document> collection,
                             Document document) {
        super(database);
        this.keyName    = keyName;
        this.collection = collection;
        this.document   = document;
    }

    public Document createFilter() {
        Document filter = new Document();
        filter.put(keyName, document.get(keyName));
        filter.put("_id", new BsonInt32(0)); // exclude _id field
        return filter;
    }

    @Override
    public Object key() {
        return document.get(keyName);
    }

    @Override
    public void set(String key, Object val) {
        document.put(key, val);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return document.get(key, type);
    }

    @Override
    public void push() {
        collection.updateOne(createFilter(), document);
    }

    @Override
    public void pull() {
        document = collection.find(createFilter()).first();
    }

}
