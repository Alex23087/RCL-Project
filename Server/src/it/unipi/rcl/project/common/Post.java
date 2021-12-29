package it.unipi.rcl.project.common;

import it.unipi.rcl.project.server.User;

import java.io.Serializable;

public class Post implements Serializable {
	private static int lastIDAssigned = 0;

	public int id;
	public int authorId;
	public String title;
	public String text;
	public boolean isRewin;
	public int rewinID;

	public Post(User author, String title, String text){
		this.authorId = author.id;
		this.title = title;
		this.text = text;
		this.isRewin = false;
		this.rewinID = -1;
		this.id = getNewID();
	}

	public Post(User author, int rewinID){
		this.authorId = author.id;
		this.title = null;
		this.text = null;
		this.isRewin = true;
		this.rewinID = rewinID;
		this.id = getNewID();
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
}
