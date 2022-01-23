package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Comment implements Serializable {
	public int commenterId;
	public int postId;
	public String text;

	public Comment(int commenterId, int postId, String text){
		this.commenterId = commenterId;
		this.postId = postId;
		this.text = text;
	}
}
