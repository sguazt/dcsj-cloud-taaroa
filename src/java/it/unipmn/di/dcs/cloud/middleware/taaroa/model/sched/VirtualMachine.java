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

import it.unipmn.di.dcs.cloud.core.middleware.model.sched.ExecutionStatus;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceHandle;

import java.io.Serializable;

/**
 * Class representing a VirtualMachine that is a running service.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class VirtualMachine implements IVirtualMachine, Serializable
{
	private static final long serialVersionUID = 100000L;

	private int id;
	private int serviceId;
	private int phyMachId;
//	@Deprecated
//	private String phost;
	private String localId;
	private String vhost;
	private int vport;
	private int reqMem;
	private float reqCpu;
	private ExecutionStatus status;

	public VirtualMachine()
	{
		// empty
	}

	public VirtualMachine(IServiceHandle shnd)
	{
		this();

		this.id = shnd.getId();
		this.serviceId = shnd.getServiceId();
		this.phyMachId = shnd.getPhysicalMachineId();
//		this.phost = shnd.getPhysicalHost();
		this.vhost = shnd.getVirtualHost();
		this.vport = shnd.getProbingPort();
	}

	//@{ IVirtualMachine implementation ////////////////////////////////////

	public void setId(int value)
	{
		this.id = value;
	}

	public void setServiceId(int value)
	{
		this.serviceId = value;
	}

	public void setPhysicalMachineId(int value)
	{
		this.phyMachId = value;
	}

//	@Deprecated
//	public void setPhysicalHost(String value)
//	{
//		this.phost = value;
//	}

	public void setLocalId(String value)
	{
		this.localId = value;
	}

	public String getLocalId()
	{
		return this.localId;
	}

	public void setVirtualHost(String value)
	{
		this.vhost = value;
	}

	public void setProbingPort(int value)
	{
		this.vport = value;
	}

	public void setRequestedMem(int value)
	{
		this.reqMem = value;
	}

	public int getRequestedMem()
	{
		return this.reqMem;
	}

	public void setRequestedCpu(float value)
	{
		this.reqCpu = value;
	}

	public float getRequestedCpu()
	{
		return this.reqCpu;
	}

	public void setStatus(ExecutionStatus value)
	{
		this.status = value;
	}

	//@{ IServiceHandle implementation /////////////////////////////////////

	public int getId()
	{
		return this.id;
	}

	public String getName()
	{
		return "Service#" + this.id;
	}

	public int getServiceId()
	{
		return this.serviceId;
	}

//	@Deprecated
//	public String getPhysicalHost()
//	{
//		return this.phost;
//	}

	public int getPhysicalMachineId()
	{
		return this.phyMachId;
	}

	public String getVirtualHost()
	{
		return this.vhost;
	}

	public int getProbingPort()
	{
		return this.vport;
	}

	public float getAllocatedCpuFraction()
	{
		return -1; //FIXME: to be implemented
	}

	public float getAllocatedRamFraction()
	{
		return -1; //FIXME: to be implemented
	}

	public float getAllocatedDiskFraction()
	{
		return -1; //FIXME: to be implemented
	}

	public ExecutionStatus getStatus()
	{
		return this.status;
	}


	//@} IServiceHandle implementation /////////////////////////////////////

	//@} IVirtualMachine implementation ////////////////////////////////////

	@Override
	public String toString()
	{
		return	"<"
			+ "Id: " + this.getId()
			+ ", Service: " + this.getServiceId()
			+ ", Physical Machine: " + this.getPhysicalMachineId()
			+ ", Local Id: " + this.getLocalId()
			+ ", Virtual Host: " + this.getVirtualHost()
			+ ", Probing Port: " + this.getProbingPort()
			+ ", Req. Mem.: " + this.getRequestedMem()
			+ ", Req. CPU: " + this.getRequestedCpu()
			+ ", Status: " + this.getStatus()
			+ ">";
	}
}
