package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

/*
 * process the message receive and send to avoid some troubles.
 */
public class sockethandle
{
	private static Logger log = Logger.getLogger(sockethandle.class.getName());

	protected static String getSocketMessage(Socket socket)
	{
		try
		{
			log.info("receive message");
			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String response = null;
			String line = null;
			StringBuffer sb = new StringBuffer();
			line = br.readLine();
			sb.append(line);
			response = sb.toString();
			return response;
		}
		catch (IOException e)
		{

			e.printStackTrace();
		}
		return null;
	}

	public static void sendSocketMessage(String message, Socket socket) throws IOException
	{
		if (socket == null || socket.isClosed())
			log.info("Socket is closed...");
		BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
		log.info("send" + message);
		output.write(message);
		output.newLine();
		output.flush();
	}
}