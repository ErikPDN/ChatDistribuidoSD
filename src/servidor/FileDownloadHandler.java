package servidor;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class FileDownloadHandler implements Runnable {
  private final ServerSocket serverSocket;
  private final String filePathOnServer;

  public FileDownloadHandler(ServerSocket serverSocket, String filePathOnServer) {
    this.serverSocket = serverSocket;
    this.filePathOnServer = filePathOnServer;
  }

  @Override
  public void run() {
    try (Socket clientSocket = serverSocket.accept();
        FileInputStream fis = new FileInputStream(new File(filePathOnServer));
        OutputStream out = clientSocket.getOutputStream()) {

      System.out.println("Cliente conectado para download de " + filePathOnServer);
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      System.out.println("Download de " + filePathOnServer + " servido com sucesso.");

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
