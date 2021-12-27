package it.unipi.rcl.project.common;

import it.unipi.rcl.project.server.ConfigurationParameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Utils {
	private static MessageDigest digest;

	static {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}


	public static String hashString(String in){
		return new String(digest.digest(in.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
	}

	private static Map<ConfigurationParameter, Object> getDefaultConf(){
		HashMap<ConfigurationParameter, Object> out = new HashMap<>(10);
		out.put(ConfigurationParameter.SERVER, "127.0.0.1");
		out.put(ConfigurationParameter.TCPPORT, 6666);
		out.put(ConfigurationParameter.UDPPORT, 33333);
		out.put(ConfigurationParameter.MULTICAST, "239.255.32.32");
		out.put(ConfigurationParameter.MCASTPORT, 44444);
		out.put(ConfigurationParameter.REGHOST, "localhost");
		out.put(ConfigurationParameter.REGPORT, 7777);
		out.put(ConfigurationParameter.TIMEOUT, 100000);
		return out;
	}

	public static Map<ConfigurationParameter, Object> readConfFile(String path){
		File confFile = new File(path);
		Map<ConfigurationParameter, Object> out = getDefaultConf();
		try {
			Scanner scanner = new Scanner(confFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return out;
		}

		//TODO: Read conf and override defaults

		return out;
	}
}
