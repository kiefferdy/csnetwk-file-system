import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FileExchangeGUI extends JFrame {
    private JTextField serverIPField, portField, commandField;
    private JButton connectButton, disconnectButton, sendButton, dirButton;
    private JTextArea outputArea;
    private JScrollPane scrollPane;

    public FileExchangeGUI() {
        // Initialize Components
        serverIPField = new JTextField(10);
        portField = new JTextField(5);
        commandField = new JTextField(20);
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        sendButton = new JButton("Send Command");
        dirButton = new JButton("Directory");
        outputArea = new JTextArea(15, 30);
        outputArea.setEditable(false);
        scrollPane = new JScrollPane(outputArea);

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
        midPanel.add(sendButton);
        midPanel.add(dirButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(midPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Action Listeners
        // Connect, Disconnect, Send Command, and Directory Listing button listeners
        // to be implemented based on your client-server logic

        // Frame Configuration
        setTitle("File Exchange System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileExchangeGUI());
    }
}