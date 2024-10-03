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

            // Thread for server messages handling
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[10240]; //10KB
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        if(receiveMessage.startsWith("[FILE]")) {
                            int numChunks = Integer.parseInt(receiveMessage.split(" ")[1]);
                            String fileName = receiveMessage.split(" ")[2];
                            int count = 0;

                            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                                File file = new File(fileName);
                                if (file.createNewFile())
                                    System.out.println("File created: " + file.getName());
                                else 
                                    System.out.println("File already exists. Overwriting...");

                                while (count < numChunks) {
                                    receivePacket = new DatagramPacket(buffer, buffer.length);
                                    socket.receive(receivePacket);
                                    fos.write(receivePacket.getData(), 0, receivePacket.getLength());
                                    count++;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else
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

                    if(message.split(" ").length < 3) {
                        System.out.println("Comando inválido. Uso: /FILE <destinatário> <caminho do arquivo>");
                        continue;
                    }

                    String parts[] = message.substring(6).trim().split(" ");
                    String fileName = message.substring(6 + parts[0].length()).trim();
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
                else {
                    byte[] buffer = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
                    socket.send(sendPacket);
                }

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
