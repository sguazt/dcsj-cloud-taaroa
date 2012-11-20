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

package it.unipmn.di.dcs.cloud.middleware.taaroa.model.is;

import it.unipmn.di.dcs.common.conversion.Convert;
import it.unipmn.di.dcs.common.util.Strings;

import it.unipmn.di.dcs.cloud.core.middleware.model.ICloudService;
import it.unipmn.di.dcs.cloud.core.middleware.model.IMachineManagerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.IPhysicalMachine;
import it.unipmn.di.dcs.cloud.core.middleware.model.IRepoManagerInfo;
//import it.unipmn.di.dcs.cloud.core.middleware.model.IServiceSchedulerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.is.InformationProviderException;
import it.unipmn.di.dcs.cloud.core.middleware.model.is.IInformationProvider;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.ExecutionStatus;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceHandle;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.ModelFactory;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.RawSocketServiceScheduler;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.RawSocketServiceSchedulerUtil;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.IVirtualMachine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information provider implementation based on raw socket.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class RawSocketInformationProvider implements IInformationProvider
{
	// ERROR
	protected static final Pattern ERROR_REPLY_PATTERN = Pattern.compile( "^ERR(?:\\s+(\\d+))?$" );
	// GETPHYMACH
	/** The "Get Physical Machine" request message: {@code GETPHYMACH}. */
	protected static final String GETPHYMACH_REQUEST = "GETPHYMACH";
	/** Pattern for the "Get Physical Machine" reply message: {@code OK&lt;b&gt;PHY_IP&lt;b&gt;MM_PORT}. */
	protected static final Pattern GETPHYMACH_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\S+)\\s+(\\d+)$" );
	// GETVM
	/** The "Get Service Handle" request message: {@code GETVM}. */
	protected static final String GETVM_REQUEST = "GETVM";
	/** Pattern for the "Get Service Handle" reply message: {@code OK&lt;b&gt;SERV_ID&lt;b&gt;PHY_ID&lt;b&gt;VM_LOCAL_ID&lt;b&gt;VIRT_IP&lt;b&gt;VIRT_PORT&lt;b&gt;STATUS}. */
	protected static final Pattern GETVM_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)$" );
	// GETVMMACHMNGR
	/**
	 * The "Get Machine Manager for Service Handle" request message:
	 * {@code GETVMSERV}.
	 */
	protected static final String GETVMMACHMNGR_REQUEST = "GETVMMACHMNGR";
	/** Pattern for the "Get Machine Manager for Service Handle" reply
	 * message: {@code OK&lt;b&gt;PHY_ID&lt;b&gt;PHY_IP&lt;b&gt;MM_PORT&lt;b&gt;VM_LOCAL_ID}. */
	protected static final Pattern GETVMMACHMNGR_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)$" );
	// GETVMSERV
	/** The "Get Service for Service Handle" request message: {@code GETVMSERV}. */
	protected static final String GETVMSERVICE_REQUEST = "GETVMSERV";
	/** Pattern for the "Get Service for Services" reply message: {@code OK&lt;b&gt;SERV_ID&lt;b&gt;SERV_NAME&lt;b&gt;RM_ID&lt;b&gt;RM_IP&lt;b&gt;RM_PORT}. */
	protected static final Pattern GETVMSERVICE_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)$" );
	// GETVMSTATUS
	/** The "Service Execution Status" request message: {@code SRVPROTOVER}. */
	protected static final String GETVMSTATUS_REQUEST = "GETVMSTATUS";
	/** Pattern for the "Service Execution Status" reply message: {@code OK&lt;b&gt;PROTO_VER}. */
	protected static final Pattern GETVMSTATUS_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)$" );
	// LISTPHYMACH
	/** The "List of Physical Machines" request message: {@code LISTPHYMACH}. */
	protected static final String LISTPHYSICALMACHINES_REQUEST = "LISTPHYMACH";
	/** Pattern for the "List of Physical Machines" reply message: {@code OK&lt;b&gt;MACH_ID&lt;b&gt;MACH_IP&lt;b&gt;MM_PORT}. */
	protected static final Pattern LISTPHYSICALMACHINES_HEAD_REPLY_PATTERN = Pattern.compile( "^OK\\s+(.+)$" );
	protected static final Pattern LISTPHYSICALMACHINES_TAIL_REPLY_PATTERN = Pattern.compile( "^(\\d+)\\s+(\\S+)\\s+(\\d+)\\s*\\.?$" );
	// LISTREPO
	/** The "List of Services" request message: {@code LISTREPO}. */
	protected static final String LISTREPOMNGRS_REQUEST = "LISTREPO";
	/** Pattern for the "List of Services" reply message: {@code OK&lt;b&gt;RM_ID&lt;b&gt;IP_ADDR&lt;b&gt;PORT&lt;b&gt;Base64(USER)&lt;b&gt;Base64(PASSWORD)}. */
	protected static final Pattern LISTREPOMNGRS_HEAD_REPLY_PATTERN = Pattern.compile( "^OK\\s+(.+)$" );
	protected static final Pattern LISTREPOMNGRS_TAIL_REPLY_PATTERN = Pattern.compile( "^(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s*\\.?$" );
	// LISTVM
	/** The "List of Service Handles" request message: {@code LISTVM}. */
	protected static final String LISTVMS_REQUEST = "LISTVM";
	/** Pattern for the "List of Service Handles" reply message: {@code OK&lt;b&gt;VM_ID&lt;b&gt;PHY_ID&lt;b&gt;VM_LOCAL_ID&lt;b&gt;VIRT_IP&lt;b&gt;VIRT_PORT&lt;b&gt;STATUS}. */
	protected static final Pattern LISTVMS_HEAD_REPLY_PATTERN = Pattern.compile( "^OK\\s+(.+)$" );
	protected static final Pattern LISTVMS_TAIL_REPLY_PATTERN = Pattern.compile( "^(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)\\s*\\.?$" );
	// LISTSERV
	/** The "List of Services" request message: {@code LISTSERV}. */
	protected static final String LISTSERVICES_REQUEST = "LISTSERV";
	/** Pattern for the "List of Services" reply message: {@code OK&lt;b&gt;SERV_ID&lt;b&gt;SERV_NAME&lt;b&gt;RM_ID&lt;b&gt;RM_IP&lt;b&gt;RM_PORT}. */
	protected static final Pattern LISTSERVICES_HEAD_REPLY_PATTERN = Pattern.compile( "^OK\\s+(.+)$" );
	protected static final Pattern LISTSERVICES_TAIL_REPLY_PATTERN = Pattern.compile( "^(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s*\\.?$" );
	// REGPHYMACH
	/** The "Physical Machine Registration" request message: {@code REGPHYMACH}. */
	protected static final String REGPHYMACH_REQUEST = "REGPHYMACH";
	/** Pattern for the "Physical Machine Registration" reply message: {@code OK&lt;b&gt;PHY_ID}. */
	protected static final Pattern REGPHYMACH_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)$" );
	// REGREPO
	/** The "Repository Manager Registration" request message: {@code REGREPO}. */
	protected static final String REGREPO_REQUEST = "REGREPO";
	/** Pattern for the "Repository Manager Registration" reply message: {@code OK&lt;b&gt;RM_ID}. */
	protected static final Pattern REGREPO_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)$" );
	// REGSERV
	/** The "Service Registration" request message: {@code REGSERV}. */
	protected static final String REGSERV_REQUEST = "REGSERV";
	/** Pattern for the "Service Registration" reply message: {@code OK&lt;b&gt;SERV_ID}. */
	protected static final Pattern REGSERV_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)$" );
	// REGVM
	/** The "Virtual Machine Registration" request message: {@code REGVM}. */
	protected static final String REGVM_REQUEST = "REGVM";
	/** Pattern for the "Virtual Machine Registration" reply message: {@code OK&lt;b&gt;VM_ID}. */
	protected static final Pattern REGVM_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\d+)$" );
	// SRVPROTOVER
	/** The "Server Protocol Version" request message: {@code SRVPROTOVER}. */
	protected static final String SRVPROTOVER_REQUEST = "SRVPROTOVER";
	/** Pattern for the "Server Protocol Version" reply message: {@code OK&lt;b&gt;PROTO_VER}. */
	protected static final Pattern SRVPROTOVER_REPLY_PATTERN = Pattern.compile( "^OK\\s+(\\S+)$" );

	private InetAddress serverAddr;
	private int serverPort;

	/** A constructor. */
	public RawSocketInformationProvider(int serverPort)
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
	public RawSocketInformationProvider(InetAddress serverAddr, int serverPort)
	{
		this.serverAddr = serverAddr;
		this.serverPort = serverPort;
	}

	//@{ IInformationProvider implementation ///////////////////////

	public boolean isRunning() throws InformationProviderException
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
			throw new InformationProviderException("Unable to connect to the Information Server", se);
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

	public IPhysicalMachine getPhysicalMachine(int phyMachId) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		IPhysicalMachine phy = null;

		try
		{
			boolean bomFound = false; // TRUE if the Begin-Of-Message is found

			out.println( GETPHYMACH_REQUEST + " " + phyMachId );
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the physical machine from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for the "Get Physical Machine" reply message
				matcher = GETPHYMACH_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String phyName = matcher.group(1);
					int machMngrPort = Integer.parseInt( matcher.group(2) );

					phy = ModelFactory.Instance().physicalMachine();
					phy.setId( phyMachId );
					phy.setName( phyName );
					phy.setMachineManagerPort( machMngrPort );
				}
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return phy;
	}

	public List<IPhysicalMachine> getPhysicalMachines() throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		List<IPhysicalMachine> phys = new ArrayList<IPhysicalMachine>();

		try
		{
			boolean bomFound = false; // TRUE if the Begin-Of-Message is found

			out.println( LISTPHYSICALMACHINES_REQUEST );
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the list of physical machines from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for end-of-communication
				if ( fromServer.equals(".") )
				{
					break;
				}

				// Checks for the beginning of "List Physical Machines" reply message
				if ( phys.size() == 0 )
				{
					matcher = LISTPHYSICALMACHINES_HEAD_REPLY_PATTERN.matcher( fromServer );
					if ( matcher.matches() )
					{
						fromServer = matcher.group(1);
					}
					else
					{
						throw new InformationProviderException("Expected begin of 'List of Physical Machines' message but found '" + fromServer + "'.");
					}
				}

				// Checks for the ending of "List Physical Machines" reply message
				matcher = LISTPHYSICALMACHINES_TAIL_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String phyId = matcher.group(1);
					String phyName = matcher.group(2);
					int machMngrPort = Integer.parseInt( matcher.group(3) );

					IPhysicalMachine phy = ModelFactory.Instance().physicalMachine();
					phy.setId( Integer.parseInt( phyId ) );
					phy.setName( phyName );
					phy.setMachineManagerPort( machMngrPort );
					phys.add( phy );
				}
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return phys;
	}

	public List<IRepoManagerInfo> getRepoManagers() throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		List<IRepoManagerInfo> repoMngrs = new ArrayList<IRepoManagerInfo>();

		try
		{
			out.println( LISTREPOMNGRS_REQUEST );
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
				matcher = ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the list of repository managers from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for end-of-communication
				if ( fromServer.equals(".") )
				{
					break;
				}

				// Checks for the beginning of "List Repo Manaers" reply message
				if ( repoMngrs.size() == 0 )
				{
					matcher = LISTREPOMNGRS_HEAD_REPLY_PATTERN.matcher( fromServer );
					if ( matcher.matches() )
					{
						fromServer = matcher.group(1);
					}
					else
					{
						throw new InformationProviderException("Expected begin of 'List of Repository Managers' message but found '" + fromServer + "'.");
					}
				}

				// Checks for the ending of "List Repository Managers" reply message
				matcher = LISTREPOMNGRS_TAIL_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String repoId = matcher.group(1);
					String repoIp = matcher.group(2);
					String repoPort = matcher.group(3);
					String user = new String( Convert.Base64ToBytes( matcher.group(4) ) );
					String passwd = new String( Convert.Base64ToBytes( matcher.group(5) ) );

					IRepoManagerInfo repoMngr = ModelFactory.Instance().repoManagerInfo();
					repoMngr.setId( Integer.parseInt( repoId ) );
					repoMngr.setHost( repoIp ); //FIXME: check for error
					repoMngr.setPort( Integer.parseInt( repoPort ) );
					repoMngr.setUser( user );
					repoMngr.setPassword( passwd );

					repoMngrs.add( repoMngr );
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return repoMngrs;
	}

	public List<IServiceHandle> getServiceHandles(ICloudService svc) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		List<IServiceHandle> shnds = new ArrayList<IServiceHandle>();

		try
		{
			out.println( LISTVMS_REQUEST + " " + svc.getId() );
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the running services from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for end-of-communication
				if ( fromServer.equals(".") )
				{
					break;
				}

				// Checks for the beginning of "List of Service Handles" reply message
				if ( shnds.size() == 0 )
				{
					matcher = LISTVMS_HEAD_REPLY_PATTERN.matcher( fromServer );
					if ( matcher.matches() )
					{
						fromServer = matcher.group(1);
					}
					else
					{
						throw new InformationProviderException("Expected begin of 'List of Service Handles' message but found '" + fromServer + "'.");
					}
				}

				// Checks for the ending of "List of Service Handles" reply message
				matcher = LISTVMS_TAIL_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int vmId = Integer.parseInt( matcher.group(1) );
					int phyId = Integer.parseInt( matcher.group(2) );
					String vmLocalId = matcher.group(3);
					String virtIp = matcher.group(4);
					int virtPort = Integer.parseInt( matcher.group(5) );
					int status = Integer.parseInt( matcher.group(6) );

					IVirtualMachine vm = ModelFactory.Instance().virtualMachine();
					vm.setId( vmId );
					vm.setLocalId( vmLocalId );
					vm.setPhysicalMachineId( phyId );
					vm.setVirtualHost( virtIp );
					vm.setProbingPort( virtPort );
					vm.setServiceId( svc.getId() );
					vm.setStatus(
						RawSocketServiceSchedulerUtil.SchedulerExecStatusToExecutionStatus( status )
					);

					shnds.add( vm );
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return shnds;
	}

	public String getServerProtocolVersion() throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		String protoVer = null;

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
				matcher = ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the server protocol version from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "List Services" reply message
				matcher = SRVPROTOVER_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					protoVer = matcher.group(1);
					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return protoVer;
	}

	public IMachineManagerInfo getMachineManager(IServiceHandle shnd) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		IMachineManagerInfo machMngr = null;

		try
		{
			out.println( GETVMMACHMNGR_REQUEST + " " + shnd.getId() );
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the machine manager from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Get Machine Manager" reply message
				matcher = GETVMMACHMNGR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String phyId = matcher.group(1);
					String phyIp = matcher.group(2);
					int machMngrPort = Integer.parseInt( matcher.group(3) );
					String vmLocalId = matcher.group(4);

					machMngr = ModelFactory.Instance().machineManagerInfo();
					machMngr.setId( -1 ); //FIXME: for the moment, no ID is available
					machMngr.setHost( phyIp );
					machMngr.setPort( machMngrPort );

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return machMngr;
	}

	public ICloudService getService(IServiceHandle shnd) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		ICloudService svc = null;

		try
		{
			out.println( GETVMSERVICE_REQUEST + " " + shnd.getId() );
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the service from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Get Service" reply message
				matcher = GETVMSERVICE_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String servId = matcher.group(1);
					String servName = new String( Convert.Base64ToBytes( matcher.group(2) ) );
					String repoId = matcher.group(3);
					String repoIp = matcher.group(4);
					String repoPort = matcher.group(5);

					IRepoManagerInfo repoMngr = ModelFactory.Instance().repoManagerInfo();
					repoMngr.setId( Integer.parseInt( repoId ) );
					repoMngr.setHost( repoIp ); //FIXME: check for error
					repoMngr.setPort( Integer.parseInt( repoPort ) );

					svc = ModelFactory.Instance().cloudService();
					svc.setId( Integer.parseInt( servId ) );
					svc.setName( servName );
					svc.setRepoManager( repoMngr );

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return svc;
	}

	public IServiceHandle getServiceHandle(int shndId) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
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
				matcher = ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the list of services from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for the "Get Service Handle" reply message
				matcher = GETVM_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int servId = Integer.parseInt( matcher.group(1) );
					int phyId = Integer.parseInt( matcher.group(2) );
					String vmLocalId = matcher.group(3);
					String virtIp = matcher.group(4);
					int virtPort = Integer.parseInt( matcher.group(5) );
					int status = Integer.parseInt( matcher.group(6) );

					vm = ModelFactory.Instance().virtualMachine();
					vm.setId( shndId );
					vm.setLocalId( vmLocalId );
					vm.setServiceId( servId );
					vm.setPhysicalMachineId( phyId );
					vm.setVirtualHost( virtIp );
					vm.setProbingPort( virtPort );
					vm.setStatus(
						RawSocketServiceSchedulerUtil.SchedulerExecStatusToExecutionStatus( status )
					);
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

	public List<ICloudService> getServices() throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		List<ICloudService> svcs = new ArrayList<ICloudService>();

		try
		{
			out.println( LISTSERVICES_REQUEST );
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
				matcher = ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the list of services from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for end-of-communication
				if ( fromServer.equals(".") )
				{
					break;
				}

				// Checks for the beginning of "List Services" reply message
				if ( svcs.size() == 0 )
				{
					matcher = LISTSERVICES_HEAD_REPLY_PATTERN.matcher( fromServer );
					if ( matcher.matches() )
					{
						fromServer = matcher.group(1);
					}
					else
					{
						throw new InformationProviderException("Expected begin of 'List of Services' message but found '" + fromServer + "'.");
					}
				}

				// Checks for the ending of "List Services" reply message
				matcher = LISTSERVICES_TAIL_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					String servId = matcher.group(1);
					String servName = new String( Convert.Base64ToBytes( matcher.group(2) ) );
					String repoId = matcher.group(3);
					String repoIp = matcher.group(4);
					String repoPort = matcher.group(5);

					IRepoManagerInfo repoMngr = ModelFactory.Instance().repoManagerInfo();
					repoMngr.setId( Integer.parseInt( repoId ) );
					repoMngr.setHost( repoIp );
					repoMngr.setPort( Integer.parseInt( repoPort ) );

					ICloudService svc = ModelFactory.Instance().cloudService();
					svc.setId( Integer.parseInt( servId ) );
					svc.setName( servName );
					svc.setRepoManager( repoMngr );

					svcs.add( svc );
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return svcs;
	}

	public ExecutionStatus getServiceStatus(IServiceHandle shnd) throws InformationProviderException
	{
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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
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
				matcher = ERROR_REPLY_PATTERN.matcher(  fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while retrieving the service execution status from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Service Execution Status" reply message
				matcher = GETVMSTATUS_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					status = RawSocketServiceSchedulerUtil.SchedulerExecStatusToExecutionStatus(
							Integer.parseInt( matcher.group(1) )
					);
					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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

		return status;
	}

	public IPhysicalMachine registerPhysicalMachine(IPhysicalMachine phyMach) throws InformationProviderException
	{
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;
		boolean allOk = false;

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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println(
				REGPHYMACH_REQUEST
				+ " " + phyMach.getName()
				+ " " + Convert.BytesToBase64( phyMach.getCpuType().getBytes() )
				+ " " + phyMach.getNumberOfCpu()
				+ " " + phyMach.getCpuClock()
				+ " " + phyMach.getRamSize()
				+ " " + phyMach.getHdSize()
				+ " " + phyMach.getNetSpeed()
				+ " " + phyMach.getMaxVmNumber()
				+ " " + Convert.BytesToBase64( phyMach.getMachineUserName().getBytes() )
				+ " " + Convert.BytesToBase64( phyMach.getMachinePassword().getBytes() )
				+ " " + Convert.BytesToBase64( phyMach.getVmManagerUserName().getBytes() )
				+ " " + Convert.BytesToBase64( phyMach.getVmManagerPassword().getBytes() )
				+ " " + phyMach.getMachineManagerPort()
			);
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while registering the physical machine from the server. Got reply '" + fromServer + "'.");
				}

				// Checks for "Physical Machine Registration" reply message
				matcher = REGPHYMACH_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int phyMachId = Integer.parseInt( matcher.group(1) );

					phyMach.setId( phyMachId );
					allOk = true;

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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
			throw new InformationProviderException( "Unable to register the physical machine to the Information Server" );
		}

		return phyMach;
	}

	public IRepoManagerInfo registerRepoManager(IRepoManagerInfo repoMngr) throws InformationProviderException
	{
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;
		boolean allOk = false;

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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println(
				REGREPO_REQUEST
				+ " " + repoMngr.getHost()
				+ " " + Integer.toString( repoMngr.getPort() )
				+ " " + Convert.BytesToBase64( repoMngr.getUser().getBytes() )
				+ " " + Convert.BytesToBase64( repoMngr.getPassword().getBytes() )
			);
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while registering the repository manager from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Service Registration" reply message
				matcher = REGREPO_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int repoId = Integer.parseInt( matcher.group(1) );

					repoMngr.setId( repoId );

					allOk = true;

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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
			throw new InformationProviderException( "Unable to register the repository manager to the Information Server" );
		}

		return repoMngr;
	}

	public IServiceHandle registerServiceHandle(IServiceHandle shnd) throws InformationProviderException
	{
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;
		boolean allOk = false;

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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;
		IVirtualMachine vm = null;

		try
		{
			vm = ModelFactory.Instance().virtualMachine(shnd);
			out.println(
				REGVM_REQUEST
				+ " " + vm.getServiceId()
				+ " " + vm.getPhysicalMachineId()
				+ " " + vm.getLocalId()
				+ " " + vm.getVirtualHost()
				+ " " + Integer.toString( vm.getProbingPort() )
				+ " " + Integer.toString( vm.getRequestedMem() )
				+ " " + Float.toString( vm.getRequestedCpu() )
			);
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while registering the running service from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Service Handle Registration" reply message
				matcher = REGVM_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int vmId = Integer.parseInt( matcher.group(1) );

					vm.setId( vmId );

					allOk = true;

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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
			throw new InformationProviderException( "Unable to register the running service to the Information Server" );
		}

		return vm;
	}

	public ICloudService registerService(ICloudService svc) throws InformationProviderException
	{
		Socket sock = null;
		PrintWriter out = null;
		BufferedReader in = null;
		boolean allOk = false;

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
			throw new InformationProviderException("Unable to connect to the Information Server", e);
		}

