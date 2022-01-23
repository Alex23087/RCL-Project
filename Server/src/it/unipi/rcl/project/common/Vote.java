package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Vote implements Serializable {
	public int voterId;
	public boolean upvote;

	public Vote(int voterId, boolean upvote){
		this.voterId = voterId;
		this.upvote = upvote;
	}
}
