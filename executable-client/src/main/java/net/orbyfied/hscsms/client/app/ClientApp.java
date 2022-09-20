package net.orbyfied.hscsms.client.app;

import net.orbyfied.hscsms.client.ClientMain;
import net.orbyfied.hscsms.client.applib.AppContextManager;
import net.orbyfied.hscsms.client.applib.display.UIDisplayManager;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.nio.file.Path;

public class ClientApp {

    public static final Logger LOGGER = Logging.getLogger("ClientApp");

    //////////////////////////////////////////

    // main instance of client
    public final ClientMain main;
    // main data folder
    public final Path dataFolder;

    // services
    public final UIDisplayManager displayManager = new UIDisplayManager(this);
    public final AppContextManager appContextManager = new AppContextManager(this);

    public ClientApp(ClientMain main) {
        this.main       = main;
        this.dataFolder = Path.of("client-data");

        // setup
        appContextManager.load();
        displayManager.load();
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public AppContextManager getAppContextManager() {
        return appContextManager;
    }

    public UIDisplayManager getDisplayManager() {
        return displayManager;
    }

    public void quit() {
        main.disconnect();
        System.exit(0);
    }

}
