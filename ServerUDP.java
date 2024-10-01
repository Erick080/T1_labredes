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
                
                // user registration
                if(command.startsWith("/REG ")) {
                    String userName = command.substring(5).trim().split(" ")[0];
                    if(!clients.contains(clientSocketAddress)) {
                        userNames.put(clientSocketAddress, userName);
                        clients.add(clientSocketAddress);
                    }
                    else {
                        System.out.println("Usuário já registrado: " + userName + " (" + clientSocketAddress + ")");
                        for(InetSocketAddress client : clients) {
                            if(userNames.get(client).equals(userName)) {
                                System.out.println(userName + " já está conectado.");
                                break;
                            }
                        }
                    }
                    System.out.println("Usuário conectado: " + userName + " (" + clientSocketAddress + ")");
                    continue;
                }

                // Verificar se o cliente já se registrou com um nome de usuário
                if(command.startsWith("/MSG ")) {
                    if(!userNames.containsKey(clientSocketAddress)) {
                        System.out.println("Cliente não registrado tentou enviar uma mensagem.");
                        continue;
                    }

                    if(command.substring(5).trim().split(" ").length < 2) {
                        System.out.println("Mensagem inválida.");
                        continue;
                    }

                    String sender = userNames.get(clientSocketAddress);
                    String receiver = command.substring(5).trim().split(" ")[0];
                    String msg = sender + ": " + command.substring(5 + receiver.length()).trim();
                    System.out.println(receiver + " <- " + msg);

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
