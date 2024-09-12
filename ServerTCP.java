import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ServerTCP {
    private class UserConnection {
        String username;
        Socket socket;
    }

    public class ConnThread extends Thread{
        public ArrayList<Socket> sockets_conectados;
        public ConnThread(){
            sockets_conectados = new ArrayList<Socket>();
        }
        public void run(){
            Socket socket = 
        }
    }

    static ArrayList<UserConnection> lista_conexoes;

    public Socket get_user_socket(String username) {
        for (UserConnection conexao : lista_conexoes) {
            if (conexao.username.equalsIgnoreCase(username)){
                return conexao.socket;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java TCPServer <port>");
            return;
        }
 
        int port = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            
            
        }
    }
    
    
}