package it.unipi.rcl.project.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unipi.rcl.project.common.*;

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
	static List<Comment> comments;
	static List<Vote> votes;

	static{
		users = new ConcurrentHashMap<>();
		loggedUsers = Collections.synchronizedList(new LinkedList<>());
		posts = Collections.synchronizedList(new LinkedList<>());
		follows = Collections.synchronizedList(new LinkedList<>());
		comments = Collections.synchronizedList(new LinkedList<>());
		votes = Collections.synchronizedList(new LinkedList<>());
		loadFromDisk();
	}

	public static List<Pair<Integer, String[]>> getUsersWithTags(String[] tags){
		return users.values().stream().filter(user -> Arrays.stream(user.tags).anyMatch(t -> Arrays.asList(tags).contains(t))).map(user -> new Pair<>(user.id, user.tags)).collect(Collectors.toList());
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
		if(follower == followed){
			return false;
		}
		if(follows.stream().anyMatch(f -> f.first == follower && f.second == followed)){
			return false;
		}else{
			follows.add(new Pair<>(follower, followed));
			return true;
		}
	}

	public static boolean unfollow(int follower, int followed){
		if(follower == followed){
			return false;
		}
		Iterator<Pair<Integer, Integer>> i = follows.iterator();
		while(i.hasNext()){
			Pair<Integer, Integer> f = i.next();
			if(f.first == follower && f.second == followed){
				i.remove();
				return true;
			}
		}
		return false;
	}

	public static List<Integer> getFollowed(int userId){
		/*List<String> followed = new ArrayList<>();
		for(Pair<Integer, Integer> f: follows){
			if(f.first != userId){
				continue;
			}
			followed.add(getUser(f.second).username);
		}
		return followed;*/
		return follows.stream().filter(f -> f.first == userId).map(p -> getUser(p.second).id).collect(Collectors.toList());
	}

	public static Post getPostWithId(int postId){
		return posts.stream().filter(p -> p.id == postId).findFirst().orElse(null);
	}

	public static int getVoteCount(int postId, boolean upvotes){
		return votes.stream().reduce(0, (count, vote) -> count + (vote.postId == postId ? vote.upvote == upvotes ? 1 : 0 : 0), Integer::sum);
	}

	public static List<Comment> getComments(int postId){
		return comments.stream().filter(comment -> comment.postId == postId).collect(Collectors.toList());
	}

	public static 


	public static void saveToDisk(){
		saveUsers();
		saveFollows();
		savePosts();
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
			writer.beginArray();
			String[] usernames = users.keySet().toArray(new String[]{});
			for(int i = 0; i < usernames.length; i++) {
				gson.toJson(users.get(usernames[i]), User.class, writer);
			}
			writer.endArray();
			gson.toJson(User.lastIDAssigned, Integer.class, writer);
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

	private static void savePosts(){
		System.out.println("Writing posts to disk...");
		String pathname = "./posts.json";
		Path path = Paths.get(pathname);
		try {
			try {
				Files.createFile(path);
			} catch (FileAlreadyExistsException ignored) {}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(pathname)));
			writer.setIndent("\t");
			writer.beginArray();
			writer.beginArray();
			for(int i = 0; i < posts.size(); i++) {
				gson.toJson(posts.get(i), Post.class, writer);
			}
			writer.endArray();
			gson.toJson(Post.lastIDAssigned, Integer.class, writer);
			writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Posts written to disk");
	}

	private static void loadFromDisk(){
		loadUsers();
		loadFollows();
		loadPosts();
	}

	private static void loadUsers(){
		System.out.println("Loading users from disk...");
		String pathname = "./users.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			reader.beginArray();
			reader.beginArray();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			while(reader.hasNext()){
				User user = gson.fromJson(reader, User.class);
				users.put(user.username, user);
			}
			reader.endArray();
			User.lastIDAssigned = gson.fromJson(reader, Integer.class);
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Users loaded");
	}

	private static void loadFollows(){
		System.out.println("Loading follows from disk...");
		String pathname = "./follows.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			reader.beginArray();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			while(reader.hasNext()){
				Pair<Integer, Integer> follow = gson.fromJson(reader, new TypeToken<Pair<Integer, Integer>>(){}.getType());
				follows.add(follow);
			}
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Follows loaded");
	}

	private static void loadPosts(){
		System.out.println("Loading posts from disk...");
		String pathname = "./posts.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			reader.beginArray();
			reader.beginArray();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			while(reader.hasNext()){
				Post post = gson.fromJson(reader, Post.class);
				posts.add(post);
			}
			reader.endArray();
			Post.lastIDAssigned = gson.fromJson(reader, Integer.class);
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Posts loaded");
	}
}
