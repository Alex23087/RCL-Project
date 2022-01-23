package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Comment implements Serializable {
	public final int commenterId;
	public final String text;
	public final long timestamp;

	public Comment(int commenterId, String text){
		this.commenterId = commenterId;
		this.text = text;
		this.timestamp = System.currentTimeMillis();
	}
}
