package it.unipi.rcl.project.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unipi.rcl.project.common.Pair;
import it.unipi.rcl.project.common.Post;
import it.unipi.rcl.project.common.Utils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerData {
	static Map<String, User> users;
	static List<User> loggedUsers;
	static List<Post> posts;
	static List<Pair<Integer, Integer>> follows; //First element is followerID, second element is followedID

	static{
		users = new ConcurrentHashMap<>();
		loggedUsers = Collections.synchronizedList(new LinkedList<>());
		posts = Collections.synchronizedList(new LinkedList<>());
		follows = Collections.synchronizedList(new LinkedList<>());
		loadFromDisk();
	}

	public static List<Pair<String, String[]>> getUsersWithTags(String[] tags){
		return users.values().stream().filter(user -> Arrays.stream(user.tags).anyMatch(t -> Arrays.asList(tags).contains(t))).map(user -> new Pair<>(user.username, user.tags)).collect(Collectors.toList());
	}

	public static void addUser(String username, String password, String[] tags){
		users.put(username, new User(username, Utils.hashString(password), tags));
		System.out.println("New user with username " + username + " successfully registered to the platform");
	}

	public static List<Post> getFeed(User user){
		List<Integer> followedList = follows.stream().filter(followRelationship -> followRelationship.first == user.id).map(f -> f.second).collect(Collectors.toList());
		return posts.stream().filter(post -> followedList.contains(post.authorId)).sorted(Comparator.comparing(c -> -(c.id))).collect(Collectors.toList());
	}

	public static List<Post> getPosts(User user){
		return posts.stream().filter(post -> post.authorId == user.id).sorted(Comparator.comparing(c -> -(c.id))).collect(Collectors.toList());
	}

	public static int addPost(User author, String title, String text){
		Post newPost = new Post(author, title, text);
		posts.add(newPost);
		return newPost.id;
	}

	public static int addRewin(User author, int rewinId){
		Post newPost = new Post(author, rewinId);
		posts.add(newPost);
		return newPost.id;
	}

	public static User getUser(int id){
		return users.values().stream().filter(u -> u.id == id).findFirst().orElse(null);
	}

	public static int getUserId(String username){
		User u = users.get(username);
		if(u == null){
			return -1;
		}else{
			return u.id;
		}
	}

	public static boolean follow(int follower, int followed){
		Pair<Integer, Integer> f = new Pair<>(follower, followed);
		if(follows.contains(f)){
			return false;
		}else{
			follows.add(f);
			return true;
		}
	}

	public static void saveToDisk(){
		saveUsers();
		saveFollows();
	}

	private static void saveUsers(){
		System.out.println("Writing users to disk...");
		String pathname = "./users.json";
		Path path = Paths.get(pathname);
		try {
			try {
				Files.createFile(path);
			} catch (FileAlreadyExistsException ignored) {}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(pathname)));
			writer.setIndent("\t");
			writer.beginArray();
			String[] usernames = users.keySet().toArray(new String[]{});
			for(int i = 0; i < usernames.length; i++) {
				gson.toJson(users.get(usernames[i]), User.class, writer);
			}
			writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Users written to disk");
	}

	private static void saveFollows(){

		System.out.println("Writing follows to disk...");
		String pathname = "./follows.json";
		Path path = Paths.get(pathname);
		try {
			try {
				Files.createFile(path);
			} catch (FileAlreadyExistsException ignored) {}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(pathname)));
			writer.setIndent("\t");
			writer.beginArray();
			for(int i = 0; i < follows.size(); i++) {
				gson.toJson(follows.get(i), Pair.class, writer);
			}
			writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Follows written to disk");
	}

	private static void loadFromDisk(){
		loadUsers();
		//TODO: read users and posts from file
	}

	private static void loadUsers(){
		System.out.println("Loading users from disk...");
		String pathname = "./users.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			reader.beginArray();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			while(reader.hasNext()){
				User user = gson.fromJson(reader, User.class);
				users.put(user.username, user);
			}
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Users loaded");
	}
}
