package unimelb.bitbox.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.sockethandle;
import unimelb.bitbox.util.FileSystemManager.EVENT;
import unimelb.bitbox.util.FileSystemManager.FileDescriptor;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

/*
 * process the coming message between the two peers at udp mode
 * 
 * 
 */
public class ThreadUdpReceive extends Thread implements Runnable
{
	private static Logger log = Logger.getLogger(ThreadTcpReceive.class.getName());
	DatagramSocket dsocket;
	FileSystemManager fileSystemManager;
	HostPort p;
	ArrayList<HostPort> peers;
	ArrayList<DatagramSocket> sockets;
	JSONObject messagejson;
	InetAddress address;

	public ThreadUdpReceive(DatagramSocket clientSocket, JSONObject messagejson, HostPort p, ArrayList<HostPort> peers,
			ArrayList<DatagramSocket> sockets, FileSystemManager fileSystemManager)
	{
		super();
		this.dsocket = clientSocket;
		this.peers = peers;
		this.sockets = sockets;
		this.p = p;
		this.fileSystemManager = fileSystemManager;
		this.messagejson = messagejson;
	}

	@Override
	public void run()
	{

		try
		{
			String hosta = p.host.replaceAll("/", "");
			System.out.println(hosta);
			address = InetAddress.getByName(hosta);
			JSONParser parser = new JSONParser();
			log.info("receive massage");
			System.out.println(messagejson.toString());
			try
			{// need to catch the JSON parser error
				String command = messagejson.get("command").toString();
				System.out.println("COMMAND RECEIVED: " + command);
				// process different types of server solution
				// first process directory request and file delete
				if (command.equals("DIRECTORY_CREATE_REQUEST"))
				{
					String path = Configuration.getConfigurationValue("path");
					String name = messagejson.get("pathName").toString();
					EVENT event = EVENT.DIRECTORY_CREATE;
					FileSystemEvent newevent = fileSystemManager.new FileSystemEvent(path, name, event);
					JSONObject res = processFileSystemEvent(newevent);
					System.out.println(res);
					DatagramPacket reply = new DatagramPacket(res.toJSONString().getBytes(),
							res.toJSONString().getBytes().length, address, p.port);
					dsocket.send(reply);
				}
				if (command.equals("DIRECTORY_DELETE_REQUEST"))
				{
					String path = Configuration.getConfigurationValue("path");
					String name = messagejson.get("pathName").toString();
					EVENT event = EVENT.DIRECTORY_DELETE;
					FileSystemEvent newevent = fileSystemManager.new FileSystemEvent(path, name, event);
					JSONObject res = processFileSystemEvent(newevent);
					System.out.println(res);
					DatagramPacket reply = new DatagramPacket(res.toJSONString().getBytes(),
							res.toJSONString().getBytes().length, address, p.port);
					dsocket.send(reply);
				}

				if (command.equals("FILE_DELETE_REQUEST"))
				{
					String path = Configuration.getConfigurationValue("path");
					String name = messagejson.get("pathName").toString();
					JSONObject fileDes = (JSONObject) messagejson.get("fileDescriptor");
					FileDescriptor fileDescriptor = fileSystemManager.new FileDescriptor(
							((Long) (fileDes.get("lastModified"))), fileDes.get("md5").toString(),
							(Long) (fileDes.get("fileSize")));
					EVENT event = EVENT.FILE_DELETE;
					FileSystemEvent newevent = fileSystemManager.new FileSystemEvent(path, name, event, fileDescriptor);
					JSONObject res = processFileSystemEvent(newevent);
					System.out.println(res);
					System.out.println(res);
					DatagramPacket reply = new DatagramPacket(res.toJSONString().getBytes(),
							res.toJSONString().getBytes().length, address, p.port);
					dsocket.send(reply);
				}
				// other events which are relevant with fileLoad , so they will be process
				// separately.
				if (command.equals("FILE_BYTES_REQUEST"))
				{
					JSONObject fileBytesRequest = messagejson;
					ProcessFileBytesRequest((String) ((JSONObject) (fileBytesRequest.get("fileDescriptor"))).get("md5"),
							(Long) fileBytesRequest.get("position"),
							(Long) ((JSONObject) (fileBytesRequest.get("fileDescriptor"))).get("fileSize"),
							(Long) ((JSONObject) (fileBytesRequest.get("fileDescriptor"))).get("lastModified"),
							(String) fileBytesRequest.get("pathName"), (Long) fileBytesRequest.get("length"));
				}
				if (command.equals("FILE_CREATE_REQUEST"))
				{
					JSONObject fileCreateRequest = messagejson;
					ProcessFileCreateRequest((String) fileCreateRequest.get("pathName"),
							(String) ((JSONObject) (fileCreateRequest.get("fileDescriptor"))).get("md5"),
							(Long) ((JSONObject) (fileCreateRequest.get("fileDescriptor"))).get("fileSize"),
							(Long) ((JSONObject) (fileCreateRequest.get("fileDescriptor"))).get("lastModified"));
					log.info("Processed File create request");
				}
				if (command.equals("FILE_BYTES_RESPONSE"))
				{
					JSONObject fileBytesResponse = messagejson;
					long position = (Long) fileBytesResponse.get("position");
					long length = (Long) fileBytesResponse.get("length");
					long fileSize = (Long) ((JSONObject) (fileBytesResponse.get("fileDescriptor"))).get("fileSize");
					String content = (String) fileBytesResponse.get("content");
					String pathName = (String) fileBytesResponse.get("pathName");
					String md5 = (String) ((JSONObject) (fileBytesResponse.get("fileDescriptor"))).get("md5");
					System.out.println(md5);
					long modified = (Long) ((JSONObject) (fileBytesResponse.get("fileDescriptor"))).get("lastModified");
					ByteBuffer src = ByteBuffer.wrap(Base64.getDecoder().decode(content));
					this.fileSystemManager.writeFile(pathName, src, position);
					this.fileSystemManager.checkWriteComplete(pathName);
					log.info("Write complete");
					if ((position + length) < fileSize)
					{
						position = position + length;
						if ((position + length) < fileSize)
						{
							SendByteRequest(pathName, md5, fileSize, modified, position, length);
						}
						else
						{
							SendByteRequest(pathName, md5, fileSize, modified, position, fileSize - position);
						}
					}
					else
					{
						this.fileSystemManager.cancelFileLoader(pathName);
					}
				}
				if (command.equals("FILE_MODIFY_REQUEST"))
				{

					JSONObject fileModifyRequest = messagejson;

					ProcessFileModifyRequest((String) fileModifyRequest.get("pathName"),
							(Long) ((JSONObject) (fileModifyRequest.get("fileDescriptor"))).get("lastModified"),
							(String) ((JSONObject) (fileModifyRequest.get("fileDescriptor"))).get("md5"),
							(Long) ((JSONObject) (fileModifyRequest.get("fileDescriptor"))).get("fileSize"));
				}
				if (messagejson.toString().isEmpty())
				{
					JSONObject INVALID_PROTOCOL = new JSONObject();
					INVALID_PROTOCOL.put("command", "INVALID_PROTOCOL");
					INVALID_PROTOCOL.put("message", "message must contain a command field as string");

					DatagramPacket reply = new DatagramPacket(INVALID_PROTOCOL.toJSONString().getBytes(),
							INVALID_PROTOCOL.toJSONString().getBytes().length, address, p.port);
					dsocket.send(reply);
				}
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	// process FILE_DELETE, DIRECTORY_CREATE and DIRECTORY_DELETE:
	public JSONObject processFileSystemEvent(FileSystemEvent fileSystemEvent)
	{

		String fPath = fileSystemEvent.path;
		String fName = fileSystemEvent.name;
		String fPathName = fileSystemEvent.pathName;
		FileSystemManager.FileDescriptor fDes = fileSystemEvent.fileDescriptor;
		JSONObject res = new JSONObject();

		System.out.println(fPathName);
		switch (fileSystemEvent.event)
		{

		case FILE_DELETE:
			if (!fileSystemManager.fileNameExists(fPathName, fDes.md5))
			{
				res.put("command", "FILE_DELETE_RESPONSE");
				JSONObject des = new JSONObject();
				des.put("md5", fDes.md5);
				des.put("lastModified", fDes.lastModified);
				des.put("fileSize", fDes.fileSize);
				res.put("fileDescriptor", des);
				res.put("pathName", fPathName);
				res.put("message", "file name not exists");
				res.put("status", false);
				return res;
			}
			boolean finish = fileSystemManager.deleteFile(fPathName, fDes.lastModified, fDes.md5);
			if (!finish)
			{
				res.put("command", "FILE_DELETE_RESPONSE");
				JSONObject des = new JSONObject();
				des.put("md5", fDes.md5);
				des.put("lastModified", fDes.lastModified);
				des.put("fileSize", fDes.fileSize);
				res.put("fileDescriptor", des);
				res.put("pathName", fPathName);
				res.put("message", "file delete error");
				res.put("status", false);
				return res;
			}
			else
			{
				res.put("command", "FILE_DELETE_RESPONSE");
				JSONObject des = new JSONObject();
				des.put("md5", fDes.md5);
				des.put("lastModified", fDes.lastModified);
				des.put("fileSize", fDes.fileSize);
				res.put("fileDescriptor", des);
				res.put("pathName", fPathName);
				res.put("message", "file delete successfully");
				res.put("status", true);
				return res;
			}

		case DIRECTORY_CREATE:
			if (!fileSystemManager.isSafePathName(fPathName))
			{
				res.put("command", "DIRECTORY_CREATE_RESPONSE");
				res.put("pathName", fPathName);
				res.put("message", "the file path is not safe");
				res.put("status", false);
				return res;
			}
			else
			{
				boolean success = fileSystemManager.makeDirectory(fPathName);
				if (!success)
				{
					res.put("command", "DIRECTORY_CREATE_RESPONSE");
					res.put("pathName", fPathName);
					res.put("message", "directory failed to be created");
					res.put("status", false);
					return res;
				}
				else
				{
					res.put("command", "DIRECTORY_CREATE_RESPONSE");
					res.put("pathName", fPathName);
					res.put("message", "directory created");
					res.put("status", true);
					return res;
				}
			}

		case DIRECTORY_DELETE:
			if (!fileSystemManager.isSafePathName(fPathName))
			{
				res.put("command", "DIRECTORY_DELETE_RESPONSE");
				res.put("pathName", fPathName);
				res.put("message", "the file path is not safe");
				res.put("status", false);
				return res;
			}
			else
			{
				if (!fileSystemManager.dirNameExists(fPathName))
				{
					res.put("command", "DIRECTORY_DELETE_RESPONSE");
					res.put("pathName", fPathName);
					res.put("message", "directory not exist");
					res.put("status", false);
					return res;
				}
				else
				{
					boolean success = fileSystemManager.deleteDirectory(fPathName);
					if (!success)
					{
						res.put("command", "DIRECTORY_DELETE_RESPONSE");
						res.put("pathName", fPathName);
						res.put("message", "fail to delete directory");
						res.put("status", false);
						return res;
					}
					else
					{
						res.put("command", "DIRECTORY_DELETE_RESPONSE");
						res.put("pathName", fPathName);
						res.put("message", "directory delete");
						res.put("status", true);
						return res;
					}
				}
			}
		}
		return new JSONObject();

	}

	private void ProcessFileModifyRequest(String pathName, long lastModified, String md5, long size) throws IOException
	{

		if (this.fileSystemManager.isSafePathName(pathName))
			if (this.fileSystemManager.fileNameExists(pathName))
				if (!this.fileSystemManager.fileNameExists(pathName, md5))
					if (this.fileSystemManager.modifyFileLoader(pathName, md5, lastModified))
					{
						SendFileModifyResponse("file loader ready", true, pathName, md5, lastModified, size);
						SendByteRequest(pathName, md5, size, lastModified, 0,
								Long.parseLong(Configuration.getConfigurationValue("blockSize")));
					}
					else
						SendFileModifyResponse("There was a problem modifying the file", false, pathName, md5,
								lastModified, size);
				else
					SendFileModifyResponse("File already exists with matching content", false, pathName, md5,
							lastModified, size);
			else
				SendFileModifyResponse("path name does not exist", false, pathName, md5, lastModified, size);
		else
			SendFileModifyResponse("unsafe pathname given", false, pathName, md5, lastModified, size);
	}

	private void SendFileModifyResponse(String message, boolean status, String pathName, String md5, long lastModified,
			long size) throws IOException
	{

		JSONObject fileModifyResponse = new JSONObject();
		fileModifyResponse.put("command", "FILE_MODIFY_RESPONSE");
		JSONObject fileDescriptor = new JSONObject();
		fileDescriptor.put("md5", md5);
		fileDescriptor.put("lastModified", lastModified);
		fileDescriptor.put("fileSize", size);
		fileModifyResponse.put("fileDescriptor", fileDescriptor);
		fileModifyResponse.put("pathName", pathName);
		fileModifyResponse.put("message", message);
		fileModifyResponse.put("status", status);
		DatagramPacket reply = new DatagramPacket(fileModifyResponse.toJSONString().getBytes(),
				fileModifyResponse.toJSONString().getBytes().length, address, p.port);
		dsocket.send(reply);
	}

	private void ProcessFileBytesRequest(String md5, long position, long size, long lastModified, String path,
			long length) throws NoSuchAlgorithmException, IOException
	{
		// check whether the size less than the length
		if (size < length)
			length = size;
		ByteBuffer buffer = this.fileSystemManager.readFile(md5, position, length);
		log.info("write Buffer: " + buffer);

		String content = Base64.getEncoder().encodeToString(buffer.array());
		log.info("Content of the file requested: " + content);
		SendByteResponse(md5, lastModified, size, path, position, length, content);
	}

	private void SendByteResponse(String md5, long lastModified, long fileSize, String pathName, long position,
			long length, String content) throws IOException
	{

		JSONObject fileBytesResponse = new JSONObject();
		fileBytesResponse.put("command", "FILE_BYTES_RESPONSE");
		JSONObject fileDescriptor = new JSONObject();
		fileDescriptor.put("md5", md5);
		fileDescriptor.put("lastModified", lastModified);
		fileDescriptor.put("fileSize", fileSize);
		fileBytesResponse.put("fileDescriptor", fileDescriptor);
		fileBytesResponse.put("pathName", pathName);
		fileBytesResponse.put("position", position);
		fileBytesResponse.put("length", length);
		fileBytesResponse.put("content", content);
		fileBytesResponse.put("message", "successful read");
		fileBytesResponse.put("status", true);
		DatagramPacket reply = new DatagramPacket(fileBytesResponse.toJSONString().getBytes(),
				fileBytesResponse.toJSONString().getBytes().length, address, p.port);
		dsocket.send(reply);
	}

	private void ProcessFileCreateRequest(String path, String md5, long size, long modified)
			throws NoSuchAlgorithmException, IOException
	{
		if (this.fileSystemManager.isSafePathName(path))
		{

			if (!this.fileSystemManager.fileNameExists(path))
			{
				// create file loader
				boolean success = this.fileSystemManager.createFileLoader(path, md5, size, modified);
				if (success)
				{
					if (this.fileSystemManager.checkShortcut(path))
						log.info("Using a local copy of the file.");
					else
					{
						// create a Byte Request to get the message
						SendFileCreateResponse("file loader ready", true, path, md5, size, modified);
						SendByteRequest(path, md5, size, modified, 0,
								Long.parseLong(Configuration.getConfigurationValue("blockSize")));
					}
				}
				else
					SendFileCreateResponse("there was a problem creating the file", false, path, md5, size, modified);
			}
			else
				SendFileCreateResponse("pathname already exists", false, path, md5, size, modified);

		}
		else
			SendFileCreateResponse("unsafe pathname given", false, path, md5, size, modified);
	}

	private void SendFileCreateResponse(String message, boolean status, String path, String md5, long size,
			long modified) throws IOException
	{

		JSONObject fileCreateResponse = new JSONObject();
		fileCreateResponse.put("command", "FILE_CREATE_RESPONSE");
		JSONObject fDes = new JSONObject();
		fDes.put("md5", md5);
		fDes.put("lastModified", modified);
		fDes.put("fileSize", size);
		fileCreateResponse.put("pathName", path);
		fileCreateResponse.put("message", message);
		fileCreateResponse.put("status", status);
		DatagramPacket reply = new DatagramPacket(fileCreateResponse.toJSONString().getBytes(),
				fileCreateResponse.toJSONString().getBytes().length, address, p.port);
		dsocket.send(reply);
	}

	private void SendByteRequest(String path, String md5, long size, long modified, long position, long length)
			throws IOException
	{

		JSONObject fileBytesRequest = new JSONObject();
		fileBytesRequest.put("command", "FILE_BYTES_REQUEST");
		JSONObject fDes = new JSONObject();
		fDes.put("md5", md5);
		fDes.put("lastModified", modified);
		fDes.put("fileSize", size);
		fileBytesRequest.put("fileDescriptor", fDes);
		fileBytesRequest.put("pathName", path);
		fileBytesRequest.put("position", position);
		fileBytesRequest.put("length", length);
		DatagramPacket reply = new DatagramPacket(fileBytesRequest.toJSONString().getBytes(),
				fileBytesRequest.toJSONString().getBytes().length, address, p.port);
		dsocket.send(reply);
	}
}
