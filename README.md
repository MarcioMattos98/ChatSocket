# ChatSocket
# Sistema de Troca de Mensagens com Sockets - Java

Este projeto implementa um sistema de chat em Java utilizando **sockets** e **interface gráfica (Swing)**. O sistema permite a comunicação entre até 3 clientes através de um servidor central.

## Funcionalidades

- Cadastro de usuário com nome;
- Envio de mensagens para todos (chat público);
- Envio de mensagens privadas usando `@NomeDoDestinatário`;
- Lista de usuários online em tempo real;
- Interface gráfica semelhante ao modelo proposto;
- Suporte a execução distribuída em até 3 computadores (cliente e servidor separados).

## Como Executar

### 1. Requisitos
- Java JDK 8 ou superior instalado

### 2. Compilação

#### Servidor
```bash
cd servidor
javac Servidor.java
java Servidor

### CLiente (nas máquinas utilizadas)
cd cliente
javac Cliente.java
java Cliente
