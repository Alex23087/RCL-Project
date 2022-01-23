package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.PostView;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

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
					case Login: {
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
								ous.writeObject(u.id);
								ous.flush();
								ServerData.loggedUsers.add(u);
							} else {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Invalid password");
								ous.writeObject(ErrorMessage.InvalidPassword);
								ous.flush();
							}
						}
						break;
					}
					case GetFeed: {
						ous.writeObject(ServerData.getFeed(user));
						break;
					}
					case GetPosts: {
						if (cmd.parameters == null) {
							ous.writeObject(ServerData.getPosts(user));
						} else {
							User u = ServerData.getUser(Integer.parseInt(cmd.parameters[0]));
							if (u == null) {
								ous.writeObject(ErrorMessage.NoSuchUser);
							} else {
								ous.writeObject(ServerData.getPosts(u));
							}
						}
						break;
					}
					case GetBalance: {
						ous.writeObject(user.balance);
						break;
					}
					case GetBTCConversion: {
						URL randomURL = new URL("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");
						URLConnection connection = randomURL.openConnection();
						BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						int factor = Integer.parseInt(in.readLine());
						in.close();
						ous.writeObject(user.balance / (double) factor);
						break;
					}
					case PublishPost: {
						//TODO: error checking
						int id = ServerData.addPost(user, cmd.parameters[0], cmd.parameters[1]);
						ous.writeObject(id);
						System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Created new post with id " + id);
						break;
					}
					case Rewin: {
						int id = ServerData.addRewin(user, Integer.parseInt(cmd.parameters[0]));
						//TODO: Finish case
						break;
					}
					case ListUsers: {
						ous.writeObject(ServerData.getUsersWithTags(user.tags));
						break;
					}
					case Follow: {
						try {
							int followedId = Integer.parseInt(cmd.parameters[0]);
							if (ServerData.follow(user.id, followedId)) {
								ous.writeObject(ErrorMessage.Success);
							} else {
								ous.writeObject(ErrorMessage.AlreadyFollowed);
							}
						} catch (NumberFormatException nfe) {
							log("Passed invalid userID to Follow command");
							ous.writeObject(ErrorMessage.InvalidUserId);
						}
						break;
					}
					case GetFollowed: {
						List<Integer> l = ServerData.getFollowed(user.id);
						ous.writeObject(l);
						break;
					}
					case GetUsernameFromId: {
						try {
							int userID = Integer.parseInt(cmd.parameters[0]);
							User u = ServerData.getUser(userID);
							if (u == null) {
								ous.writeObject(ErrorMessage.InvalidUsername);
							} else {
								ous.writeObject(u.username);
							}
						} catch (NumberFormatException nfe) {
							ous.writeObject(ErrorMessage.InvalidCommand);
						}
						break;
					}
					case GetPostViewFromId: {
						try {
							int postID = Integer.parseInt(cmd.parameters[0]);
							PostView p = ServerData.getPostViewWithId(postID, user.id);
							if (p == null) {
								ous.writeObject(ErrorMessage.InvalidPostId);
							} else {
								ous.writeObject(p);
							}
						} catch (NumberFormatException nfe) {
							ous.writeObject(ErrorMessage.InvalidCommand);
						}
						break;
					}
					case Unfollow: {
						try {
							int followedId = Integer.parseInt(cmd.parameters[0]);
							if (ServerData.unfollow(user.id, followedId)) {
								ous.writeObject(ErrorMessage.Success);
							} else {
								ous.writeObject(ErrorMessage.NotFollowing);
							}
						} catch (NumberFormatException nfe) {
							log("Passed invalid userID to Unfollow command");
							ous.writeObject(ErrorMessage.InvalidUserId);
						}
						break;
					}
					case Vote:{
						if(cmd.parameters == null || cmd.parameters.length < 2){
							ous.writeObject(ErrorMessage.InvalidCommand);
						} else {
							try {
								int postId = Integer.parseInt(cmd.parameters[0]);
								boolean upvote = Boolean.parseBoolean(cmd.parameters[1]);
								ous.writeObject(ServerData.vote(postId, user.id, upvote));
							}catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
							}
						}
						break;
					}
					case AddComment: {
						if(cmd.parameters == null || cmd.parameters.length < 2){
							ous.writeObject(ErrorMessage.InvalidCommand);
						} else {
							try {
								int postId = Integer.parseInt(cmd.parameters[0]);
								String text = cmd.parameters[1];
								ous.writeObject(ServerData.addComment(postId, user.id, text));
							}catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
							}
						}
						break;
					}
					case Logout: {
						ServerData.loggedUsers.removeIf(u -> u.id == user.id);
						user = null;
						ous.writeObject(ErrorMessage.Success);
						break;
					}
					case DeletePost: {
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ErrorMessage.InvalidCommand);
						}else{
							try{
								int postId = Integer.parseInt(cmd.parameters[0]);
								ous.writeObject(ServerData.deletePost(postId, user.id));
							} catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
							}
						}
						break;
					}
					default: {
						ous.writeObject(ErrorMessage.InvalidCommand);
						break;
					}
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

	private void log(String msg){
		String prelude = "[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]";
		System.out.println(prelude + msg);
	}
}