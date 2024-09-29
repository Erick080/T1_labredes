// Cliente UDP
// - Le uma linha do teclado
// - Envia o pacote (linha digitada) ao servidor UDP

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClienteUDP implements Cliente {
   public static void main(String args[]) throws Exception {
      if (args.length < 2) {
         System.out.println("Usage: java UDPClient <server_ip> <port>");
         return;
      }

      String serverAddr = args[0];
      int port = Integer.parseInt(args[1]);

      // cria o stream do teclado
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

      // declara socket cliente
      DatagramSocket clientSocket = new DatagramSocket();

      // obtem endereco IP do servidor a partir de uma string (IP ou nome)
      InetAddress ipAddress = InetAddress.getByName(serverAddr);

      byte[] sendData = new byte[1024];

      // le uma linha do teclado
      System.out.print("Digite uma mensagem: ");
      String sentence = inFromUser.readLine();
      sendData = sentence.getBytes();

      // cria pacote com o dado, o endereco do server e porta do servidor
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);

      // envia o pacote
      clientSocket.send(sendPacket);

      // ===================================================================

      //resposta do servidor
      byte[] receiveData = new byte[1024];

      // declara o pacote a ser recebido
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);

      // obtem a mensagem do servidor
      String response = new String(receivePacket.getData());

      // imprime a resposta do servidor
      System.out.println(response);

      // fecha o cliente
      clientSocket.close();
   }
}
