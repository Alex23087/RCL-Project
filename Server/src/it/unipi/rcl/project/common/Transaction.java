package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Transaction implements Serializable {
	public double amount;
	public long timestamp;

	public Transaction(double amount){
		this.amount = amount;
		this.timestamp = System.currentTimeMillis();
	}
}
