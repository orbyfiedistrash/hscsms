package net.orbyfied.hscsms.core.resource;

import net.orbyfied.hscsms.db.Database;
import net.orbyfied.hscsms.db.DatabaseItem;
import net.orbyfied.hscsms.db.QueryPool;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;

import java.lang.reflect.Constructor;
import java.util.UUID;

public abstract class ServerResourceType<R extends ServerResource> {

    // the type identifier
    final Identifier id;
    // the type identifier hash
    // cached for performance
    final int idHash;

    // the runtime resource type
    Class<R> resourceClass;
    // the
    Constructor<R> constructor;

    public ServerResourceType(Identifier id,
                              Class<R> resourceClass) {
        this.id     = id;
        this.idHash = id.hashCode();

        this.resourceClass = resourceClass;
    }

    /* Getters */

    public Identifier getIdentifier() {
        return id;
    }

    public int getIdentifierHash() {
        return idHash;
    }

    public Class<R> getResourceClass() {
        return resourceClass;
    }

    /* -------- Functional --------- */

    public R newInstance(UUID uuid, UUID localId) {
        try {
            return constructor.newInstance(uuid, this.getIdentifierHash(), localId);
        } catch (Exception e) {
            // rethrow error
            throw new RuntimeException(e);
        }
    }

    public R loadResourceLocal(ServerResourceManager manager,
                               UUID localId) {
        // find document
        DatabaseItem item = findDatabaseResourceLocal(manager, manager.database(), localId);

        // get properties
        UUID uuid = item.get("uuid", UUID.class);

        // construct instance
        R resource = newInstance(uuid, localId);

        // load data
        loadResourceSafe(manager, item, resource);

        // add loaded
        manager.addLoaded(resource);

        // return
        return resource;
    }

    @SuppressWarnings("unchecked")
    public ServerResourceType<R> saveResource(ServerResourceManager manager,
                                              ServerResource resource) {
        // find document
        DatabaseItem item = manager.findOrCreateDatabaseResource(resource.universalID());

        // set properties
        item.set("uuid",    resource.universalID());
        item.set("localId", resource.localID());
        item.set("type",    getIdentifierHash());

        // save data
        saveResourceSafe(manager, item, (R) resource);

        // return
        return this;
    }

    public static record ResourceLoadResult(boolean success, Throwable t) {
        public static ResourceLoadResult ofSuccess() {
            return new ResourceLoadResult(true, null);
        }
    }

    public static record ResourceSaveResult(boolean success, Throwable t) {
        public static ResourceSaveResult ofSuccess() {
            return new ResourceSaveResult(true, null);
        }
    }

    /**
     * Finds the database item of the resource by UUID.
     * @param manager The manager.
     * @param uuid The resource UUID.
     * @return The item or null if absent.
     */
    public DatabaseItem findDatabaseResource(ServerResourceManager manager, UUID uuid) {
        return manager.findDatabaseResource(uuid);
    }

    /**
     * Finds the database item of the resource by local ID.
     * @param manager The resource manager.
     * @param database The database.
     * @param localId The resource' local ID.
     * @return The item or null if absent.
     */
    public DatabaseItem findDatabaseResourceLocal(ServerResourceManager manager,
                                                  Database database,
                                                  UUID localId) {
        QueryPool pool = manager.getLocalQueryPool();
        return pool.current(database)
                .querySync("find_resource_local", new Values()
                        .put("localId", localId)
                        .put("type", this.getIdentifierHash())
                );
    }

    /**
     * Saves a resource to the database. This does not
     * push the data into the database, that must be
     * done manually if needed.
     * @param manager The resource manager.
     * @param dbItem The database item to save to.
     * @param resource The resource to save.
     * @return Result.
     */
    public abstract ResourceSaveResult saveResource(ServerResourceManager manager,
                                                    DatabaseItem dbItem,
                                                    R resource);

    /**
     * Loads a resource from the database. This does not
     * pull the data from database, that must be
     * done manually if needed.
     * @param manager The resource manager.
     * @param dbItem The database item to load from.
     * @param resource The resource to load to.
     * @return Result.
     */
    public abstract ResourceLoadResult loadResource(ServerResourceManager manager,
                                                    DatabaseItem dbItem,
                                                    R resource);

    /**
     * Safe wrapper.
     * @see ServerResourceType#saveResource(ServerResourceManager, DatabaseItem, ServerResource)
     */
    public ResourceSaveResult saveResourceSafe(ServerResourceManager manager,
                                               DatabaseItem dbItem,
                                               R resource) {
        final Logger logger = ServerResourceManager.LOGGER;

        try {
            // call save
            ResourceSaveResult result = saveResource(manager, dbItem, resource);

            // push data if successful
            if (result.success()) {
                dbItem.push();
            }

            return result;
        } catch (Exception e) {
            logger.err("Error while saving resource " + resource.universalID() + " of type " +
                    resource.type().id);
            e.printStackTrace();
            return new ResourceSaveResult(false, e);
        }
    }

    /**
     * Safe wrapper.
     * @see ServerResourceType#loadResource(ServerResourceManager, DatabaseItem, ServerResource)
     */
    public ResourceLoadResult loadResourceSafe(ServerResourceManager manager,
                                               DatabaseItem dbItem,
                                               R resource) {
        final Logger logger = ServerResourceManager.LOGGER;

        try {
            // pull data
            dbItem.pull();

            // call load and return
            return loadResource(manager, dbItem, resource);
        } catch (Exception e) {
            logger.err("Error while loading resource " + resource.universalID() + " of type " +
                    resource.type().id);
            e.printStackTrace();
            return new ResourceLoadResult(false, e);
        }
    }

}
