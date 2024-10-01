import java.net.*;
import java.util.Scanner;
import java.io.*;

public class ClienteTCP {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println("Uso: java ClienteTCP <server_ip> <port>");
            return;
        }
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        Scanner sc = new Scanner(System.in);

        try (Socket socket = new Socket(hostname, port)) {
            OutputStream output = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            PrintWriter writer = new PrintWriter(output, true);

            // thread para receber msgs
            new Thread(() -> {
                try {
                    String resposta;
                    while ((resposta = in.readLine()) != null) {
                        System.out.println(resposta);
                        if (resposta.startsWith("[SERVER]: Ate a proxima,")){
                            System.exit(1);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // le input do usuario
            while (true){
                String line = sc.nextLine();

                if(line.startsWith("/FILE")){
                    File arquivo = new File(line.split(" ")[1]);
                    byte[] bytes = new byte[8192];
                    writer.println(line);
                    
                    Thread.sleep(1000); // espera 1 segundo para que o server se prepare para ler a stream
                    InputStream stream_arquivo = new FileInputStream(arquivo);
                    int count;
                    while ((count = stream_arquivo.read(bytes)) > 0) {
                        output.write(bytes, 0, count);
                    }
                    System.out.println("saiu");
                    stream_arquivo.close();
                }
                else
                    writer.println(line);
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