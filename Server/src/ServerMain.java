import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class ServerMain {

    public static void main(String[] args) {
        HashMap<ConfigurationParameter, Object> conf = Utils.readConfFile("./conf.conf");

        Registry registry;
        try {
            int regport = (int) conf.get(ConfigurationParameter.REGPORT);
            System.out.println(regport);
            registry = LocateRegistry.createRegistry(regport);
        }catch (RemoteException re){
            re.printStackTrace();
            return;
        }

        SignUpService sus = new SignUpService();
        try {
            ISignUpService sustub = (ISignUpService) UnicastRemoteObject.exportObject(sus, 0);
            registry.rebind(Constants.signUpServiceName, sustub);
        }catch(RemoteException re){
            re.printStackTrace();
            return;
        }

    }
}
