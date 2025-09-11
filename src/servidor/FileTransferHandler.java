package servidor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Uma classe Runnable dedicada a gerenciar uma única transferência de arquivo.
 * Ela abre um ServerSocket temporário, espera que o remetente e o destinatário
 * se conectem,
 * e então transfere os bytes de um para o outro.
 */
public class FileTransferHandler implements Runnable {

  private final ServerSocket fileSocket;
  private final String sender;
  private final String recipient;

  // Timeout para conexões (60 segundos)
  private static final int CONNECTION_TIMEOUT = 60000;
  // Timeout para operações de I/O (30 segundos)
  private static final int IO_TIMEOUT = 30000;

  public FileTransferHandler(ServerSocket fileSocket, String senderUsername, String recipientUsername) {
    this.fileSocket = fileSocket;
    this.sender = senderUsername;
    this.recipient = recipientUsername;

    try {
      // Define timeout para aceitar conexões
      this.fileSocket.setSoTimeout(CONNECTION_TIMEOUT);
    } catch (Exception e) {
      System.err.println("Erro ao configurar timeout do socket: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    System.out
        .println("Handler de transferência de arquivo iniciado. Aguardando conexão do remetente e destinatário...");

    Socket senderSocket = null;
    Socket recipientSocket = null;

    try {
      // Espera conexão do primeiro cliente (pode ser remetente ou destinatário)
      Socket firstSocket = fileSocket.accept();
      Socket secondSocket = fileSocket.accept();

      // Determina qual é qual baseado na ordem de conexão
      // Na prática, ambos tentam conectar quase simultaneamente
      senderSocket = firstSocket;
      recipientSocket = secondSocket;

      System.out.println(
          "Remetente (" + sender + ") e destinatário (" + recipient + ") conectados para transferência de arquivo.");

      // Configura timeouts para os sockets de transferência
      senderSocket.setSoTimeout(IO_TIMEOUT);
      recipientSocket.setSoTimeout(IO_TIMEOUT);

      this.transferBytes(senderSocket, recipientSocket);
    } catch (SocketTimeoutException e) {
      System.err.println("Timeout na transferência de arquivo entre " + sender + " e " + recipient);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (senderSocket != null)
          senderSocket.close();
        if (recipientSocket != null)
          recipientSocket.close();
        if (this.fileSocket != null)
          this.fileSocket.close();

        System.out.println("Transferência de arquivo finalizada");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void transferBytes(Socket senderSocket, Socket recipientSocket) throws IOException {
    System.out.println("Copiando bytes...");

    try (InputStream senderStream = senderSocket.getInputStream();
        OutputStream recipientStream = recipientSocket.getOutputStream()) {

      byte[] buffer = new byte[8192];
      int bytesRead;
      long totalBytes = 0;
      long lastProgressReport = System.currentTimeMillis();

      // If no byte is available because the stream is at the end of the file,
      // the value `-1` is returned;
      while ((bytesRead = senderStream.read(buffer)) != -1) {
        // write (byte[] b, int off, int len)
        recipientStream.write(buffer, 0, bytesRead);
        totalBytes += bytesRead;

        // Relatório de progresso a cada 5 segundos
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProgressReport > 5000) {
          System.out.printf("Transferência em progresso: %.2f MB transferidos%n",
              totalBytes / (1024.0 * 1024.0));
          lastProgressReport = currentTime;
        }
      }

      recipientStream.flush();

      System.out.printf("Transferência de arquivo concluída. Total: %.2f MB%n",
          totalBytes / (1024.0 * 1024.0));

      System.out.println("Cópia de bytes concluída.");
    } catch (SocketTimeoutException e) {
      System.err.println("Timeout durante transferência de bytes.");
      throw e;
    } catch (IOException e) {
      System.err.println("Erro de I/O durante transferência: " + e.getMessage());
      throw e;
    }
  }
}
