package net.orbyfied.hscsms.core.resource;

public interface ResourceService {

    /**
     * Get the server resource manager
     * this service is applied to.
     * @return The manager.
     */
    ServerResourceManager manager();

    /**
     * Called when the service has been
     * added to the manager.
     */
    void added();

    /**
     * Called when the service is removed
     * from the manager.
     */
    void removed();

}
