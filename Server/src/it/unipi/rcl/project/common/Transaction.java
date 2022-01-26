package it.unipi.rcl.project.common;

import java.io.Serializable;

/**
 * Data class that holds information about a single transaction generated
 * by the reward handler.
 */
public class Transaction implements Serializable {
	public double amount;
	public long timestamp;


	public Transaction(double amount){
		this.amount = amount;
		this.timestamp = System.currentTimeMillis();
	}
}
