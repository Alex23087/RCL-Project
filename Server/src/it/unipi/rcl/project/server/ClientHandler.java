package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable{
	Socket client;
	ObjectInputStream ois;
	ObjectOutputStream ous;
	User user;

	public ClientHandler(Socket clientSocket){
		this.client = clientSocket;
		try {
			ous = new ObjectOutputStream(client.getOutputStream());
			ois = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Connected");
	}

	@Override
	public void run() {
		while (true) {
			try {
				Command cmd = (Command) ois.readObject();
				System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Received " + cmd);
				if (user == null && (cmd.op != Command.Operation.Login)) {
					System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]User not logged in");
					ous.writeObject(ErrorMessage.UserNotLoggedIn);
					ous.flush();
					continue;
				}
				switch (cmd.op) {
					case Login:
						if (user != null) {
							System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]User already logged in");
							ous.writeObject(ErrorMessage.UserAlreadyLoggedIn);
							ous.flush();
						} else {
							User u = ServerData.users.get(cmd.parameters[0]);
							if (u == null) {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Invalid username \"" + cmd.parameters[0] + "\"");
								ous.writeObject(ErrorMessage.InvalidUsername);
								ous.flush();
							} else if (u.password.equals(cmd.parameters[1])) {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Logged in as " + u.username);
								this.user = u;
								ous.writeObject(ErrorMessage.Success);
								ous.flush();
							} else {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Invalid password");
								ous.writeObject(ErrorMessage.InvalidPassword);
								ous.flush();
							}
						}
						break;
					default:
						break;
				}
			} catch (SocketException se){
				System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Disconnected");
				break;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}