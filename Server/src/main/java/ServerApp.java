import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class ServerApp {

    public static final int PORT = 8080;
    public static LinkedList<Server> serverList = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server Started");
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new Server(socket));

                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}
