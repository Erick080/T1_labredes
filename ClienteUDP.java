import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteUDP {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java UDPclient <endereco_servidor> <porta_servidor>");
            return;
        }
        
        String strServerAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(strServerAddress);

            // Thread para receber mensagens do servidor
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println(receiveMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Enviar mensagens ao servidor
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();
                if(message.startsWith("/FILE ")) {
                    String splitMessage[] = message.substring(6).trim().split(" ");
                    String fileName = message.substring(6 + splitMessage[0].length()).trim();
                    File file = new File(fileName);

                    if(!file.exists()) {
                        System.out.println("Arquivo não encontrado.");
                        continue;
                    }

                    byte[] fileBytes = new byte[(int) file.length()];
                    FileInputStream streamReadFile = new FileInputStream(file);
                    BufferedInputStream bufferReadFile = new BufferedInputStream(streamReadFile);
                    bufferReadFile.read(fileBytes, 0, fileBytes.length);

                    int chunkSize = 8192; // 8KB
                    int numChunks = (int) Math.ceil(fileBytes.length / (double) chunkSize); 

                    message = message + " " + numChunks;
                    byte[] buffer = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                    socket.send(sendPacket);

                    for (int i = 0; i < numChunks; i++) {
                        int start = i * chunkSize;
                        int length = Math.min(chunkSize, fileBytes.length - start);
                        byte[] chunk = new byte[length];
                        System.arraycopy(fileBytes, start, chunk, 0, length);
        
                        DatagramPacket packet = new DatagramPacket(chunk, chunk.length, serverAddress, serverPort);
                        socket.send(packet);
                    }
                    streamReadFile.close();
                    bufferReadFile.close();
                    System.out.println("File sent successfully.");
                }

                byte[] buffer = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                socket.send(sendPacket);

                if(message.trim().equals("/FIM")) {
                    System.out.println("Encerrando.");
                    socket.close();
                    scanner.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
