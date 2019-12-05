package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.HandleClient;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.ThreadTcpReceive;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.ThreadUdpReceive;
import unimelb.bitbox.util.ThreadUdpSend;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
/*
 * UDP's ServerMain
 * its functions are very similar to ServerMain
 * 
 */
public class ServerMain2 implements FileSystemObserver
{
	private static Logger log = Logger.getLogger(ServerMain2.class.getName());
	protected FileSystemManager fileSystemManager;
	protected int counter = 0;
	public ArrayList<HostPort> peers = new ArrayList<HostPort>();
	public ArrayList<DatagramSocket> sockets = new ArrayList<DatagramSocket>();
	HashMap<HostPort, DatagramSocket> map = new HashMap<HostPort, DatagramSocket>();
	DatagramSocket aSocket = null;

	public ServerMain2() throws NumberFormatException, IOException, NoSuchAlgorithmException
	{
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
		try
		{
			aSocket = new DatagramSocket(Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
			// create socket at agreed port
			byte[] buffer = new byte[8192];
			JSONParser parser = new JSONParser();
			DatagramPacket request = new DatagramPacket(buffer, buffer.length);
			new Udpinit(aSocket, peers, counter, fileSystemManager).start();
			//new Udpsyn(peers, aSocket, Integer.parseInt(Configuration.getConfigurationValue("syncInterval")),
			//		fileSystemManager).start();
			new UdpClientConnect(peers,this).start();
			while (true)
			{			
				System.out.println("Server waiting to receive data");
				aSocket.receive(request);
				String message = new String(request.getData(),0,request.getLength());
				System.out.println("Received Data: " + message);
				System.out.println(message);
				JSONObject messagejson = null;
				try
				{
					messagejson = (JSONObject) parser.parse(message);
				}
				catch (ParseException|NullPointerException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String command = messagejson.get("command").toString();
				System.out.println("COMMAND RECEIVED: " + command);
				InetAddress address = request.getAddress();
				HostPort p = new HostPort(address.toString(), request.getPort());
				if (peers.contains(p))
				{
					new ThreadUdpReceive(aSocket, messagejson, p, peers, sockets, fileSystemManager).run();
				}
				else
				{
					if (command.equals("HANDSHAKE_REQUEST"))
					{
						if (counter < Integer
								.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")))
						{
							log.info("add" + address + p.port + " to list");
							peers.add(p);
							counter++;
							JSONObject HANDSHAKE_RESPONSE = new JSONObject();
							JSONObject hostPort = new JSONObject();
							hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
							hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
							HANDSHAKE_RESPONSE.put("hostPort", hostPort);
							HANDSHAKE_RESPONSE.put("command", "HANDSHAKE_RESPONSE");
							System.out.println(HANDSHAKE_RESPONSE);
							DatagramPacket reply = new DatagramPacket(HANDSHAKE_RESPONSE.toJSONString().getBytes(),
									HANDSHAKE_RESPONSE.toJSONString().getBytes().length, address, p.port);
							aSocket.send(reply);

						}
						else
						{
							JSONObject CONNECTION_REFUSED = new JSONObject();
							String hostPort1 = JSONArray.toJSONString(peers);
							CONNECTION_REFUSED.put("command", "CONNECTION_REFUSED");
							CONNECTION_REFUSED.put("message", "connection limit reached");
							CONNECTION_REFUSED.put("peers", hostPort1);
							System.out.println(CONNECTION_REFUSED);
							DatagramPacket reply = new DatagramPacket(CONNECTION_REFUSED.toJSONString().getBytes(),
									CONNECTION_REFUSED.toJSONString().getBytes().length, address, p.port);
							aSocket.send(reply);
						}
					}
					if (command.equals("HANDSHAKE_RESPONSE"))
					{
						log.info("add" + address + p.port + " to list");
						peers.add(p);
						counter++;
						for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
						{
							new ThreadUdpSend(aSocket, p, fileSystemManager, events).start();
						}
					}
					if (command.equals("CONNECTION_REFUSED"))
					{
						log.info("CONNECTION_REFUSED");
						System.out.println("CONNECTION_REFUSED");
						JSONArray array = (JSONArray) messagejson.get("peers");
						JSONObject object = (JSONObject) array.get(0);
						HostPort hp = new HostPort(object.get("host").toString(),
								Integer.parseInt(object.get("port").toString()));
						JSONObject HANDSHAKE_REQUEST = new JSONObject();
						JSONObject hostPort = new JSONObject();
						hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
						hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
						HANDSHAKE_REQUEST.put("hostPort", hostPort);
						HANDSHAKE_REQUEST.put("command", "HANDSHAKE_REQUEST");
						System.out.println(HANDSHAKE_REQUEST);
						DatagramPacket reply = new DatagramPacket(HANDSHAKE_REQUEST.toJSONString().getBytes(),
								HANDSHAKE_REQUEST.toJSONString().getBytes().length, address, p.port);
						aSocket.send(reply);

					}
				}

			}
		}
		catch (SocketException e)
		{
			System.out.println("Socket: " + e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println("IO: " + e.getMessage());
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		finally
		{
			if (aSocket != null)
				aSocket.close();
		}
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent)
	{
		log.info("observer start" + fileSystemEvent.event.name());
		for (int i = 0; i < peers.size(); i++)
		{
			new ThreadUdpSend(aSocket, peers.get(i), fileSystemManager, fileSystemEvent).start();
		}
	}

	public boolean connecttopeer(String address, int port)
	{
		try
		{
			address=address.replace("/", "");
			InetAddress addressc = InetAddress.getByName(address);
			JSONObject HANDSHAKE_REQUEST = new JSONObject();
			JSONObject hostPort = new JSONObject();
			hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
			hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
			HANDSHAKE_REQUEST.put("hostPort", hostPort);
			HANDSHAKE_REQUEST.put("command", "HANDSHAKE_REQUEST");
			System.out.println(HANDSHAKE_REQUEST);
			DatagramPacket reply = new DatagramPacket(HANDSHAKE_REQUEST.toJSONString().getBytes(),
					HANDSHAKE_REQUEST.toJSONString().getBytes().length, addressc, port);
			aSocket.send(reply);
			log.info("request sent");
			return true;

			
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