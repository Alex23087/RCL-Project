package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Vote implements Serializable {
	public final int voterId;
	public final boolean upvote;
	public final long timestamp;

	public Vote(int voterId, boolean upvote){
		this.voterId = voterId;
		this.upvote = upvote;
		this.timestamp = System.currentTimeMillis();
	}
}
