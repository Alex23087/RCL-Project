package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.Transaction;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class User implements Serializable {
	public static int lastIDAssigned = 0;

	public int id;
	public String username;
	public String password;
	public String[] tags;
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

	private static synchronized int getNewID(){
		lastIDAssigned++;
		return lastIDAssigned;
	}
}