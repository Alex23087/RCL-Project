package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.ErrorMessage;
import it.unipi.rcl.project.common.ISignUpService;
import it.unipi.rcl.project.common.Utils;

import java.rmi.RemoteException;
import java.util.Map;

public class SignUpService implements ISignUpService {
	private Map<String, User> users;

	public SignUpService(Map<String, User> users){
		this.users = users;
	}

	@Override
	public ErrorMessage signUp(String username, String password, String[] tags) throws RemoteException {
		if(users.containsKey(username)){
			return ErrorMessage.UserAlreadyExists;
		}
		if(!isUsernameValid(username)){
			return ErrorMessage.InvalidUsername;
		}
		if(!isPasswordValid(password)){
			return ErrorMessage.InvalidPassword;
		}
		if(!isTagArrayValid(tags)){
			return ErrorMessage.InvalidTags;
		}

		ServerData.addUser(username, password, tags);
		return ErrorMessage.Success;
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
