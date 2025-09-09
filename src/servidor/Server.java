package servidor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Server {
  private static final int PORT = 12345;

  // Um pool de threads para gerenciar os clientes de forma eficiente.
  // Evita o custo de criar uma nova thread para cada cliente.
  private static final ExecutorService pool = Executors.newCachedThreadPool();

  private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static void main(String[] args) {
    System.out.println("Iniciando servidor do chat...");

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Servidor iniciando na porta " + PORT + ". Aguardando clientes...");

      while (true) {
        // O método accept() é bloqueante: ele espera até que um cliente se conecte.
        Socket clientSocket = serverSocket.accept();
        System.out.println("Novo cliente conectado: " + clientSocket.getRemoteSocketAddress());

        // Cria um novo handler para o cliente e o submete ao pool de threads.
        // O servidor principal não fica bloqueado e pode aceitar outros clientes.
        ClientHandler clientHandler = new ClientHandler(clientSocket);
        pool.execute(clientHandler);
      }
    } catch (IOException e) {
      System.err.println("Erro no servidor: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Adiciona um cliente à lista de clientes conectados.
   * Este método é chamado pelo ClientHandler após o usuário se identificar.
   * 
   * @param username O nome de usuário.
   * @param handler  A instância do ClientHandler associada.
   */
  public static void addClient(String username, ClientHandler handler) {
    clients.put(username, handler);
    System.out.println("Usuário " + username + " entrou no chat");
  }

  /**
   * Remove um cliente da lista quando ele se desconecta.
   * 
   * @param username O nome de usuário a ser removido.
   */
  public static void removeClient(String username) {
    clients.remove(username);
    System.out.println("Usuário " + username + " saiu do chat.");
  }

  /**
   * Envia uma mensagem para todos os clientes conectados, exceto para o
   * remetente.
   * 
   * @param senderUsername O nome do remetente.
   * @param message        O conteúdo da mensagem.
   */
  public static void broadcastMessage(String senderUsername, String message) {
    String timestamp = LocalDateTime.now().format(FORMATTER);

    for (ClientHandler handler : clients.values()) {

      if (!handler.getUsername().equals(senderUsername)) {
        handler.sendMessage(String.format("[%s] %s: %s", timestamp, senderUsername, message));
      }
    }
  }

  /**
   * Envia uma mensagem privada de um usuário para outro.
   *
   * @param senderUsername    O nome do remetente.
   * @param recipientUsername O nome do destinatário.
   * @param message           O conteúdo da mensagem.
   */
  public static void sendPrivateMessage(String senderUsername, String recipientUsername, String message) {
    String timestamp = LocalDateTime.now().format(FORMATTER);

    ClientHandler recipientHandler = clients.get(recipientUsername);

    if (recipientHandler != null) {
      // Se o destinatário for encontrado, envia a mensagem para ele.
      String formattedMessage = String.format("[%s] (privado) %s: %s", timestamp, senderUsername, message);
      recipientHandler.sendMessage(formattedMessage);
    } else {
      // Se o destinatário não for encontrado, avisa o remetente.
      ClientHandler senderHandler = clients.get(senderUsername);
      if (senderHandler != null) {
        senderHandler.sendMessage("Servidor: Usuário '" + recipientUsername + "' não encontrado ou offline.");
      }
    }
  }

  public static void requestFileTransfer(String sender, String recipient, String filePath, long fileSize) {
    ClientHandler recipientHandler = clients.get(recipient);
    if (recipientHandler != null) {
      recipientHandler.sendMessage(String.format("INCOMING_FILE @%s %s %d", sender, filePath, fileSize));
    } else {
      clients.get(sender).sendMessage("Servidor: Destinatário " + recipient + " não encontrado.");
    }
  }

  public static void prepareFileTransfer(String sender, String recipient) {
    try {
      ServerSocket fileSocket = new ServerSocket(0); // porta aleatoria
      int port = fileSocket.getLocalPort();

      // Inicia uma nova thread para gerenciar a transferência
      FileTransferHandler transferHandler = new FileTransferHandler(fileSocket, sender, recipient);
      pool.execute(transferHandler);

      // Obtém o endereço IP local do servidor dinamicamente
      String ip = InetAddress.getLocalHost().getHostAddress();

      System.out.println("Iniciando transferência de arquivo. IP: " + ip + ", Porta: " + port);

      // Avisa ambos os clientes para se conectarem ao novo canal usando o IP correto
      clients.get(sender).sendMessage(String.format("TRANSFER_READY %s %d @%s", ip, port, recipient));
      clients.get(recipient).sendMessage(String.format("TRANSFER_READY %s %d @%s", ip, port, sender));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
