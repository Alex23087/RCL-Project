package it.unipi.rcl.project.server;

import it.unipi.rcl.project.common.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static void main(String[] args) {

        Registry registry;
        try {
            int regport = (int) ServerData.conf.get(ConfigurationParameter.REGPORT);
            System.out.println(regport);
            registry = LocateRegistry.createRegistry(regport);
        }catch (RemoteException re){
            re.printStackTrace();
            return;
        }

        SignUpService sus = new SignUpService();
        try {
            ISignUpService sustub = (ISignUpService) UnicastRemoteObject.exportObject(sus, 0);
            registry.rebind((String) ServerData.conf.get(ConfigurationParameter.SIGNUP_SERVICE_NAME), sustub);
        }catch(RemoteException re){
            re.printStackTrace();
            return;
        }


        ExecutorService pool = Executors.newCachedThreadPool();
        ServerSocket socket;
        try {
            socket = new ServerSocket((int) ServerData.conf.get(ConfigurationParameter.TCPPORT));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        RewardHandler rewardHandler;
        try {
            rewardHandler = new RewardHandler((int) ServerData.conf.get(ConfigurationParameter.REWARD_INTERVAL));
        }catch(UnknownHostException | SocketException uhe){
            uhe.printStackTrace();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            rewardHandler.stop();
            ServerData.saveToDisk();
        }));
        rewardHandler.start();

        while(true){
            Socket clientSocket = null;
            try {
                clientSocket = socket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            pool.submit(new ClientHandler(clientSocket));
        }
    }


}