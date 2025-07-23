
// ChatServer.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    // Usamos um ConcurrentHashMap para garantir a segurança em ambiente com
    // múltiplas threads.
    // Mapeia o nome do usuário para o seu respectivo PrintWriter (canal de
    // escrita).
    private static Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static final int PORT = 55555;

    public static void main(String[] args) {
        System.out.println("[INICIADO] Servidor de Chat está rodando na porta " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Aceita uma nova conexão e cria uma thread para cuidar do cliente.
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    // A classe interna ClientHandler cuida da comunicação com um único cliente.
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // A primeira linha que o cliente envia é seu nome de usuário.
                username = reader.readLine();
                if (username == null || clients.containsKey(username)) {
                    writer.println("INFO:Nome de usuário já existe ou é inválido. Desconectando.");
                    return; // Encerra a thread se o nome for inválido.
                }

                clients.put(username, writer);
                System.out.println("[CONEXÃO] " + username + " se conectou.");
                broadcastMessage("INFO:" + username + " entrou no chat.");
                broadcastUserList();

                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("@")) {
                        handlePrivateMessage(message);
                    } else {
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("[ERRO] Conexão com " + username + " perdida.");
            } finally {
                // Bloco de limpeza: remove o cliente e avisa a todos.
                if (username != null && clients.containsKey(username)) {
                    clients.remove(username);
                    System.out.println("[DESCONEXÃO] " + username + " saiu.");
                    broadcastMessage("INFO:" + username + " saiu do chat.");
                    broadcastUserList();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignora
                }
            }
        }

        private void handlePrivateMessage(String message) {
            // O método split(" ", 2) é mais robusto.
            // Ele divide a string no primeiro espaço em no máximo 2 partes.
            // Ex: "@Fulano olá tudo bem?" se torna ["@Fulano", "olá tudo bem?"]
            String[] parts = message.split(" ", 2);

            // Garante que a mensagem tenha o formato "@usuario mensagem"
            if (parts.length < 2) {
                writer.println("INFO: Formato de mensagem privada inválido. Use @usuario mensagem.");
                return;
            }

            String targetUser = parts[0].substring(1); // Remove o "@" para obter o nome
            String privateMsg = parts[1];

            PrintWriter targetWriter = clients.get(targetUser);

            if (targetWriter != null) {
                // O usuário de destino foi encontrado
                String formattedMsg = "(Privado de " + username + "): " + privateMsg;

                // Envia a mensagem formatada para o destinatário
                targetWriter.println(formattedMsg);

                // Envia uma cópia para o remetente, para que ele veja sua própria mensagem
                this.writer.println(formattedMsg);
            } else {
                // O usuário de destino não foi encontrado no mapa de clientes
                writer.println("INFO: Usuário '" + targetUser + "' não encontrado ou offline.");
            }
        }

        // Envia uma mensagem para todos os clientes conectados.
        private void broadcastMessage(String message) {
            for (PrintWriter writer : clients.values()) {
                writer.println(message);
            }
        }

        // Envia a lista de usuários atualizada para todos.
        private void broadcastUserList() {
            String userList = "LIST:" + String.join(",", clients.keySet());
            broadcastMessage(userList);
        }
    }
}