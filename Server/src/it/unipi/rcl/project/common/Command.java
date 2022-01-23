package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.util.Arrays;

public class Command implements Serializable {
	public enum Operation{
		Login,
		Follow,
		GetBalance,
		GetBTCConversion,
		GetFeed,
		GetFollowed,
		GetFollowers,
		GetPostFromId,
		GetPosts,
		GetUsernameFromId,
		ListUsers,
		PublishPost,
		Rewin,
		ShowPost,
		Unfollow
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
