package it.unipi.rcl.project.common;

import java.io.Serializable;

public class PostViewShort implements Serializable {
	public final int id;
	public final int authorId;
	public final String title;
	public final long timestamp;
	public final int rewinnerId;

	public PostViewShort(int id, int authorId, String title, long timestamp, int rewinnerId){
		this.id = id;
		this.authorId = authorId;
		this.title = title;
		this.timestamp = timestamp;
		this.rewinnerId = rewinnerId;
	}
}
