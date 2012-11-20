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

import it.unipmn.di.dcs.common.annotation.TODO;

import it.unipmn.di.dcs.cloud.core.middleware.IMiddlewareManager;
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
 * Service scheduler passing through the repository manager, based on raw-socket
 * implementation.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class RawSocketServiceSchedulerThroughRepoMngr implements IServiceScheduler
{
	// "Server Protocol Version" messages.
	private static final String SRVID_REQUEST = "SRVID";
	private static final Pattern SRVID_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\S+)$");
	private static final Pattern SRVID_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Server Protocol Version" messages.
	private static final String SRVPROTOVER_REQUEST = "SRVPROTOVER";
	private static final Pattern SRVPROTOVER_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\S+)$");
	private static final Pattern SRVPROTOVER_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
//	// "Service Status" messages.
//	private static final String SERVICESTATUS_REQUEST = "GETVMSTATUS";
//	private static final Pattern SERVICESTATUS_OK_REPLY_PATTERN = Pattern.compile("^OK\\s+(\\d+)$");
//	private static final Pattern SERVICESTATUS_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Service Stop" messages.
	private static final String STOPSERVICE_REQUEST = "STOPVM";
	private static final Pattern STOPSERVICE_OK_REPLY_PATTERN = Pattern.compile("^OK(?:\\s+(\\d+))?$");
	private static final Pattern STOPSERVICE_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");
	// "Submit Service" messages.
	private static final String SUBMITSERVICE_REQUEST = "SUBMITVM";
	private static final Pattern SUBMITSERVICE_OK_REPLY_PATTERN = Pattern.compile("^OK(?:\\s+(\\d+))?$");
	private static final Pattern SUBMITSERVICE_ERROR_REPLY_PATTERN = Pattern.compile("^ERR(?:\\s+(-?\\d+))?$");

	private IMiddlewareManager middleware;
	private int nextServiceId__dontUsetMe = 1;//FIXME: a workaround to use until the server-side code is complete.

	public RawSocketServiceSchedulerThroughRepoMngr(IMiddlewareManager middleware)
	{
		this.middleware = middleware;
	}

	//@{ IServiceScheduler implementation /////////////////////

	public boolean isRunning() throws CloudSchedulerException
	{
		// This method always returns TRUE because the repo manager
		// to contact depends on the service to be scheduled.

		return true;
	}

	public String getId() throws CloudSchedulerException
	{
		return "0";
	}

	public String getServerProtocolVersion() throws CloudSchedulerException
	{
		// This method always returns "n/a" because the repo manager
		// to contact depends on the service to be scheduled.

		return "n/a";
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
			sock = new Socket( GetServerAddressFromService( svc ), GetServerPortFromService( svc ) );
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
			out.println( SUBMITSERVICE_REQUEST + " " + svc.getId() + " " + machine.getId() );
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
                                matcher = SUBMITSERVICE_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while submitting the service '" + svc.getId() + "' to the server. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Submit Service" reply message
				matcher = SUBMITSERVICE_OK_REPLY_PATTERN.matcher( fromServer );
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
//						svcHnd = new ServiceHandle( this.nextServiceId__dontUsetMe++ );
//					}
//					svcHnd.setName( svc.getName() ); //FIXME: Is this right?!

                                        allOk = true;
					break;
				}
			}
		}
		catch (CloudSchedulerException cse)
		{
			throw cse;
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

		return vm;
	}

	public IServiceHandle getServiceHandle(int shndId) throws CloudSchedulerException
	{
		IServiceHandle shnd = null;

		try
		{
			shnd = this.middleware.getInformationService().getServiceHandle(shndId);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to get the service handle '" + shndId + "'.");
		}

		return shnd;
	}

	public ExecutionStatus getServiceStatus(IServiceHandle shnd) throws CloudSchedulerException
	{
		try
		{
			return this.middleware.getInformationService().getServiceStatus(shnd);
		}
		catch (Exception e)
		{
			throw new CloudSchedulerException("Unable to get to the execution status of service '" + shnd + "'", e);
		}
	}

	public boolean stopService(IServiceHandle shnd) throws CloudSchedulerException
	{
		boolean allOk = false;
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;
		ICloudService svc = null;
		InetAddress schedSrvAddr = null;
		int schedSrvPort = 0;

		try
		{
			svc = this.middleware.getInformationService().getService(shnd);

			schedSrvAddr = GetServerAddressFromService( svc );
			schedSrvPort = GetServerPortFromService( svc );

			sock = new Socket( GetServerAddressFromService( svc ), GetServerPortFromService( svc ) );
			out = new PrintWriter( sock.getOutputStream(), true );
			in = new BufferedReader(
				new InputStreamReader(
					sock.getInputStream()
				)
			);
			svc = null;
		}
		catch (Exception e)
		{
			if ( svc == null )
			{
				throw new CloudSchedulerException("Unable to connect to retrieve the service from the Information Server.", e);
			}
			else
			{
				throw new CloudSchedulerException("Unable to connect to the Scheduler Server (" + schedSrvAddr + ":" + schedSrvPort + ").", e);
			}
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		int exitCode = 0;

		try
		{
			out.println( STOPSERVICE_REQUEST + " " + shnd.getId() );
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
                                matcher = STOPSERVICE_ERROR_REPLY_PATTERN.matcher(  fromServer );
                                if ( matcher.matches() )
                                {
					throw new CloudSchedulerException("Error while stopping the service '" + shnd.getId() + "' to the server. Got reply '" + fromServer + "'.");
                                }

                                // Checks for "Service Status" reply message
				matcher = STOPSERVICE_OK_REPLY_PATTERN.matcher( fromServer );
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
		catch (CloudSchedulerException cse)
		{
			throw cse;
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

	/**
	 * Returns the address of the scheduler server from the given service.
	 */
	private static InetAddress GetServerAddressFromService(ICloudService svc) throws Exception
	{
		return InetAddress.getByName( svc.getRepoManager().getHost() );
	}

	/**
	 * Returns the port of the scheduler server from the given service.
	 */
	private static int GetServerPortFromService(ICloudService svc) throws Exception
	{
		return svc.getRepoManager().getPort();
	}
}
