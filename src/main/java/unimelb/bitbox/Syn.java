package unimelb.bitbox;

import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.ThreadTcpSend;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/*
 * process the Synchronization function
 */
public class Syn extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(Syn.class.getName());
	ArrayList<HostPort> peers;
	ArrayList<Socket> sockets;
	int count;
	FileSystemManager fileSystemManager;
	int syncInterval;

	public Syn(ArrayList<HostPort> peers, ArrayList<Socket> sockets, int syncInterval,
			FileSystemManager fileSystemManager)
	{
		this.peers = peers;
		this.sockets = sockets;
		this.fileSystemManager = fileSystemManager;
		this.syncInterval = syncInterval;
	}

	public void run()
	{
		while (true)
		{
			log.info("start syn");
			for (Socket socket : sockets)
			{

				for (FileSystemEvent events : fileSystemManager.generateSyncEvents())
				{
					new ThreadTcpSend(socket, peers, sockets, fileSystemManager, events).start();
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
