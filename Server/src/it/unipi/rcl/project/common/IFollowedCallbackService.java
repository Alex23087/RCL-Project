package it.unipi.rcl.project.common;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for the client RMI callback object
 */
public interface IFollowedCallbackService extends Remote, Serializable {
	void notifyFollow(int userId) throws RemoteException;
	void notifyUnfollow(int userId) throws RemoteException;
}
