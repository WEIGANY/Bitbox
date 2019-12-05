package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import unimelb.bitbox.util.AES;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.HandleClient;
import unimelb.bitbox.util.HandleClient2;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.RSA;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.ThreadTcpReceive;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver
{
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	protected int counter = 0;
	public ArrayList<HostPort> peers = new ArrayList<HostPort>();
	public ArrayList<Socket> sockets = new ArrayList<Socket>();
	HashMap<HostPort, Socket> map = new HashMap<HostPort, Socket>();

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException
	{
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		ServerSocket tcpServer = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("port")));
		try
		{

			new Initialization(peers, sockets, counter, fileSystemManager,map).start();
			new Syn(peers, sockets, Integer.parseInt(Configuration.getConfigurationValue("syncInterval")),
					fileSystemManager).start();
			new TcpClientConnect(peers,this).start();
			while (true)
			{
				log.info("start next listening");
				Socket clientSocket = tcpServer.accept();
				log.info("accept socket");

				JSONParser parser = new JSONParser();
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				String message = input.readLine();
				System.out.println(message);
				JSONObject messagejson = (JSONObject) parser.parse(message);
				String command = messagejson.get("command").toString();
				
				
				if (command.equals("HANDSHAKE_REQUEST"))
				{
					if (counter < Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")))
					{
						JSONObject HANDSHAKE_RESPONSE = new JSONObject();
						JSONObject hostPort = new JSONObject();
						hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
						hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
						HANDSHAKE_RESPONSE.put("hostPort", hostPort);
						HANDSHAKE_RESPONSE.put("command", "HANDSHAKE_RESPONSE");
						sockethandle.sendSocketMessage(HANDSHAKE_RESPONSE.toJSONString(), clientSocket);
						messagejson = (JSONObject) messagejson.get("hostPort");
						System.out.println(messagejson);
						String host = messagejson.get("host").toString();
						int b = Integer.parseInt(messagejson.get("port").toString());
						log.info("add" + host + b + " to list");
						HostPort p = new HostPort(host, b);
						counter++;
						peers.add(p);
						sockets.add(clientSocket);
						map.put(p, clientSocket);
						new ThreadTcpReceive(clientSocket, p, peers, sockets, fileSystemManager).start();
						for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
						{
							new ThreadTcpSend(clientSocket, peers, sockets, fileSystemManager, events).start();
						}

					}
					else
					{
						//JSONObject CONNECTION_REFUSED = new JSONObject();
						//String hostPort = JSONArray.toJSONString(peers);
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
						doc3.append("command", "CONNECTION_REFUSED");
						doc3.append("message", "connection limit reached");
						//CONNECTION_REFUSED.put("command", "CONNECTION_REFUSED");
						//CONNECTION_REFUSED.put("message", "connection limit reached");
						//CONNECTION_REFUSED.put("peers", hostPort);
						
						output.write(doc3.toJson());
						output.flush();
						clientSocket.close();
					}
				}
				else
				{
					JSONObject INVALID_PROTOCOL = new JSONObject();
					INVALID_PROTOCOL.put("command", "INVALID_PROTOCOL");
					INVALID_PROTOCOL.put("message", "message must contain a command field as string");

					output.write(INVALID_PROTOCOL.toString());
					output.flush();
					clientSocket.close();
				}

			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.info("error:" + e);
		}
		finally
		{
			log.info("listening end..");
		}
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent)
	{
		log.info("observer start" + fileSystemEvent.event.name());
		for (int i = 0; i < sockets.size(); i++)
		{
			new ThreadTcpSend(sockets.get(i), peers, sockets, fileSystemManager, fileSystemEvent).start();
		}
	}

	public boolean connecttopeer(String address, int port)
	{
		try
		{
			Socket initsocket = new Socket(address, port);
			log.info("connect to " + address + port);
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(initsocket.getOutputStream()));
			JSONObject HANDSHAKE_REQUEST = new JSONObject();
			JSONObject hostPort = new JSONObject();
			hostPort.put("host", address);
			hostPort.put("port", port);
			HANDSHAKE_REQUEST.put("hostPort", hostPort);
			HANDSHAKE_REQUEST.put("command", "HANDSHAKE_REQUEST");
			System.out.println(HANDSHAKE_REQUEST);
			output.write(HANDSHAKE_REQUEST.toJSONString());
			output.newLine();
			log.info("request sent");
			output.flush();
			String message = sockethandle.getSocketMessage(initsocket);
			System.out.println(message);
			JSONParser parser = new JSONParser();
			JSONObject messagejson = (JSONObject) parser.parse(message);
			String command = messagejson.get("command").toString();
			log.info("command: " + command);
			if (command.equals("HANDSHAKE_RESPONSE"))
			{
				messagejson = (JSONObject) messagejson.get("hostPort");
				String a = messagejson.get("host").toString();
				int b = Integer.parseInt(messagejson.get("port").toString());
				if (counter < Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")))
				{
					log.info("add" + a + b + " to list");
					HostPort p = new HostPort(a, b);
					peers.add(p);
					counter++;
					sockets.add(initsocket);
					new ThreadTcpReceive(initsocket, p, peers, sockets, fileSystemManager).start();
					for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
					{
						new ThreadTcpSend(initsocket, peers, sockets, fileSystemManager, events).start();
					}
					return true;
				}
				else
				{
					JSONObject CONNECTION_REFUSED = new JSONObject();
					String hostPort1 = JSONArray.toJSONString(peers);

					CONNECTION_REFUSED.put("command", "CONNECTION_REFUSED");
					CONNECTION_REFUSED.put("message", "connection limit reached");
					CONNECTION_REFUSED.put("peers", hostPort1);
					sockethandle.sendSocketMessage(CONNECTION_REFUSED.toJSONString(), initsocket);
					initsocket.close();
					return false;
				}

			}
			else
			{
				if (command.equals("CONNECTION_REFUSED"))
				{
					log.info("CONNECTION_REFUSED");
					initsocket.close();
					return false;
				}
				else
				{
					log.info("command error");
					initsocket.close();
					return false;
				}
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.info("error:" + e);
			return false;
		}

	}

	public boolean disconnecttopeer(String address, int port)
	{
		HostPort key = new HostPort(address, port);
		try
		{
			for (HostPort peer : peers)
			{
				if (peer.equals(key))
				{				
					//Socket target = map.get(peer);				
					//target.close();
					peers.remove(peer);
					return true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.info("error:" + e);
			return false;
		}
		return false;

	}

}