//		BufferedReader stdIn = new BufferedReader(
//				new InputStreamReader(System.in)
//		);
		String fromServer;

		try
		{
			out.println(
				REGSERV_REQUEST
				+ " " + svc.getRepoManager().getId()
				+ " " + Convert.BytesToBase64(svc.getName().getBytes())
				+ " " + Convert.BytesToBase64(
						new String(
							svc.getCpuRequirements()
							+ "#" + svc.getMemRequirements()
							+ "#" + svc.getStorageRequirements()
						).getBytes()
				)
			);
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
				matcher = ERROR_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					throw new InformationProviderException("Error while registering the service from the server. Got replay '" + fromServer + "'.");
				}

				// Checks for "Service Registration" reply message
				matcher = REGSERV_REPLY_PATTERN.matcher( fromServer );
				if ( matcher.matches() )
				{
					int servId = Integer.parseInt( matcher.group(1) );

					svc.setId( servId );

					allOk = true;

					break;
				}

				// Unknown reply from server... Just ignore it!
			}
		}
		catch (InformationProviderException ipe)
		{
			throw ipe;
		}
		catch (Exception e)
		{
			throw new InformationProviderException( "Error while communicating with Information Server.", e );
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
			throw new InformationProviderException( "Unable to register the service to the Information Server" );
		}

		return svc;
	}

	//@} IInformationProvider implementation ///////////////////////
}
