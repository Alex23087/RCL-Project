package it.unipi.rcl.project.common;

import java.io.Serializable;

/**
 * Data class that represents a single vote to one post in the social network.
 */
public class Vote implements Serializable {
	public final int voterId;
	public final boolean upvote; //Upvote or downvote
	public final long timestamp;


	public Vote(int voterId, boolean upvote){
		this.voterId = voterId;
		this.upvote = upvote;
		this.timestamp = System.currentTimeMillis();
	}
}
