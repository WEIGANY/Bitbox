package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.ThreadTcpReceive;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/*
 * process the Initialization function.
 */
public class Initialization extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(Initialization.class.getName());
	ArrayList<HostPort> peers;
	ArrayList<Socket> sockets;
	int count;
	FileSystemManager fileSystemManager;
	HashMap<HostPort, Socket> map;

	public Initialization(ArrayList<HostPort> peers, ArrayList<Socket> sockets, int count,
			FileSystemManager fileSystemManager,HashMap<HostPort, Socket> map)
	{
		this.peers = peers;
		this.sockets = sockets;
		this.count = count;
		this.fileSystemManager = fileSystemManager;
		this.map= map;
	}

	public void run()
	{
		try
		{
			String initlists = Configuration.getConfigurationValue("peers");
			String[] initpeer = initlists.split(",");
			for (int i = 0; i < initpeer.length; i++)
			{
				String[] address = initpeer[i].split(":");
				Socket initsocket = new Socket(address[0], Integer.parseInt(address[1]));
				log.info("connect to " + address[0] + Integer.parseInt(address[1]));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(initsocket.getOutputStream()));
				JSONObject HANDSHAKE_REQUEST = new JSONObject();
				JSONObject hostPort = new JSONObject();
				hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
				hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
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
					if (count < Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")))
					{
						log.info("add" + a + b + " to list");
						HostPort p = new HostPort(a, b);
						peers.add(p);
						count++;
						sockets.add(initsocket);
						new ThreadTcpReceive(initsocket, p, peers, sockets, fileSystemManager).start();
						for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
						{
							new ThreadTcpSend(initsocket, peers, sockets, fileSystemManager, events).start();
						}
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
					}

				}
				else
				{
					if (command.equals("CONNECTION_REFUSED"))
					{
						System.out.println("CONNECTION_REFUSED");
						JSONArray array = (JSONArray) messagejson.get("peers");
						Queue<HostPort> queue = new LinkedList<HostPort>();
						ArrayList<HostPort> used = new ArrayList<HostPort>();
						for (int i1 = 0; i1 < array.size(); i1++)
						{
							JSONObject object = (JSONObject) array.get(i1);
							HostPort hp = new HostPort(object.get("host").toString(),
									Integer.parseInt(object.get("port").toString()));
							queue.add(hp);
						}
						while (!queue.isEmpty())
						{
							HostPort peer = queue.poll();
							if (isconnected(peer, queue, used))
								break;
						}
						// create a queue to save the next peer to connect
						// use BFS model and need to remove the peer in the queue which has been
						// connected
						// so need another list to save already connected or refused peer
						// repeat this process until one connection is created successfully
					}
					else
					{
						log.info("command error");
						initsocket.close();
					}
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
			log.info("init  end..");
		}
	}

	public boolean isconnected(HostPort peer, Queue<HostPort> queue, ArrayList<HostPort> used)
	{
		try
		{
			if (used.contains(peer))
			{
				return false;
			}
			String[] address = peer.toString().split(":");
			log.info("connect to " + address[0] + Integer.parseInt(address[1]));
			Socket initsocket = new Socket(address[0], Integer.parseInt(address[1]));
			BufferedReader input = new BufferedReader(new InputStreamReader(initsocket.getInputStream()));
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(initsocket.getOutputStream()));
			JSONObject HANDSHAKE_REQUEST = new JSONObject();
			JSONObject hostPort = new JSONObject();
			hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
			hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
			HANDSHAKE_REQUEST.put("hostPort", hostPort);
			HANDSHAKE_REQUEST.put("command", "HANDSHAKE_REQUEST");
			System.out.println(HANDSHAKE_REQUEST);
			output.write(HANDSHAKE_REQUEST.toJSONString());
			output.newLine();
			log.info("request sent");
			output.flush();
			String message = input.readLine();
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
				log.info("add" + a + b + " to list");
				HostPort p = new HostPort(a, b);
				peers.add(p);
				count++;
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
				if (command.equals("CONNECTION_REFUSED"))
				{
					System.out.println("CONNECTION_REFUSED");
					JSONArray array = (JSONArray) messagejson.get("peers");
					for (int i1 = 0; i1 < array.size(); i1++)
					{
						JSONObject object = (JSONObject) array.get(i1);
						HostPort hp = new HostPort(object.get("host").toString(),
								Integer.parseInt(object.get("port").toString()));
						queue.add(hp);
						used.add(hp);
					}
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
		}
		return false;
	}

}
