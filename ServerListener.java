import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerListener extends Thread {
    private Socket socket;

    public ServerListener(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response;
            while ((response = reader.readLine()) != null) {
                System.out.println(response);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}