import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Cliente {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DefaultListModel<String> modeloUsuarios;
    private JTextArea areaMensagens;
    private JTextField campoMensagem;
    private String nome;

    public static void main(String[] args) {
        new Cliente().iniciar();
    }

    public void iniciar() {
        String ip = JOptionPane.showInputDialog("Digite o IP do servidor:");
        nome = JOptionPane.showInputDialog("Digite seu nome:");
        JFrame frame = new JFrame("Chat - " + nome);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        JScrollPane scroll = new JScrollPane(areaMensagens);

        campoMensagem = new JTextField();
        JButton botaoEnviar = new JButton("Enviar");

        modeloUsuarios = new DefaultListModel<>();
        JList<String> listaUsuarios = new JList<>(modeloUsuarios);

        botaoEnviar.addActionListener(e -> enviarMensagem());

        JPanel painelMensagem = new JPanel(new BorderLayout());
        painelMensagem.add(campoMensagem, BorderLayout.CENTER);
        painelMensagem.add(botaoEnviar, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(painelMensagem, BorderLayout.SOUTH);
        frame.add(new JScrollPane(listaUsuarios), BorderLayout.EAST);

        frame.setVisible(true);

        try {
            Socket socket = new Socket(ip, 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(nome);
            out.flush();

            new Thread(() -> {
                try {
                    Object obj;
                    while ((obj = in.readObject()) != null) {
                        if (obj instanceof String) {
                            String msg = (String) obj;
                            if (msg.startsWith("USUARIOS:")) {
                                modeloUsuarios.clear();
                                String[] nomes = msg.substring(9).split(",");
                                for (String n : nomes) {
                                    modeloUsuarios.addElement(n);
                                }
                            } else {
                                areaMensagens.append(msg + "\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarMensagem() {
        String msg = campoMensagem.getText();
        if (!msg.isEmpty()) {
            try {
                out.writeObject(msg);
                out.flush();
                campoMensagem.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}