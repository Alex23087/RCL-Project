package it.unipi.rcl.project.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISignUpService extends Remote {
	ErrorMessage signUp(String username, String password, String[] tags) throws RemoteException;
	void registerFollowCallback(IFollowedCallbackService fcs, int userId) throws RemoteException;
}
