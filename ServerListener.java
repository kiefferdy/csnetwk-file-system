import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerListener extends Thread {
    private Socket socket;
    private Consumer<String> onResponseReceived;

    public ServerListener(Socket socket, Consumer<String> onResponseReceived) {
        this.socket = socket;
        this.onResponseReceived = onResponseReceived;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response;
            while ((response = reader.readLine()) != null) {
                onResponseReceived.accept(response);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}