package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.util.Arrays;

public class Command implements Serializable {
	public enum Operation{
		Login,
		GetFeed,
		GetPosts,
		PublishPost,
		Rewin,
		GetBalance,
		GetBTCConversion,
		ListUsers,
		Follow,
		GetFollowers,
		GetFollowed
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
