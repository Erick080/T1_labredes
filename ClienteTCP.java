import java.net.*;
import java.util.Scanner;
import java.io.*;

public class ClienteTCP implements Cliente {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TCPClient <server_ip> <port>");
            return;
        }
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        Scanner sc = new Scanner(System.in);
        try (Socket socket = new Socket(hostname, port)) {
 
            while (true){
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
     
                String line = sc.nextLine();
    
                writer.println(line);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
                
                String resposta = in.readLine();
                System.out.println("Resposta recebida -> " + resposta);

                if (line.trim().equalsIgnoreCase("EXIT"))
                    break;
            } 
 
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
        }
        finally {
            sc.close();
        }
      
    }
    
}