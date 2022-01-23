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

	static{
		users = new ConcurrentHashMap<>();
		loggedUsers = Collections.synchronizedList(new LinkedList<>());
		posts = Collections.synchronizedList(new LinkedList<>());
		follows = Collections.synchronizedList(new LinkedList<>());
		loadFromDisk();
	}

	public static List<Pair<Integer, String[]>> getUsersWithTags(String[] tags){
		return users.values().stream().filter(user -> Arrays.stream(user.tags).anyMatch(t -> Arrays.asList(tags).contains(t))).map(user -> new Pair<>(user.id, user.tags)).collect(Collectors.toList());
	}

	public static void addUser(String username, String password, String[] tags){
		users.put(username, new User(username, Utils.hashString(password), tags));
		System.out.println("New user with username " + username + " successfully registered to the platform");
	}

	public static List<PostViewShort> getFeed(User user){
		List<Integer> followedList = follows.stream().filter(followRelationship -> followRelationship.first == user.id).map(f -> f.second).collect(Collectors.toList());
		return posts.stream().filter(post -> followedList.contains(post.authorId)).sorted(Comparator.comparing(c -> -(c.id))).map(Post::getPostViewShort).collect(Collectors.toList());
	}

	public static List<PostViewShort> getPosts(User user){
		return posts.stream().filter(post -> post.authorId == user.id).sorted(Comparator.comparing(c -> -(c.id))).map(Post::getPostViewShort).collect(Collectors.toList());
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

	public static boolean userFollows(int follower, int followed){
		return follows.stream().anyMatch(f -> f.first == follower && f.second == followed);
	}

	public static boolean follow(int follower, int followed){
		if(follower == followed){
			return false;
		}
		if(userFollows(follower, followed)){
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

	public static PostView getPostViewWithId(int postId, int userId){
		Post p = getPostWithId(postId);
		if(p == null){
			return null;
		}else{
			PostView out = p.getPostView();
			Vote v = p.votes.stream().filter(vote -> vote.voterId == userId).findFirst().orElse(null);
			if(v != null){
				if(v.upvote){
					out.setUpvoted();
				}else{
					out.setDownvoted();
				}
			}
			return out;
		}
	}

	public static PostViewShort getPostViewShortWithId(int postId){
		Post p = getPostWithId(postId);
		if(p == null){
			return null;
		}else{
			return p.getPostViewShort();
		}
	}

	public static List<Comment> getComments(int postId){
		Post p = getPostWithId(postId);
		if(p == null){
			return new LinkedList<>();
		}
		return Collections.unmodifiableList(p.comments);
	}

	public static ErrorMessage vote(int postId, int userId, boolean upvote){
		Post p = getPostWithId(postId);
		if(p == null) {
			return ErrorMessage.InvalidPostId;
		}
		if(p.authorId == userId){
			return ErrorMessage.VoterIsAuthor;
		}
		if(p.votes.stream().anyMatch(vote -> vote.voterId == userId)){
			return ErrorMessage.AlreadyVoted;
		}

		p.votes.add(new Vote(userId, upvote));
		return ErrorMessage.Success;
	}

	public static ErrorMessage addComment(int postId, int userId, String text){
		Post p = getPostWithId(postId);
		if(p == null){
			return ErrorMessage.InvalidPostId;
		}
		p.comments.add(new Comment(userId, text));
		return ErrorMessage.Success;
	}

	public static ErrorMessage deletePost(int postId, int userId){
		Post p = getPostWithId(postId);
		if(p == null){
			return ErrorMessage.InvalidPostId;
		}
		if(p.authorId != userId){
			return ErrorMessage.UserNotAuthor;
		}

		posts.removeIf(post -> (post.id == postId) || (post.isRewin && post.rewinID == p.id));
		return ErrorMessage.Success;
	}


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
