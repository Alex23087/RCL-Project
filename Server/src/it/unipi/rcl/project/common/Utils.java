package it.unipi.rcl.project.common;

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
		out.put(ConfigurationParameter.AUTHOR_REWARD, 70d);
		out.put(ConfigurationParameter.REWARD_INTERVAL, 120);
		out.put(ConfigurationParameter.SIGNUP_SERVICE_NAME, "signup_service");
		return out;
	}

	public static Map<ConfigurationParameter, Object> readConfFile(String path){
		File confFile = new File(path);
		Map<ConfigurationParameter, Object> out = getDefaultConf();
		try {
			Scanner scanner = new Scanner(confFile);
			while(scanner.hasNextLine()){
				String line = scanner.nextLine().trim();
				if(line.startsWith("#")){
					continue;
				}
				int equalsIndex = line.indexOf('=');
				if(equalsIndex == -1){ //Badly formatted line, skipping
					continue;
				}

				try {
					ConfigurationParameter parameter = ConfigurationParameter.valueOf(line.substring(0, equalsIndex).trim().toUpperCase());
					String stringValue = line.substring(equalsIndex + 1).trim();
					Object value;

					switch (parameter){
						case TCPPORT:
						case REGPORT:
						case MCASTPORT:
						case UDPPORT:
						case TIMEOUT:
						case REWARD_INTERVAL: {
							value = Integer.parseInt(stringValue);
							break;
						}
						case AUTHOR_REWARD: {
							value = Double.parseDouble(stringValue);
							break;
						}
						default: {
							value = stringValue;
						}
					}

					out.put(parameter, value);
				} catch (Exception iae){
					//Name doesn't match any configurable value, skipping
					continue;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return out;
		}

		return out;
	}
}
