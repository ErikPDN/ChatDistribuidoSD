# Chat Distribuído em Java ☕

Este é um projeto de um sistema de chat distribuído, desenvolvido em Java puro, utilizando Sockets para a comunicação. A arquitetura segue o modelo Cliente-Servidor e permite a troca de mensagens e arquivos em tempo real entre múltiplos usuários.

## Funcionalidades ✨

* **Chat em Grupo (Broadcast):** Mensagens enviadas por um usuário são recebidas por todos os outros usuários conectados.
* **Mensagens Privadas:** Um usuário pode enviar uma mensagem que será lida apenas pelo destinatário específico.
* **Envio de Arquivos entre Usuários:** Permite que um usuário envie um arquivo (de qualquer tipo) para outro de forma segura, com um protocolo de negociação (handshake) que exige a aceitação do destinatário.
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

### Enviando um Arquivo 📂

O envio de arquivos ocorre em duas etapas: a oferta e a aceitação.

1.  **Para oferecer um arquivo:**
    Use o comando `/sendfile <destinatario> <caminho_completo_do_arquivo>`.

    * **Exemplo no Windows:**
        ```
        /sendfile ana C:\Users\Erik\Documentos\trabalho.pdf
        ```
    * **Exemplo no Linux/macOS:**
        ```
        /sendfile ana /home/erik/documentos/foto.jpg
        ```
2.  **Para aceitar um arquivo:**
    O destinatário receberá uma notificação. Para confirmar o recebimento, ele deve usar o comando `/accept <remetente>`.

    * **Exemplo:**
        ```
        /accept erik
        ```
A transferência iniciará automaticamente. O arquivo será salvo em uma pasta `downloads` criada no diretório onde o cliente foi executado.

## Estrutura do Projeto

```
.
├── out/                      # Diretório para os arquivos .class compilados
├── src/
│   ├── cliente/
│   │   └── Client.java       # Lógica do lado do cliente
│   └── servidor/
│       ├── Server.java            # Lógica principal do servidor
│       ├── ClientHandler.java     # Lógica para gerenciar o chat de texto de um cliente
│       └── FileTransferHandler.java # Lógica para gerenciar a transferência de um arquivo
├── config.properties         # Arquivo de configuração do cliente
└── README.md
```
