package unimelb.bitbox;

import org.kohsuke.args4j.Option;

public class CmdLineArg
{

	@Option(required = true, name = "-c", usage = "Commandname")
	private String command;

	@Option(required = true, name = "-s", usage = "ServerAddress")
	private String server;

	@Option(required = false, name = "-p", usage = "PeerAddress")
	private String peer;

	public String getCmd()
	{
		return command;
	}

	public String getServer()
	{
		return server;
	}

	public String getPeer()
	{
		return peer;
	}

}
