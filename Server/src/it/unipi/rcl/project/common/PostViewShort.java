package it.unipi.rcl.project.common;

import java.io.Serializable;

/**
 * Data class used to send a short view of a Post to a client.
 * This class is used to display the list of posts when the client requests
 * the feed or the blog for its user. As such it only holds the info that
 * has to be displayed and the id of the original post, used to then retrieve
 * additional info.
 */
public class PostViewShort implements Serializable {

	/**
	 * All the fields are the same as the Post this view refers to
	 */
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
