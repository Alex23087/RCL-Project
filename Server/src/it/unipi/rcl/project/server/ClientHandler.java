package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.IFollowedCallbackService;
import it.unipi.rcl.project.common.PostView;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable{
	private Socket client;
	private ObjectInputStream ois;
	private ObjectOutputStream ous;
	private User user;
	private IFollowedCallbackService dummyFCS;
	private int btcFactor = -1;

	public ClientHandler(Socket clientSocket){
		this.client = clientSocket;
		try {
			ous = new ObjectOutputStream(client.getOutputStream());
			ois = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Connected");
		dummyFCS = new IFollowedCallbackService() {
			@Override
			public void notifyFollow(int userId) throws RemoteException {}
			@Override
			public void notifyUnfollow(int userId) throws RemoteException {}
		};
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
						if (user != null || ServerData.isLoggedIn(cmd.parameters[0])) {
							System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + cmd.parameters[0] + ")]User already logged in");
							ous.writeObject(ErrorMessage.UserAlreadyLoggedIn);
							ous.flush();
						} else {
							User u = ServerData.getUser(ServerData.getUserId(cmd.parameters[0]));
							if (u == null) {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Invalid username \"" + cmd.parameters[0] + "\"");
								ous.writeObject(ErrorMessage.InvalidUsername);
								ous.flush();
							} else if (u.password.equals(cmd.parameters[1])) {
								System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "]Logged in as " + u.username);
								this.user = u;
								ous.writeObject(u.id);
								ous.flush();
								ServerData.loggedUsers.put(u.id, dummyFCS);
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
					case GetTransactions: {
						ous.reset();
						ous.writeObject(user.transactions.stream().sorted(Comparator.comparing(t -> -(t.timestamp))).collect(Collectors.toList()));
						break;
					}
					case GetBTCConversion: {
						if(btcFactor == -1){
							updateBtcFactor();
						}
						ous.writeObject(user.balance / (double) btcFactor);
						updateBtcFactor();
						break;
					}
					case PublishPost: {
						if(cmd.parameters == null || cmd.parameters.length < 2 || cmd.parameters[0].length() > 20 || cmd.parameters[1].length() > 500){
							ous.writeObject(ErrorMessage.InvalidCommand);
						} else {
							int id = ServerData.addPost(user.id, cmd.parameters[0], cmd.parameters[1]);
							ous.writeObject(id);
							System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Created new post with id " + id);
						}
						break;
					}
					case Rewin: {
						try {
							ErrorMessage errorMessage = ServerData.rewin(user.id, Integer.parseInt(cmd.parameters[0]));
							ous.writeObject(errorMessage);
						} catch (NonexistentPostException npe){
							ous.writeObject(ErrorMessage.InvalidPostId);
						}
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
					case GetFollowers: {
						List<Integer> l = ServerData.getFollowers(user.id);
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
							ous.reset();
							ous.writeObject(p);
						} catch (NumberFormatException nfe) {
							ous.writeObject(ErrorMessage.InvalidCommand);
						} catch (NonexistentPostException e) {
							ous.writeObject(ErrorMessage.InvalidPostId);
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
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
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
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
							}
						}
						break;
					}
					case Logout: {
						ServerData.logout(user.id);
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
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
							}
						}
						break;
					}
					default: {
						ous.writeObject(ErrorMessage.InvalidCommand);
						break;
					}
				}
			} catch (IOException | ClassNotFoundException e){
				System.out.println("[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]Disconnected");
				ServerData.logout(user.id);
				try {
					ous.close();
					ois.close();
					client.close();
				} catch (IOException ignored) {}
				break;
			}
		}
	}

	private void updateBtcFactor() throws IOException {
		URL randomURL = new URL("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");
		URLConnection connection = randomURL.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		btcFactor = Integer.parseInt(in.readLine());
		in.close();
	}

	private void log(String msg){
		String prelude = "[" + client.getInetAddress() + ":" + client.getPort() + "(" + user.username + ")]";
		System.out.println(prelude + msg);
	}
}