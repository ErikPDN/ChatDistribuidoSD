package servidor;

import java.io.BufferedReader;
import java.io.IOException;
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

                Server.broadcastMessage(this.username, clientMessage);
            }

        } catch (IOException e) {
            System.out.println("Erro no handler do cliente " + this.username + " saiu do chat.");
        } finally {
            // ---- LÓGICA DE LIMPEZA E DESCONEXÃO ----
            // Este bloco 'finally' garante que a limpeza sempre ocorra,
            // seja por desconexão normal ('sair') ou por um erro.
            if (this.username != null) {
                Server.removeClient(this.username);
                Server.broadcastMessage("Servidor", this.username + " saiu do chat.");
            }

            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Envia uma mensagem para o cliente que este handler está gerenciando.
     * Este método é chamado pelo Server para retransmitir as mensagens.
     * @param message A mensagem a ser enviada.
     */
    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    /**
     * Retorna o nome de usuário deste cliente.
     * @return O nome de usuário.
     */
    public String getUsername() {
        return this.username;
    }

}
