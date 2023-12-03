import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private static final Set<String> registeredUsers = Collections.synchronizedSet(new HashSet<>());

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error setting up streams: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String clientCommand;
            while ((clientCommand = reader.readLine()) != null) {
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
            }
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
        try {
            writer.println("START");
            FileOutputStream fileOut = new FileOutputStream(filename);
            BufferedInputStream inStream = new BufferedInputStream(socket.getInputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }

            fileOut.close();
            writer.println("END");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
}