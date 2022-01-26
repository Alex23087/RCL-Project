package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.*;
import it.unipi.rcl.project.common.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton class that handles the communication with the server
 */
public class ServerProxy{
	/**
	 * Singleton instance
	 */
	public static ServerProxy instance = new ServerProxy();

	/**
	 * Social network data
	 */
	public String user = null;
	public int userId = -1;
	public double balance = -1;
	private List<Integer> followed;
	private List<Integer> followers;
	private final Map<Integer, String> usernames; //Contains the associations between user ids and usernames

	/**
	 * Connection variables
	 */
	private ISignUpService signUpService;
	private MulticastSocket multicastSocket;
	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream ous;

	/**
	 * Configuration parameters
	 */
	private final Map<ConfigurationParameter, Object> conf;


	private final ExecutorService workerThread;
	private Runnable unknownExceptionHandler = () -> {};
	private Callback<Integer> followedNotificationHandler = id -> {};
	private Callback<Integer> unfollowedNotificationHandler = id -> {};


	private ServerProxy(){
		workerThread = Executors.newFixedThreadPool(1);
		followed = Collections.synchronizedList(new LinkedList<>());
		followers = Collections.synchronizedList(new LinkedList<>());
		usernames = new HashMap<>();
		conf = Utils.readConfFile("./conf.conf");

		connectToServerUDP();
	}

	/**
	 * Connects to server RMI if not already connected
	 */
	private boolean connectToRMIIfNeeded(){
		if(signUpService == null){
			return connectToServerRMI();
		}else{
			return true;
		}
	}

	/**
	 * Connects to server TCP if not already connected
	 */
	private boolean connectToTCPIfNeeded(){
		if(socket == null || !socket.isConnected()){
			return connectToServerTCP((String) conf.get(ConfigurationParameter.SERVER), (int) conf.get(ConfigurationParameter.TCPPORT));
		}else{
			return true;
		}
	}

	private boolean connectToServerTCP(String address, int port){
		try {
			socket = new Socket(address, port);
			ous = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connected to server TCP");
		} catch (IOException e) {
			e.printStackTrace();
			unknownExceptionHandler.run();
			return false;
		}
		return true;
	}

