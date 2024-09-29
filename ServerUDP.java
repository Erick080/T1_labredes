// Servidor UDP
// - Recebe um pacote de algum cliente
// - Separa o dado, o endereco IP e a porta deste cliente
// - Imprime o mensagem recebida

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerUDP implements Server {

   @Override
    public String get_user_address(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'get_user_address'");
    }

   public static void main(String args[]) throws Exception {
         if (args.length < 1) {
            System.out.println("Usage: java UDPServer <port>");
            return;
         }

         int port = Integer.parseInt(args[0]);

         // cria socket do servidor com a porta especificada
         DatagramSocket serverSocket = new DatagramSocket(port);

         while(true) {
            byte[] receiveData = new byte[1024];

            // declara o pacote a ser recebido
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // recebe o pacote do cliente
            serverSocket.receive(receivePacket);

            // obtem os dados, o endereco IP e a porta do cliente
            String sentence = new String(receivePacket.getData());
            InetAddress ipAddress = receivePacket.getAddress();
            int receivePort = receivePacket.getPort();

            // imprime remetente da mensagem
            System.out.println("Recebi mensagem de " + ipAddress.getHostAddress() + ":" + receivePort);
            
            // imprime a linha recebida do cliente
            System.out.println("Mensagem recebida: " + sentence);

            // Confirmar Recebimento ===========================================

            // cria pacote com o dado, o endereco do server e porta do servidor
            byte[] sendData = new byte[1024];
            sendData = ("Recebido").getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, receivePort);
            serverSocket.send(sendPacket);

            // Finaliza o servidor caso a mensagem seja "FIM"
            if(sentence.trim().toUpperCase().equals("FIM")) {
               System.out.println("Finalizando.");
               serverSocket.close();
               break;
            }
         }

         //serverSocket.close();
      }
}
