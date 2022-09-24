package net.orbyfied.hscsms.core.resource;

import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.orbyfied.hscsms.core.resource.event.ResourceCreateEvent;
import net.orbyfied.hscsms.core.resource.event.ResourceHandleAcquireEvent;
import net.orbyfied.hscsms.core.resource.event.ResourceHandleReleaseEvent;
import net.orbyfied.hscsms.core.resource.event.ResourceUnloadEvent;
import net.orbyfied.hscsms.core.resource.impl.ResourceGCService;
import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.hscsms.db.DatabaseType;
import net.orbyfied.hscsms.db.QueryPool;
import net.orbyfied.hscsms.db.impl.MongoDatabaseItem;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.event.ComplexEventBus;
import net.orbyfied.j8.event.util.Pipelines;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.reflect.Reflector;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
        // set fields
        this.server = server;

        // configure event bus
        eventBus.withDefaultPipelineFactory((bus, eventClass) -> Pipelines.mono(bus));

        eventBus.bake(ResourceCreateEvent.class);
        eventBus.bake(ResourceUnloadEvent.class);
        eventBus.bake(ResourceHandleAcquireEvent.class);

        // create global query pool
        globalQueryPool = server.databaseManager().queryPool();

        // add garbage collection service
        withService(ResourceGCService::new);
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

    // services
    protected final ComplexEventBus eventBus = new ComplexEventBus();
    protected final List<ResourceService>                                      services = new ArrayList<>();
    protected final Object2ObjectOpenHashMap<Class<?>, ResourceService> servicesByClass = new Object2ObjectOpenHashMap<>();

    public void setup() {
        // load query pool presets
        loadQueryPoolPresets(globalQueryPool);
    }

    /* ----- Services ----- */

    public ComplexEventBus getEventBus() {
        return eventBus;
    }

    public ServerResourceManager withService(ResourceService service) {
        services.add(service);
        servicesByClass.put(service.getClass(), service);
        service.added();
        return this;
    }

    public <S extends ResourceService> ServerResourceManager withService(Function<ServerResourceManager, S> constructor,
                                                                         BiConsumer<ServerResourceManager, S> consumer) {
        S service = constructor.apply(this);
        if (consumer != null)
            consumer.accept(this, service);
        return withService(service);
    }

    public <S extends ResourceService> ServerResourceManager withService(Function<ServerResourceManager, S> constructor) {
        return withService(constructor, null);
    }

    @SuppressWarnings("unchecked")
    public <S extends ResourceService> S serviceByClass(Class<S> sClass) {
        return (S) servicesByClass.get(sClass);
    }

    public ServerResourceManager withoutService(ResourceService service) {
        services.remove(service);
        servicesByClass.remove(service.getClass(), service);
        service.removed();
        return this;
    }

    public ServerResourceManager withoutService(Class<? extends ResourceService> klass) {
        return withoutService(serviceByClass(klass));
    }

    /* ----- Databases ----- */

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

    /* ---- Resource Types ---- */

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

    public boolean isLoaded(ServerResource resource) {
        return resourcesByUUID.containsKey(resource.universalID());
    }

    public boolean isLoaded(UUID uuid) {
        return resourcesByUUID.containsKey(uuid);
    }

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

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> R getLoadedLocal(ServerResourceType type,
                                                       UUID id) {
        return (R) resourcesByLID.get(getMemoryMapLocalKey(type.idHash, id));
    }

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> R getLoadedUniversal(UUID uuid) {
        return (R) resourcesByUUID.get(uuid);
    }

    /**
     * Creates a new universal unique ID
     * for a new recourse. This should have
     * no possibility of colliding with another.
     * @return The universal unique ID.
     */
    public UUID createUniversalID() {
        return new UUID(
                System.currentTimeMillis(),
                System.nanoTime()
        );
    }

    public <R extends ServerResource> R createResourceUnwrapped(ServerResourceType<R> type) {
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

    /**
     * Creates a new resource of type {@code type}
     * with a unique universal and local ID, and
     * registers it to the loaded resources.
     * @param type The resource type.
     * @param <R> The resource object class.
     * @return The resource.
     */
    public <R extends ServerResource> ServerResourceHandle<R> createResource(ServerResourceType<R> type) {
        return createHandleLoaded(createResourceUnwrapped(type));
    }

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> R loadResourceUnwrapped(UUID uuid) {
        // try and index cache
        ServerResource res;
        if ((res = getLoadedUniversal(uuid)) != null)
            return (R) res;

        // find document
        DatabaseItem item = findDatabaseResource(uuid);
        if (item != null) {
            // get properties
            UUID localId = item.get("localId", UUID.class);
            int typeHash = item.get("type", Integer.class);

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
        } else {
            return null;
        }
    }

    /**
     * Loads a resource using the universal ID, first
     * checking if it has already been loaded, if not,
     * it will retrieve the data from the database,
     * resolve the type and load the data by calling
     * {@link ServerResourceType#loadResourceSafe(ServerResourceManager, DatabaseItem, ServerResource)}
     *
     * @param uuid The universal ID.
     * @param <R> The resource class.
     * @return The resource or null if absent.
     */
    public <R extends ServerResource> ServerResourceHandle<R> loadResource(UUID uuid) {
       return createHandleLoaded(loadResourceUnwrapped(uuid));
    }

    /**
     * Loads a resource using the type and local ID,
     * it first checks if the resource has already been
     * loaded, if not, it will call {@link ServerResourceType#loadResourceLocal(ServerResourceManager, UUID)}
     * @param type The resource type.
     * @param localId The local ID.
     * @param <R> The resource class.
     * @return The resource or null if absent.
     */
    @SuppressWarnings("unchecked")
    public <R extends ServerResource> ServerResourceHandle<R> loadResourceLocal(ServerResourceType<R> type,
                                                                                UUID localId) {
        ServerResource resource;
        if ((resource = getLoadedLocal(type, localId)) != null)
            return createHandleLoaded((R) resource);
        return createHandleLoaded(type.loadResourceLocal(this, localId));
    }

    /**
     * Loads a database resource using a filter provided,
     * the filter will check each key value pair by equality.
     * Once an item is retrieved, it will first check if the
     * resource is already loaded, if not, it will load the resource.
     * @param type The resource type.
     * @param eqFilter The equality filter.
     * @param <R> The resource class.
     * @return The resource or null if absent.
     */
    public <R extends ServerResource> R loadDatabaseResourceFiltered(ServerResourceType<R> type,
                                                                     Values eqFilter) {
        // find database item
        DatabaseItem item = findDatabaseResourceFiltered(type, eqFilter);

        // load resource if not null
        if (item != null) {
            // check resource is loaded already
            UUID uuid = item.get("uuid", UUID.class);
            R resource;
            if ((resource = getLoadedUniversal(uuid)) == null) {
                // construct resource
                resource = type.newInstanceInternal(
                        uuid,
                        item.get("localId", UUID.class)
                );

                addLoaded(resource);

                // load data
                type.loadResourceSafe(this, item, resource);
            }

            // return
            return resource;
        } else {
            return null;
        }
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

        // call unloaded
        eventBus.post(new ResourceUnloadEvent(this, resource));

        // return
        return this;
    }

    /**
     * Loads a resource asynchronously.
     * @see ServerResourceManager#loadResource(UUID)
     */
    public <R extends ServerResource> CompletableFuture<ServerResourceHandle<R>> loadResourceAsync(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadResource(uuid));
    }

    /**
     * Loads a resource by local ID asynchronously.
     * @see ServerResourceManager#loadResourceLocal(ServerResourceType, UUID)
     */
    public <R extends ServerResource> CompletableFuture<ServerResourceHandle<R>> loadResourceLocalAsync(final ServerResourceType<R> type,
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

    public <R extends ServerResource> ServerResourceHandle<R> loadResourceReferenceSync(final UUID uuid) {
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

    public DatabaseItem findDatabaseResourceFiltered(ServerResourceType type,
                                                     Values eqFilter) {
        // execute query
        QueryPool pool = getLocalQueryPool();
        DatabaseItem item = pool.current(requireDatabase())
                .querySync("find_resource_filter", new Values()
                        .setRaw("typeHash", type.idHash)
                        .setRaw("filter", eqFilter));

        // return item
        return item;
    }

    /* ---- Resource Handles ---- */

    @SuppressWarnings("unchecked")
    public <R extends ServerResource> ServerResourceHandle<R> createHandleUniversal(UUID uuid) {
        return (ServerResourceHandle<R>) new ServerResourceHandle<>(this, uuid).acquire();
    }

    public <R extends ServerResource> ServerResourceHandle<R> createHandleLoaded(R resource) {
        return new ServerResourceHandle<>(this, resource).acquire();
    }

    protected void doHandleAcquire(ServerResourceHandle handle) {
        // call event
        eventBus.post(new ResourceHandleAcquireEvent(this, handle));
    }

    protected void doHandleRelease(ServerResourceHandle handle) {
        // call event
        eventBus.post(new ResourceHandleReleaseEvent(this, handle));
    }

    /* ---- Database Utilities ---- */

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

    private Bson mongoToFilterEq(Values values) {
        Bson[] bsons = new Bson[values.getSize()];
        int i = 0;
        for (Map.Entry<Object, Object> entry : values.entrySet()) {
            bsons[i++] = Filters.eq((String)entry.getKey(), entry.getValue());
        }
        return Filters.and(bsons);
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

        pool.putQuery("find_resource_filter", DatabaseType.MONGO_DB, (query, database, values) -> {
            // get collection
            MongoCollection<Document> collection = mongoGetOrCreateResCollection(database.getDatabaseClient());
            Document doc = collection.find(
                    Filters.and(Filters.eq("type", values.get("typeHash")),
                            mongoToFilterEq(values.get("filter")))
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
