import java.io.*;
import java.net.Socket;

public class FileExchangeClient {
    private Socket socket;
    private BufferedReader consoleReader, serverResponse;
    private PrintWriter serverWriter;

    public FileExchangeClient(String hostname, int port) throws Exception {
        this.socket = new Socket(hostname, port);
        System.out.println("Connected to the server");

        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
        this.serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.serverWriter = new PrintWriter(socket.getOutputStream(), true);

        // Start a thread to listen to server responses
        new ServerListener(socket).start();
    }

    public void start() {
        try {
            String userInput;
            while ((userInput = consoleReader.readLine()) != null) {
                processCommand(userInput);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void processCommand(String command) throws IOException {
        if (command.startsWith("/store ")) {
            String filename = command.substring(7); // Extract filename
            serverWriter.println(command); // Inform server about file transfer
            // Wait for server to signal start
            if (serverResponse.readLine().equals("START")) {
                sendFile(filename);
                // Wait for server to signal end
                if (serverResponse.readLine().equals("END")) {
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
                socket.close();
                System.out.println("Disconnected from the server.");
                System.exit(0); // Or handle this differently if you want to keep the client running
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

    private void sendFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(file);
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                System.out.println("Error sending file: " + e.getMessage());
            }
        } else {
            System.out.println("File not found: " + filename);
        }
    }

    private void receiveFile(String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
            DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // The client now needs the server IP and port to start.
        // For testing, you can hardcode these values or pass them as arguments.
        String hostname = "localhost"; // Example hostname
        int port = 5000; // Example port

        try {
            FileExchangeClient client = new FileExchangeClient(hostname, port);
            client.start();
        } catch (Exception e) {
            System.out.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}