package servidor;

import java.io.BufferedReader; import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ClientHandler é uma classe Runnable que gerencia a comunicação
 * com um único cliente conectado ao servidor. Cada cliente terá sua
 * própria instância de ClientHandler rodando em uma thread separada.
 */
public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private PrintWriter writer;
  private BufferedReader reader;
  private String username;

  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
  }

  @Override
  public void run() {
    try {
      // Inicializa os streams de entrada e saída para este cliente.
      // O 'true' no PrintWriter habilita o auto-flush, garantindo que as mensagens
      // sejam enviadas imediatamente.
      this.writer = new PrintWriter(clientSocket.getOutputStream(), true);
      this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      // ---- LÓGICA DE "LOGIN" ----
      // Solicita um nome de usuário até que um válido seja fornecido.
      // (Aqui, estamos apenas garantindo que não seja nulo ou vazio).
      writer.println("Bem-vindo ao Chat! Por favor, digite seu nome de usuário:");
      this.username = reader.readLine();

      Server.addClient(this.username, this);

      Server.broadcastMessage("Servidor", this.username + " entrou no chat");
      writer.println("Você entrou no chat. Digite 'sair' para se desconectar.");

      String clientMessage;

      while ((clientMessage = reader.readLine()) != null) {
        if ("sair".equalsIgnoreCase(clientMessage.trim())) {
          break;
        }

        if (clientMessage.startsWith("SENDFILE_REQUEST")) {
          String[] parts = clientMessage.split(" ", 4);
          if (parts.length == 4) {
            // O ClientHandler não acessa o arquivo, apenas repassa a intenção
            // A lógica de ler o arquivo fica no próprio Cliente.
            // Ex: SENDFILE_REQUEST @bob relatorio.pdf 123456
            String recipient = parts[1];
            String filePath = parts[2];
            long fileSize = Long.parseLong(parts[3]);

            Server.requestFileTransfer(this.username, recipient, filePath, fileSize);
          }
        } else if (clientMessage.startsWith("SENDFILE_ACCEPT")) {
          String[] parts = clientMessage.split(" ", 2);
          if (parts.length == 2) {
            String sender = parts[1];
            Server.prepareFileTransfer(sender, this.username);
          }
        } else if (clientMessage.startsWith("@")) {
          // Divide a msg em destinatario e conteudo
          String[] parts = clientMessage.split(" ", 2);
          if (parts.length == 2) {
            String recipient = parts[0].substring(1); // remove o @
            String privateMessage = parts[1];

            Server.sendPrivateMessage(this.username, recipient, privateMessage);
          } else {
            sendMessage("Servidor: Formato inválido. Use @user <mensagem>");
          }
        } else {
          Server.broadcastMessage(this.username, clientMessage);
        }
      }
    } catch (IOException e) {
      System.out.println("Erro no handler do cliente " + this.username + " saiu do chat.");
    } finally {
      // Este bloco 'finally' garante que a limpeza sempre ocorra,
      // seja por desconexão normal ('sair') ou por um erro.
      if (this.username != null) {
        Server.removeClient(this.username);
        Server.broadcastMessage("Servidor", this.username + " saiu do chat.");
      }

      try {
        if (reader != null)
          reader.close();
        if (writer != null)
          writer.close();
        if (clientSocket != null)
          clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Envia uma mensagem para o cliente que este handler está gerenciando.
   * Este método é chamado pelo Server para retransmitir as mensagens.
   * 
   * @param message A mensagem a ser enviada.
   */
  public void sendMessage(String message) {
    if (writer != null) {
      writer.println(message);
    }
  }

  /**
   * Retorna o nome de usuário deste cliente.
   * 
   * @return O nome de usuário.
   */
  public String getUsername() {
    return this.username;
  }

}
