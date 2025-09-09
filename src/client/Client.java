package client;

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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Client {

  // Armazena informações sobre ofertas de arquivos recebidas
  private static Map<String, String[]> pendingFileOffers = new HashMap<>();
  // Armazena o caminho do arquivo que este cliente ofereceu para envio
  private static String fileToSendPath = null;

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
            } else if (serverMessage.startsWith("TRANSFER_READY")) {
              String[] parts = serverMessage.split(" ", 4);
              String ip = parts[1];
              int port = Integer.parseInt(parts[2]);
              String peer = parts[3].substring(1);

              String fileNameToReceive = null;
              if (pendingFileOffers.containsKey(peer)) {
                fileNameToReceive = pendingFileOffers.get(peer)[0];
                pendingFileOffers.remove(peer);
              }

              String finalFileName = fileNameToReceive;
              new Thread(() -> handlerFileTransfer(ip, port, finalFileName)).start();
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
        }

        else {
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
  private static void handlerFileTransfer(String ip, int port, String fileName) {
    if (fileToSendPath != null) {
      // Se temos um arquivo para enviar, somos o remetente
      sendFile(fileToSendPath, ip, port);
      fileToSendPath = null;
    } else {
      // Caso contrário, somos o destinatário. Precisamos encontrar a oferta.
      receiveFile(ip, port, fileName);
    }
  }

  /**
   * Conecta-se ao socket de transferência e envia o arquivo especificado.
   */
  private static void sendFile(String filePath, String ip, int port) {
    File file = new File(filePath);
    System.out.printf("Iniciando envio de '%s' para %s:%d...%n", file.getName(), ip, port);

    try (Socket fileSocket = new Socket(ip, port);
        FileInputStream fileIn = new FileInputStream(file);
        OutputStream socketOut = fileSocket.getOutputStream()) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fileIn.read(buffer)) != -1) {
        socketOut.write(buffer, 0, bytesRead);
      }

      System.out.println("Envio de arquivo concluído.");
    } catch (Exception e) {
      System.out.println("Erro ao enviar arquivo: " + e.getMessage());
    }
  }

  /**
   * Conecta-se ao socket de transferência e recebe um arquivo.
   */
  private static void receiveFile(String ip, int port, String fileName) {
    System.out.printf("Iniciando recebimento de arquivo para %s:%d...%n", ip, port);

    File downloadsDir = new File("Downloads");
    if (!downloadsDir.exists()) {
      downloadsDir.mkdir();
    }

    if (fileName == null) {
      System.err.println("Erro: Nome do arquivo para receber é desconhecido.");
      return;
    }

    try (Socket fileSocket = new Socket(ip, port);
        InputStream socketIn = fileSocket.getInputStream();
        FileOutputStream fileOut = new FileOutputStream(new File(downloadsDir, fileName))) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = socketIn.read(buffer)) != -1) {
        fileOut.write(buffer, 0, bytesRead);
      }
      System.out.println("Arquivo '" + fileName + "' recebido com sucesso.");
    } catch (IOException e) {
      System.err.println("Erro ao receber arquivo: " + e.getMessage());
    }
  }
}
