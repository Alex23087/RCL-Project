package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.IFollowedCallbackService;
import it.unipi.rcl.project.common.ISignUpService;

import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * RMI service to register a user on the platform
 */
public class SignUpService implements ISignUpService {

	/**
	 * Signs a user up
	 */
	@Override
	public ErrorMessage signUp(String username, String password, String[] tags) throws RemoteException {
		//Remove whitespace from the start and end of the username and the password
		username = username.trim();
		password = password.trim();
		if(ServerData.getUserId(username) != -1){
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

	/**
	 * Register the RMI callback service to notify users that someone has followed/unfollowed them.
	 * Only registers the service if the associated user is logged in.
	 */
	@Override
	public void registerFollowCallback(IFollowedCallbackService fcs, int userId) throws RemoteException {
		if(ServerData.loggedUsers.containsKey(userId)){
			ServerData.loggedUsers.put(userId, fcs);
			System.out.println("Follow Callback registered for user " + userId);
		}
	}

	private static boolean isUsernameValid(String username){
		return username != null && username.trim().length() > 0;
	}

	private static boolean isPasswordValid(String password){
		return password != null && password.length() > 0;
	}

	private static boolean isTagArrayValid(String[] tags){
		return tags != null && tags.length > 0;
	}
}
