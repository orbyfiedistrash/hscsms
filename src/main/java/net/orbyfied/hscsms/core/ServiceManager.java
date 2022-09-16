package net.orbyfied.hscsms.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ServiceManager implements ServiceHolder {

    // services
    final List<Service> services = new ArrayList<>();
    final Map<String, Service> serviceMap = new HashMap<>();

    @Override
    public ServiceManager getServiceManager() {
        return this;
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
    public ServiceManager addService(Service service) {
        services.add(service);
        serviceMap.put(service.name, service);
        return this;
    }

    @Override
    public ServiceManager removeService(Service service) {
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
