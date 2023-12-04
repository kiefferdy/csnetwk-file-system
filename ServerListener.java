import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerListener implements Runnable {
    private Socket socket;
<<<<<<< Updated upstream
    private Consumer<String> onResponseReceived;
=======
    private Server Server;

    public ServerListener(Socket socket, Server Server) {
        this.socket = socket;
        this.Server = Server;
    }
>>>>>>> Stashed changes

    public ServerListener(Socket socket, Consumer<String> onResponseReceived) {
        this.socket = socket;
<<<<<<< Updated upstream
        this.onResponseReceived = onResponseReceived;
=======
        this.Server = null;
>>>>>>> Stashed changes
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String response;
            while ((response = reader.readLine()) != null) {
<<<<<<< Updated upstream
                onResponseReceived.accept(response);
=======
                if ("/leave".equals(response)) {
                    if (Server != null) {
                        Server.notifyClientDisconnected(socket);
                    }
                    break; // Exit the loop and end this thread
                }
                System.out.println(response);
>>>>>>> Stashed changes
            }
        } catch (Exception e) {
            if (Server != null) {
                Server.notifyClientDisconnected(socket);
            }
        }
    }
}

