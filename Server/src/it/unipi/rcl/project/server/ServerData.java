package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Post;
import it.unipi.rcl.project.common.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerData {
	static Map<String, User> users;
	static List<Post> posts;
	static List<Pair<Integer, Integer>> follows; //First element is followerID, second element is followedID

	static{
		users = new ConcurrentHashMap<>();
		posts = Collections.synchronizedList(new LinkedList<>());
		follows = Collections.synchronizedList(new LinkedList<>());
		loadFromDisk();
	}

	public static List<User> getUsersWithTags(String[] tags){
		//TODO: implement
		return null;
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

	private static void saveToDisk(){

	}

	private static void loadFromDisk(){
		//TODO: read users and posts from file
	}
}
