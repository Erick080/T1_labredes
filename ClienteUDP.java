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
                    byte[] buffer = new byte[8192]; //8KB
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivePacket);
                        String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        if(receiveMessage.startsWith("[FILE]")) {
                            String[] parts = receiveMessage.split(" ");
                            int numChunks = Integer.parseInt(parts[1]);
                            String fileName = receiveMessage.substring(7 + parts[1].length()).trim();
                            int count = 0;

                            try {
                                File file = new File(fileName);
                                if (file.createNewFile()) {
                                    System.out.println("File created: " + file.getName());
                                }
                                else {
                                    System.out.println("File already exists. Overwriting...");
                                }
                                
                                FileOutputStream fos = new FileOutputStream(fileName);

                                while (count < numChunks) {
                                    receivePacket = new DatagramPacket(buffer, buffer.length);
                                    socket.receive(receivePacket);
                                    fos.write(receivePacket.getData(), 0, receivePacket.getLength());
                                    count++;
                                }
                                fos.close();
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

                    int chunkSize = 8; // 8KB
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

                        System.out.printf("enviando pacote %d p/ servidor\n", (i+1));
                    }
                    streamReadFile.close();
                    bufferReadFile.close();
                    System.out.println("File sent to server.");
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
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
