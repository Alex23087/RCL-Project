package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Command;
import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.IFollowedCallbackService;
import it.unipi.rcl.project.common.PostView;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that handles the communication with a single client
 */
public class ClientHandler implements Runnable{
	private final Socket client;
	private ObjectInputStream ois;
	private ObjectOutputStream ous;
	private User user;
	//Factor used to calculate the conversion between WIN and BTC
	private int btcFactor = -1;
	//Dummy object used to avoid storing null values into a Map
	private static final IFollowedCallbackService dummyFCS = new IFollowedCallbackService() {
		@Override
		public void notifyFollow(int userId) throws RemoteException {}
		@Override
		public void notifyUnfollow(int userId) throws RemoteException {}
	};


	public ClientHandler(Socket clientSocket){
		this.client = clientSocket;
		try {
			ous = new ObjectOutputStream(client.getOutputStream());
			ois = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		log("Connected");
	}

	@Override
	public void run() {
		while (true) { //Keep looping as long as the client is connected
			try {
				Command cmd = (Command) ois.readObject();
				log("Received " + cmd);
				//If the user is not logged in yet and tries to issue any command that's not Login
				if (user == null && (cmd.op != Command.Operation.Login)) {
					log("User not logged in");
					ous.writeObject(ErrorMessage.UserNotLoggedIn);
					continue;
				}
				switch (cmd.op) {
					case AddComment: { //parameters[0] = postId, parameter[1] = text
						if(cmd.parameters == null || cmd.parameters.length < 2){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						} else {
							try {
								int postId = Integer.parseInt(cmd.parameters[0]);
								String text = cmd.parameters[1];
								ous.writeObject(ServerData.addComment(postId, user.id, text));
								log("Commented to post " + postId);
							}catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
								log("Invalid parameters");
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
								log("Invalid postId");
							}
						}
						break;
					}
					case CreatePost: { //parameters[0] = title, parameters[1] = text
						//Check if both title and text have been passed and if they are not too long
						if(cmd.parameters == null || cmd.parameters.length < 2 || cmd.parameters[0].length() > 20 || cmd.parameters[1].length() > 500){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						} else {
							int id = ServerData.addPost(user.id, cmd.parameters[0], cmd.parameters[1]);
							ous.writeObject(id);
						}
						break;
					}
					case DeletePost: { //parameters[0] = postId
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}else{
							try{
								int postId = Integer.parseInt(cmd.parameters[0]);
								ous.writeObject(ServerData.deletePost(postId, user.id));
							} catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
								log("Invalid parameters");
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
								log("Invalid postId");
							}
						}
						break;
					}
					case FollowUser: { //parameters[0] = userId of the person the user will be following
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}else {
							try {
								int followedId = Integer.parseInt(cmd.parameters[0]);
								ous.writeObject(ServerData.follow(user.id, followedId));
							} catch (NumberFormatException nfe) {
								ous.writeObject(ErrorMessage.InvalidUserId);
								log("Passed invalid userId");
							}
						}
						break;
					}
					case GetBalance: { //No parameters
						ous.writeObject(user.balance);
						break;
					}
					case GetBalanceInBTC: { //No parameters
						/**
						 * The factor used to get the BTC conversion is not calculated on demand, otherwise
						 * the client might experience delays while the server connects to the random.org page.
						 * It is instead stored in a field and calculated after fulfilling the request
						 * (this behaviour is only visible from the second invocation on,
						 * the first invocation will request it on demand, and then request the next one)
						 */
						if(btcFactor == -1){
							updateBtcFactor();
						}
						ous.writeObject(user.balance / (double) btcFactor);
						updateBtcFactor();
						break;
					}
					case GetTransactions: { //No parameters
						/**
						 * The output stream is reset in order to avoid the ObjectStreams caching the list and not
						 * sending the updated version of the list
						 */
						ous.reset();
						ous.writeObject(user.transactions);
						break;
					}
					case GetUsernameFromId: { //parameters[0] = userId
						try {
							int userID = Integer.parseInt(cmd.parameters[0]);
							User u = ServerData.getUser(userID);
							if (u == null) {
								ous.writeObject(ErrorMessage.InvalidUsername);
								log("Trying to get the id of a nonexistent user");
							} else {
								ous.writeObject(u.username);
							}
						} catch (NumberFormatException nfe) {
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}
						break;
					}
					case ListFollowers: { //No parameters
						List<Integer> l = ServerData.getFollowers(user.id);
						ous.writeObject(l);
						break;
					}
					case ListFollowing: { //No parameters
						List<Integer> l = ServerData.getFollowed(user.id);
						ous.writeObject(l);
						break;
					}
					case ListUsers: { //No parameters
						ous.writeObject(ServerData.getUsersWithTags(user.tags));
						break;
					}
					case Login: { //parameters[0] = username, parameters[1] = password hash
						if(cmd.parameters == null || cmd.parameters.length < 2){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}else if(user != null || ServerData.isLoggedIn(cmd.parameters[0])) {
							ous.writeObject(ErrorMessage.UserAlreadyLoggedIn);
							log("User already logged in");
						}else{
							User u = ServerData.getUser(ServerData.getUserId(cmd.parameters[0]));
							if(u == null){ //If the user wasn't found
								ous.writeObject(ErrorMessage.InvalidUsername);
								log("Invalid username \"" + cmd.parameters[0] + "\"");
							}else if(u.password.equals(cmd.parameters[1])) {
								this.user = u;
								/**
								 * Putting the dummyFCS in the loggedUsers Map to signify the user has logged in but
								 * hasn't sent the callback service object yet.
								 */
								ServerData.loggedUsers.put(u.id, dummyFCS);
								ous.writeObject(u.id);
								log("Logged in as " + u.username);
							}else{
								ous.writeObject(ErrorMessage.InvalidPassword);
								log("Invalid password");
							}
						}
						break;
					}
					case Logout: { //No parameters
						ServerData.logout(user.id);
						user = null;
						ous.writeObject(ErrorMessage.Success);
						break;
					}
					case RatePost: { //parameters[0] = postId, parameters[1] = upvote (true) or downvote (false)
						if(cmd.parameters == null || cmd.parameters.length < 2){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						} else {
							try {
								int postId = Integer.parseInt(cmd.parameters[0]);
								boolean upvote = Boolean.parseBoolean(cmd.parameters[1]);
								ous.writeObject(ServerData.vote(postId, user.id, upvote));
							}catch (NumberFormatException nfe){
								ous.writeObject(ErrorMessage.InvalidCommand);
								log("Invalid parameters");
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
								log("Invalid postId");
							}
						}
						break;
					}
					case RewinPost: { //parameters[0] = postId
						try {
							ErrorMessage errorMessage = ServerData.rewin(user.id, Integer.parseInt(cmd.parameters[0]));
							ous.writeObject(errorMessage);
						} catch (NonexistentPostException npe){
							ous.writeObject(ErrorMessage.InvalidPostId);
							log("Invalid postId");
						}
						break;
					}
					case ShowFeed: { //No parameters
						ous.writeObject(ServerData.getFeed(user.id));
						break;
					}
					case ShowPost: { //parameters[0] = postId
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}else {
							try {
								int postID = Integer.parseInt(cmd.parameters[0]);
								PostView p = ServerData.getPostViewWithId(postID, user.id);
								/**
								 * Resetting the ObjectOutputStream to invalidate the cache. Removing it might result
								 * in the client receiving a cached version of the object.
								 */
								ous.reset();
								ous.writeObject(p);
							} catch (NumberFormatException nfe) {
								ous.writeObject(ErrorMessage.InvalidCommand);
								log("Invalid parameters");
							} catch (NonexistentPostException e) {
								ous.writeObject(ErrorMessage.InvalidPostId);
								log("Invalid postId");
							}
						}
						break;
					}
					case UnfollowUser: { //parameters[0] = userId of the user to unfollow
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ErrorMessage.InvalidCommand);
							log("Invalid parameters");
						}else {
							try {
								int followedId = Integer.parseInt(cmd.parameters[0]);
								ous.writeObject(ServerData.unfollow(user.id, followedId));
							} catch (NumberFormatException nfe) {
								ous.writeObject(ErrorMessage.InvalidUserId);
								log("Passed invalid userID to Unfollow command");
							}
						}
						break;
					}
					case ViewBlog: { //If no parameters returns own blog, otherwise parameters[0] = userId of the user whose blog is to be retrieved
						if(cmd.parameters == null || cmd.parameters.length < 1){
							ous.writeObject(ServerData.getPosts(user.id));
						}else{
							User u = ServerData.getUser(Integer.parseInt(cmd.parameters[0]));
							if (u == null) {
								ous.writeObject(ErrorMessage.NoSuchUser);
								log("No user with the specified userId");
							} else {
								ous.writeObject(ServerData.getPosts(u.id));
							}
						}
						break;
					}
					default: { //Invalid command sent
						ous.writeObject(ErrorMessage.InvalidCommand);
						break;
					}
				}
			} catch (IOException | ClassNotFoundException e){
				log("Disconnected");
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

	/**
	 * Method to retrieve a new conversion factor from random.org
	 */
	private void updateBtcFactor(){
		try {
			URL randomURL = new URL("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");
			URLConnection connection = randomURL.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			btcFactor = Integer.parseInt(in.readLine());
			in.close();
		}catch(IOException ioe){
			//Set a factor of 1 if the retrieval doesn't succeed
			btcFactor = 1;
		}
	}

	/**
	 * Utility method to write logs
	 */
	private void log(String msg){
		StringBuilder prelude = new StringBuilder("[")
				.append(client.getInetAddress())
				.append(":")
				.append(client.getPort())
				.append("(")
				.append(user == null ? "not logged in" : user.username)
				.append(")]");
		System.out.println(prelude.append(msg));
	}
}