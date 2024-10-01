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
