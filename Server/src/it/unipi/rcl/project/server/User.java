package it.unipi.rcl.project.server;

public class User {
	public String username;
	public String password;
	public String[] tags;

	public User(String username, String password, String[] tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
	}
}
