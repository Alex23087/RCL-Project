package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

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
						if (user != null || ServerData.loggedUsers.stream().anyMatch(us -> us.username.equals(cmd.parameters[0]))) {
							System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + cmd.parameters[0] + ")]User already logged in");
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
								ServerData.loggedUsers.add(u);
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
					case GetBTCConversion:
						URL randomURL = new URL("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");
						URLConnection connection = randomURL.openConnection();
						BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						int factor = Integer.parseInt(in.readLine());
						in.close();
						ous.writeObject(user.balance / (double) factor);
						break;
					case PublishPost:
						//TODO: error checking
						int id = ServerData.addPost(user, cmd.parameters[0], cmd.parameters[1]);
						ous.writeObject(id);
						System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Created new post with id " + id);
						break;
					case Rewin:
						id = ServerData.addRewin(user, Integer.parseInt(cmd.parameters[0]));
						//TODO: Finish case
						break;
					case ListUsers:
						ous.writeObject(ServerData.getUsersWithTags(user.tags));
						break;
					case Follow:
						int followedId = ServerData.getUserId(cmd.parameters[1]);
						if(followedId != -1) {
							if(ServerData.follow(user.id, followedId)) {
								ous.writeObject(ErrorMessage.Success);
							}else{
								ous.writeObject(ErrorMessage.AlreadyFollowed);
							}
						}else{
							ous.writeObject(ErrorMessage.InvalidUsername);
						}
						break;
					default:
						break;
				}
			} catch (SocketException se){
				System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Disconnected");
				ServerData.loggedUsers.remove(this.user);
				break;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}