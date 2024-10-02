import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.*;
import java.util.*;

public class ServerUDP {
    private static List<InetSocketAddress> clients = new ArrayList<>();
    private static Map<InetSocketAddress, String> userNames = new HashMap<>();

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Uso: java UDPserver <porta>");
            return;
        }

        int porta = Integer.parseInt(args[0]);

        try(DatagramSocket socket = new DatagramSocket(porta)) {
            System.out.println("Servidor pronto na porta " + porta);
            byte[] buffer = new byte[1024];

            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                // Client info
                InetAddress clinetAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                InetSocketAddress clientSocketAddress = new InetSocketAddress(clinetAddress, clientPort);

                // Message
                String command = new String(receivePacket.getData(), 0, receivePacket.getLength());
                
                // User registration
                if(command.startsWith("/REG ")) {
                    String userName = command.substring(5).trim().split(" ")[0];

                    // Add the user if it's not already registered
                    if(!clients.contains(clientSocketAddress)) {
                        userNames.put(clientSocketAddress, userName);
                        clients.add(clientSocketAddress);
                    }
                    // If the user is already registered, print a message
                    else {
                        System.out.println("Usuário já registrado: " + userName + " (" + clientSocketAddress + ")");
                        for(InetSocketAddress client : clients) {
                            if(userNames.get(client).equals(userName)) {
                                System.out.println(userName + " já está conectado.");
                                continue;
                            }
                        }
                    }
                    System.out.println("Usuário conectado: " + userName + " (" + clientSocketAddress + ")");
                    continue;
                }

                // Verify if the client has already registered with a username
                if(!userNames.containsKey(clientSocketAddress)) {
                    System.out.println("Cliente não registrado, favor registrar com /REG <nome de usuário>.");
                    continue;
                }

                // Send messages
                if(command.startsWith("/MSG ")) {
                    if(command.substring(5).trim().split(" ").length < 2) {
                        System.out.println("Mensagem inválida. Uso: /MSG <usuário> <mensagem>");
                        continue;
                    }

                    // Get sender, receiver and the message
                    String sender = userNames.get(clientSocketAddress);
                    String receiver = command.substring(5).trim().split(" ")[0];
                    String msg = sender + ": " + command.substring(5 + receiver.length()).trim();
                    System.out.println(receiver + " <- " + msg);

                    // Send the message to all users
                    if(receiver.toLowerCase().equals("all")) {
                        for(InetSocketAddress client : clients) {
                            if(!client.equals(clientSocketAddress)) {
                                DatagramPacket sendPacket = new DatagramPacket(
                                        msg.getBytes(),
                                        msg.length(),
                                        client.getAddress(),
                                        client.getPort()
                                );
                                socket.send(sendPacket);
                            }
                        }
                    }
                    // Send the message to a specific user
                    else {
                        for(InetSocketAddress client : clients) {
                            if(userNames.get(client).equals(receiver)) {
                                DatagramPacket sendPacket = new DatagramPacket(
                                        msg.getBytes(),
                                        msg.length(),
                                        client.getAddress(),
                                        client.getPort()
                                );
                                socket.send(sendPacket);
                                break;
                            }
                        }
                    }
                    continue;
                }

                // Send files
                if (command.startsWith("/FILE ")) {
                    if(command.substring(6).trim().split(" ").length < 2) {
                        System.out.println("Comando inválido. Uso: /FILE <usuário> <arquivo>");
                        continue;
                    }

                    // Get sender, receiver and the file name
                    String splitCommand[] = command.substring(6).trim().split(" ");
                    String sender = userNames.get(clientSocketAddress);
                    String receiver = splitCommand[0];
                    int numChunks = Integer.parseInt(splitCommand[splitCommand.length - 1]);
                    int infoSize = splitCommand[splitCommand.length - 1].length();
                    String fileName = command.substring(6 + receiver.length(), command.length() - infoSize).trim();
                    System.out.println(sender + " -> " + receiver + " - envio de arquivo: " + fileName);

                    // Receive the file
                    FileOutputStream streamNewFile = new FileOutputStream("arquivo_teste_recebido");
                    // BufferedOutputStream bufferNewFile = new BufferedOutputStream(streamNewFile);

                    byte[] fileBuffer = new byte[8192]; // 8KB
                    DatagramPacket packet = new DatagramPacket(fileBuffer, fileBuffer.length);

                    for (int i = 0; i < numChunks; i++) {
                        socket.receive(packet);
                        String test = new String(packet.getData(), 0, packet.getLength());
                        //System.out.println(test);
                        streamNewFile.write(test.getBytes(), 0, test.getBytes().length * i);
                        streamNewFile.flush();
                    }
                    // bufferNewFile.close();
                    streamNewFile.close();

                    // Send the file to all users
                    // if(receiver.toLowerCase().equals("all")) {
                    //     for(InetSocketAddress client : clients) {
                    //         if(!client.equals(clientSocketAddress)) {
                    //             DatagramPacket sendPacket = new DatagramPacket(
                    //                     command.getBytes(),
                    //                     command.length(),
                    //                     client.getAddress(),
                    //                     client.getPort()
                    //             );
                    //             socket.send(sendPacket);
                    //         }
                    //     }
                    // }
                    // Send the file to a specific user
                    // else {
                    //     for(InetSocketAddress client : clients) {
                    //         if(userNames.get(client).equals(receiver)) {



                    //         }
                    //     }
                    // }
                }

                if(command.startsWith("/FIM")) {
                    if(userNames.containsKey(clientSocketAddress)) {
                        String userName = userNames.get(clientSocketAddress);
                        System.out.println("Usuário desconectado: " + userName + " (" + clientSocketAddress + ")");
                        clients.remove(clientSocketAddress);
                        userNames.remove(clientSocketAddress);
                    }
                    continue;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
