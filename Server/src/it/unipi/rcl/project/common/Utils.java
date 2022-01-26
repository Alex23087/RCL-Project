package it.unipi.rcl.project.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * Utility class with static methods used to perform various operations
 */
public class Utils {
	/**
	 * MessageDigest object used to hash passwords
	 */
	private static MessageDigest digest;

	static {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}


	public static String hashString(String in){
		return new String(digest.digest(in.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
	}

	/**
	 * Static method that returns a map containing the default client/server configuration
	 */
	private static Map<ConfigurationParameter, Object> getDefaultConf(){
		HashMap<ConfigurationParameter, Object> out = new HashMap<>(11);
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

	/**
	 * Static method that reads a configuration file from the specified file.
	 * If some parameter is not specified in the config file, the default value is used instead.
	 * The format for each line is
	 *     PARAMETER=value
	 */
	public static Map<ConfigurationParameter, Object> readConfFile(String path){
		File confFile = new File(path);
		//Init the map to the one with defaults
		Map<ConfigurationParameter, Object> out = getDefaultConf();
		try {
			Scanner scanner = new Scanner(confFile);
			while(scanner.hasNextLine()){
				String line = scanner.nextLine().trim();
				if(line.startsWith("#")){ //Skip comments (lines starting with #)
					continue;
				}
				int equalsIndex = line.indexOf('=');
				if(equalsIndex == -1){ //Badly formatted line, skipping it
					continue;
				}

				try {
					//Get the ConfigurationParameter value from the left side of the '=', ignoring case and whitespace
					ConfigurationParameter parameter = ConfigurationParameter.valueOf(line.substring(0, equalsIndex).trim().toUpperCase());
					//Get the value as the right side substring of the '=', removing whitespace
					String stringValue = line.substring(equalsIndex + 1).trim();
					//This will hold the value converted to the right type
					Object value;

					switch (parameter){
						//Parameters that require conversion to an integer
						case TCPPORT:
						case REGPORT:
						case MCASTPORT:
						case UDPPORT:
						case TIMEOUT:
						case REWARD_INTERVAL: {
							value = Integer.parseInt(stringValue);
							break;
						}
						//Parameter that requires conversion to double
						case AUTHOR_REWARD: {
							value = Double.parseDouble(stringValue);
							break;
						}
						//All other parameters require string values, which we already have
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
