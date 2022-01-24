package it.unipi.rcl.project.common;

import java.io.Serializable;

public class Transaction implements Serializable {
	double amount;
	long timestamp;

	public Transaction(double amount){
		this.amount = amount;
		this.timestamp = System.currentTimeMillis();
	}
}
