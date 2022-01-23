package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.*;
import it.unipi.rcl.project.common.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
	public long balance = -1;
	public List<Integer> followed;
	public List<Integer> followers;
	private final Map<Integer, String> usernames; //Contains the associations between user ids and usernames

	private final ExecutorService pool;

	private ServerProxy(){
		pool = Executors.newFixedThreadPool(1);
		followed = Collections.synchronizedList(new LinkedList<>());
		followers = Collections.synchronizedList(new LinkedList<>());
		usernames = new HashMap<>();
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
			return connectToServerTCP("localhost", 6666);
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
			return false;
		}
		System.out.println("Connected to server TCP");
		return true;
	}

	private boolean connectToServerRMI(){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(Constants.registryPort);
			signUpService = (ISignUpService) registry.lookup(Constants.signUpServiceName);
		}catch(RemoteException | NotBoundException re){
			re.printStackTrace();
			return false;
		}
		System.out.println("Connected to RMI service");
		return true;
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}
		});
	}

	public void getPosts(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetPosts, null));
				successCallback.run((List<PostViewShort>) ois.readObject());
				return;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}
		});
	}

	public void getFeed(Callback<List<PostViewShort>> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetFeed, null));
				successCallback.run((List<PostViewShort>) ois.readObject());
				return;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}
		});
	}

	public void getBalance(Callback<Long> successCallback, Callback<ErrorMessage> errorCallback){
		getBalance(false, successCallback, errorCallback);
	}

	public void getBalance(boolean forceUpdate, Callback<Long> successCallback, Callback<ErrorMessage> errorCallback){
		if(forceUpdate || balance == -1) {
			pool.submit(() -> {
				if (!connectToTCPIfNeeded() || user == null) {
					errorCallback.run(ErrorMessage.UnknownError);
					return;
				}

				try {
					ous.writeObject(new Command(Command.Operation.GetBalance, null));
					successCallback.run((long) ois.readObject());
					return;
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					errorCallback.run(ErrorMessage.UnknownError);
					return;
				}
			});
		}else{
			successCallback.run(balance);
		}
	}

	public void getBTCBalance(Callback<Double> successCallback, Callback<ErrorMessage> errorCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try {
				ous.writeObject(new Command(Command.Operation.GetBTCConversion, null));
				successCallback.run((double) ois.readObject());
				return;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
				return;
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void follow(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
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
			}catch (IOException | ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void unfollow(int userId, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
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
			}catch (IOException | ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void getFollowers(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
				return;
			}

			try{
				ous.writeObject(new Command(Command.Operation.GetFollowers, null));
				Object response = ois.readObject();
				if(response instanceof ErrorMessage){
					errorMessageCallback.run((ErrorMessage) response);
				}else{
					followed = (List<Integer>) response;
					successCallback.run(followed);
				}
			}catch (IOException | ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void getFollowed(Callback<List<Integer>> successCallback, Callback<ErrorMessage> errorMessageCallback){
		pool.submit(() -> {
			if (!connectToTCPIfNeeded() || user == null) {
				errorMessageCallback.run(ErrorMessage.UnknownError);
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
			}catch (IOException | ClassNotFoundException ioe){
				ioe.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
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
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					errorMessageCallback.run(ErrorMessage.UnknownError);
				}
			}
		});
	}

	public void getPostViewFromId(Integer postID, Callback<PostView> successCallback, Callback<ErrorMessage> errorMessageCallback){
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void vote(Integer postID, Boolean upvote, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	public void comment(Integer postID, String text, Runnable successCallback, Callback<ErrorMessage> errorMessageCallback){
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
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
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				errorMessageCallback.run(ErrorMessage.UnknownError);
			}
		});
	}

	private void resetStatus(){
		user = null;
		userId = -1;
		followed.clear();
		followers.clear();
	}

	public interface Callback<T> {
		void run(T value);
	}
}