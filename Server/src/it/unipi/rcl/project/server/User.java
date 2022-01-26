package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Transaction;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that represents a user inside the social network
 */
public class User implements Serializable {
	/**
	 * Variable used to assign a unique ID to each user
	 */
	public static volatile int lastIDAssigned = 0;

	public int id;
	public String username;
	public String password;
	public String[] tags;

	/**
	 * Wallet fields
	 */
	public double balance;
	public List<Transaction> transactions;

	public User(String username, String password, String[] tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.balance = 0;
		this.id = getNewID();
		this.transactions = Collections.synchronizedList(new LinkedList<>());
	}

	/**
	 * Method that returns a new increasing ID at each invocation
	 */
	private static synchronized int getNewID(){
		lastIDAssigned++;
		return lastIDAssigned;
	}
}