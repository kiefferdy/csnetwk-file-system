import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class Client {

    private JFrame frame;
    private JTextField commandInputField;
    private JTextArea responseArea;
    private JButton sendButton;
    private Socket socket;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private final String separator = "---------------------------------------------------------------------------------------------------------------------------------------------\n";
    private boolean isConnected = false;
    private boolean isRegistered = false;

    public Client() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        commandInputField = new JTextField();
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        sendButton = new JButton("Send");

        frame.add(commandInputField, BorderLayout.NORTH);
        frame.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        frame.add(sendButton, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendCommand());

        frame.setVisible(true);
    }

    private void sendCommand() {
        try {
            String command = commandInputField.getText().trim();
            commandInputField.setText(""); // Clear the input field

            if (command.equals("/?")) {
                printHelp();
                return;
            } 
            
            if (!isConnected && !command.startsWith("/join ")) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "Not connected to any server. Please connect to a server first.",
                        "Not Connected",
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            // Handle /join command
            if (command.startsWith("/join ")) {
                String[] parts = command.split(" ");
                if (parts.length == 3) {
                    String hostname = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    connectToServer(hostname, port);
                } else {
                    updateResponseArea("Invalid command. Usage: /join <server-ip> <port>\n" + separator);
                }
                return; // Return after handling /join
            }

            // Check if the user is not registered for certain commands
            if (!isRegistered && (command.startsWith("/store ") || command.startsWith("/get ") ||
                                command.startsWith("/broadcast ") || command.equals("/dir"))) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "You must be registered to use this command.",
                        "Not Registered",
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            // Process other commands
            if (command.startsWith("/store ")) {
                handleStoreCommand(command);
            } else if (command.startsWith("/get ")) {
                handleGetCommand(command);
            } else if (command.equals("/leave")) {
                disconnectFromServer();
            } else if (command.equals("/dir")) {
                sendCommandToServer(command);
                handleDirResponse();
            } else if (command.startsWith("/register ")) {
                sendCommandToServer(command);
                readServerResponse(command);
            } else if (command.startsWith("/broadcast ")) {
                sendCommandToServer(command);
                readServerResponse(command);
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                            "That is not a valid input. Please use \"/?\" to see the valid commands.",
                            "Invalid Command",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                updateResponseArea("IO Exception: " + e.getMessage() + "\n" + separator);
            });
        }
    }

    private void connectToServer(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
            dataOut = new DataOutputStream(socket.getOutputStream());
            dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            updateResponseArea("Connected to server at " + hostname + ":" + port + "\n" + separator);
            isConnected = true;
        } catch (IOException e) {
            updateResponseArea("Failed to connect to server: " + e.getMessage() + "\n" + separator);
        }
    }  

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                dataOut.writeUTF("/leave");
                dataOut.flush();
                socket.close();
                updateResponseArea("Disconnected from the server.\n" + separator);
                isConnected = false;
                isRegistered = false;
            }
        } catch (IOException e) {
            updateResponseArea("Error while disconnecting: " + e.getMessage() + "\n" + separator);
        }
    }

    private void handleDirResponse() {
        new Thread(() -> {
            try {
                String serverResponse;
                while (!(serverResponse = dataIn.readUTF()).equals("END_OF_DIR")) {
                    String finalResponse = serverResponse; // effectively final variable for lambda
                    SwingUtilities.invokeLater(() -> {
                        updateResponseArea(finalResponse + "\n" + separator);
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    updateResponseArea("Error receiving directory listing: " + e.getMessage() + "\n" + separator);
                });
            }
        }).start();
    }

    private void printHelp() {
        SwingUtilities.invokeLater(() -> {
            updateResponseArea("Available commands:\n");
            updateResponseArea("/join <server_ip> <port> - Connect to the server.\n");
            updateResponseArea("/leave - Disconnect from the server.\n");
            updateResponseArea("/register <handle> - Register your handle.\n");
            updateResponseArea("/store <filename> - Send a file to the server. Include the File Type in the command (.txt)\n");
            updateResponseArea("/get <filename> - Download a file from the server. Include the File Type in the command (.txt)\n");
            updateResponseArea("/dir - List files in the server's directory.\n");
            updateResponseArea("/? - Show this help message.\n" + separator);
        });
    }

    private void updateResponseArea(String message) {
        SwingUtilities.invokeLater(() -> {
            responseArea.append(message);
        });
    }


    private void handleStoreCommand(String command) throws IOException {
        String filename = command.substring(7); // Extract filename
        File file = new File(filename);
        if (file.exists()) {
            sendCommandToServer(command); // Send '/store' command to server

            String serverSignal = this.dataIn.readUTF();
            if (serverSignal.equals("START_OF_FILE")) {
                sendFile(filename);
                serverSignal = this.dataIn.readUTF();
                if (serverSignal.equals("END_OF_FILE")) {
                    updateResponseArea("File transfer completed." + "\n" + separator);
                }
            }
        } else {
            updateResponseArea("File not found: " + filename + "\n" + separator);
        }
    }

    private void handleGetCommand(String command) throws IOException {
        sendCommandToServer(command); // Send '/get' command to server

        String serverSignal = this.dataIn.readUTF();
        if (serverSignal.equals("START_OF_FILE")) {
            String filename = command.substring(5); // Extract filename
            receiveFile(filename);
            serverSignal = this.dataIn.readUTF();
            if (serverSignal.equals("END_OF_FILE")) {
                updateResponseArea("File received from Server: " + filename + "\n" + separator);
            }
        } else {
            updateResponseArea(serverSignal + "\n" + separator); // May be an error message
        }
    }

    private void sendCommandToServer(String command) throws IOException {
        this.dataOut.writeUTF(command);
        this.dataOut.flush();
    }

    private void readServerResponse() throws IOException {
        String response = this.dataIn.readUTF();
        updateResponseArea("[Server] " + response + "\n" + separator);
    }

    private void readServerResponse(String commandType) throws IOException {
    StringBuilder responseBuilder = new StringBuilder();
    String line;

    if (commandType.startsWith("/register ")) {
        while (!(line = this.dataIn.readUTF()).equals("END_OF_RESPONSE")) {
            responseBuilder.append(line).append("\n");
            if (line.startsWith("Welcome")) { // Check if the line starts with "Welcome"
                isRegistered = true; // Set isRegistered to true if registration is successful
            }
        }
    }
    SwingUtilities.invokeLater(() -> {
        updateResponseArea("[Server] " + responseBuilder.toString() + separator);
    });
}

    private void sendFile(String filename) {
        File file = new File(filename);
        try (FileInputStream fileIn = new FileInputStream(file)) {
            long fileSize = file.length();
            this.dataOut.writeLong(fileSize);
            this.dataOut.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                this.dataOut.write(buffer, 0, bytesRead);
            }
            this.dataOut.flush();
            updateResponseArea("File sent to Server: " + filename + "\n" + separator);
        } catch (IOException e) {
            updateResponseArea("Error sending file: " + e.getMessage() + "\n" + separator);
        }
    }

    private void receiveFile(String filename) throws IOException {
        // Specify the directory where files should be saved
        File directory = new File("./client_downloads");
        if (!directory.exists()) {
            directory.mkdir(); // Create the directory if it doesn't exist
        }
    
        // Create a file reference within that directory
        File file = new File(directory, filename);
    
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            // Read the file size first
            long fileSize = this.dataIn.readLong();
            long totalBytesRead = 0;
            byte[] buffer = new byte[4096];
            int bytesRead;
    
            while (totalBytesRead < fileSize) {
                bytesRead = dataIn.read(buffer, 0, Math.min(buffer.length, (int)(fileSize - totalBytesRead)));
                fileOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
    
            updateResponseArea("The received file was saved to " + file.getAbsolutePath() + "\n" + separator);
        } catch (IOException e) {
            updateResponseArea("Error receiving file: " + e.getMessage() + "\n" + separator);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client());
    }
}
