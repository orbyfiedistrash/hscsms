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
    // main asset folder
    public final Path assetFolder;
    // main data folder
    public final Path userDataFolder;

    // services
    public final UIDisplayManager displayManager = new UIDisplayManager(this);
    public final AppContextManager appContextManager = new AppContextManager(this);

    public ClientApp(ClientMain main) {
        this.main = main;

        // setup
        this.assetFolder = main.workDir.resolve("assets");
        this.userDataFolder = main.workDir.resolve("userdata");

        appContextManager.load();
        displayManager.load();
    }

    public Path getAssetFolder() {
        return assetFolder;
    }

    public Path getUserDataFolder() {
        return userDataFolder;
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
