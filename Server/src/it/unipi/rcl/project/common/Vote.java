package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Vote implements Serializable {
	public int voterId;
	public int postId;
	public boolean upvote;

	public Vote(int voterId, int postId, boolean upvote){
		this.voterId = voterId;
		this.postId = postId;
		this.upvote = upvote;
	}
}
