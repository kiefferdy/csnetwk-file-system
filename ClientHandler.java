import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Server server;  // Reference to the Server
    private static final Set<String> registeredUsers = Collections.synchronizedSet(new HashSet<>());

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;  // Assign the Server instance
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            server.updateLog("Error setting up streams: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String clientCommand;
            while ((clientCommand = reader.readLine()) != null) {
<<<<<<< Updated upstream
                if (clientCommand.startsWith("/store ")) {
                    String filename = clientCommand.substring(7);
                    receiveFile(filename);
                    continue;
                }
                if (clientCommand.startsWith("/get ")) {
                    String filename = clientCommand.substring(5);
                    sendFile(filename);
                    continue;
                }
                String response = handleClientCommand(clientCommand);
                writer.println(response); // Send response back to client
=======
                if ("/leave".equals(clientCommand)) {
                    server.notifyClientDisconnected(socket);
                    break; // Exit the loop and end this thread
                }
                handleClientCommand(clientCommand);
>>>>>>> Stashed changes
            }
        } catch (IOException e) {
            server.updateLog("Client handler exception: " + e.getMessage());
        } finally {
            try {
                socket.close();
                server.updateLog("Client disconnected.");
            } catch (IOException e) {
                server.updateLog("Error closing socket: " + e.getMessage());
            }
        }
    }

<<<<<<< Updated upstream
    private String handleClientCommand(String clientCommand) {
        // ... handle other commands like /register, /dir, etc. ...

        return "Command executed.";
    }

    private void sendFile(String filename) {
        try {
            File file = new File(filename);
            if (file.exists()) {
                writer.println("START");
                FileInputStream fileIn = new FileInputStream(file);
                BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());

=======
    private void handleClientCommand(String clientCommand) {
        if (clientCommand.startsWith("/register ")) {
            String handle = clientCommand.substring(10).trim();
            synchronized (registeredUsers) {
                if (!registeredUsers.contains(handle)) {
                    registeredUsers.add(handle);
                    writer.println("Handle registered: " + handle);
                    isRegistered = true; // Set the flag to true on successful registration
                } else {
                    writer.println("Error: Handle already exists");
                }
            }
        } else if (!isRegistered) {
            writer.println("Error: You must register first with /register <handle>");
        } else {
            if (clientCommand.startsWith("/store ")) {
                String filename = clientCommand.substring(7); // Extract filename
                writer.println("START"); // Signal to start file transfer
                writer.flush(); // Ensure the message is sent immediately
                receiveFile(filename);
                writer.println("END"); // Signal end of file transfer
                writer.flush(); // Ensure the message is sent immediately
            } else if (clientCommand.equals("/dir")) {
                File dir = new File("./files"); // Current directory
                File[] filesList = dir.listFiles();
                if (filesList != null) {
                    for (File file : filesList) {
                        if (file.isFile()) {
                            writer.println(file.getName());
                        }
                    }
                }
            } else if (clientCommand.startsWith("/get ")) {
                String filename = clientCommand.substring(5); // Extract filename
                sendFile(filename);
            }
        }
    }

    private void sendFile(String filename) {
        File file = new File("./files/" + filename); // Assuming the files are in a 'files' directory
        if (file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(file);
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
>>>>>>> Stashed changes
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }

                outStream.flush();
                fileIn.close();
                writer.println("END");
            } else {
                writer.println("Error: File not found");
            }
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private void receiveFile(String filename) {
<<<<<<< Updated upstream
        try {
            writer.println("START");
            FileOutputStream fileOut = new FileOutputStream(filename);
            BufferedInputStream inStream = new BufferedInputStream(socket.getInputStream());

=======
        File directory = new File("files");
        if (!directory.exists()) {
            directory.mkdir();
        }

        File file = new File(directory, filename);
        try (FileOutputStream fileOut = new FileOutputStream(file);
            DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
            long fileSize = dataIn.readLong();
            long totalBytesRead = 0;
>>>>>>> Stashed changes
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
<<<<<<< Updated upstream

            fileOut.close();
            writer.println("END");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
=======
            System.out.println("Server: File received and saved.");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }   
>>>>>>> Stashed changes
}