package net.orbyfied.hscsms.core;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.orbyfied.hscsms.db.DatabaseType;
import net.orbyfied.hscsms.db.QueryPool;
import net.orbyfied.hscsms.db.impl.MongoDatabaseItem;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.reflect.Reflector;
import org.bson.BsonInt32;
import org.bson.Document;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class ServerResourceManager {

    public static final Logger LOGGER = Logging.getLogger("ServerResources");
    private static final Reflector reflector = new Reflector("ServerResources");

    ///////////////////////////////////////////////////////////

    public ServerResourceManager(Server server) {
        this.server = server;
        globalQueryPool = server.databaseManager().queryPool();
    }

    // the server
    private final Server server;

    // the loaded resource types
    private final Int2ObjectOpenHashMap<ServerResourceType> typesByHash = new Int2ObjectOpenHashMap<>();
    private final ArrayList<ServerResourceType>             types       = new ArrayList<>();

    // the loaded resources
    private final List<ServerResource> allResources = new ArrayList<>();

    private final QueryPool globalQueryPool;

    // the thread local query pools
    private final ThreadLocal<QueryPool> queryPool = new ThreadLocal<>();

    public void setup() {
        // load query pool presets
        loadQueryPoolPresets(globalQueryPool);
    }

    /**
     * The global query pool, should only be
     * used for defining new queries, not for
     * executing them.
     * @return The global query pool.
     */
    public QueryPool getGlobalQueryPool() {
        return globalQueryPool;
    }

    /**
     * Get the thread local query pool.
     * If it has not been created yet it will be
     * forked from the global query pool.
     * @see ServerResourceManager#getGlobalQueryPool()
     * @return The query pool.
     */
    public QueryPool getLocalQueryPool() {
        QueryPool q;
        if ((q = queryPool.get()) != null)
            return q;
        q = globalQueryPool.fork();
        queryPool.set(q);
        return q;
    }

    public ServerResourceManager register(ServerResourceType type) {
        types.add(type);
        typesByHash.put(type.getIdentifierHash(), type);
        return this;
    }

    public List<ServerResource> getAllResources() {
        return allResources;
    }

    public ArrayList<ServerResourceType> getTypes() {
        return types;
    }

    public ServerResourceType getType(String id) {
        return getType(Identifier.of(id));
    }

    public ServerResourceType getType(Identifier id) {
        return getType(id.hashCode());
    }

    public ServerResourceType getType(int hash) {
        return typesByHash.get(hash);
    }

    public ServerResourceManager compileResourceClass(Class<? extends ServerResource> klass) {
        try {
            // get and register type
            Field field = reflector.reflectDeclaredField(klass, "TYPE");
            ServerResourceType<?> type = reflector.reflectGetField(field, null);
            register(type);

            // return
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    /* ---- Database ---- */

    public String getCollectionName() {
        return server.getName() + "_resources";
    }

    private MongoCollection<Document> mongoGetOrCreateResCollection(MongoDatabase mongoDatabase) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(getCollectionName());
        return collection;
    }

    private void loadQueryPoolPresets(QueryPool pool) {
        pool.putQuery("find_resource_uuid", DatabaseType.MONGO_DB, (query, database, values) -> {
            // create filter
            Document filter = new Document();
            filter.put("_id", new BsonInt32(0)); // ignore _id field
            filter.put("uuid", values.get("uuid"));

            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());
            Document doc = collection.find(filter).first();
            if (doc != null) {
                return new MongoDatabaseItem(database, "uuid", collection, doc);
            } else {
                return null;
            }
        });

        pool.putQuery("find_resource_local", DatabaseType.MONGO_DB, (query, database, values) -> {
            // create filter
            Document filter = new Document();
            filter.put("_id", new BsonInt32(0)); // ignore _id field
            filter.put("localId", values.get("localId"));

            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());
            Document doc = collection.find(filter).first();
            if (doc != null) {
                return new MongoDatabaseItem(database, "uuid", collection, doc);
            } else {
                return null;
            }
        });
    }

}
