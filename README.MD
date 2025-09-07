# Chat Distribuído em Java ☕

Este é um projeto de um sistema de chat distribuído, desenvolvido em Java puro, utilizando Sockets para a comunicação. A arquitetura segue o modelo Cliente-Servidor e permite a troca de mensagens em tempo real entre múltiplos usuários.

## Funcionalidades ✨

* **Chat em Grupo (Broadcast):** Mensagens enviadas por um usuário são recebidas por todos os outros usuários conectados.
* **Mensagens Privadas:** Um usuário pode enviar uma mensagem que será lida apenas pelo destinatário específico.
* **Notificações de Conexão/Desconexão:** O chat anuncia quando um novo usuário entra ou sai.
* **Timestamp:** Todas as mensagens exibem o horário em que foram enviadas, facilitando a leitura.
* **Servidor Concorrente:** O servidor utiliza um pool de threads para gerenciar múltiplas conexões de clientes simultaneamente.


## Pré-requisitos

Para compilar e executar este projeto, você precisará ter instalado:

* **JDK (Java Development Kit)** - Versão 11 ou superior.

## Como Compilar e Executar 🚀

Siga os passos abaixo para colocar o chat em funcionamento. Todos os comandos devem ser executados a partir da raiz do projeto.

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

Para cada cliente que você quiser conectar, **abra um novo terminal** e execute o comando abaixo. Você pode repetir este passo quantas vezes quiser para simular uma conversa com múltiplos usuários.

```bash
java -cp out client.Client
```
Cada terminal se tornará uma janela de chat para um usuário diferente.

## Como Usar o Chat

Após se conectar, você verá uma mensagem de boas-vindas.

* **Enviar mensagem para todos:** Simplesmente digite sua mensagem e pressione `Enter`.
    ```
    Olá, pessoal!
    ```
* **Enviar mensagem privada:** Use o formato `@nome_do_usuario <sua mensagem>`.
    ```
    @João Tudo certo para mais tarde?
    ```
* **Sair do chat:** Digite `sair` e pressione `Enter`.

## Estrutura do Projeto

```
.
├── out/                  # Diretório para os arquivos .class compilados
└── src/
    ├── cliente/
    │   └── Client.java   # Lógica do lado do cliente
    └── servidor/
        ├── Server.java        # Lógica principal do servidor
        └── ClientHandler.java # Lógica para gerenciar cada cliente
```
