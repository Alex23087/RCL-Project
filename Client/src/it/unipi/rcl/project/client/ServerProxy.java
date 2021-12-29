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
	public String user = null;

	private ServerProxy(){}

	private boolean connectToRMIIfNeeded(){
		if(signUpService == null){
			return connectToServerRMI();
		}else{
			return true;
		}
	}

	private boolean connectToTCPIfNeeded(){
		if(socket == null || !socket.isConnected()){
			return connectToServerTCP("localhost", 6666);
		}else{
			return true;
		}
	}

	private boolean connectToServerTCP(String address, int port){
		try {
			socket = new Socket(address, port);
			ous = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("Connected to server");
		return true;
	}

	private boolean connectToServerRMI(){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(Constants.registryPort);
			signUpService = (ISignUpService) registry.lookup(Constants.signUpServiceName);
		}catch(RemoteException | NotBoundException re){
			re.printStackTrace();
			return false;
		}
		System.out.println("Connected to RMI service");
		return true;
	}

	public boolean register(String username, String password, String[] tags){
		if(!connectToRMIIfNeeded()){
			return false;
		}
		ErrorMessage em;
		try {
			em = signUpService.signUp(username, password, tags);
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
		switch(em){
			case Success:
				return true;
			default:
				return false;
		}
	}

	public boolean login(String username, String password){
		if(!connectToTCPIfNeeded()){
			return false;
		}
		try {
			ous.writeObject(new Command(Command.Operation.Login, new String[]{username, Utils.hashString(password)}));
			ous.flush();
			switch((ErrorMessage) ois.readObject()){
				case Success:
					user = username;
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
