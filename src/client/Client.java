package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

  private static final String SERVER_ADDRESS = "localhost";

  private static final int SERVER_PORT = 12345;

  public static void main(String[] args) {
    // O try-with-resources garante que todos os recursos (Socket, Reader, Writer)
    // serão fechados automaticamente ao final do bloco
    try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))

    ) {
      System.out.println("Conectado ao servidor de chat. ");

      Thread messageListerner = new Thread(() -> {
        try {
          String serverMessage;
          while ((serverMessage = serverReader.readLine()) != null) {
            System.out.println(serverMessage);
          }
        } catch (IOException e) {
          System.out.println("Conexão com o servidor perdida. ");
        }
      });

      messageListerner.start();

      // --- THREAD PRINCIPAL PARA ENVIAR MENSAGENS DO USUÁRIO ---
      // A primeira mensagem do servidor será o pedido de nome de usuário.
      // O cliente apenas precisa enviar o nome de usuário como sua primeira mensagem.
      // A thread de escuta (acima) irá imprimir a solicitação na tela.
      String userInput;

      while ((userInput = consoleReader.readLine()) != null) {
        writer.println(userInput);
        if ("sair".equalsIgnoreCase(userInput.trim())) {
          break;
        }
      }
      
    } catch (UnknownHostException e) {
      System.out.println("Endereço do servidor não encontrado: " + SERVER_ADDRESS);
    } catch (IOException e) {
      System.out.println("Não foi possível conectar ao servidor. Verifique se ele está ativo.");
    }
  }

}
