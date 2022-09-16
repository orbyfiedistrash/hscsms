package net.orbyfied.hscsms.core;

import java.util.function.Function;

public interface ServiceHolder {

    /**
     * Get the top level service manager.
     * @return The manager.
     */
    ServiceManager getServiceManager();

    /**
     * Get a service of specified type {@code type}
     * by name {@code name}.
     *
     * @param type The type.
     * @param name The name.
     * @param <S> The service type.
     * @return The service.
     */
    <S extends Service> S getService(Class<S> type, String name);

    /**
     * Get a service by name.
     *
     * @param name The name.
     * @param <S> The service type.
     * @return The service.
     */
    <S extends Service> S getService(String name);

    /**
     * Add a service to the holder.
     * @param service The service.
     * @return This.
     */
    ServiceHolder addService(Service service);

    /**
     * Remove a service from the holder.
     * @param service The service.
     * @return This.
     */
    ServiceHolder removeService(Service service);

    /**
     * Create a service with the specified
     * type and name.
     *
     * @param name The name.
     * @param type The type.
     * @param <S> The service type.
     * @return The service.
     */
    <S extends Service> S createService(Class<S> type, String name,
                                        Function<ServiceHolder, S> constructor);

    /**
     * Get or create a service with
     * the specified type and name.
     *
     * @param name The name.
     * @param type The type.
     * @param <S> The service type.
     * @return The service.
     */
    <S extends Service> S requireService(Class<S> type, String name,
                                         Function<ServiceHolder, S> constructor);

}
