package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Class to encode the commands the client sends to the server
 */

public class Command implements Serializable {
	public enum Operation{ //API commands
		AddComment,
		CreatePost,
		DeletePost,
		FollowUser,
		GetBalance,
		GetBalanceInBTC,
		GetTransactions,
		GetUsernameFromId,
		ListFollowers,
		ListFollowing,
		ListUsers,
		Login,
		Logout,
		RatePost,
		RewinPost,
		ShowFeed,
		ShowPost,
		UnfollowUser,
		ViewBlog
	}

	public Operation op;
	public String[] parameters; //Null or empty if the command doesn't require parameters


	public Command(Operation op, String[] parameters){
		this.op = op;
		this.parameters = parameters;
	}

	@Override
	public String toString(){
		return op.name() + " " + Arrays.toString(parameters);
	}
}
