package it.unipi.rcl.project.common;

import it.unipi.rcl.project.server.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Post implements Serializable {
	public static int lastIDAssigned = 0;

	public final int id;
	public final int authorId;
	public final String title;
	public final String text;
	public final boolean isRewin;
	public final int rewinID;
	public final int rewinnerId;
	public final List<Vote> votes;
	public final List<Comment> comments;
	public final long timestamp;

	public Post(int authorId, String title, String text){
		this.authorId = authorId;
		this.title = title;
		this.text = text;
		this.isRewin = false;
		this.rewinID = -1;
		this.rewinnerId = -1;
		this.id = getNewID();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
		this.timestamp = System.currentTimeMillis();
	}

	public Post(int authorId, int rewinID, int rewinnerId, String title){
		this.authorId = authorId;
		this.title = title;
		this.text = null;
		this.isRewin = true;
		this.rewinID = rewinID;
		this.rewinnerId = rewinnerId;
		this.id = getNewID();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
		this.timestamp = System.currentTimeMillis();
	}

	private static synchronized int getNewID(){
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
				", rewinID=" + rewinID +
				'}';
	}

	private int getVoteCount(boolean upvotes){
		return votes.stream().reduce(0, (count, vote) -> count + (vote.upvote == upvotes ? 1 : 0), Integer::sum);
	}

	public int getUpvoteCount(){
		return getVoteCount(true);
	}

	public int getDownvoteCount(){
		return getVoteCount(false);
	}

	public PostView getPostView(){
		int upvoteCount = getUpvoteCount();
		int downvoteCount = votes.size() - upvoteCount;
		return new PostView(this.id, this.authorId, this.title, this.text, upvoteCount, downvoteCount, this.comments, this.timestamp, this.isRewin ? this.rewinnerId : -1);
	}

	public PostViewShort getPostViewShort(){
		return new PostViewShort(this.id, this.authorId, this.title, this.timestamp, this.isRewin ? this.rewinnerId : -1);
	}

	public Post makeRewin(int rewinnerId){
		return new Post(this.authorId, this.id, rewinnerId, this.title);
	}
}
