package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import unimelb.bitbox.util.AES;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HandleClient;
import unimelb.bitbox.util.HandleClient2;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.RSA;

/*
 * process the client connection in the tcp mode.
 */

public class TcpClientConnect extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(TcpClientConnect.class.getName());
	ArrayList<HostPort> peers;
	ServerMain servermain;

	public TcpClientConnect(ArrayList<HostPort> peers, ServerMain servermain)
	{
		super();
		this.peers = peers;
		this.servermain = servermain;
	}

	public void run()
	{
		try
		{
			ServerSocket tcpServer = new ServerSocket(
					Integer.parseInt(Configuration.getConfigurationValue("clientPort")));
			while (true)
			{
				log.info("start next client listening");
				Socket clientSocket = tcpServer.accept();
				log.info("accept socket");
				JSONParser parser = new JSONParser();
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				String message = input.readLine();
				System.out.println(message);
				JSONObject messagejson = (JSONObject) parser.parse(message);
				String command = messagejson.get("command").toString();
				if (command.equals("AUTH_REQUEST"))
				{
					byte[] key = AES.aesInitKey();
					String str = Base64.getEncoder().encodeToString(key);
					JSONObject AUTH_RESPONSE = new JSONObject();
					String[] a= Configuration.getConfigurationValue("authorized_keys").split(" ");
					String id = a[2];
					if (messagejson.get("identity").toString().equals(id))
					{
						AUTH_RESPONSE.put("command", "AUTH_RESPONSE");
						AUTH_RESPONSE.put("AES128", RSA.publicEncrypt1(str,
								RSA.strToPublicKey(Configuration.getConfigurationValue("authorized_keys"))));
						AUTH_RESPONSE.put("status", true);
						AUTH_RESPONSE.put("message", "public key found");
						sockethandle.sendSocketMessage(AUTH_RESPONSE.toJSONString(), clientSocket);
						new HandleClient(clientSocket, peers, servermain, key).start();
					}
					else
					{
						AUTH_RESPONSE.put("command", "AUTH_RESPONSE");
						AUTH_RESPONSE.put("status", false);
						AUTH_RESPONSE.put("message", "public key not found");
						sockethandle.sendSocketMessage(AUTH_RESPONSE.toJSONString(), clientSocket);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}