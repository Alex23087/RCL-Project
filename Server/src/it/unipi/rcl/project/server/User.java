package it.unipi.rcl.project.server;

import java.io.Serializable;

public class User implements Serializable {
	public static int lastIDAssigned = 0;

	public int id;
	public String username;
	public String password;
	public String[] tags;
	public long balance;

	public User(String username, String password, String[] tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.balance = 428753;
		this.id = getNewID();
	}

	private static synchronized int getNewID(){
		lastIDAssigned++;
		return lastIDAssigned;
	}
}