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
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class that holds all the data for the server
 */
public class ServerData {
	static Map<Integer, User> users;
	//Users that are logged in, associated with their callback services
	static Map<Integer, IFollowedCallbackService> loggedUsers;
	static List<Post> posts;
	//List of the follow relationships in the server. The first element is followerID, second element is followedID
	static List<Pair<Integer, Integer>> follows;
	//Map of the configurable values set with the config file
	static Map<ConfigurationParameter, Object> conf = Utils.readConfFile("./conf.conf");


	static{
		users = new ConcurrentHashMap<>();
		loggedUsers = new ConcurrentHashMap<>();
		posts = Collections.synchronizedList(new LinkedList<>());
		follows = Collections.synchronizedList(new LinkedList<>());
		loadFromDisk();
	}

	/**
	 * Returns a list of all the users that have at least one tag in common with the ones passed as argument
	 * @return a list of pairs where the first element is the userId, the second one is the array of tags associated with that user
	 */
	public static List<Pair<Integer, String[]>> getUsersWithTags(String[] tags){
		return users.values().stream()
				.filter(user -> Arrays.stream(user.tags).anyMatch(t -> Arrays.asList(tags).contains(t)))
				.map(user -> new Pair<>(user.id, user.tags)).collect(Collectors.toList());
	}

	/**
	 * Registers a new user to the platform. Checks on the validity of the username and
	 * the password are done in the SignUpService
	 */
	public static void addUser(String username, String password, String[] tags){
		User newUser = new User(username, Utils.hashString(password), tags);
		users.put(newUser.id, newUser);
	}

	/**
	 * Returns a list of PostViewShorts with data for all posts where the user follows the poster (if regular post)
	 * or the rewinner of the post (if rewin). The posts are then sorted in decreasing timestamp order.
	 */
	public static List<PostViewShort> getFeed(int userId){
		List<Integer> followedUsers = getFollowed(userId);
		return posts.stream()
				.filter(post -> post.isRewin ? followedUsers.contains(post.rewinnerId) : followedUsers.contains(post.authorId))
				.sorted(Comparator.comparing(c -> -(c.timestamp))).map(Post::getPostViewShort).collect(Collectors.toList());
	}
	/**
	 * Returns a list of PostViewShorts with data for all posts posted by the user (if regular post)
	 * or rewinned by the user (if rewin). The posts are then sorted in decreasing timestamp order.
	 */
	public static List<PostViewShort> getPosts(int userId){
		return posts.stream()
				.filter(post -> post.isRewin ? post.rewinnerId == userId : post.authorId == userId)
				.sorted(Comparator.comparing(c -> -(c.timestamp))).map(Post::getPostViewShort).collect(Collectors.toList());
	}

	/**
	 * Adds a new post to the social network
	 */
	public static int addPost(int authorId, String title, String text){
		Post newPost = new Post(authorId, title, text);
		posts.add(0, newPost);
		return newPost.id;
	}

	/**
	 * Returns the user that has the id passed as parameter if exists. Null otherwise
	 */
	public static User getUser(int userId){
		return users.getOrDefault(userId, null);
	}

	/**
	 * Returns the userId of the user that has this username. -1 if there is no user with this username.
	 * Note: this method only makes sense because usernames are unique.
	 */
	public static int getUserId(String username){
		return users.values().stream().filter(user -> user.username.equals(username)).map(user -> user.id).findFirst().orElse(-1);
	}

	/**
	 * Returns true if follower follows followed. False otherwise
	 */
	public static boolean userFollows(int follower, int followed){
		return follows.stream().anyMatch(f -> f.first == follower && f.second == followed);
	}