	private boolean connectToServerRMI(){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry((String) conf.get(ConfigurationParameter.REGHOST), (Integer) conf.get(ConfigurationParameter.REGPORT));
			signUpService = (ISignUpService) registry.lookup((String) conf.get(ConfigurationParameter.SIGNUP_SERVICE_NAME));
			System.out.println("Connected to RMI service");
		} catch (ConnectException ce) {
			unknownExceptionHandler.run();
			return false;
		}catch (RemoteException | NotBoundException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void connectToServerUDP(){
		try {
			//Join multicast group
			InetAddress group = InetAddress.getByName((String) conf.get(ConfigurationParameter.MULTICAST));
			multicastSocket = new MulticastSocket((int) conf.get(ConfigurationParameter.MCASTPORT));
			multicastSocket.joinGroup(group);

			//Initialising and starting the reward notification thread
			Thread notificationThread = new Thread(() -> {
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[64], 64);
					try {
						//The packet is received, but as it is only a notification, it doesn't contain any useful data
						multicastSocket.receive(packet);
						/*
						 * Call to getBalance with forceUpdate set to true, to force the client to request
						 * the new balance from the server and update its cached value.
						 */
						getBalance(true, l -> {}, e -> {});
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			});
			notificationThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Registers the handler for when the connection is dropped or an unknown exception occurs
	 */
	public void registerUnknownExceptionHandler(Runnable handler){
		unknownExceptionHandler = () -> {
			handler.run();
			resetStatus();
		};
	}

	/**
	 * Registers a handler for when the client receives a notification of a user starting to follow them
	 */
	public void registerFollowedNotificationHandler(Callback<Integer> handler){
		followedNotificationHandler = handler;
	}

	/**
	 * Registers a handler for when the client receives a notification of a user no longer following them
	 */
	public void registerUnfollowedNotificationHandler(Callback<Integer> handler){
		unfollowedNotificationHandler = handler;
	}

	/**
	 * Registers a user on the social network
	 */
	public void register(String username, String password, String[] tags, Runnable successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if(!connectToRMIIfNeeded()){
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			ErrorMessage em;
			try {
				em = signUpService.signUp(username, password, tags);
			} catch (RemoteException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}
			switch(em){
				case Success: {
					successCallback.run();
					return;
				}
				default: {
					errorCallback.run(em);
					return;
				}
			}
		});
	}

	/**
	 * Logs in the user to the social network
	 */
	public void login(String username, String password, Runnable successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if(!connectToTCPIfNeeded()){
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.Login, new String[]{username, Utils.hashString(password)}));

				Object response = ois.readObject();
				if(response instanceof ErrorMessage) {
					//If the response is an ErrorMessage, something has gone wrong
					errorCallback.run((ErrorMessage) response);
				}else{
					//If the response is not an ErrorMessage, login has been successful and the response is the userId
					user = username;
					userId = (int) response;
					//Registering the RMI callback object to get notified of following/unfollowing events
					registerServerFollowerCallback();
					//Call listFollowing/listFollowers to force an update of the respective lists
					listFollowing(f -> {}, em -> {});
					listFollowers(f -> {}, em -> {});

					successCallback.run();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets the list of posts posted by this user
	 */
	public void viewBlog(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.ViewBlog, null));

				Object response = ois.readObject();
				if(response instanceof List){
					successCallback.run((List<PostViewShort>) response);
				}else{
					errorCallback.run(ErrorMessage.UnknownError);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets the list of posts posted by people this user follows
	 */
	public void showFeed(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.ShowFeed, null));

				Object response = ois.readObject();
				if(response instanceof List){
					successCallback.run((List<PostViewShort>) response);
				}else{
					errorCallback.run(ErrorMessage.UnknownError);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Posts a new post to the social network
	 */
	public void createPost(String title, String text, Callback<Integer> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.CreatePost, new String[]{title, text}));

				Object response = ois.readObject();
				if(response instanceof ErrorMessage) {
					errorCallback.run((ErrorMessage) response);
				}else{
					successCallback.run((int) response);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets the current balance for this user. If the value has been cached, it returns the cached value.
	 */
	public void getBalance(Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		getBalance(false, successCallback, errorCallback);
	}

	/**
	 * Gets the current balance for this user. If the value has been cached and forceUpdate is set to false, returns
	 * the cached value. Otherwise, it requests the value to the server and caches it for future requests.
	 */
	public void getBalance(boolean forceUpdate, Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		if(forceUpdate || balance == -1) {
			workerThread.submit(() -> {
				if (!connectToTCPIfNeeded() || user == null) {
					errorCallback.run(ErrorMessage.UnknownError);
					return;
				}

				try {
					ous.writeObject(new Command(Command.Operation.GetBalance, null));
					balance = (double) ois.readObject();
					successCallback.run(balance);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					errorCallback.run(ErrorMessage.UnknownError);
				} catch (IOException e) {
					unknownExceptionHandler.run();
				}
			});
		}else{
			successCallback.run(balance);
		}
	}

	/**
	 * Gets the list of transactions for the current user
	 */
	public void getTransactions(Callback<List<Transaction>> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetTransactions, null));

				successCallback.run((List<Transaction>) ois.readObject());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets the balance for the current user, converted to BTC.
	 */
	public void getBalanceInBTC(Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetBalanceInBTC, null));

				successCallback.run((double) ois.readObject());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets a list of users that share at least one tag with the current user.
	 */
	public void listUsers(Callback<List<Pair<Integer, String[]>>> successCallback, Callback<ErrorMessage> errorCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.ListUsers, null));

