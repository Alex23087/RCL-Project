package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
		System.out.println("Incoming connection from " + client.getInetAddress() + ":" + client.getPort());
	}

	@Override
	public void run() {
		while (true) {
			try {
				Command cmd = (Command) ois.readObject();
				System.out.println("Received " + cmd + " from client " + client.getInetAddress() + ":" + client.getPort());
				if (user == null && (cmd.op != Command.Operation.Login)) {
					ous.writeObject(ErrorMessage.UserNotLoggedIn);
					ous.flush();
					continue;
				}
				switch (cmd.op) {
					case Login:
						if (user != null) {
							ous.writeObject(ErrorMessage.UserAlreadyLoggedIn);
							ous.flush();
						} else {
							User u = ServerData.users.get(cmd.parameters[0]);
							if (u == null) {
								ous.writeObject(ErrorMessage.InvalidUsername);
								ous.flush();
							} else if (u.password.equals(cmd.parameters[1])) {
								this.user = u;
								ous.writeObject(ErrorMessage.Success);
								ous.flush();
							} else {
								ous.writeObject(ErrorMessage.InvalidPassword);
								ous.flush();
							}
						}
						break;
					default:
						break;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}