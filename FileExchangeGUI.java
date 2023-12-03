import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class FileExchangeGUI extends JFrame {
    private JTextField serverIPField, portField, commandField, fileField;
    private JButton connectButton, disconnectButton, sendCommandButton, dirButton, sendFileButton, getFileButton;
    private JTextArea outputArea;
    private FileExchangeClient client;

    public FileExchangeGUI() {
        // Initialize Components
        serverIPField = new JTextField("localhost", 10);
        portField = new JTextField("5000", 5);
        commandField = new JTextField(20);
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        sendCommandButton = new JButton("Send Command");
        dirButton = new JButton("Directory");
        outputArea = new JTextArea(15, 30);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        fileField = new JTextField(20);
        sendFileButton = new JButton("Send File");
        getFileButton = new JButton("Get File");

        // Layout Setup
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIPField);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        JPanel midPanel = new JPanel();
        midPanel.add(new JLabel("Command:"));
        midPanel.add(commandField);
        midPanel.add(sendCommandButton);
        midPanel.add(dirButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(midPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        JPanel filePanel = new JPanel();
        filePanel.add(new JLabel("File:"));
        filePanel.add(fileField);
        filePanel.add(sendFileButton);
        filePanel.add(getFileButton);

        add(filePanel, BorderLayout.SOUTH);

        // Action Listeners
        setUpActionListeners();

        // Frame Configuration
        setTitle("File Exchange System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void setUpActionListeners() {
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnectFromServer();
            }
        });

        sendCommandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand();
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        getFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getFile();
            }
        });

        // Additional listeners for other buttons like dirButton...
    }

    private void connectToServer() {
        String serverIp = serverIPField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        if (client == null) {
            client = new FileExchangeClient();
        }
        client.connectToServer(serverIp, port, response -> {
            SwingUtilities.invokeLater(() -> outputArea.append("Server: " + response + "\n"));
        }, () -> {
            SwingUtilities.invokeLater(() -> outputArea.append("Connected to server.\n"));
        }, () -> {
            SwingUtilities.invokeLater(() -> outputArea.append("Failed to connect.\n"));
        });
    }

    private void disconnectFromServer() {
        if (client != null) {
            client.disconnectFromServer(() -> {
                SwingUtilities.invokeLater(() -> outputArea.append("Disconnected from server.\n"));
            });
        }
    }

    private void sendCommand() {
        if (client != null) {
            String command = commandField.getText();
            client.sendCommand(command, () -> {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Command sent: " + command + "\n");
                    commandField.setText("");
                });
            });
        }
    }

    // Read response from server (Optional)
    private void readResponse() {
        Consumer<String> responseHandler = response -> SwingUtilities.invokeLater(() -> outputArea.append("Server: " + response + "\n"));
        client.readResponse(responseHandler);
    }

    private void sendFile() {
        if (client != null) {
            String filename = fileField.getText();
            client.sendFile(filename, () -> {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("File sent: " + filename + "\n");
                    fileField.setText("");
                });
            });
        }
    }

    private void getFile() {
        if (client != null) {
            String filename = fileField.getText();
            client.getFile(filename, () -> {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("File received: " + filename + "\n");
                });
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileExchangeGUI());
    }
}