import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISignUpService extends Remote {
	boolean signUp(String username, String password, String[] tags) throws RemoteException;
}
