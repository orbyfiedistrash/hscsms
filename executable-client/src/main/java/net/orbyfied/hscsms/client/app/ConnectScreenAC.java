package net.orbyfied.hscsms.client.app;

import net.orbyfied.hscsms.client.ClientMain;
import net.orbyfied.hscsms.client.applib.*;
import net.orbyfied.hscsms.client.applib.display.UIDisplay;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.util.Values;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConnectScreenAC extends AppContext {

    public ConnectScreenAC(AppContextManager manager) {
        super(manager, "connect_screen");
    }

    UIDisplay display;

    // gui
    JPanel mainPanel;
    JList<ServerListItem> serverList;

    // data
    List<ServerListItem> serverItems = new ArrayList<>();

    // gui utils
    private Font fontA = new Font("Sans Serif", Font.BOLD, 20);
    private Font fontB = new Font("Sans Serif", Font.PLAIN, 10);

    @Override
    public void enter(Values values) {
        // create display
        display = manager.getApp().displayManager.create("connect_screen");
        // setup display
        display.setTitle("Connect")
                .setImportant(true)
                .setSize(800, 600)
                .create()
                .window((uiDisplay, window) -> {
                    // comp: main panel
                    mainPanel = new JPanel();
                    window.add(mainPanel);
                    mainPanel.setBackground(Color.DARK_GRAY);

                    // set null layout
                    mainPanel.setLayout(null);

                    // create list
                    serverList = new JList<>(new DefaultListModel<>());
                    mainPanel.add(serverList);
                    serverList.setLayoutOrientation(JList.VERTICAL);
                    serverList.setAutoscrolls(true);
                    serverList.setBackground(Color.GRAY);
                    serverList.setBounds(300, 0, 500, 600);
                    serverList.setForeground(Color.WHITE);
                    serverList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
                        Color bg = Color.GRAY;
                        Color fg = Color.WHITE;
                        if (isSelected) {
                            bg = Color.WHITE.darker();
                            fg = Color.BLACK;
                        }

                        JPanel item = new JPanel();
                        item.setBackground(bg);
                        JLabel nameLabel = new JLabel();
                        nameLabel.setForeground(fg);
                        nameLabel.setFont(fontA);
                        nameLabel.setText(value.name);
                        JLabel ipLabel = new JLabel();
                        ipLabel.setForeground(fg);
                        ipLabel.setFont(fontB);
                        ipLabel.setText(value.ip);

                        item.add(nameLabel);
                        item.add(ipLabel);
                        item.setSize(500, 30);
                        return item;
                    });

                    CompletableFuture.runAsync(this::loadServerList)
                            .whenComplete((__, t) -> {

                            });
                })
                .show(true);
    }

    @Override
    public void exit() {
        // destroy display
        display.show(false);
        display.destroy();
    }

    /* ---- Functional ---- */

    public record ServerListItem(String name, String ip) { }

    private void loadServerList() {
        try {
            Path file = manager.getApp().getUserDataFolder().resolve("serverlist.txt");
            if (!Files.exists(file))
                return;
            InputStream stream = Files.newInputStream(file);
            String str = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            // read lines
            String[] lns = str.split("\n");
            for (String line : lns) {
                // parse line into server list item
                String[] parts = line.split("=");
                String   name  = parts[0];
                String   ip    = parts[1];

                // create and add server item
                ServerListItem item = new ServerListItem(name, ip);
                serverItems.add(item);
            }
        } catch (Exception e) {
            ClientApp.LOGGER.err("Could not load server list");
            e.printStackTrace();
        }

        // display server list
        DefaultListModel<ServerListItem> listModel = (DefaultListModel<ServerListItem>) serverList.getModel();
        listModel.addAll(serverItems);

        // log
        ClientApp.LOGGER.ok("Loaded and displayed server list");
    }

}
