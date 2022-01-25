package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.IFollowedCallbackService;
import it.unipi.rcl.project.common.ISignUpService;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;

public class SignUpService implements ISignUpService {

	@Override
	public ErrorMessage signUp(String username, String password, String[] tags) throws RemoteException {
		if(ServerData.users.containsKey(username)){
			System.out.println("User tried to sign up with an already existing username: " + username);
			return ErrorMessage.UserAlreadyExists;
		}
		if(!isUsernameValid(username)){
			System.out.println("User tried to sign up with an invalid username: " + username);
			return ErrorMessage.InvalidUsername;
		}
		if(!isPasswordValid(password)){
			System.out.println("User tried to sign up with an invalid password: " + password);
			return ErrorMessage.InvalidPassword;
		}
		if(!isTagArrayValid(tags)){
			System.out.println("User tried to sign up with invalid tags: " + Arrays.toString(tags));
			return ErrorMessage.InvalidTags;
		}

		ServerData.addUser(username, password, tags);
		System.out.println("User " + username + " correctly registered");
		return ErrorMessage.Success;
	}

	@Override
	public void registerFollowCallback(IFollowedCallbackService fcs, int userId) throws RemoteException {
		if(ServerData.loggedUsers.containsKey(userId)){
			ServerData.loggedUsers.put(userId, fcs);
			System.out.println("Follow Callback registered for user " + userId);
		}
	}

	private static boolean isUsernameValid(String username){
		return true;
	}

	private static boolean isPasswordValid(String password){
		return true;
	}

	private static boolean isTagArrayValid(String[] tags){
		return true;
	}
}
