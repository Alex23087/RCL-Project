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

/**
 * Class that handles the server initialisation
 */
public class ServerMain {

    public static void main(String[] args) {

        /*
         * RMI registry initialisation
         */
        Registry rmiRegistry;
        try {
            int registryPort = (int) ServerData.conf.get(ConfigurationParameter.REGPORT);
            rmiRegistry = LocateRegistry.createRegistry(registryPort);
            System.out.println("RMI registry created on port " + registryPort);
        }catch (RemoteException re){
            re.printStackTrace();
            return;
        }

        /*
         * Publishing service on the registry
         */
        SignUpService sus = new SignUpService();
        try {
            ISignUpService sustub = (ISignUpService) UnicastRemoteObject.exportObject(sus, 0);
            String serviceName = (String) ServerData.conf.get(ConfigurationParameter.SIGNUP_SERVICE_NAME);
            rmiRegistry.rebind(serviceName, sustub);
            System.out.println("Published SignUpService as \"" + serviceName + "\"");
        }catch(RemoteException re){
            re.printStackTrace();
            return;
        }

        /*
         * Starting thread pool and listening socket
         */
        ExecutorService pool = Executors.newCachedThreadPool();
        ServerSocket socket;
        try {
            int socketPort = (int) ServerData.conf.get(ConfigurationParameter.TCPPORT);
            socket = new ServerSocket(socketPort);
            System.out.println("Started listening on TCP port " + socketPort);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        /*
         * Initialising RewardHandler
         */
        RewardHandler rewardHandler;
        try {
            rewardHandler = new RewardHandler((int) ServerData.conf.get(ConfigurationParameter.REWARD_INTERVAL));
        }catch(UnknownHostException | SocketException e){
            e.printStackTrace();
            return;
        }

        /*
         * Adding shutdown hook to save server data when the server is stopped
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            rewardHandler.stop();
            ServerData.saveToDisk();
        }));
        rewardHandler.start();

        /*
         * Accept connections loop
         */
        while(true){
            try {
                Socket clientSocket = socket.accept();
                pool.submit(new ClientHandler(clientSocket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}