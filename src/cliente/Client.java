package cliente;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Client {

  // Armazena informações sobre ofertas de arquivos recebidas
  private static Map<String, String[]> pendingFileOffers = new HashMap<>();
  // Armazena o caminho do arquivo que este cliente ofereceu para envio
  private static String fileToSendPath = null;

  // Timeout para conexões de transferência de arquivo (30 segundos)
  private static final int FILE_TRANSFER_TIMEOUT = 30000;

  public static void main(String[] args) {
    Properties props = new Properties();
    String serverAddress = "localhost";
    int serverPort = 12345;

    try (FileInputStream in = new FileInputStream("config.properties")) {
      props.load(in);
      serverAddress = props.getProperty("server.address", "localhost");
      serverPort = Integer.parseInt(props.getProperty("server.port", "12345"));
    } catch (IOException e) {
      System.out.println("Arquivo 'config.properties' não encontrado. Usando valores padrão.");
    }
    // O try-with-resources garante que todos os recursos (Socket, Reader, Writer)
    // serão fechados automaticamente ao final do bloco
    try (Socket socket = new Socket(serverAddress, serverPort);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))

    ) {
      System.out.println("Conectado ao servidor de chat. ");

      Thread messageListerner = new Thread(() -> {
        try {
          String serverMessage;
          while ((serverMessage = serverReader.readLine()) != null) {
            if (serverMessage.startsWith("INCOMING_FILE")) {
              // Servidor avisando de um pedido de arquivo:
              // INCOMING_FILE @remetente nome_arquivo tamanho
              String[] parts = serverMessage.split(" ", 4);
              String sender = parts[1].substring(1);
              String fileName = parts[2];
              String fileSize = parts[3];
              pendingFileOffers.put(sender, new String[] { fileName, fileSize });

              System.out.printf("\n>>> %s quer te enviar o arquivo '%s' (%s bytes).%n", sender, fileName, fileSize);
              System.out.printf(">>> Para aceitar, digite: /accept %s%n", sender);
            } else if (serverMessage.startsWith("BROADCAST_FILE")) {
              // Servidor notifica sobre um arquivo compartilhado: BROADCAST_FILE @remetente
              // nome_arquivo
              String[] parts = serverMessage.split(" ", 4);
              String sender = parts[1].substring(1); // remove o @
              String fileName = parts[2];
              String fileSize = parts[3];
              System.out.printf("\n>>> %s compartilhou o arquivo '%s' (%s bytes).%n", sender, fileName, fileSize);
              System.out.printf(">>> Para baixar, digite: /download %s%n", fileName);
            } else if (serverMessage.startsWith("UPLOAD_READY")) {
              // Servidor está pronto para receber nosso upload: UPLOAD_READY ip porta
              // nome_arquivo
              String[] parts = serverMessage.split(" ", 4);
              String ip = parts[1];
              int port = Integer.parseInt(parts[2]);
              new Thread(() -> uploadFile(fileToSendPath, ip, port)).start();
            } else if (serverMessage.startsWith("DOWNLOAD_READY")) {
              // Servidor está pronto para nos enviar um arquivo: DOWNLOAD_READY ip porta
              // nome_arquivo
              String[] parts = serverMessage.split(" ", 5);
              String ip = parts[1];
              int port = Integer.parseInt(parts[2]);
              String fileName = parts[3];
              long fileSize = Long.parseLong(parts[4]);
              new Thread(() -> receiveFile(ip, port, fileName, fileSize));
            } else if (serverMessage.startsWith("TRANSFER_READY")) {
              String[] parts = serverMessage.split(" ", 4);
              String ip = parts[1];
              int port = Integer.parseInt(parts[2]);
              String peer = parts[3].substring(1);

              String fileNameToReceive = null;
              long fileSizeToReceive = 0;

              if (pendingFileOffers.containsKey(peer)) {
                String[] offer = pendingFileOffers.get(peer);
                fileNameToReceive = offer[0];
                fileSizeToReceive = Long.parseLong(offer[1]);
                pendingFileOffers.remove(peer);
              }

              String finalFileName = fileNameToReceive;
              long finalFileSize = fileSizeToReceive;
              new Thread(() -> handlerFileTransfer(ip, port, finalFileName, finalFileSize)).start();
            } else {
              System.out.println(serverMessage);
            }
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
        if (userInput.startsWith("/sendfile")) {
          // Usuário quer enviar um arquivo: /sendfile destinatario
          // /caminho/para/arquivo.txt
          String[] parts = userInput.split(" ", 3);
          if (parts.length == 3) {
            String recipient = parts[1];
            String filePath = parts[2];
            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
              fileToSendPath = filePath; // Armazena o caminho para quando o servidor confirmar
              String command = String.format("SENDFILE_REQUEST %s %s %d", recipient, file.getName(), file.length());
              writer.println(command);
            } else {
              System.out.println("Erro: Arquivo não encontrado ou não é um arquivo válido.");
            }
          } else {
            System.out.println("Formato inválido. Use: /sendfile <destinatario> <caminho_do_arquivo>");
          }

        } else if (userInput.startsWith("/accept")) {
          // Usuário aceitou um arquivo: /aceitar remetente
          String[] parts = userInput.split(" ", 2);
          if (parts.length == 2) {
            String sender = parts[1];
            if (pendingFileOffers.containsKey(sender)) {
              writer.println("SENDFILE_ACCEPT " + sender);
              // pendingFileOffers.remove(sender); // Limpa a oferta pendente
            } else {
              System.out.println("Nenhuma oferta de arquivo pendente de " + sender);
            }
          } else {
            System.out.println("Formato inválido. Use: /accept <remetente>");
          }
        } else if (userInput.startsWith("/download")) {
          writer.println(userInput);
        } else {
          // Mensagem normal de chat
          writer.println(userInput);
          if ("sair".equalsIgnoreCase(userInput.trim())) {
            break;
          }
        }
      }

    } catch (UnknownHostException e) {
      System.out.println("Endereço do servidor não encontrado: " + serverAddress);
    } catch (IOException e) {
      System.out.println("Não foi possível conectar ao servidor. Verifique se ele está ativo.");
    }
  }

  /**
   * Lida com a conexão ao socket de transferência e decide se envia ou recebe.
   */
  private static void handlerFileTransfer(String ip, int port, String fileName, long fileSize) {
    if (fileToSendPath != null) {
      // Se temos um arquivo para enviar, somos o remetente
      sendFile(fileToSendPath, ip, port);
      fileToSendPath = null;
    } else {
      // Caso contrário, somos o destinatário. Precisamos encontrar a oferta.
      receiveFile(ip, port, fileName, fileSize);
    }
  }

  /**
   * Conecta-se ao socket de transferência e envia o arquivo especificado.
   */
  private static void sendFile(String filePath, String ip, int port) {
    File file = new File(filePath);
    System.out.printf("Iniciando envio de '%s' para %s:%d...%n", file.getName(), ip, port);

    try (Socket fileSocket = new Socket(ip, port)) {
      fileSocket.setSoTimeout(FILE_TRANSFER_TIMEOUT);

      try (FileInputStream fileIn = new FileInputStream(file);
          OutputStream socketOut = fileSocket.getOutputStream()) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        long totalBytes = 0;
        long totalSize = file.length();
        int lastPercent = -1;

        while ((bytesRead = fileIn.read(buffer)) != -1) {
          socketOut.write(buffer, 0, bytesRead);
          totalBytes += bytesRead;

          int currentPercent = (int) ((double) totalBytes / totalSize * 100);
          if (currentPercent > lastPercent) {
            printProgress(totalBytes, totalSize);
            lastPercent = currentPercent;
          }
        }

        if (lastPercent < 100) {
          printProgress(totalSize, totalSize);
        }

        System.out.printf("Envio de arquivo '%s' concluído. Total: %.2f MB%n",
            file.getName(), totalBytes / (1024.0 * 1024.0));
      }
    } catch (SocketTimeoutException e) {
      System.out.println("Timeout ao enviar arquivo - conexão muito lenta ou perdida.");
    } catch (Exception e) {
      System.out.println("Erro ao enviar arquivo: " + e.getMessage());
    }
  }

  /**
   * Conecta-se ao socket de transferência e recebe um arquivo.
   */
  private static void receiveFile(String ip, int port, String fileName, long totalSize) {
    System.out.printf("Iniciando recebimento de arquivo de %s:%d", ip, port);

    File downloadsDir = new File("Downloads");
    if (!downloadsDir.exists()) {
      if (!downloadsDir.mkdir()) {
        System.err.println("Erro: Não foi possível criar diretório Downloads");
        return;
      }
    }

    if (fileName == null) {
      System.err.println("Erro: Nome do arquivo para receber é desconhecido.");
      return;
    }

    // Evita sobrescrever arquivos
    File targetFile = new File(downloadsDir, fileName);
    int counter = 1;
    while (targetFile.exists()) {
      String nameWithoutExt = fileName;
      String extension = "";
      int lastDot = fileName.lastIndexOf('.');
      if (lastDot > 0) {
        nameWithoutExt = fileName.substring(0, lastDot);
        extension = fileName.substring(lastDot);
      }
      targetFile = new File(downloadsDir, nameWithoutExt + "_" + counter + extension);
      counter++;
    }

    try (Socket fileSocket = new Socket(ip, port)) {
      fileSocket.setSoTimeout(FILE_TRANSFER_TIMEOUT);

      try (InputStream socketIn = fileSocket.getInputStream();
          FileOutputStream fileOut = new FileOutputStream(targetFile)) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        long totalBytes = 0;

        while ((bytesRead = socketIn.read(buffer)) != -1) {
          fileOut.write(buffer, 0, bytesRead);
          totalBytes += bytesRead;
          printProgress(totalBytes, totalSize);
        }

        System.out.printf("Arquivo '%s' recebido com sucesso. Total: %.2f MB%n",
            targetFile.getName(), totalBytes / (1024.0 * 1024.0));
      }
    } catch (SocketTimeoutException e) {
      System.out.println("Timeout ao receber arquivo - conexão muito lenta ou perdida.");
    } catch (IOException e) {
      System.err.println("Erro ao receber arquivo: " + e.getMessage());
    }
  }

  /*
   * Barra de progresso
   */
  private static void printProgress(long bytesTransferred, long totalBytes) {
    int barLength = 50; // número de caracteres da barra
    double percent = (double) bytesTransferred / totalBytes;
    int filled = (int) (barLength * percent);

    StringBuilder bar = new StringBuilder();
    bar.append("\r["); // \r volta o cursor pro início da linha
    for (int i = 0; i < filled; i++) {
      bar.append("=");
    }
    for (int i = filled; i < barLength; i++) {
      bar.append(" ");
    }
    bar.append("] ");
    bar.append(String.format("%.2f%%", percent * 100));

    System.out.print(bar.toString());

    if (bytesTransferred >= totalBytes) {
      System.out.println(); // quebra linha no fim
    }
  }

  /*
   * Barra de progresso
   */
  private static void uploadFile(String filePath, String ip, int port) {
    File file = new File(filePath);
    System.out.printf("Iniciando upload de '%s' para o servidor em %s:%d...%n", file.getName(), ip, port);
    sendFile(filePath, ip, port);
    fileToSendPath = null;
  }

}
