/*
 * Copyright (C) 2008  Distributed Computing System (DCS) Group, Computer
 * Science Department - University of Piemonte Orientale, Alessandria (Italy).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched;

import it.unipmn.di.dcs.cloud.core.middleware.model.ICloudService;
import it.unipmn.di.dcs.cloud.core.middleware.model.IPhysicalMachine;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.CloudSchedulerException;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.ExecutionStatus;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceHandle;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceScheduler;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.ModelFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service scheduler based on raw-socket implementation.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class RawSocketServiceScheduler implements IServiceScheduler
{
	// "Server Protocol Version" messages.
	private static final String SRVID_REQUEST = "SRVID";
	private static final Pattern SRVID_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\S+)$");
	private static final Pattern SRVID_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Server Protocol Version" messages.
	private static final String SRVPROTOVER_REQUEST = "SRVPROTOVER";
	private static final Pattern SRVPROTOVER_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\S+)$");
	private static final Pattern SRVPROTOVER_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Get Service Handle" messages.
	private static final String GETVM_REQUEST = "GETVM";
	private static final Pattern GETVM_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)$");
	private static final Pattern GETVM_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Service Status" messages.
	private static final String GETVMSTATUS_REQUEST = "GETVMSTATUS";
	private static final Pattern GETVMSTATUS_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\d+)$");
	private static final Pattern GETVMSTATUS_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Service Stop" messages.
	private static final String STOPVM_REQUEST = "STOPVM";
	private static final Pattern STOPVM_OK_REPLY_PATTERN = Pattern.compile("^OK(?:\\s+(\\d+))?$");
	private static final Pattern STOPVM_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Submit Service" messages.
	private static final String SUBMITVM_REQUEST = "SUBMITVM";
	private static final Pattern SUBMITVM_OK_REPLY_PATTERN = Pattern.compile("^OK(?:\\s+(\\d+))?$");
	private static final Pattern SUBMITVM_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");

	private InetAddress serverAddr;
	private int serverPort;

	/** A constructor. */
	public RawSocketServiceScheduler(int serverPort)
	{
		try
		{
			this.serverAddr = InetAddress.getLocalHost();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Cannot bind to the localhost address");
		}

		this.serverPort = serverPort;
	}

	/** A constructor. */
	public RawSocketServiceScheduler(InetAddress serverAddr, int serverPort)
	{
		this.serverAddr = serverAddr;
		this.serverPort = serverPort;
	}

	//@{ IServiceScheduler implementation /////////////////////

