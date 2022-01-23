package it.unipi.rcl.project.common;

import java.io.Serializable;

public class PostViewShort implements Serializable {
	public int id;
	public int authorId;
	public String title;

	public PostViewShort(int id, int authorId, String title){
		this.id = id;
		this.authorId = authorId;
		this.title = title;
	}
}
