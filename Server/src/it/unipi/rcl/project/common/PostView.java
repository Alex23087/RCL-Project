package it.unipi.rcl.project.common;

import java.util.List;

/**
 * Data class used to send information about a post to a client, without sending unnecessary information
 * such as the parameters used to calculate rewards.
 *
 * This class is used to display the post once it has been opened. As such, it can only refer to a regular
 * post, and not to a rewin.
 */
public class PostView extends PostViewShort {
	/**
	 * These fields are just like the ones in the original Post class.
	 */
	public final String text;
	public final List<Comment> comments;
	public final long timestamp;

	/**
	 * These fields hold the upvote/downvote counts, instead of having an entire list, which wouldn't be useful
	 * to the client.
	 */
	public int upvotes;
	public int downvotes;

	/**
	 * These fields are used by the client to signify the current user has upvoted/downvoted the post
	 * (and thus can't vote again)
	 */
	public boolean upvoted = false;
	public boolean downvoted = false;


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
