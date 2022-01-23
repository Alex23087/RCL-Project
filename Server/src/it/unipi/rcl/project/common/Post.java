package it.unipi.rcl.project.common;

import it.unipi.rcl.project.server.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Post implements Serializable {
	public static int lastIDAssigned = 0;

	public int id;
	public int authorId;
	public String title;
	public String text;
	public boolean isRewin;
	public int rewinID;
	public List<Vote> votes;
	public List<Comment> comments;

	public Post(User author, String title, String text){
		this.authorId = author.id;
		this.title = title;
		this.text = text;
		this.isRewin = false;
		this.rewinID = -1;
		this.id = getNewID();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
	}

	public Post(User author, int rewinID){
		this.authorId = author.id;
		this.title = null;
		this.text = null;
		this.isRewin = true;
		this.rewinID = rewinID;
		this.id = getNewID();
		this.votes = Collections.synchronizedList(new LinkedList<>());
		this.comments = Collections.synchronizedList(new LinkedList<>());
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
		return new PostView(this.id, this.authorId, this.title, this.text, upvoteCount, downvoteCount, Collections.unmodifiableList(this.comments));
	}

	public PostViewShort getPostViewShort(){
		return new PostViewShort(this.id, this.authorId, this.title);
	}
}
