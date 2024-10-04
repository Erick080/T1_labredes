import java.io.*;
import java.net.*;
import java.util.*;

public class ServerUDP {
    private static List<InetSocketAddress> clients = new ArrayList<>();
    private static Map<InetSocketAddress, String> userNames = new HashMap<>();
    private static final boolean DEBUG = true;
    public static DatagramSocket socket;

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Uso: java UDPserver <porta>");
            return;
        }

        int porta = Integer.parseInt(args[0]);

        try(DatagramSocket serverSocket = new DatagramSocket(porta)) {
            socket = serverSocket;

            System.out.println("Servidor pronto na porta " + porta);
            byte[] buffer = new byte[8192];

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
                if(command.startsWith("/REG")) {
                    if(command.split(" ").length != 2) {
                        sendTo("Comando inválido. Uso: /REG <nome_de_usuário>", clientSocketAddress);
                        continue;
                    }

                    String userName = command.substring(5).trim().split(" ")[0];

                    if(userName.toLowerCase().trim().equals("all") || userName.trim().equals("")) {
                        sendTo("Nome de usuário inválido", clientSocketAddress);
                        continue;
                    }

                    // Add the user if it's not already registered
                    if(!clients.contains(clientSocketAddress)) {
                        userNames.put(clientSocketAddress, userName);
                        clients.add(clientSocketAddress);
                    }
                    // If the user is already registered, print a message
                    else {
                        sendTo("Já registrado como " + userName, clientSocketAddress);
                        continue;
                    }
                    String msg = "Usuário conectado: " + userName + " (" + clientSocketAddress + ")";
                    sendTo(msg, clientSocketAddress);
                    System.out.println(msg);
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
                    if (DEBUG) System.out.println(receiver + " <- " + msg);

                    // Send the message to all users
                    if(receiver.toLowerCase().equals("all"))
                        sendAll(msg, clientSocketAddress);
                    else // Send the message to a specific user
                        sendTo(msg, receiver);
                    continue;
                }

                // Send files
                if (command.startsWith("/FILE ")) {
                    // Args length preventions on client side

                    // Get sender and receiver
                    String parts[] = command.substring(6).trim().split(" ");
                    String sender = userNames.get(clientSocketAddress);
                    String receiver = parts[0];
                    InetSocketAddress receiverSocket = null;

                    if(!receiver.toLowerCase().equals("all")) {
                        receiverSocket = findUser(receiver);
                        if(receiverSocket == null) {
                            sendTo("Usuário não encontrado", clientSocketAddress);
                            continue;
                        }
                    }

                    // Get the number of chunks and the file name
                    int numChunks = Integer.parseInt(parts[parts.length - 1]);
                    int fileNameEndIndex = command.length() - parts[parts.length - 1].length();
                    String fileName = command.substring(6 + receiver.length(), fileNameEndIndex).trim();
                    String msg = "Envio de arquivo: " + sender + " -> " + fileName;

                    // Receive and deliver the file
                    byte[] fileBuffer = new byte[400]; // 400B
                    DatagramPacket packet = new DatagramPacket(fileBuffer, fileBuffer.length);

                    // Send the number of chunks to the receiver
                    if(receiver.toLowerCase().equals("all")) {
                        //atualmente quebrado
                        continue;

                        // for(InetSocketAddress client : clients) {
                        //     if(!client.equals(clientSocketAddress) && userNames.containsKey(client)) {
                        //         if (DEBUG) System.out.println(sender + " -> " + userNames.get(client) + " - envio de arquivo: " + fileName);
                        //         sendTo("[FILE] " + numChunks + " " + sender + "-" + fileName, clientSocketAddress);
                        //     }
                        // }
                    }
                    else {
                        if (DEBUG) System.out.println(sender + " -> " + receiver + " - envio de arquivo: " + fileName);
                        sendTo("[FILE] " + numChunks + " " + sender + "-" + fileName, receiverSocket);
                    }

                    for (int i = 0; i < numChunks; i++) {
                        socket.receive(packet);
                        String filePart = new String(packet.getData(), 0, packet.getLength());

                        System.out.printf("enviando pacote %d p/ " + receiver + "\n", (i+1));
                        
                        if(receiver.toLowerCase().equals("all")) {
                            sendAll(filePart, clientSocketAddress);
                        }
                        else {
                            sendTo(filePart, receiver);
                        }
                    }

                    sendTo(msg, receiverSocket);
                }

                // Disconnect
                if(command.startsWith("/FIM")) {
                    if(userNames.containsKey(clientSocketAddress)) {
                        String userName = userNames.get(clientSocketAddress);
                        System.out.println("Usuário desconectado: " + userName + " (" + clientSocketAddress + ")");

                        // Remove the user from the lists
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

    // Overcharge to send messages to a specific user without having its InetSocketAddress	
    public static void sendTo(String msg, String receiverName) throws IOException {
        sendTo(msg, findUser(receiverName));
    }

    // Send messages to a specific user
    public static void sendTo(String msg, InetSocketAddress cliSocket) throws IOException {
        if(cliSocket == null) {
            System.out.println("Usuário não encontrado");
            return;
        }
        byte[] buffer = msg.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
                buffer,
                buffer.length,
                cliSocket.getAddress(),
                cliSocket.getPort()
        );
        socket.send(sendPacket);
    }

    // Send messages to all users
    public static void sendAll(String msg, InetSocketAddress cliAddress) throws IOException {
        for(InetSocketAddress client : clients) {
            if((!client.equals(cliAddress)) && userNames.containsKey(client)) {
                sendTo(msg, client);
            }
        }
    }

    // Find a user by its username
    public static InetSocketAddress findUser(String userName) {
        for(InetSocketAddress client : clients) {
            if(userNames.get(client).equals(userName)) {
                return client;
            }
        }
        return null;
    }
}
