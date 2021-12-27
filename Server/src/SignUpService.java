import java.rmi.RemoteException;
import java.util.Arrays;

public class SignUpService implements ISignUpService {
	@Override
	public boolean signUp(String username, String password, String[] tags) throws RemoteException {
		System.out.println(username);
		System.out.println(Utils.hashString(password));
		System.out.println(Arrays.toString(tags));
		return false;
	}
}
