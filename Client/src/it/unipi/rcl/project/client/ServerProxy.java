package it.unipi.rcl.project.client;

import it.unipi.rcl.project.common.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerProxy{
	public static ServerProxy instance = new ServerProxy();
	private ISignUpService signUpService;
	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream ous;

	private ServerProxy(){
		System.out.println("a");
		connectToServerRMI();
		connectToServerTCP("localhost", 6666);
	}

	private static void register(String username, String password, String[] tags, ISignUpService sus){
		try {
			System.out.println(sus.signUp(username, password, tags));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void connectToServerTCP(String address, int port){
		try {
			socket = new Socket(address, port);
			ous = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Connected to server");
	}

	private void connectToServerRMI(){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(Constants.registryPort);
			signUpService = (ISignUpService) registry.lookup(Constants.signUpServiceName);
		}catch(RemoteException | NotBoundException re){
			re.printStackTrace();
		}
	}

	public boolean register(String username, String password, String[] tags){
		register(username, password, tags, signUpService);
		return true;
	}

	public boolean login(String username, String password){
		try {
			ous.writeObject(new Command(Command.Operation.Login, new String[]{username, Utils.hashString(password)}));
			ous.flush();
			switch((ErrorMessage) ois.readObject()){
				case Success:
					return true;
				default:
					return false;
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
}
