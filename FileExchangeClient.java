import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;


public class FileExchangeClient {
    private Socket socket;
    private PrintWriter serverWriter;
    private BufferedReader serverReader;
    private ServerListener serverListener;


    // Connect to the server
    public void connectToServer(String serverIp, int port, Consumer<String> onResponseReceived, Runnable onConnectionSuccess, Runnable onConnectionFailure) {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, port);
                serverWriter = new PrintWriter(socket.getOutputStream(), true);

                // Start listening to server responses
                serverListener = new ServerListener(socket, onResponseReceived);
                serverListener.start();

                onConnectionSuccess.run();
            } catch (IOException e) {
                onConnectionFailure.run();
            }
        }).start();
    }

<<<<<<< Updated upstream
    // Disconnect from the server
    public void disconnectFromServer(Runnable onDisconnection) {
        new Thread(() -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
=======
    private void processCommand(String command) throws IOException {
        if (command.startsWith("/join ")) {
            String[] parts = command.split(" ");
            if (parts.length == 3) {
                String hostname = parts[1];
                int port = Integer.parseInt(parts[2]);
                try {
                    connectToServer(hostname, port);
                } catch (IOException e) {
                    System.out.println("Error connecting to server: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid command. Usage: /join <server-ip> <port>");
            }
        } else if (command.startsWith("/store ")) {
            String filename = command.substring(7); // Extract filename
            serverWriter.println(command); // Inform server about file transfer
            serverWriter.flush(); // Ensure the message is sent immediately

            String serverSignal = serverResponse.readLine();
            System.out.println("Client: Received signal from server: " + serverSignal); // Debugging
            if ("START".equals(serverSignal)) {
                sendFile(filename);
                serverSignal = serverResponse.readLine();
                System.out.println("Client: Received signal from server: " + serverSignal); // Debugging
                if ("END".equals(serverSignal)) {
                    System.out.println("File transfer completed.");
                }
            }
        } else if (command.startsWith("/get ")) {
            String filename = command.substring(5); // Extract filename
            serverWriter.println(command); // Request file from server
            // Wait for server to signal start
            if (serverResponse.readLine().equals("START")) {
                receiveFile(filename);
                // Wait for server to signal end
                if (serverResponse.readLine().equals("END")) {
                    System.out.println("File received.");
                }
            }
        } else if (command.equals("/leave")) {
        try {
            serverWriter.println("/leave"); // Inform the server of intentional disconnection
            socket.close();
            System.out.println("Disconnected from the server.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Error while disconnecting: " + e.getMessage());
        }
        } else if (command.equals("/?")) {
            printHelp();
        } else {
            serverWriter.println(command); // Other commands
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("/join <server_ip> <port> - Connect to the server.");
        System.out.println("/leave - Disconnect from the server.");
        System.out.println("/register <handle> - Register your handle.");
        System.out.println("/store <filename> - Send a file.");
        System.out.println("/get <filename> - Fetch a file.");
        System.out.println("/dir - List files in the directory.");
        System.out.println("/? - Show this help message.");
    }

    private void connectToServer(String hostname, int port) throws IOException {
        this.socket = new Socket(hostname, port);
        System.out.println("Connected to the server");

        this.serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.serverWriter = new PrintWriter(socket.getOutputStream(), true);

        new Thread(new ServerListener(socket)).start(); // Use the constructor that only requires a Socket
    }    

    private void sendFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            long fileSize = file.length();
            try (FileInputStream fileIn = new FileInputStream(file);
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
                // Send the file size first
                dataOut.writeLong(fileSize);
                dataOut.flush();
    
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
>>>>>>> Stashed changes
                }
                onDisconnection.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Send a command to the server
    public void sendCommand(String command, Runnable onCommandSent) {
        new Thread(() -> {
            if (serverWriter != null) {
                serverWriter.println(command);
                onCommandSent.run();
            }
        }).start();
    }

    // Read a response from the server
    public void readResponse(Consumer<String> onResponseReceived) {
        new Thread(() -> {
            try {
                String response;
                while ((response = serverReader.readLine()) != null) {
                    onResponseReceived.accept(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFile(String filename, Runnable onFileSent) {
        new Thread(() -> {
            try {
                // Implement file sending logic
                File file = new File(filename);
                if (file.exists()) {
                    serverWriter.println("/store " + filename);
                    // Read the server's response (expecting "START")
                    if ("START".equals(serverReader.readLine())) {
                        FileInputStream fileIn = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fileIn.read(buffer)) != -1) {
                            socket.getOutputStream().write(buffer, 0, bytesRead);
                        }
                        socket.getOutputStream().flush();
                        fileIn.close();
                    }
                }
                onFileSent.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Receive a file from the server
    public void getFile(String filename, Runnable onFileReceived) {
        new Thread(() -> {
            try {
                // Implement file receiving logic
                serverWriter.println("/get " + filename);
                // Read the server's response (expecting "START")
                if ("START".equals(serverReader.readLine())) {
                    FileOutputStream fileOut = new FileOutputStream(filename);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                    }
                    fileOut.close();
                }
                onFileReceived.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    // ... other methods ...
}