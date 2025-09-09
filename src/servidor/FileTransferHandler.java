package servidor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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

  public FileTransferHandler(ServerSocket fileSocket, String senderUsername, String recipientUsername) {
    this.fileSocket = fileSocket;
    this.sender = senderUsername;
    this.recipient = recipientUsername;
  }

  @Override
  public void run() {
    System.out
        .println("Handler de transferência de arquivo iniciado. Aguardando conexão do remetente e destinatário...");

    Socket senderSocket = null;
    Socket recipientSocket = null;

    try {
      senderSocket = fileSocket.accept();
      System.out.println("Remetente (" + sender + ") conectado para transferencia de arquivo.");

      recipientSocket = fileSocket.accept();
      System.out.println("Destinatário (" + recipient + ") conectado para transferencia de arquivo.");

      this.transferBytes(senderSocket, recipientSocket);
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

      byte[] buffer = new byte[4096];
      int bytesRead;

      // If no byte is available because the stream is at the end of the file,
      // the value `-1` is returned;
      while ((bytesRead = senderStream.read(buffer)) != -1) {
        // write (byte[] b, int off, int len)
        recipientStream.write(buffer, 0, bytesRead);
      }

      System.out.println("Cópia de bytes concluída.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
