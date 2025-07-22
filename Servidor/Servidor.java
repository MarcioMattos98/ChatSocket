import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final int PORTA = 12345;
    private static Map<String, ClienteHandler> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Servidor iniciado na porta " + PORTA);
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClienteHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClienteHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String nome;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        public void enviarMensagem(String msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                nome = (String) in.readObject();
                clientes.put(nome, this);
                broadcast("[" + nome + "] entrou no chat.");

                atualizarUsuarios();

                Object obj;
                while ((obj = in.readObject()) != null) {
                    if (obj instanceof String) {
                        String msg = (String) obj;
                        if (msg.startsWith("@")) {
                            String[] partes = msg.split(" ", 2);
                            String destino = partes[0].substring(1);
                            String conteudo = partes.length > 1 ? partes[1] : "";
                            ClienteHandler destinatario = clientes.get(destino);
                            if (destinatario != null) {
                                destinatario.enviarMensagem("[Privado de " + nome + "]: " + conteudo);
                            }
                        } else {
                            broadcast("[" + nome + "]: " + msg);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(nome + " saiu.");
            } finally {
                try {
                    clientes.remove(nome);
                    atualizarUsuarios();
                    broadcast("[" + nome + "] saiu do chat.");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(String mensagem) {
            for (ClienteHandler ch : clientes.values()) {
                ch.enviarMensagem(mensagem);
            }
        }

        private void atualizarUsuarios() {
            String usuarios = "USUARIOS:" + String.join(",", clientes.keySet());
            for (ClienteHandler ch : clientes.values()) {
                ch.enviarMensagem(usuarios);
            }
        }
    }
}