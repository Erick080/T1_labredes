import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ServerTCP {
    static ArrayList<ConnThread> lista_threads_clientes = new ArrayList<ConnThread>();
    
    public static class ConnThread extends Thread{ // classe usada para escutar a conexao de um cliente conectado
        public Socket socket_cliente;
        public String nickname;
        private BufferedReader in;

        ConnThread(Socket socket_cliente) throws IOException{
            this.socket_cliente = socket_cliente;
            this.in = new BufferedReader(new InputStreamReader(socket_cliente.getInputStream()));
        }
        
        public static Socket get_user_socket(String username){
            for (ConnThread thread : lista_threads_clientes) {
                if (thread.nickname.equalsIgnoreCase(username))
                    return thread.socket_cliente;
            }
            return null;
        }

        public void send_message(Socket destino, String message, Boolean server_msg){
            OutputStream output;
            if (server_msg){
                message = "[SERVER]: " + message;
            }
            else
               message = this.nickname + ": " + message;
    
           try {
                output = destino.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(message);
               // writer.close();
               // output.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }

        public void run(){
            System.out.println("New client connected: " + socket_cliente.getRemoteSocketAddress());
            try {
                while (true){
                    String line = in.readLine();
                    if (line.startsWith("/REG")){
                        this.nickname = line.split(" ")[1];
                        System.out.println("Novo usuario registrado: " + nickname);
                        send_message(socket_cliente, "Bem vindo, " + nickname , true);
                    }
                    else if(line.startsWith("/MSG")){
                        String destinatario = line.split(" ")[1];
                        String message = line.split(" ")[2];

                        if (destinatario.equalsIgnoreCase("all")){
                            for (ConnThread thread : lista_threads_clientes) {
                                send_message(thread.socket_cliente, message, false);
                            }
                        }
                        else{
                            Socket destinatario_socket = get_user_socket(destinatario);
                            System.out.println("destinatario_socket = " + destinatario_socket.getRemoteSocketAddress());
                            send_message(destinatario_socket, message, false);
                        }
                    }
                    else if(line.startsWith("/QUIT")){
                        System.out.println("Desconectando " + nickname);
                        send_message(socket_cliente, "Ate a proxima, " + nickname, true);
                        break;
                        // destruir thread apos encerramento da conexao do cliente?
                    }
                    else{
                        send_message(socket_cliente, "Comando invalido!", true);
                    }
                }      
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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