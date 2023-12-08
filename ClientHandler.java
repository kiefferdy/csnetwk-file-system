import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private static final Set<String> registeredUsers = Collections.synchronizedSet(new HashSet<>());
    private boolean isRegistered = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            this.dataOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error setting up streams: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String clientCommand;
            while (true) {
                clientCommand = this.dataIn.readUTF();
                if (clientCommand == null || clientCommand.equals("/exit")) {
                    break;
                }
                handleClientCommand(clientCommand);
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.out.println("Client handler exception: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void sendMessageToClient(String message) throws IOException {
        this.dataOut.writeUTF(message);
        this.dataOut.flush();
    }

    private void handleClientCommand(String clientCommand) {
        try {
            if (clientCommand.startsWith("/register ")) {
                handleRegisterCommand(clientCommand);
            } else if (!isRegistered) {
                sendMessageToClient("Error: You must register first with /register <handle>");
            } else {
                if (clientCommand.startsWith("/store ")) {
                    handleStoreCommand(clientCommand);
                } else if (clientCommand.startsWith("/get ")) {
                    handleGetCommand(clientCommand);
                } else if (clientCommand.equals("/dir")) {
                    handleDirCommand();
                } else {
                    sendMessageToClient("Unknown command! Type \"/?\" for a list of commands.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client command: " + e.getMessage());
        } 
    }

    private void handleRegisterCommand(String clientCommand) throws IOException {
        String handle = clientCommand.substring(10).trim();
        synchronized (registeredUsers) {
            if (!registeredUsers.contains(handle)) {
                registeredUsers.add(handle);
                sendMessageToClient("Welcome, " + handle + "!");
                isRegistered = true;
            } else {
                sendMessageToClient("Error: Handle already exists");
            }
            sendMessageToClient("END_OF_RESPONSE"); // End of response signal
        }
    }

    private void handleGetCommand(String clientCommand) throws IOException {
        String filename = clientCommand.substring(5).trim();
        File directory = new File("./server_files");
        File file = new File(directory, filename);
    
        if (file.exists() && file.isFile()) {
            System.out.println("A file upload has been initiated...");
            sendMessageToClient("START_OF_FILE");
            sendFile(filename);
            sendMessageToClient("END_OF_FILE");
        } else {
            sendMessageToClient(filename + " does not exist in the server file directory!");
        }
    }

    private void handleStoreCommand(String clientCommand) throws IOException {
        String filename = clientCommand.substring(7);
        System.out.println("A file download has been initiated...");
        sendMessageToClient("START_OF_FILE");
        receiveFile(filename);
        sendMessageToClient("END_OF_FILE");
    }

    private void handleDirCommand() throws IOException {
        File dir = new File("./server_files");
        File[] filesList = dir.listFiles();
        if (filesList != null && filesList.length > 0) {
            StringBuilder fileListBuilder = new StringBuilder();
            for (File file : filesList) {
                if (file.isFile()) {
                    fileListBuilder.append(file.getName()).append(",");
                }
            }
            sendMessageToClient(fileListBuilder.toString());
        } else {
            sendMessageToClient("No files available in server file directory.");
        }
    }

    private void sendFile(String filename) {
        File directory = new File("./server_files");
        File file = new File(directory, filename);

        if (file.exists()) {
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
                System.out.println("File sent to Client: " + filename);
            } catch (IOException e) {
                System.out.println("Error sending file: " + e.getMessage());
            }
        } else {
            try {
                sendMessageToClient("Error: File not found in server file directory!");
            } catch (IOException e) {
                System.out.println("Error sending error message: " + e.getMessage());
            }
        }
    }

    private void receiveFile(String filename) {
        // Specify the directory where files should be saved
        File directory = new File("./server_files");
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
    
            while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
    
            System.out.println("File '" + filename + "' successfully received and saved in '" + file.getAbsolutePath() + "'");
        } catch (IOException e) {
            System.out.println("Error receiving file '" + filename + "': " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            // Close the input and output streams first
            if (dataOut != null) {
                dataOut.close();
            }
            if (dataIn != null) {
                dataIn.close();
            }
            // Close the socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Connection with client closed.");
        } catch (IOException e) {
            System.out.println("Error closing client connection: " + e.getMessage());
        }
    }
}