				successCallback.run((List<Pair<Integer, String[]>>) ois.readObject());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Follows the user with the id passed as parameter.
	 */
	public void followUser(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.FollowUser, new String[]{Integer.toString(userId)}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						followed.add(userId);
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
				}
			}catch (ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Unfollows the user with the id passed as parameter.
	 */
	public void unfollowUser(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.UnfollowUser, new String[]{Integer.toString(userId)}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						followed.remove((Integer) userId);
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
				}
			}catch (ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets a list of users who follow the current user. Only the first time this method is invoked
	 * will trigger a request to the server. The list will then be cached for the following invocations.
	 */
	public void listFollowers(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			//Check if followers have been cached
			if(followers != null && followers.size() > 0){
				successCallback.run(followers);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.ListFollowers, null));

				Object response = ois.readObject();
				if(response instanceof ErrorMessage){
					errorMessageCallback.run((ErrorMessage) response);
				}else{
					followers = (List<Integer>) response;
					successCallback.run(followers);
				}
			}catch (ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets a list of users followed by the current user. Only the first time this method is invoked
	 * will trigger a request to the server. The list will then be cached for the following invocations.
	 */
	public void listFollowing(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			//Check if the list has been cached
			if(followed != null && followed.size() > 0){
				successCallback.run(followed);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.ListFollowing, null));

				Object response = ois.readObject();
				if(response instanceof ErrorMessage){
					errorMessageCallback.run((ErrorMessage) response);
				}else{
					followed = (List<Integer>) response;
					successCallback.run(followed);
				}
			}catch (ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Gets the username for the user with id passed as parameter. The result is then cached to avoid contacting the server
	 * to get the same translation more than once.
	 */
	public void getUsernameFromId(Integer userID, Callback<String> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			//Check if there's already a username cached for that id
			if(usernames.containsKey(userID)){
				successCallback.run(usernames.get(userID));
			}else{
				try {
					ous.writeObject(new Command(Command.Operation.GetUsernameFromId, new String[]{userID.toString()}));

					Object response = ois.readObject();
					if(response instanceof String){
						String username = (String) response;
						usernames.put(userID, username);
						successCallback.run(username);
					}else{
						errorMessageCallback.run((ErrorMessage) response);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					errorMessageCallback.run(ErrorMessage.UnknownError);
				} catch (IOException e) {
					unknownExceptionHandler.run();
				}
			}
		});
	}

	/**
	 * Gets a PostView for the post passed as parameter
	 */
	public void showPost(Integer postID, Callback<PostView> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.ShowPost, new String[]{postID.toString()}));

				Object response = ois.readObject();
				if(response instanceof PostView){
					successCallback.run((PostView) response);
				}else{
					errorMessageCallback.run((ErrorMessage) response);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Sends an upvote/downvote (depending on the variable upvote) for the post with id postId
	 */
	public void ratePost(Integer postId, Boolean upvote, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.RatePost, new String[]{postId.toString(), upvote.toString()}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Posts a comment for the post with id postId
	 */
	public void addComment(Integer postId, String text, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.AddComment, new String[]{postId.toString(), text}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Logs out of the social network
	 */
	public void logout(Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.Logout, null));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						resetStatus();
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Deletes a post from the social network. If the post is a regular post, all rewins are deleted too.
	 */
	public void deletePost(Integer postId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.DeletePost, new String[]{postId.toString()}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Rewins a post.
	 */
	public void rewinPost(Integer postId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.RewinPost, new String[]{postId.toString()}));

				ErrorMessage em = (ErrorMessage) ois.readObject();
				switch (em){
					case Success:
						successCallback.run();
						break;
					default:
						errorMessageCallback.run(em);
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	/**
	 * Registers the RMI follow/unfollow callback.
	 */
	private void registerServerFollowerCallback(){
		workerThread.submit(() -> {
			if(!connectToRMIIfNeeded()){
				return;
			}

			IFollowedCallbackService fcs = new FollowedCallbackServiceImpl();

			try {
				signUpService.registerFollowCallback((IFollowedCallbackService) UnicastRemoteObject.exportObject(fcs, 0), userId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Returns true if the user with id passed as parameter is following the current user.
	 * Called on the worker thread to ensure that the response is updated.
	 */
	public void isFollowing(int userId, Callback<Boolean> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> listFollowers(followers -> successCallback.run(followers.contains(userId)), errorMessageCallback));
	}

	/**
	 * Returns the number of people who follow the current user.
	 * Called on the worker thread to ensure that the response is updated.
	 */
	public void getFollowerCount(Callback<Integer> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> listFollowers(followers -> successCallback.run(followers.size()), errorMessageCallback));
	}

	/**
	 * Returns the number of people followed by the current user.
	 * Called on the worker thread to ensure that the response is updated.
	 */
	public void getFollowedCount(Callback<Integer> successCallback, Callback<ErrorMessage> errorMessageCallback){
		workerThread.submit(() -> listFollowing(followed -> successCallback.run(followed.size()), errorMessageCallback));
	}

	/**
	 * Resets the ServerProxy instance.
	 */
	private void resetStatus(){
		user = null;
		userId = -1;
		followed.clear();
		followers.clear();
		balance = -1;
		try {
			socket.close();
		} catch (Exception ignored) {}
		socket = null;
	}

	/**
	 * Interface that represents a callback with one parameter
	 */
	public interface Callback<T> {
		void run(T value);
	}

	/**
	 * Implementation of the RMI callback notification service
	 */
	class FollowedCallbackServiceImpl extends RemoteObject implements IFollowedCallbackService{
		/**
		 * Called when another user has followed the current user
		 */
		@Override
		public void notifyFollow(int userId) throws RemoteException {
			followers.add(userId);
			followedNotificationHandler.run(userId);
		}

		/**
		 * Called when another user has unfollowed the current user
		 */
		@Override
		public void notifyUnfollow(int userId) throws RemoteException {
			followers.remove((Integer) userId); //Casting to Integer to ensure the right method is called
			unfollowedNotificationHandler.run(userId);
		}
	}
}