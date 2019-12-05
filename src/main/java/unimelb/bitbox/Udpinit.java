package unimelb.bitbox;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.ThreadTcpReceive;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


/*
 * process the Initialization function at the udp mode
 */

public class Udpinit extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(Udpinit.class.getName());
	DatagramSocket socket;
	ArrayList<HostPort> peers;
	int count;
	FileSystemManager fileSystemManager;

	public Udpinit(DatagramSocket socket, ArrayList<HostPort> peers, int count, FileSystemManager fileSystemManager)
	{
		this.peers = peers;
		this.socket = socket;
		this.count = count;
		this.fileSystemManager = fileSystemManager;
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
				log.info("connect to " + address[0] + Integer.parseInt(address[1]));
				InetAddress addressc = InetAddress.getByName(address[0]);
				JSONObject HANDSHAKE_REQUEST = new JSONObject();
				JSONObject hostPort = new JSONObject();
				hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
				hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
				HANDSHAKE_REQUEST.put("hostPort", hostPort);
				HANDSHAKE_REQUEST.put("command", "HANDSHAKE_REQUEST");
				System.out.println(HANDSHAKE_REQUEST);
				DatagramPacket reply = new DatagramPacket(HANDSHAKE_REQUEST.toJSONString().getBytes(),
						HANDSHAKE_REQUEST.toJSONString().getBytes().length, addressc, Integer.parseInt(address[1]));
				socket.send(reply);
				log.info("request sent");

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
}