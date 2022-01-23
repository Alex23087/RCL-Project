package it.unipi.rcl.project.common;

import it.unipi.rcl.project.common.Comment;

import java.io.Serializable;
import java.util.List;

public class PostView extends PostViewShort {
	public String text;
	public int upvotes;
	public int downvotes;
	public List<Comment> comments;
	public boolean upvoted = false;
	public boolean downvoted = false;

	public PostView(int id, int authorId, String title, String text, int upvotes, int downvotes, List<Comment> comments) {
		super(id, authorId, title);
		this.text = text;
		this.upvotes = upvotes;
		this.downvotes = downvotes;
		this.comments = comments;
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
