# Chat Distribuído em Java ☕

Este é um projeto de um sistema de chat distribuído, desenvolvido em Java puro, utilizando Sockets para a comunicação. A arquitetura segue o modelo Cliente-Servidor e permite a troca de mensagens e arquivos em tempo real entre múltiplos usuários.

## Funcionalidades ✨

* **Chat em Grupo (Broadcast):** Mensagens enviadas por um usuário são recebidas por todos os outros usuários conectados.
* **Mensagens Privadas:** Um usuário pode enviar uma mensagem que será lida apenas pelo destinatário específico.
* **Envio de Arquivos Privado:** Permite que um usuário envie um arquivo diretamente para outro de forma segura, com um protocolo de negociação (handshake) que exige a aceitação do destinatário.
* **Compartilhamento de Arquivos em Grupo:** Um usuário pode fazer o upload de um arquivo para o servidor, que então notifica a todos os outros usuários. Os demais podem então baixar o arquivo individualmente.
* **Notificações de Conexão/Desconexão:** O chat anuncia quando um novo usuário entra ou sai.
* **Timestamp:** Todas as mensagens exibem o horário em que foram enviadas, facilitando a leitura.
* **Servidor Concorrente:** O servidor utiliza um pool de threads para gerenciar múltiplas conexões de clientes simultaneamente.

## Pré-requisitos

Para compilar e executar este projeto, você precisará ter instalado:

* **JDK (Java Development Kit)** - Versão 11 ou superior.

## Configurando o Cliente (`config.properties`)

Antes de executar o cliente, é necessário informar a ele como encontrar o servidor. Isso é feito através de um arquivo de configuração.

1.  Crie um arquivo chamado `config.properties` na raiz do seu projeto.
2.  Adicione o seguinte conteúdo a ele, substituindo o IP pelo endereço da máquina onde o servidor está rodando:

    ```properties
    # Endereço IP da máquina onde o Servidor está rodando
    server.address=192.168.0.15

    # Porta em que o Servidor está escutando (deve ser a mesma do servidor)
    server.port=12345
    ```
* **Para testes locais (servidor e cliente na mesma máquina):** use `server.address=localhost`.
* **Para conectar via rede local:** use o endereço IP da máquina servidora (ex: `192.168.0.15`).

## Como Compilar e Executar 🚀

Siga os passos abaixo para colocar o chat em funcionamento.

### 1. Clone o Repositório

Se você ainda não o fez, clone o projeto para a sua máquina local:
```bash
git clone <URL_DO_SEU_REPOSITORIO>
cd ChatDistribuido
```

### 2. Compile o Código-Fonte

Use o comando `javac` para compilar todos os arquivos `.java`. O comando `-d out` criará um diretório chamado `out` para armazenar os arquivos `.class` compilados.

```bash
javac -d out src/**/*.java
```
*Se o seu shell não suportar `**`, você pode usar `find src -name "*.java" -print | xargs javac -d out`.*

### 3. Inicie o Servidor

Em um terminal, execute a classe `Server`. O servidor ficará ativo, aguardando conexões dos clientes. **Este terminal deve permanecer aberto.**

```bash
java -cp out servidor.Server
```
Você deverá ver a mensagem: `Servidor iniciando na porta 12345. Aguardando clientes...`

### 4. Inicie os Clientes

Após configurar o arquivo `config.properties`, para cada cliente que você quiser conectar, **abra um novo terminal** e execute o seguinte comando simples:

```bash
java -cp out client.Client
```
O cliente lerá automaticamente as configurações do arquivo `config.properties` para saber a qual servidor se conectar. Repita este passo para quantos clientes desejar.

## Como Usar o Chat

Após se conectar, você verá uma mensagem de boas-vindas.

### Comandos de Mensagem

* **Enviar mensagem para todos:** Simplesmente digite sua mensagem e pressione `Enter`.
    ```
    Olá, pessoal!
    ```
* **Enviar mensagem privada:** Use o formato `@nome_do_usuario <sua mensagem>`.
    ```
    @João Tudo certo para mais tarde?
    ```
* **Sair do chat:** Digite `sair` e pressione `Enter`.

### Compartilhando Arquivos 📂

O compartilhamento de arquivos pode ser feito de duas maneiras: enviando para um único usuário ou para todos.

#### 1. Envio Privado (Usuário para Usuário)

Este envio ocorre em duas etapas: a oferta e a aceitação.

* **Para oferecer um arquivo:** Use o comando `/sendfile <destinatario> <caminho_completo_do_arquivo>`.
    * **Exemplo:** `/sendfile ana C:\Users\Erik\Documentos\trabalho.pdf`

* **Para aceitar um arquivo:** O destinatário receberá uma notificação. Para confirmar, ele deve usar o comando `/accept <remetente>`.
    * **Exemplo:** `/accept erik`

A transferência iniciará automaticamente entre os dois usuários, com o servidor atuando como intermediário.

#### 2. Envio para Todos (Broadcast)

Este envio ocorre em três etapas: upload, notificação e download.

* **Passo A: O remetente envia o arquivo para o servidor.**
    Use o destinatário especial `@all` no comando `/sendfile`.
    * **Exemplo:** `/sendfile @all C:\Users\Erik\Documentos\anuncio.pdf`
    O arquivo será enviado para o servidor, que o armazenará temporariamente.

* **Passo B: O servidor notifica a todos.**
    Os outros usuários verão uma mensagem no chat:
    ```
    >>> Erik compartilhou o arquivo 'anuncio.pdf'.
    >>> Para baixar, digite: /download anuncio.pdf
    ```

* **Passo C: Outros usuários baixam o arquivo.**
    Qualquer usuário que desejar o arquivo pode usar o comando `/download <nome_do_arquivo>`.
    * **Exemplo:** `/download anuncio.pdf`

A transferência iniciará do servidor para o cliente. O arquivo será salvo em uma pasta `downloads`.

## Estrutura do Projeto

```
.
├── out/                      # Diretório para os arquivos .class compilados
├── src/
│   ├── cliente/
│   │   └── Client.java       # Lógica do lado do cliente
│   └── servidor/
│       ├── Server.java            # Lógica principal do servidor
│       ├── ClientHandler.java     # Gerencia o chat de texto de um cliente
│       ├── FileTransferHandler.java # Gerencia transferência privada (ponto-a-ponto)
│       ├── FileUploadHandler.java   # Gerencia o upload de um arquivo para o servidor
│       └── FileDownloadHandler.java # Gerencia o download de um arquivo do servidor
├── config.properties         # Arquivo de configuração do cliente
└── README.md
```