	/**
	 * Adds a new pair representing a follow relationship to the social network data.
	 * Checks are made to ensure users cannot follow themselves,
	 * that no duplicates are added, and
	 * that the user that will be followed exists.
	 */
	public static ErrorMessage follow(int follower, int followed){
		if(follower == followed){
			return ErrorMessage.FollowingYourself;
		}
		if(userFollows(follower, followed)){
			return ErrorMessage.AlreadyFollowed;
		}
		if(getUser(followed) == null){
			return ErrorMessage.NoSuchUser;
		}

		follows.add(new Pair<>(follower, followed));

		//Notifying the followed user
		IFollowedCallbackService fcs = loggedUsers.get(followed);
		if(fcs != null){
			try {
				fcs.notifyFollow(follower);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return ErrorMessage.Success;
	}

	/**
	 * Removing a follow relationship between follower and followed.
	 */
	public static ErrorMessage unfollow(int follower, int followed){
		if(follower == followed){
			return ErrorMessage.FollowingYourself;
		}
		Iterator<Pair<Integer, Integer>> iterator = follows.iterator();
		while(iterator.hasNext()){
			Pair<Integer, Integer> follow = iterator.next();
			if(follow.first == follower && follow.second == followed){
				iterator.remove();
				//Notifying the unfollowed user
				IFollowedCallbackService fcs = loggedUsers.get(followed);
				if(fcs != null){
					try {
						fcs.notifyUnfollow(follower);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				return ErrorMessage.Success;
			}
		}
		//No pair representing a follow relationship between follower and followed has been found
		return ErrorMessage.NotFollowing;
	}

	/**
	 * Returns the list of people the user follows.
	 */
	public static List<Integer> getFollowed(int userId){
		return follows.stream()
				.filter(followPair -> followPair.first == userId)
				.map(followPair -> followPair.second).collect(Collectors.toList());
	}

	/**
	 * Returns the list of people that follow the user
	 */
	public static List<Integer> getFollowers(int userId){
		return follows.stream()
				.filter(followPair -> followPair.second == userId)
				.map(followPair -> followPair.second).collect(Collectors.toList());
	}

	/**
	 * Returns the Post that has the is passed as parameter.
	 * If recursive == true and the Post is a rewin, then the method is called recursively
	 * until it returns the original post that has been rewinned.
	 * If recursive == false, it returns the post even if it's a rewin
	 *
	 * @throws NonexistentPostException If there is no Post with this id
	 */
	public static Post getPostWithId(int postId, boolean recursive) throws NonexistentPostException{
		Post post = posts.stream()
				.filter(p -> p.id == postId)
				.findFirst().orElseThrow(NonexistentPostException::new);

		if(recursive && post.isRewin){
			return getPostWithId(post.rewinId, true);
		}else{
			return post;
		}
	}

	/**
	 * Overloaded method that always returns the original post (by calling getPostWithId with recursive = true).
	 */
	public static Post getPostWithId(int postId) throws NonexistentPostException{
		return getPostWithId(postId, true);
	}

	/**
	 * Returns a PostView of the Post with the id passed as parameter
	 * @param userId Id of the user that is requesting the PostView. Used to set the upvote/downvote variables in the view.
	 * @throws NonexistentPostException
	 */
	public static PostView getPostViewWithId(int postId, int userId) throws NonexistentPostException{
		Post p = getPostWithId(postId);
		PostView out = p.getPostView();

		//Finding if the user voted on this post
		Vote v = p.votes.stream()
				.filter(vote -> vote.voterId == userId)
				.findFirst().orElse(null);

		if(v != null){
			//Setting the view variables accordingly
			if(v.upvote){
				out.setUpvoted();
			}else{
				out.setDownvoted();
			}
		}
		return out;
	}

	/**
	 * Returns a PostViewShort of the Post with id passed as parameter.
	 */
	public static PostViewShort getPostViewShortWithId(int postId) throws NonexistentPostException{
		return getPostWithId(postId).getPostViewShort();
	}

	/**
	 * Returns the list of comments posted for this post.
	 */
	public static List<Comment> getComments(int postId) throws NonexistentPostException{
		Post p = getPostWithId(postId);
		return Collections.unmodifiableList(p.comments);
	}

	/**
	 * Adds a vote to the post. Checks if the vote is valid (i.e.
	 * if the voter is not the author and
	 * if the voter has not voted yet on this post)
	 */
	public static ErrorMessage vote(int postId, int userId, boolean upvote) throws NonexistentPostException{
		Post p = getPostWithId(postId);
		if(p.authorId == userId){
			return ErrorMessage.VoterIsAuthor;
		}
		if(p.votes.stream().anyMatch(vote -> vote.voterId == userId)){
			return ErrorMessage.AlreadyVoted;
		}

		p.votes.add(new Vote(userId, upvote));
		return ErrorMessage.Success;
	}

	/**
	 * Adds a comment to a post. Checks if the user that's posting is the author of the post.
	 */
	public static ErrorMessage addComment(int postId, int userId, String text) throws NonexistentPostException{
		Post p = getPostWithId(postId);
		if(p.authorId == userId){
			return ErrorMessage.CommentingOwnPost;
		}
		p.comments.add(new Comment(userId, text));
		return ErrorMessage.Success;
	}

	/**
	 * Deletes a post.
	 * If the post is a rewin, checks that the rewinner is the user who's trying to delete it.
	 * If the post is not a rewin, checks that the original poster is the user who's trying to delete it.
	 * Deletes all the rewins that are rewinning the post to be deleted.
	 */
	public static ErrorMessage deletePost(int postId, int userId) throws NonexistentPostException{
		Post p = getPostWithId(postId, false);
		if(p.isRewin ? p.rewinnerId != userId : p.authorId != userId){
			return ErrorMessage.UserNotAuthor;
		}

		posts.removeIf(post -> (post.id == postId) || (post.isRewin && post.rewinId == p.id));
		return ErrorMessage.Success;
	}

	/**
	 * Rewins a post. Users cannot rewin their own posts.
	 */
	public static ErrorMessage rewin(int rewinnerId, int postId) throws NonexistentPostException{
		Post p = getPostWithId(postId);
		if(p.authorId == rewinnerId){
			return ErrorMessage.RewinningOwnPost;
		}
		posts.add(p.makeRewin(rewinnerId));
		return ErrorMessage.Success;
	}

	/**
	 * Checks if a user is logged in.
	 */
	public static boolean isLoggedIn(String username){
		int userId = getUserId(username);
		return loggedUsers.containsKey(userId);
	}

	/**
	 * Logs a user out.
	 */
	public static void logout(int userId){
		loggedUsers.remove(userId);
	}


	/**
	 * Saves all server data to disk.
	 */
	public static void saveToDisk(){
		File dir = new File("./data/");
		if (!dir.exists()){
			dir.mkdirs();
		}
		saveUsers();
		saveFollows();
		savePosts();
	}

	/**
	 * Saves the user data to disk
	 */
	private static void saveUsers(){
		System.out.println("Writing users to disk...");
		String pathname = "./data/users.json";
		Path path = Paths.get(pathname);
		try {
			try {
				Files.createFile(path);
			} catch (FileAlreadyExistsException ignored) {}

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(pathname)));

			writer.setIndent("\t");
			//The outer array is used to contain the inner user array and the last userId assigned
			writer.beginArray();
			writer.beginArray();
			Integer[] userIds = users.keySet().toArray(new Integer[]{});
			for(int i = 0; i < userIds.length; i++) {
				gson.toJson(users.get(userIds[i]), User.class, writer);
			}
			writer.endArray();
			//Saving the last Id assigned, so it can be restored
			gson.toJson(User.lastIDAssigned, Integer.class, writer);
			writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Users written to disk");
	}

	/**
	 * Saves the follow data to disk.
	 */
	private static void saveFollows(){
		System.out.println("Writing follows to disk...");
		String pathname = "./data/follows.json";
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

	/**
	 * Saves the posts to disk.
	 */
	private static void savePosts(){
		System.out.println("Writing posts to disk...");
		String pathname = "./data/posts.json";
		Path path = Paths.get(pathname);
		try {
			try {
				Files.createFile(path);
			} catch (FileAlreadyExistsException ignored) {}

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(pathname)));

			writer.setIndent("\t");
			//The outer array is used to contain the inner post array and the last postId assigned
			writer.beginArray();
			writer.beginArray();
			for(int i = 0; i < posts.size(); i++) {
				gson.toJson(posts.get(i), Post.class, writer);
			}
			writer.endArray();
			//Saving the last id assigned so it can be restored later
			gson.toJson(Post.lastIDAssigned, Integer.class, writer);
			writer.endArray();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Posts written to disk");
	}

	/**
	 * Loads all server data from disk (if saved data exists)
	 */
	private static void loadFromDisk(){
		loadUsers();
		loadFollows();
		loadPosts();
	}

	/**
	 * Loads all the user data from disk.
	 */
	private static void loadUsers(){
		System.out.println("Loading users from disk...");
		String pathname = "./data/users.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			reader.beginArray();
			reader.beginArray();
			while(reader.hasNext()){
				User user = gson.fromJson(reader, User.class);
				users.put(user.id, user);
			}
			reader.endArray();
			//Restoring the last Id assigned so it doesn't reset
			User.lastIDAssigned = gson.fromJson(reader, Integer.class);
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Users loaded");
	}

	/**
	 * Loads all follow data from disk.
	 */
	private static void loadFollows(){
		System.out.println("Loading follows from disk...");
		String pathname = "./data/follows.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			reader.beginArray();
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

	/**
	 * Loads all post data from disk.
	 */
	private static void loadPosts(){
		System.out.println("Loading posts from disk...");
		String pathname = "./data/posts.json";
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(pathname)))){
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			reader.beginArray();
			reader.beginArray();
			while(reader.hasNext()){
				Post post = gson.fromJson(reader, Post.class);
				posts.add(post);
			}
			reader.endArray();
			//Restoring the last Id assigned so it doesn't reset
			Post.lastIDAssigned = gson.fromJson(reader, Integer.class);
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Posts loaded");
	}
}