//	public IServiceHandle submitService(ICloudService svc) throws CloudSchedulerException
//	{
//		throw new CloudSchedulerException("Not Implemented");
//	}

	public boolean isRunning() throws CloudSchedulerException
	{
		boolean running = false;
		Socket sock = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );

			running = sock.isConnected();
		}
		catch (SecurityException se)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", se);
		}
		catch (Exception e)
		{
			running = false;
		}
		finally
		{
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		return running;
	}

	public String getId() throws CloudSchedulerException
	{
		String id = null;
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println( SRVID_REQUEST );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
                                matcher = SRVID_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while requesting the server ID. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Server ID" reply message
				matcher = SRVID_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					id = matcher.group(1);

                                        allOk = true;
					break;
				}

				// Unexpected reply from server... retry
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to retrieve the server identifier.");
		}

		return id;
	}

	public String getServerProtocolVersion() throws CloudSchedulerException
	{
		String protoVer = null;
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println( SRVPROTOVER_REQUEST );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
                                matcher = SRVPROTOVER_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while requesting the protocol version. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Server Protocol Version" reply message
				matcher = SRVPROTOVER_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					protoVer = matcher.group(1);

                                        allOk = true;
					break;
				}

				// Unexpected reply from server... retry
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to retrieve the server protocol version.");
		}

		return protoVer;
	}

	public IServiceHandle submitService(ICloudService svc, IPhysicalMachine machine) throws CloudSchedulerException
	{
		IVirtualMachine vm = null;
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println( SUBMITVM_REQUEST + " " + svc.getId() + " " + machine.getId() );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
                                matcher = SUBMITVM_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while submitting service '" + svc.getId() + "' to the server. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Submit Service" reply message
				matcher = SUBMITVM_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					if ( matcher.groupCount() > 0 && matcher.group(1) != null )
					{
						int vmId = Integer.parseInt( matcher.group(1) );

						vm = ModelFactory.Instance().virtualMachine();
						vm.setId( vmId );
						vm.setServiceId( svc.getId() );
						//vm.setPhysicalHost( machine.getName() );
						vm.setPhysicalMachineId( machine.getId() );
					}
//					else
//					{
//						svcHnd = new ServiceHandle( 0 );
//					}
//					svcHnd.setName( svc.getName() ); //FIXME: Is this right?!

                                        allOk = true;
					break;
				}
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to submit the service.");
		}

		return vm;
	}

	public IServiceHandle getServiceHandle(int shndId) throws CloudSchedulerException
	{
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		IVirtualMachine vm = null;

		try
		{
			out.println( GETVM_REQUEST + " " + shndId );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
				matcher = GETVM_ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new CloudSchedulerException("Error while requesting service '" + shndId + "' to the server. Got reply '" + fromServer + "'.");
				}

				// Checks for "Get Service Handle" reply message
				matcher = GETVM_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int servId = Integer.parseInt( matcher.group(1) );
					int phyId = Integer.parseInt( matcher.group(2) );
					String localId = matcher.group(3);
					String virtIp = matcher.group(4);
					int virtPort = Integer.parseInt( matcher.group(5) );

					vm = ModelFactory.Instance().virtualMachine();
					vm.setId( shndId );
					vm.setServiceId( servId );
					//vm.setPhysicalHost( phyIp );
					vm.setPhysicalMachineId( phyId );
					vm.setLocalId( localId );
					vm.setVirtualHost( virtIp );
					vm.setProbingPort( virtPort );

                    allOk = true;
					break;
				}
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to retrieve the service handle for id '" + shndId + "'.");
		}

		return vm;
	}

	public ExecutionStatus getServiceStatus(IServiceHandle shnd) throws CloudSchedulerException
	{
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		ExecutionStatus status = ExecutionStatus.UNKNOWN;

		try
		{
			out.println( GETVMSTATUS_REQUEST + " " + shnd.getId() );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
                                matcher = GETVMSTATUS_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while requesting the status of service '" + shnd.getId() + "' to the server. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Service Status" reply message
				matcher = GETVMSTATUS_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					status = RawSocketServiceSchedulerUtil.SchedulerExecStatusToExecutionStatus(
						Integer.parseInt( matcher.group(1) )
					);

                                        allOk = true;
					break;
				}
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to retrieve service status.");
		}

		return status;
	}

	public boolean stopService(IServiceHandle shnd) throws CloudSchedulerException
	{
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try
		{
			sock = new Socket( this.serverAddr, this.serverPort );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to connect to the Scheduler Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		int exitCode = 0;

		try
		{
			out.println( STOPVM_REQUEST + " " + shnd.getId() );
			while ( ( fromServer = in.readLine() ) != null )
			{
				fromServer = fromServer.trim();

				// Checks for empty lines
				if ( fromServer.length() == 0 )
				{
					// Skip empty lines
					continue;
				}

				Matcher matcher = null;

				// Checks for error
                                matcher = STOPVM_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while stopping the service '" + shnd.getId() + "' to the server. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Service Status" reply message
				matcher = STOPVM_OK_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					if ( matcher.groupCount() > 0 && matcher.group(1) != null )
					{
						exitCode = Integer.parseInt( matcher.group(1) );
					}

                                        allOk = true;
					break;
				}
			}
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException( "Error while communicating with Scheduler Server.", e );
		}
		finally
		{
			if ( out != null )
			{
				try { out.close(); } catch (Exception e) { /* Ignore */ }
				out = null;
			}
			if ( in != null )
			{
				try { in.close(); } catch (Exception e) { /* Ignore */ }
				in = null;
			}
//			if ( stdIn != null )
//			{
//				try { stdIn.close(); } catch (Exception e) { /* Ignore */ }
//				stdIn = null;
//			}
			if ( sock != null )
			{
				try { sock.close(); } catch (Exception e) { /* Ignore */ }
				sock = null;
			}
		}

		if ( !allOk )
		{
			throw new CloudSchedulerException("Unable to stop the service.");
		}

		return allOk;
	}

	//@} IServiceScheduler implementation /////////////////////
}
