package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static void main(String[] args) {
        Map<ConfigurationParameter, Object> conf = Utils.readConfFile("./conf.conf");
        ServerData.users = new ConcurrentHashMap<>();
        //TODO: read users from file

        Registry registry;
        try {
            int regport = (int) conf.get(ConfigurationParameter.REGPORT);
            System.out.println(regport);
            registry = LocateRegistry.createRegistry(regport);
        }catch (RemoteException re){
            re.printStackTrace();
            return;
        }

        SignUpService sus = new SignUpService(ServerData.users);
        try {
            ISignUpService sustub = (ISignUpService) UnicastRemoteObject.exportObject(sus, 0);
            registry.rebind(Constants.signUpServiceName, sustub);
        }catch(RemoteException re){
            re.printStackTrace();
            return;
        }


        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            ServerSocket socket = new ServerSocket((int) conf.get(ConfigurationParameter.TCPPORT));
            Socket clientSocket = socket.accept();
            pool.submit(new ClientHandler(clientSocket));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}