package servidor;

import java.io.IOException;
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

  // Futuramente, podemos adicionar um método para mensagens privadas.
  // public static void sendPrivateMessage(String recipient, String message) { ...
  // }
}
