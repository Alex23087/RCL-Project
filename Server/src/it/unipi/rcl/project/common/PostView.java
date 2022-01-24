package it.unipi.rcl.project.common;

import java.util.List;

public class PostView extends PostViewShort {
	public final String text;
	public int upvotes;
	public int downvotes;
	public final List<Comment> comments;
	public boolean upvoted = false;
	public boolean downvoted = false;
	public final long timestamp;

	public PostView(int id, int authorId, String title, String text, int upvotes, int downvotes, List<Comment> comments, long timestamp, int rewinnerId) {
		super(id, authorId, title, timestamp, rewinnerId);
		this.text = text;
		this.upvotes = upvotes;
		this.downvotes = downvotes;
		this.comments = comments;
		this.timestamp = timestamp;
	}

	public void setUpvoted(){
		upvoted = true;
		downvoted = false;
	}

	public void setDownvoted(){
		upvoted = false;
		downvoted = true;
	}
}
