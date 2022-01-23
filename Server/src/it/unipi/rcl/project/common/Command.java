package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.util.Arrays;

public class Command implements Serializable {
	public enum Operation{
		AddComment,
		DeletePost,
		Follow,
		GetBalance,
		GetBTCConversion,
		GetFeed,
		GetFollowed,
		GetFollowers,
		GetPosts,
		GetPostViewFromId,
		GetUsernameFromId,
		ListUsers,
		Login,
		Logout,
		PublishPost,
		Rewin,
		Unfollow,
		Vote
	}

	public Operation op;
	public String[] parameters;

	public Command(Operation op, String[] parameters){
		this.op = op;
		this.parameters = parameters;
	}

	@Override
	public String toString(){
		return op.name() + " " + Arrays.toString(parameters);
	}
}
