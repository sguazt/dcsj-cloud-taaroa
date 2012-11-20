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

package it.unipmn.di.dcs.cloud.middleware.taaroa;

import it.unipmn.di.dcs.cloud.core.middleware.IMiddlewareEnv;
import it.unipmn.di.dcs.cloud.core.middleware.IMiddlewareManager;
//import it.unipmn.di.dcs.cloud.core.middleware.model.IServiceSchedulerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.is.IInformationProvider;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceScheduler;
import it.unipmn.di.dcs.cloud.core.middleware.service.IInformationService;
import it.unipmn.di.dcs.cloud.core.middleware.service.IServiceSchedulerService;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.is.RawSocketInformationProvider;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.RawSocketServiceScheduler;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.RawSocketServiceSchedulerThroughRepoMngr;
import it.unipmn.di.dcs.cloud.middleware.taaroa.service.InformationService;
import it.unipmn.di.dcs.cloud.middleware.taaroa.service.ServiceSchedulerService;

import java.net.InetAddress;

/**
 * The middleware manager for the TAAROA middleware.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class MiddlewareManager implements IMiddlewareManager
{
	private IMiddlewareEnv env;
	private IInformationProvider is;
	private IServiceScheduler svcSched;

	public MiddlewareManager()
	{
		this.env = new MiddlewareEnv();
	}

	public MiddlewareManager(IMiddlewareEnv env)
	{
		this.env = env;
	}

	/** Returns an instance of the information provider. */
	public synchronized IInformationProvider getInformationProvider()
	{
		if ( this.is == null )
		{
//FIXME: how to distinguish from different IS classes?
//@{ EXPERIMENTAL
// Add this code...
//				IInformationProviderFactory isFactory = Class.forName( this.env.getInformationProviderFactoryClass() ).newInstance();
//				this.is = isFactory.getInformationProvider( this.env );
// ... and remove the code below
//@} EXPERIMENTAL

			InetAddress addr;
			try
			{
				addr = InetAddress.getByName( this.env.getInformationServiceHost() );
			}
			catch (Exception e)
			{
				addr = null;
			}

			if ( addr != null )
			{
				this.is = new RawSocketInformationProvider(
					addr,
					this.env.getInformationServicePort()
				);
			}
		}

		return this.is;
	}

	/** Returns an instance of the service scheduler. */
	public synchronized IServiceScheduler getServiceScheduler()
	{
		if ( this.svcSched == null )
		{
//FIXME: how to distinguish from different scheduler classes?
//@{ EXPERIMENTAL
// Add this code...
//				IServiceSchedulerFactory svcSchedFactory = Class.forName( this.env.getServiceSchedulerFactoryClass() ).newInstance();
//				this.svcSched = svcSchedFactory.getServiceScheduler( this.env );
// ... and remove the code below
//@} EXPERIMENTAL

//			InetAddress addr;
//			//int port;
//
//			try
//			{
//				addr = InetAddress.getByName( this.env.getServiceSchedulerServiceHost() );
//
//			}
//			catch (Exception e)
//			{
//				addr = null;
//				//port = 0;
//			}
//
//			if ( addr != null )
//			{
//				this.svcSched = new RawSocketServiceScheduler(
//					addr,
//					this.env.getServiceSchedulerServicePort()
//				);
				this.svcSched = new RawSocketServiceSchedulerThroughRepoMngr( this );
//			}
		}

		return this.svcSched;
	}

	//@{ IMiddlewareManager implementation /////////////////////////////////

	public void setEnv(IMiddlewareEnv env)
	{
		this.env = env;

		this.is = null;
		this.svcSched = null;
	}

	public IMiddlewareEnv getEnv()
	{
		return this.env;
	}

	public IInformationService getInformationService()
	{
		return new InformationService( this.getInformationProvider() );
	}

	public IServiceSchedulerService getServiceSchedulerService()
	{
		return new ServiceSchedulerService( this.getServiceScheduler() );
	}

	//@} IMiddlewareManager implementation /////////////////////////////////
}
