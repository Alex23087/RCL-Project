package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that represents a single post inside the social network
 * together with all the comments and votes tied to it.
 * A Post object can represent either a regular post, or a rewin.
 */

public class Post implements Serializable {
	/**
	 * Variable used to assign a unique ID to each post
	 */
	public static volatile int lastIDAssigned = 0;

	/**
	 * These fields are used for all types of posts
	 */
	public final int id;
	public final int authorId;
	public final String title;
	public final long timestamp;

	/**
	 * If this object represents a rewin, then isRewin == true, rewinId contains the id
	 * of the post that's being rewinned, and rewinnerId contains the id of the user who
	 * rewinned the post.
	 */
	public final boolean isRewin;
	public final int rewinId;
	public final int rewinnerId;

	/**
	 * If the object is not a rewin, then isRewin == false, rewinId == rewinnerId == -1,
	 * and the parameters text, votes, and comments are used
	 */
	public final String text;
	public final List<Vote> votes;
	public final List<Comment> comments;

	/**
	 * Parameters used for the reward calculation
	 */
	public int rewardIterations;
	public double lastRewardCalculation;


	/**
	 * Constructor used to make a regular post
	 */
	public Post(int authorId, String title, String text){
		this.authorId = authorId;
		this.title = title;
		this.text = text;
		this.isRewin = false;
		this.rewinId = -1;
		this.rewinnerId = -1;
		this.id = getNewId();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
		this.timestamp = System.currentTimeMillis();
		this.rewardIterations = 0;
		this.lastRewardCalculation = 0;
	}

	/**
	 * Constructor used to make a rewin
	 */
	public Post(int authorId, int rewinID, int rewinnerId, String title){
		this.authorId = authorId;
		this.title = title;
		this.text = null;
		this.isRewin = true;
		this.rewinId = rewinID;
		this.rewinnerId = rewinnerId;
		this.id = getNewId();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
		this.timestamp = System.currentTimeMillis();
		this.rewardIterations = 0;
		this.lastRewardCalculation = 0;
	}

	/**
	 * Method that returns a new increasing ID at each invocation
	 */
	private static synchronized int getNewId(){
		lastIDAssigned++;
		return lastIDAssigned;
	}

	@Override
	public String toString() {
		return "Post{" +
				"id=" + id +
				", authorId=" + authorId +
				", text='" + text + '\'' +
				", isRewin=" + isRewin +
				", rewinID=" + rewinId +
				'}';
	}

	/**
	 * Counts the number of votes of the specified type
	 * @param upvotes if true, counts the number of upvotes, if false it counts downvotes
	 */
	private int getVoteCount(boolean upvotes){
		return votes.stream().reduce(0, (count, vote) -> count + (vote.upvote == upvotes ? 1 : 0), Integer::sum);
	}

	public int getUpvoteCount(){
		return getVoteCount(true);
	}

	public int getDownvoteCount(){
		return getVoteCount(false);
	}

	/**
	 * Returns a PostView with information on this post
	 */
	public PostView getPostView(){
		int upvoteCount = getUpvoteCount();
		int downvoteCount = votes.size() - upvoteCount;
		return new PostView(this.id, this.authorId, this.title, this.text, upvoteCount, downvoteCount, this.comments, this.timestamp, this.isRewin ? this.rewinnerId : -1);
	}

	/**
	 * Returns a PostViewShort with information on this post
	 */
	public PostViewShort getPostViewShort(){
		return new PostViewShort(this.id, this.authorId, this.title, this.timestamp, this.isRewin ? this.rewinnerId : -1);
	}

	/**
	 * Returns a new Post that represents a rewin to this post
	 */
	public Post makeRewin(int rewinnerId){
		return new Post(this.authorId, this.id, rewinnerId, this.title);
	}
}
