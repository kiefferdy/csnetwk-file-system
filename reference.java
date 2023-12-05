import java.io.*;
import java.net.Socket;
public class FileExchangeClient {
    private Socket socket;
    private BufferedReader consoleReader;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    public FileExchangeClient() {
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }
    public void start() {
        try {
            System.out.println("Welcome to File Exchange Client! Enter \"/?\" for a list of commands.");
            String userInput;
            while ((userInput = consoleReader.readLine()) != null) {
                processCommand(userInput);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
    private void connectToServer(String hostname, int port) throws IOException {
        this.socket = new Socket(hostname, port);
        this.dataOut = new DataOutputStream(socket.getOutputStream());
        this.dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        System.out.println("Connection to the File Exchange Server is successful!");
    }
    private void processCommand(String command) throws IOException {
        if (command.startsWith("/join ")) {
            String[] parts = command.split(" ");
            if (parts.length == 3) {
                String hostname = parts[1];
                int port = Integer.parseInt(parts[2]);
                connectToServer(hostname, port);
            } else {
                System.out.println("Invalid command. Usage: /join <server-ip> <port>");
            }
        } else if (command.startsWith("/store ")) {
            handleStoreCommand(command);
        } else if (command.startsWith("/get ")) {
            handleGetCommand(command);
        } else if (command.equals("/leave")) {
            if (socket != null) {
                socket.close();
            }
            System.out.println("Disconnected from the server.");
            System.exit(0);
        } else if (command.equals("/dir")) {
            sendCommandToServer(command);
            System.out.println("Server Directory");
            String[] fileNames = this.dataIn.readUTF().split(",");
            for (String fileName : fileNames) {
                System.out.println("[Server] " + fileName);
            }            
        } else if (command.equals("/?")) {
            printHelp();
        } else {
            sendCommandToServer(command);
            readServerResponse();
        }
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
                    System.out.println("File transfer completed.");
                }
            }
        } else {
            System.out.println("File not found: " + filename);
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
                System.out.println("File received from Server: " + filename);
            }
        } else {
            System.out.println(serverSignal); // May be an error message
        }
    }
    private void sendCommandToServer(String command) throws IOException {
        this.dataOut.writeUTF(command);
        this.dataOut.flush();
    }
    private void readServerResponse() throws IOException {
        String response = this.dataIn.readUTF();
        System.out.println("[Server] " + response);
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
            System.out.println("File sent to Server: " + filename);
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
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
    
            System.out.println("The received file was saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
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
    public static void main(String[] args) {
        try {
            FileExchangeClient client = new FileExchangeClient();
            client.start();
        } catch (Exception e) {
            System.out.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}