package net.orbyfied.hscsms.client.app;

import net.orbyfied.hscsms.client.applib.AppContext;
import net.orbyfied.hscsms.client.applib.AppContextManager;
import net.orbyfied.hscsms.client.applib.display.UIDisplay;
import net.orbyfied.hscsms.util.Values;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.net.InetSocketAddress;
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
    JPanel controlPanel;
    JButton connectButton;
    JTextField addressField;

    // data
    List<ServerListItem> serverItems = new ArrayList<>();

    // gui utils
    private Font fontA = new Font("Sans Serif", Font.BOLD, 20);
    private Font fontB = new Font("Sans Serif", Font.PLAIN, 10);
    private Font fontC = new Font("Sans Serif", Font.PLAIN, 16);

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
                    mainPanel.setLayout(null);

                    // create control panel
                    controlPanel = new JPanel();
                    controlPanel.setBounds(0, 0, 300, 600);
                    controlPanel.setLayout(null);
                    controlPanel.setBackground(Color.DARK_GRAY);
                    mainPanel.add(controlPanel);

                    // create connect controls
                    connectButton = new JButton("Connect");
                    addressField  = new JTextField();
                    addressField.setToolTipText("Enter Address");
                    addressField.setFont(fontC);
                    connectButton.setBackground(Color.GRAY);
                    connectButton.setForeground(Color.BLACK);
                    addressField.setBackground(Color.GRAY);
                    addressField.setForeground(Color.WHITE);
                    connectButton.setBounds(0, 15, 300, 35);
                    addressField.setBounds(0, 15 + 35 + 5, 300, 35);

                    connectButton.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            prepareConnect(addressField.getText());
                        }
                    });

                    controlPanel.add(connectButton);
                    controlPanel.add(addressField);

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

                    CompletableFuture.runAsync(this::loadServerList);
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

    private void initiateConnect(InetSocketAddress address) {
        ClientApp.LOGGER.info("GUI initiated connect to " + address);
        manager.getApp().main.reconnect(address);
    }

    private void errDialogIaf() {
        // error
        JDialog dialog = new JDialog();
        dialog.setTitle("Connect Error");
        JLabel label = new JLabel();
        label.setText("Invalid Address Format");
        label.setFont(fontC);
        dialog.add(label);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void prepareConnect(String address) {
        if (address == null || address.isEmpty()) {
            errDialogIaf();
            return;
        }
        String[] addrParts = address.split(":");
        if (addrParts.length < 1) {
            errDialogIaf();
            return;
        }
        String host = addrParts[0];
        int port;
        if (addrParts.length < 2)
            port = 42069;
        else
            port = Integer.parseInt(addrParts[1]);
        if (host.equals("localhost"))
            host = "0.0.0.0";
        final InetSocketAddress socketAddress = new InetSocketAddress(host, port);

        // confirm connect
        JDialog dialog = new JDialog();
        dialog.setPreferredSize(new Dimension(400, 150));
        dialog.setLayout(null);
        dialog.setTitle("Confirm Connection");
        JLabel label = new JLabel("Confirm Connection To " + socketAddress);
        label.setBounds(0, 0, 400, 50);
        label.setFont(fontC);
        dialog.add(label);

        JButton cancel = new JButton("Cancel");
        cancel.setBounds(0, 50, 200, 50);
        cancel.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        JButton confirm = new JButton("Confirm");
        confirm.setBounds(200, 50, 200, 50);
        confirm.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // initiate connect
                dialog.setVisible(false);
                initiateConnect(socketAddress);
            }
        });

        dialog.add(label);
        dialog.add(cancel);
        dialog.add(confirm);
        dialog.pack();
        dialog.setVisible(true);
    }

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
