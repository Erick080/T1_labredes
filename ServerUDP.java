import java.io.*;
import java.net.*;
import java.util.*;

public class ServerUDP {
    private static List<InetSocketAddress> clients = new ArrayList<>();
    private static Map<InetSocketAddress, String> userNames = new HashMap<>();
    private static final boolean DEBUG = false;
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
                
                // Registrar usuario
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

                    // Adiciona o usuario se ja nao foi registrado
                    if(!clients.contains(clientSocketAddress)) {
                        userNames.put(clientSocketAddress, userName);
                        clients.add(clientSocketAddress);
                    }
                    else {
                        sendTo("Já registrado como " + userName, clientSocketAddress);
                        continue;
                    }
                    String msg = "Usuário conectado: " + userName + " (" + clientSocketAddress + ")";
                    sendTo(msg, clientSocketAddress);
                    System.out.println(msg);
                    continue;
                }

                // Garante que o usuario se registrou
                if(!userNames.containsKey(clientSocketAddress)) {
                    System.out.println("Cliente não registrado, favor registrar com /REG <nome de usuário>.");
                    continue;
                }
                
                if(command.startsWith("/MSG ")) {
                    if(command.substring(5).trim().split(" ").length < 2) {
                        System.out.println("Mensagem inválida. Uso: /MSG <usuário> <mensagem>");
                        continue;
                    }

                    String sender = userNames.get(clientSocketAddress);
                    String receiver = command.substring(5).trim().split(" ")[0];
                    String msg = sender + ": " + command.substring(5 + receiver.length()).trim();
                    if (DEBUG) System.out.println(receiver + " <- " + msg);

                
                    if(receiver.toLowerCase().equals("all"))
                        sendAll(msg, clientSocketAddress);
                    else 
                        sendTo(msg, receiver);
                    continue;
                }

                // Envia arquivos
                if (command.startsWith("/FILE ")) {
                    // Pega remetente e destinatario
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

                    // Pega numero de chunks e nome do arquivo
                    int numChunks = Integer.parseInt(parts[parts.length - 1]);
                    int fileNameEndIndex = command.length() - parts[parts.length - 1].length();
                    String fileName = command.substring(6 + receiver.length(), fileNameEndIndex).trim();
                    String msg = "Envio de arquivo: " + sender + " -> " + fileName;

                    byte[] fileBuffer = new byte[400]; // 400B
                    DatagramPacket packet = new DatagramPacket(fileBuffer, fileBuffer.length);

                    // Envia o numero de chunks para o destinatario
                    if(receiver.toLowerCase().equals("all")) {
                        continue;
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

                        // Remove clientes das listas
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

    // Envia msgs para usuario especifico sem precisar ter seu InetSocketAddress	
    public static void sendTo(String msg, String receiverName) throws IOException {
        sendTo(msg, findUser(receiverName));
    }

    // Envia msgs para usuario
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

    // Envia msgs para todos usuarios
    public static void sendAll(String msg, InetSocketAddress cliAddress) throws IOException {
        for(InetSocketAddress client : clients) {
            if((!client.equals(cliAddress)) && userNames.containsKey(client)) {
                sendTo(msg, client);
            }
        }
    }

    // Encontra o endereco de um usuario pelo seu username
    public static InetSocketAddress findUser(String userName) {
        for(InetSocketAddress client : clients) {
            if(userNames.get(client).equals(userName)) {
                return client;
            }
        }
        return null;
    }
}
