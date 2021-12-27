import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {

	public static final int registryPort = 9999;

    public static void main(String[] args) {
	    Registry registry;
		ISignUpService sus;
		try {
			registry = LocateRegistry.getRegistry(registryPort);
			sus = (ISignUpService) registry.lookup(Parameters.signUpServiceName);
		}catch(RemoteException | NotBoundException re){
			re.printStackTrace();
			return;
		}
		register("User", "Pass", new String[]{"test"}, sus);
    }

	public static void register(String username, String password, String[] tags, ISignUpService sus){

		try {
			System.out.println(sus.signUp(username, password, tags));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
