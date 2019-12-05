package unimelb.bitbox.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import unimelb.bitbox.sockethandle;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/*
 * transform different kinds of event to jsonString and send them to the opposite peer. 
 */
public class ThreadTcpSend extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(ThreadTcpSend.class.getName());
	private Socket socket;
	FileSystemManager fileSystemManager;
	HostPort p;
	ArrayList<HostPort> peers;
	ArrayList<Socket> sockets;
	FileSystemEvent fileSystemEvent;

	public ThreadTcpSend(Socket socket, ArrayList<HostPort> peers, ArrayList<Socket> sockets,
			FileSystemManager fileSystemManager, FileSystemEvent fileSystemEvent)
	{
		super();
		this.socket = socket;
		this.peers = peers;
		this.sockets = sockets;
		this.fileSystemManager = fileSystemManager;
		this.fileSystemEvent = fileSystemEvent;

	}

	@Override
	public void run()
	{
		try
		{
			System.out.println("SEND:  "+fileSystemEvent.event);
			System.out.println(processrequest(fileSystemEvent).toJSONString());
			sockethandle.sendSocketMessage(processrequest(fileSystemEvent).toJSONString(), socket);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
	}

	public JSONObject processrequest(FileSystemEvent fileSystemEvent)
	{
		String fPath = fileSystemEvent.path;
		String fName = fileSystemEvent.name;
		String fPathName = fileSystemEvent.pathName;
		FileSystemManager.FileDescriptor fDes = fileSystemEvent.fileDescriptor;
		JSONObject res = new JSONObject();

		switch (fileSystemEvent.event)
		{
		case FILE_CREATE:
			res.put("command", "FILE_CREATE_REQUEST");
			JSONObject des = new JSONObject();
			des.put("md5", fDes.md5);
			des.put("lastModified", fDes.lastModified);
			des.put("fileSize", fDes.fileSize);
			res.put("fileDescriptor", des);
			res.put("pathName", fPathName);
			return res;
		case FILE_DELETE:
			res.put("command", "FILE_DELETE_REQUEST");
			JSONObject des2 = new JSONObject();
			des2.put("md5", fDes.md5);
			des2.put("lastModified", fDes.lastModified);
			des2.put("fileSize", fDes.fileSize);
			res.put("fileDescriptor", des2);
			res.put("pathName", fPathName);
			return res;
		case FILE_MODIFY:
			res.put("command", "FILE_MODIFY_REQUEST");
			JSONObject des3 = new JSONObject();
			des3.put("md5", fDes.md5);
			des3.put("lastModified", fDes.lastModified);
			des3.put("fileSize", fDes.fileSize);
			res.put("fileDescriptor", des3);
			res.put("pathName", fPathName);
			return res;
		case DIRECTORY_CREATE:
			res.put("command", "DIRECTORY_CREATE_REQUEST");
			res.put("pathName", fPathName);
			return res;
		case DIRECTORY_DELETE:
			res.put("command", "DIRECTORY_DELETE_REQUEST");
			res.put("pathName", fPathName);
			return res;
		}
		return res;
	}

}
