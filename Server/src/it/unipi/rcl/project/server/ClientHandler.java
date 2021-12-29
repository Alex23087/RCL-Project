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
							System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]User already logged in");
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
					case GetFeed:
						ous.writeObject(ServerData.getFeed(user));
						break;
					case GetPosts:
						if(cmd.parameters == null) {
							ous.writeObject(ServerData.getPosts(user));
						}else{
							User u = ServerData.getUser(Integer.parseInt(cmd.parameters[0]));
							if(u == null){
								ous.writeObject(ErrorMessage.NoSuchUser);
							}else{
								ous.writeObject(ServerData.getPosts(u));
							}
						}
						break;
					case GetBalance:
						ous.writeObject(user.balance);
						break;
					case PublishPost:
						//TODO: error checking
						int id = ServerData.addPost(user, cmd.parameters[0], cmd.parameters[1]);
						ous.writeObject(id);
						System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Created new post with id " + id);
						break;
					case Rewin:
						id = ServerData.addRewin(user, Integer.parseInt(cmd.parameters[0]));
						break;
					default:
						break;
				}
			} catch (SocketException se){
				System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Disconnected");
				break;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}