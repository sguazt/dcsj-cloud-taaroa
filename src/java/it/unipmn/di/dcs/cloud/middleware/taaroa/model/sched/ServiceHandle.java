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

import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceHandle;

/**
 * Service Handle for a submitted TAAROA service.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
@Deprecated
public class ServiceHandle //implements IServiceHandle
{
	/** The service identifier. */
	private String id;

	/** The service mnemonic name. */
	private String name;

	private int serviceId;

	private int phyMachId;

//	@Deprecated
//	private String phost;

	/** A constructor. */
	public ServiceHandle(String id)
	{
		this.id = String.valueOf( id );
	}

	/** Sets the service identifier. */
	public void setId(String value)
	{
		this.id = value;
	}

	/** Sets the service name. */
	public void setName(String value)
	{
		this.name = value;
	}

	/** Sets the service id. */
	public void setServiceId(int value)
	{
		this.serviceId = value;
	}

	/** Sets the physical machine id. */
	public void setPhysicalMachineId(int value)
	{
		this.phyMachId = value;
	}

	@Override
	public String toString()
	{
		return this.id;
	}

	//@{ IServiceHandle implementation /////////////////////////////////////

	public String getId()
	{
		return this.id;
	}

	public String getName()
	{
		return this.name;
	}

	public int getServiceId()
	{
		return this.serviceId;
	}

	public int getPhysicalMachineId()
	{
		return this.phyMachId;
	}

//	@Deprecated
//	public String getPhysicalHost()
//	{
//		return this.phost;
//	}

	//@} IServiceHandle implementation /////////////////////////////////////
}
