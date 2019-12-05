package unimelb.bitbox.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.ServerMain;
import unimelb.bitbox.ServerMain2;
import unimelb.bitbox.sockethandle;

public class HandleClient2 extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(ThreadTcpReceive.class.getName());
	Socket clientSocket;
	ArrayList<HostPort> peers;
	String secret_key;
	ServerMain2 servermain2;
	byte[] key;

	public HandleClient2(Socket clientSocket, ArrayList<HostPort> peers, ServerMain2 servermain2, byte[] key)
	{
		super();
		this.clientSocket = clientSocket;
		this.peers = peers;
		this.servermain2 = servermain2;
		this.key = key;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run()
	{
		try
		{
			JSONParser parser = new JSONParser();
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			log.info("receive massage");
			String message = input.readLine();
			System.out.println(message);
			JSONObject message1 = (JSONObject) parser.parse(message.toString());
			String c = message1.get("payload").toString();
			String p = AES.aesDecrypt(c, key);
			JSONObject messagejson = (JSONObject) parser.parse(p);
			String command = messagejson.get("command").toString();
			System.out.println("COMMAND RECEIVED: " + command);
			if (command.equals("LIST_PEERS_REQUEST"))
			{
				// JSONObject LIST_PEERS_RESPONSE = new JSONObject();

				// JSONObject hostPort = new JSONObject();
				ArrayList<Document> docs = new ArrayList<Document>();
				for (HostPort p1 : peers)
				{
					Document peer = new Document();
					peer.append("host", p1.host);
					peer.append("port", p1.port);
					docs.add(peer);
				}
				Document doc3 = new Document();
				doc3.append("peers", docs);
				doc3.append("command", "LIST_PEERS_RESPONSE");
				// hostPort=doc3.obj;
				// LIST_PEERS_RESPONSE.put("command", "LIST_PEERS_RESPONSE");
				// LIST_PEERS_RESPONSE.put("peers", hostPort);
				String payload = AES.aesEncrypt(doc3.toJson(), key);
				JSONObject res2 = new JSONObject();
				res2.put("payload", payload);
				sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);

			}
			if (command.equals("CONNECT_PEER_REQUEST"))
			{
				HostPort hp = new HostPort(messagejson.get("host").toString(),
						Integer.parseInt(messagejson.get("port").toString()));
				JSONObject res = new JSONObject();
				res.put("command", "CONNECT_PEER_RESPONSE");
				res.put("host", messagejson.get("host").toString());
				res.put("port", Integer.parseInt(messagejson.get("port").toString()));

				if (peers.contains(hp))
				{
					res.put("status", false);
					res.put("message", "already connected");
					String payload = AES.aesEncrypt(res.toJSONString(), key);
					JSONObject res2 = new JSONObject();
					res2.put("payload", payload);
					sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
				}
				else
				{
					if (servermain2.connecttopeer(messagejson.get("host").toString(),
							Integer.parseInt(messagejson.get("port").toString())))
					{
						res.put("status", true);
						res.put("message", "connected to peer");
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
					}
					else
					{
						res.put("status", false);
						res.put("message", "connection failed");
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
					}
				}

			}
			if (command.equals("DISCONNECT_PEER_REQUEST"))
			{
				HostPort hp = new HostPort(messagejson.get("host").toString(),
						Integer.parseInt(messagejson.get("port").toString()));
				JSONObject res = new JSONObject();
				res.put("command", "DISCONNECT_PEER_RESPONSE");
				res.put("host", messagejson.get("host").toString());
				res.put("port", Integer.parseInt(messagejson.get("port").toString()));

				if (!peers.contains(hp))
				{
					res.put("status", false);
					res.put("message", "connection not active");
					String payload = AES.aesEncrypt(res.toJSONString(), key);
					JSONObject res2 = new JSONObject();
					res2.put("payload", payload);
					sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
				}
				else
				{
					if (servermain2.disconnecttopeer(messagejson.get("host").toString(),
							Integer.parseInt(messagejson.get("port").toString())))
					{
						res.put("status", true);
						res.put("message", "disconnected from peer");
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
					}
					else
					{
						res.put("status", false);
						res.put("message", "unable to disconnect from peer");
						String payload = AES.aesEncrypt(res.toJSONString(), key);
						JSONObject res2 = new JSONObject();
						res2.put("payload", payload);
						sockethandle.sendSocketMessage(res2.toJSONString(), clientSocket);
					}
				}
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally {
			this.stop();
		}

	}
}