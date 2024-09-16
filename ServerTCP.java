import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ServerTCP {
    static ArrayList<ConnThread> lista_threads_clientes = new ArrayList<ConnThread>();
    
    public static class ConnThread extends Thread{ // classe usada para escutar a conexao de um cliente conectado
        public Socket socket_cliente;
        public String nickname;

        ConnThread(Socket socket_cliente){
            this.socket_cliente = socket_cliente;
        }

        public void run(){
            System.out.println("New client connected: " + socket_cliente.getRemoteSocketAddress());
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket_cliente.getInputStream()));
                while (true){
                    String line = in.readLine();
                    if (line.startsWith("/REG")){
                        this.nickname = line.split(" ")[1];
                        System.out.println("Novo usuario registrado: " + nickname);
                    }
                    else if(line.startsWith("/MSG")){
                        String destinatario = line.split(" ")[1];
                        if (destinatario.equalsIgnoreCase("all")){
                            for (ConnThread thread : lista_threads_clientes) {
                                Socket destinatario_socket = thread.socket_cliente;
                                // enviar para destinatario a msg
                            }
                        }
                        else{
                            Socket destinatario_socket = get_user_socket(destinatario);
                            // enviar para destinatario a msg
                        }
                    }
                    else if(line.startsWith("/QUIT")){
                        System.out.println("Saindo...");
                        break;
                    }
                }      
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Socket get_user_socket(String username){
        for (ConnThread thread : lista_threads_clientes) {
            if (thread.nickname.equalsIgnoreCase(username))
                return thread.socket_cliente;
        }
        return null;
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java TCPServer <port>");
            return;
        }
 
        int port = Integer.parseInt(args[0]);
        
        ConnThread conexao;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true){
                Socket cliente = serverSocket.accept();
                conexao = new ConnThread(cliente);
                conexao.start();
                lista_threads_clientes.add(conexao);
            }
            
        }
    }
    
}