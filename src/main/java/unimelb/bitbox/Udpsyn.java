package unimelb.bitbox;

import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.ThreadUdpSend;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/*
 * process the Synchronization function
 */
public class Udpsyn extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(Udpsyn.class.getName());
	ArrayList<HostPort> peers;
	int count;
	DatagramSocket socket;
	FileSystemManager fileSystemManager;
	int syncInterval;

	public Udpsyn(ArrayList<HostPort> peers, DatagramSocket socket, int syncInterval,
			FileSystemManager fileSystemManager)
	{
		this.peers = peers;
		this.socket = socket;
		this.fileSystemManager = fileSystemManager;
		this.syncInterval = syncInterval;
	}

	public void run()
	{
		while (true)
		{
			log.info("start syn");
			for (HostPort peer : peers)
			{

				for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
				{
					new ThreadUdpSend(socket, peer, fileSystemManager, events).start();
				}
			}
			try
			{
				Thread.sleep(syncInterval * 1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
