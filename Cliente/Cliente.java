// ChatClient.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Cliente extends JFrame {

    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;

    public Cliente() {
        // 1. Obter informações do usuário antes de construir a janela
        this.username = JOptionPane.showInputDialog(this, "Digite seu nome de usuário:", "Nome de Usuário", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) System.exit(0);

        String serverIp = (String) JOptionPane.showInputDialog(this, "Digite o IP do servidor:", "IP do Servidor", JOptionPane.QUESTION_MESSAGE, null, null, "localhost");
        if (serverIp == null || serverIp.trim().isEmpty()) System.exit(0);

        // 2. Construir a Interface Gráfica
        buildGUI();

        // 3. Conectar ao servidor
        try {
            socket = new Socket(serverIp, 55555);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(username); // Envia o nome de usuário para o servidor

            // Inicia uma thread para ficar escutando por mensagens do servidor
            new Thread(new MessageReceiver()).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void buildGUI() {
        setTitle("ChatSocket - Conectado como: " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Área de Chat (Centro) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        add(chatScrollPane, BorderLayout.CENTER);

        // --- Lista de Usuários (Direita) ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.BOLD, 14));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Duplo clique
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        messageField.setText("@" + selectedUser + " ");
                        messageField.requestFocus();
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(180, 0));
        add(userScrollPane, BorderLayout.EAST);
        
        // --- Painel de Envio (Abaixo) ---
        JPanel southPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.addActionListener(e -> sendMessage()); // Envia com a tecla Enter
        
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        
        southPanel.add(messageField, BorderLayout.CENTER);
        southPanel.add(sendButton, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);

        setVisible(true);
    }
    
    private void sendMessage() {
        String message = messageField.getText();
        if (message != null && !message.trim().isEmpty()) {
            writer.println(message);
            messageField.setText("");
        }
    }

    // Classe interna para receber mensagens em uma thread separada
    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    final String finalMessage = message;
                    // Atualizações da GUI devem ser feitas na Event Dispatch Thread (EDT)
                    SwingUtilities.invokeLater(() -> {
                        if (finalMessage.startsWith("LIST:")) {
                            String[] users = finalMessage.substring(5).split(",");
                            userListModel.clear();
                            for (String user : users) {
                                userListModel.addElement(user);
                            }
                        } else {
                            chatArea.append(finalMessage + "\n");
                            // Auto-scroll para o final
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        }
                    });
                }
            } catch (IOException e) {
                // Servidor caiu ou a conexão foi perdida
                SwingUtilities.invokeLater(() -> chatArea.append("INFO: Conexão com o servidor foi perdida.\n"));
            }
        }
    }
    
    public static void main(String[] args) {
        // Garante que a GUI seja criada na thread correta
        SwingUtilities.invokeLater(() -> new Cliente());
    }
}