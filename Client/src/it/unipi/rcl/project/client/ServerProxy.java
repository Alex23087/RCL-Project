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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerProxy{
	public static ServerProxy instance = new ServerProxy();
	private ISignUpService signUpService;
	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream ous;
	public String user = null;
	public int userId = -1;
	public double balance = -1;
	public List<Integer> followed;
	public List<Integer> followers;
	private final Map<Integer, String> usernames; //Contains the associations between user ids and usernames
	private Runnable unknownExceptionHandler = () -> {};
	private Map<ConfigurationParameter, Object> conf;
	private MulticastSocket multicastSocket;
	private Thread notificationThread;

	private final ExecutorService pool;

	private ServerProxy(){
		pool = Executors.newFixedThreadPool(1);
		followed = Collections.synchronizedList(new LinkedList<>());
		followers = Collections.synchronizedList(new LinkedList<>());
		usernames = new HashMap<>();
		conf = Utils.readConfFile("./conf.conf");

		notificationThread = new Thread(() -> {
			DatagramPacket packet = new DatagramPacket(new byte[64], 64);
			try {
				multicastSocket.receive(packet);
				String msg = new String(packet.getData(), StandardCharsets.UTF_8);
				System.out.println(msg);
				getWallet(true, l -> {}, e -> {});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		connectToServerUDP();
	}

	private boolean connectToRMIIfNeeded(){
		if(signUpService == null){
			return connectToServerRMI();
		}else{
			return true;
		}
	}

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
		} catch (IOException e) {
			e.printStackTrace();
			unknownExceptionHandler.run();
			return false;
		}
		System.out.println("Connected to server TCP");
		return true;
	}

	private boolean connectToServerRMI(){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry((String) conf.get(ConfigurationParameter.REGHOST), (Integer) conf.get(ConfigurationParameter.REGPORT));
			signUpService = (ISignUpService) registry.lookup((String) conf.get(ConfigurationParameter.SIGNUP_SERVICE_NAME));
		} catch (ConnectException ce) {
			unknownExceptionHandler.run();
			return false;
		}catch (RemoteException | NotBoundException re){
			re.printStackTrace();
			return false;
		}
		System.out.println("Connected to RMI service");
		return true;
	}

	private void connectToServerUDP(){
		try {
			InetAddress group = InetAddress.getByName((String) conf.get(ConfigurationParameter.MULTICAST));
			multicastSocket = new MulticastSocket((int) conf.get(ConfigurationParameter.MCASTPORT));
			multicastSocket.joinGroup(group);
		} catch (IOException e) {
			e.printStackTrace();
		}
		notificationThread.start();
	}

	public void registerUnknownExceptionHandler(Runnable handler){
		unknownExceptionHandler = () -> {
			handler.run();
			resetStatus();
		};
	}

	public void register(String username, String password, String[] tags, Runnable successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
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
				case Success:
					successCallback.run();
					return;
				default:
					errorCallback.run(em);
					return;
			}
		});
	}

	public void login(String username, String password, Runnable successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if(!connectToTCPIfNeeded()){
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}
			try {
				ous.writeObject(new Command(Command.Operation.Login, new String[]{username, Utils.hashString(password)}));
				ous.flush();
				Object response = ois.readObject();
				if(response instanceof ErrorMessage) {
					errorCallback.run((ErrorMessage) response);
				}else{
					user = username;
					userId = (int) response;
					successCallback.run();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	public void viewBlog(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetPosts, null));
				successCallback.run((List<PostViewShort>) ois.readObject());
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	public void showFeed(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetFeed, null));
				successCallback.run((List<PostViewShort>) ois.readObject());
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	public void createPost(String title, String text, Callback<Integer> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.PublishPost, new String[]{title, text}));
				successCallback.run((int) ois.readObject());
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	public void getWallet(Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		getWallet(false, successCallback, errorCallback);
	}

	public void getWallet(boolean forceUpdate, Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		if(forceUpdate || balance == -1) {
			pool.submit(() -> {
				if (!connectToTCPIfNeeded() || user == null) {
					errorCallback.run(ErrorMessage.UnknownError);
					return;
				}

				try {
					ous.writeObject(new Command(Command.Operation.GetBalance, null));
					successCallback.run((double) ois.readObject());
					return;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					errorCallback.run(ErrorMessage.UnknownError);
					return;
				} catch (IOException e) {
					unknownExceptionHandler.run();
				}
			});
		}else{
			successCallback.run(balance);
		}
	}

	public void getWalletInBitcoin(Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetBTCConversion, null));
				successCallback.run((double) ois.readObject());
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			} catch (IOException e) {
				unknownExceptionHandler.run();
			}
		});
	}

	public void listUsers(Callback<List<Pair<Integer, String[]>>> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
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

	public void followUser(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.Follow, new String[]{Integer.toString(userId)}));
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

	public void unfollowUser(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.Unfollow, new String[]{Integer.toString(userId)}));
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

	public void listFollowers(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			//TODO: cache
			try{
				ous.writeObject(new Command(Command.Operation.GetFollowers, null));
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

	public void listFollowing(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			if(followed != null && followed.size() > 0){
				successCallback.run(followed);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.GetFollowed, null));
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

	public void getUsernameFromId(Integer userID, Callback<String> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

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

	public void showPost(Integer postID, Callback<PostView> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}
			try {
				ous.writeObject(new Command(Command.Operation.GetPostViewFromId, new String[]{postID.toString()}));
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

	public void ratePost(Integer postID, Boolean upvote, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}
			try {
				ous.writeObject(new Command(Command.Operation.Vote, new String[]{postID.toString(), upvote.toString()}));
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

	public void addComment(Integer postID, String text, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}
			try {
				ous.writeObject(new Command(Command.Operation.AddComment, new String[]{postID.toString(), text}));
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

	public void logout(Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
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

	public void deletePost(Integer postId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
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

	public void rewinPost(Integer postId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}
			try {
				ous.writeObject(new Command(Command.Operation.Rewin, new String[]{postId.toString()}));
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

	private void resetStatus(){
		user = null;
		userId = -1;
		followed.clear();
		followers.clear();
		try {
			socket.close();
		} catch (Exception e) {}
		socket = null;
	}

	public interface Callback<T> {
		void run(T value);
	}
}