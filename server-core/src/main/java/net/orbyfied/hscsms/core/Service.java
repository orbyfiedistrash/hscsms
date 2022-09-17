package net.orbyfied.hscsms.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Service implements ServiceHolder {

    // the service holder (parent)
    final ServiceHolder holder;
    // the service manager
    final ServiceManager manager;
    // the name of this service
    final String name;
    // if it should be running
    final AtomicBoolean running = new AtomicBoolean(true);

    // sub-services
    final List<Service> services = new ArrayList<>();
    final Map<String, Service> serviceMap = new HashMap<>();

    public Service(ServiceManager manager,
                   ServiceHolder holder,
                   String name) {
        this.holder  = holder;
        this.manager = manager;
        this.name    = name;
    }

    public ServiceManager getManager() {
        return manager;
    }

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return running.get();
    }

    public Service setRunning(boolean b) {
        running.set(b);
        return this;
    }

    /* -------- Sub-Services --------- */

    @Override
    public ServiceManager getServiceManager() {
        return manager;
    }

    private <T> T reqType(T o, Class<?> t) {
        if (o == null || !t.isAssignableFrom(o.getClass()))
            throw new IllegalArgumentException("expected service of type " + t.getName());
        return o;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Service> S getService(Class<S> type, String name) {
        return (S) reqType(serviceMap.get(name), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Service> S getService(String name) {
        return (S) serviceMap.get(name);
    }

    @Override
    public Service addService(Service service) {
        services.add(service);
        serviceMap.put(service.name, service);
        return this;
    }

    @Override
    public Service removeService(Service service) {
        services.remove(service);
        serviceMap.remove(service.name, service);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Service> S createService(Class<S> type, String name, Function<ServiceHolder, S> constructor) {
        S serv = constructor.apply(this);
        addService(serv);
        return serv;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Service> S requireService(Class<S> type, String name, Function<ServiceHolder, S> constructor) {
        S service;
        if ((service = (S) serviceMap.get(name)) == null) {
            service = createService(type, name, constructor);
        }
        return service;
    }

}
