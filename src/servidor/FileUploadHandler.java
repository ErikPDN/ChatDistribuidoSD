package servidor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import servidor.Server;

public class FileUploadHandler implements Runnable {
  private final ServerSocket serverSocket;
  private final String filePathOnServer;
  private final String senderUsername;
  private final String originalFileName;

  public FileUploadHandler(ServerSocket serverSocket, String filePathOnServer, String sender, String originalFileName) {
    this.serverSocket = serverSocket;
    this.filePathOnServer = filePathOnServer;
    this.senderUsername = sender;
    this.originalFileName = originalFileName;
  }

  @Override
  public void run() {
    try {
      // Cria o diretório de uploads se não existir
      new File("temp_uploads").mkdir();

      try (Socket clientSocket = serverSocket.accept();
          InputStream in = clientSocket.getInputStream();
          FileOutputStream fos = new FileOutputStream(filePathOnServer)) {

        System.out.println("Cliente " + senderUsername + " conectado para upload.");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
          fos.write(buffer, 0, bytesRead);
        }
        System.out.println("Upload de " + originalFileName + " concluído.");

        // Avisa o servidor principal para notificar a todos
        long fileSize = new File(filePathOnServer).length();
        Server.notifyFileBroadcast(senderUsername, originalFileName, filePathOnServer, fileSize);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        serverSocket.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
