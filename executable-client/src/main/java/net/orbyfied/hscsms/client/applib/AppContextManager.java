package net.orbyfied.hscsms.client.applib;

import net.orbyfied.hscsms.client.app.ClientApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AppContextManager {

    List<AppContext> contexts = new ArrayList<>();
    Map<String, AppContext> contextsByName = new HashMap<>();

    AppContext currentContext;

    final ClientApp app;

    public ClientApp getApp() {
        return app;
    }

    public AppContextManager(ClientApp app) {
        this.app = app;
    }

    public void load() {

    }

    public AppContext getCurrentContext() {
        return currentContext;
    }

    public AppContextManager setCurrentContext(String name) {
        return setCurrentContext(getContextByName(name));
    }

    public AppContextManager setCurrentContext(AppContext currentContext) {
        AppContext oldCtx = this.currentContext;
        this.currentContext = currentContext;
        if (currentContext != oldCtx) {
            // transfer to new
            if (oldCtx != null) {
                oldCtx.exit();
            }

            if (currentContext != null) {
                currentContext.enter();
            }

            // log switch
            ClientApp.LOGGER.ok("Switched to context {0} from {1}",
                    (currentContext != null ? currentContext.name : "null"),
                    (oldCtx != null ? oldCtx.name : "null"));
        }
        return this;
    }

    public List<AppContext> getContexts() {
        return contexts;
    }

    public Map<String, AppContext> getContextsByName() {
        return contextsByName;
    }

    public AppContext getContextByName(String name) {
        return contextsByName.get(name);
    }

    public AppContext addContext(Function<AppContextManager, AppContext> cc) {
        AppContext context = cc.apply(this);
        contexts.add(context);
        contextsByName.put(context.name, context);
        return context;
    }

    public AppContextManager addContext(AppContext context) {
        contexts.add(context);
        contextsByName.put(context.name, context);
        return this;
    }

}
