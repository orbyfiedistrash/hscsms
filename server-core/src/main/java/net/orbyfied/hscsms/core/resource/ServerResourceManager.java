package net.orbyfied.hscsms.core.resource;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.hscsms.db.DatabaseType;
import net.orbyfied.hscsms.db.QueryPool;
import net.orbyfied.hscsms.db.impl.MongoDatabaseItem;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.reflect.Reflector;
import org.bson.BsonInt32;
import org.bson.Document;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public class ServerResourceManager {

    public static final Logger LOGGER = Logging.getLogger("ServerResources");
    private static final Reflector reflector = new Reflector("ServerResources");

    // a utility random instance for generating IDs
    protected static final Random RANDOM =
            new Random(System.currentTimeMillis() * 91 ^ System.nanoTime() << 3);

    public static final UUID NULL_ID = new UUID(0, 0);

    public static UUID getMemoryMapLocalKey(int typeHash, UUID id) {
        return new UUID(
                id.getMostSignificantBits() ^ ((long)typeHash * 31L),
                id.getLeastSignificantBits());
    }

    public static UUID getMemoryMapLocalKey(ServerResource resource) {
        return getMemoryMapLocalKey(resource.type().idHash, resource.localID());
    }

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
    private final Object2ObjectOpenHashMap<UUID, ServerResource> resourcesByLID  = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, ServerResource> resourcesByUUID = new Object2ObjectOpenHashMap<>();

    // the database
    private Database database;
    // the global query pool
    private final QueryPool globalQueryPool;
    // the thread local query pools
    private final ThreadLocal<QueryPool> queryPool = new ThreadLocal<>();

    public void setup() {
        // load query pool presets
        loadQueryPoolPresets(globalQueryPool);
    }

    public ServerResourceManager database(Database database) {
        this.database = database;
        return this;
    }

    public Database database() {
        return database;
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

    /* ---- Type ---- */

    public ServerResourceManager registerType(ServerResourceType type) {
        types.add(type);
        typesByHash.put(type.getIdentifierHash(), type);
        return this;
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
            registerType(type);

            // return
            return this;
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return this;
        }
    }

    /* ---- Resources ---- */

    public ServerResourceManager addLoaded(ServerResource resource) {
        resourcesByUUID.put(resource.universalID(), resource);
        resourcesByLID.put(getMemoryMapLocalKey(resource), resource);
        return this;
    }

    public ServerResourceManager removeLoaded(ServerResource resource) {
        resourcesByUUID.remove(resource.universalID());
        resourcesByLID.remove(getMemoryMapLocalKey(resource));
        return this;
    }

    public ServerResource getLoadedLocal(ServerResourceType type,
                                   UUID id) {
        return resourcesByLID.get(getMemoryMapLocalKey(type.idHash, id));
    }

    public ServerResource getLoadedUniversal(UUID uuid) {
        return resourcesByUUID.get(uuid);
    }

    /* ---- Functional ---- */

    /**
     * Creates a new universal unique ID
     * for a new recourse. This should have
     * no possibility of colliding with another.
     * @return The universal unique ID.
     */
    public UUID createUniversalID() {
        return new UUID(
                System.currentTimeMillis(),
                System.nanoTime() ^ RANDOM.nextInt()
        );
    }

    /**
     * Creates a new resource of type {@code type}
     * with a unique universal and local ID, and
     * registers it to the loaded resources.
     * @param type The resource type.
     * @param <R> The resource object class.
     * @return The resource.
     */
    public <R extends ServerResource> R createResource(ServerResourceType<R> type) {
        // generate ids
        UUID uuid    = createUniversalID();
        UUID localId = type.createLocalID();

        // create new resource
        // and register it
        R resource = type.newInstanceInternal(uuid, localId);
        addLoaded(resource);

        // return
        return resource;
    }

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> R loadResource(UUID uuid) {
        // try and index cache
        ServerResource res;
        if ((res = getLoadedUniversal(uuid)) != null)
            return (R) res;

        // check database open

        // find document
        DatabaseItem item = findDatabaseResource(uuid);

        // get properties
        UUID localId  = item.get("localId", UUID.class);
        int  typeHash = item.get("type", Integer.class);

        // get type
        ServerResourceType<R> type = getType(typeHash);

        // construct instance
        R resource = type.newInstanceInternal(uuid, localId);

        // load data
        type.loadResourceSafe(this, item, resource);

        // add loaded
        addLoaded(resource);

        // return
        return resource;
    }

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> R loadResourceLocal(ServerResourceType<R> type,
                                                          UUID localId) {
        ServerResource resource;
        if ((resource = getLoadedLocal(type, localId)) != null)
            return (R) resource;
        return type.loadResourceLocal(this, localId);
    }

    @SuppressWarnings("unchecked")
    public ServerResourceManager saveResource(ServerResource resource) {
        // get type
        ServerResourceType<? extends ServerResource> type = resource.type();

        // call
        type.saveResource(this, resource);

        // return
        return this;
    }

    public ServerResourceManager unloadResource(ServerResource resource) {
        // remove resource
        removeLoaded(resource);

        // return
        return this;
    }

    /**
     * Loads a resource asynchronously.
     * @see ServerResourceManager#loadResource(UUID)
     */
    public <R extends ServerResource> CompletableFuture<R> loadResourceAsync(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadResource(uuid));
    }

    /**
     * Loads a resource by local ID asynchronously.
     * @see ServerResourceManager#loadResourceLocal(ServerResourceType, UUID)
     */
    public <R extends ServerResource> CompletableFuture<R> loadResourceLocalAsync(final ServerResourceType<R> type,
                                                                                  final UUID localId) {
        return CompletableFuture.supplyAsync(() -> loadResourceLocal(type, localId));
    }

    /**
     * Saves a resource asynchronously.
     * @see ServerResourceManager#saveResource(ServerResource)
     */
    public CompletableFuture<Void> saveResourceAsync(final ServerResource resource) {
        return CompletableFuture.runAsync(() -> saveResource(resource));
    }

    public UUID saveResourceReference(final ServerResource resource) {
        if (resource == null)
            return NULL_ID;
        saveResourceAsync(resource);
        return resource.universalID();
    }

    public <R extends ServerResource> R loadResourceReferenceSync(final UUID uuid) {
        if (uuid.equals(NULL_ID))
            return null;
        return loadResource(uuid);
    }

    /**
     * Finds the database item of the resource by UUID.
     * @param uuid The resource UUID.
     * @return The item or null if absent.
     */
    public DatabaseItem findDatabaseResource(UUID uuid) {
        QueryPool pool = getLocalQueryPool();
        return pool.current(requireDatabase())
                .querySync("find_resource_uuid", new Values().setRaw("uuid", uuid));
    }

    /**
     * Find or create the database item of the resource
     * by UUID. It will create a new entry if absent.
     * @param uuid The UUID.
     * @return Database item.
     */
    public DatabaseItem findOrCreateDatabaseResource(UUID uuid) {
        // try to find document
        DatabaseItem item = findDatabaseResource(uuid);

        // create if null
        if (item == null) {
            QueryPool pool = getLocalQueryPool();
            item = pool.current(requireDatabase())
                    .querySync("create_get_resource_uuid", new Values().setRaw("uuid", uuid));
        }

        // return item
        return item;
    }

    /* ---- Database ---- */

    public boolean isDatabaseOpen() {
        return database != null && database.isOpen();
    }

    @SuppressWarnings("unchecked")
    public <D extends Database> D requireDatabase() {
        if (!isDatabaseOpen())
            throw new IllegalStateException("resource database isn't opened");
        return (D) database;
    }

    @SuppressWarnings("unchecked")
    public <D extends Database> D requireDatabase(Class<D> dClass) {
        if (!isDatabaseOpen())
            throw new IllegalStateException("resource database isn't opened");
        return (D) database;
    }

    public String getCollectionName() {
        return server.name() + "_resources";
    }

    private MongoCollection<Document> mongoGetOrCreateResCollection(MongoDatabase mongoDatabase) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(getCollectionName());
        return collection;
    }

    private void loadQueryPoolPresets(QueryPool pool) {

        pool.putQuery("find_resource_uuid", DatabaseType.MONGO_DB, (query, database, values) -> {
            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());
            UUID uuid = values.get("uuid");
            Document doc = collection.find(
                    Filters.and(Projections.excludeId(),
                            Filters.eq("uuid", values.get("uuid")))
            ).projection(Projections.excludeId()).first();
            if (doc != null) {
                return new MongoDatabaseItem(database, "uuid", collection, uuid)
                        .pull();
            } else {
                return null;
            }
        });

        pool.putQuery("find_resource_local", DatabaseType.MONGO_DB, (query, database, values) -> {
            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());
            Document doc = collection.find(
                    Filters.and(Filters.eq("localId", values.get("localId")),
                            Filters.eq("type", values.get("typeHash")))
            ).projection(Projections.excludeId()).first();
            if (doc != null) {
                return new MongoDatabaseItem(database, "uuid", collection, doc.get("uuid", UUID.class))
                        .pull();
            } else {
                return null;
            }
        });

        pool.putQuery("create_get_resource_uuid", DatabaseType.MONGO_DB, (query, database, values) -> {
            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());

            // create document
            Document document = new Document();
            UUID uuid = values.getRaw("uuid");
            document.put("uuid", uuid);
            collection.insertOne(document);

            // create database item
            return new MongoDatabaseItem(database, "uuid", collection, uuid)
                    .pull();
        });

    }

}
