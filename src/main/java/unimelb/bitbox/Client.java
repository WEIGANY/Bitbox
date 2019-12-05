package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import unimelb.bitbox.CmdLineArg;
import unimelb.bitbox.util.AES;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.RSA;

public class Client
{
	/*
	 * process the client function
	 */
	public static void main(String[] args) throws Exception
	{

		// Object that will store the parsed command line arguments
		CmdLineArg argsBean = new CmdLineArg();

		// Parser provided by args4j
		CmdLineParser parser = new CmdLineParser(argsBean);
		byte[] key = null;
		try
		{

			// Parse the arguments
			parser.parseArgument(args);

			// After parsing, the fields in argsBean have been updated with the given
			// command line arguments

			// TODO create secure connection with the server
			System.out.println("Command : " + argsBean.getCmd());
			String[] address = argsBean.getServer().split(":");
			Socket clientsocket = new Socket(address[0], Integer.parseInt(address[1]));
			JSONObject AUTH_REQUEST = new JSONObject();
			AUTH_REQUEST.put("command", "AUTH_REQUEST");
			String[] a= Configuration.getConfigurationValue("authorized_keys").split(" ");
			String id = a[2];
			AUTH_REQUEST.put("identity", id);
			sockethandle.sendSocketMessage(AUTH_REQUEST.toJSONString(), clientsocket);
			String response = sockethandle.getSocketMessage(clientsocket);
			System.out.println(response);
			JSONParser parser1 = new JSONParser();
			JSONObject message = (JSONObject) parser1.parse(response);
			if (message.get("command").toString().equals("AUTH_RESPONSE"))
			{
				if (message.get("status").toString().equals("true"))
				{
					String aes = message.get("AES128").toString();
					String decodedData = RSA.privateDecrypt1(aes, RSA.getPrivateKey1(RSA.PRIVATE_KEY_PATH));
					key = Base64.getDecoder().decode(decodedData);

					System.out.println(argsBean.getCmd());
					if (argsBean.getCmd().equals("list_peers"))
					{
						JSONObject res = new JSONObject();
						res.put("command", "LIST_PEERS_REQUEST");
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientsocket);
					}
					if (argsBean.getCmd().equals("connect_peer"))
					{
						String[] peer = argsBean.getPeer().split(":");
						JSONObject res = new JSONObject();
						res.put("command", "CONNECT_PEER_REQUEST");
						res.put("host", peer[0]);
						res.put("port", Integer.parseInt(peer[1]));
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientsocket);
					}
					if (argsBean.getCmd().equals("disconnect_peer"))
					{
						String[] peer = argsBean.getPeer().split(":");
						JSONObject res = new JSONObject();
						res.put("command", "DISCONNECT_PEER_REQUEST");
						res.put("host", peer[0]);
						res.put("port", Integer.parseInt(peer[1]));
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientsocket);
					}
					String response2 = sockethandle.getSocketMessage(clientsocket);
					System.out.println(response2);
					JSONParser parser2 = new JSONParser();
					JSONObject message2 = (JSONObject) parser2.parse(response2);
					String c=message2.get("payload").toString();
					String p =AES.aesDecrypt(c, key);
					System.out.println(p);
				}
			}
		}
		catch (CmdLineException | NumberFormatException | IOException | ParseException e)
		{

			System.err.println(e.getMessage());

			// Print the usage to help the user understand the arguments expected
			// by the program
			parser.printUsage(System.err);
		}

	}

}
