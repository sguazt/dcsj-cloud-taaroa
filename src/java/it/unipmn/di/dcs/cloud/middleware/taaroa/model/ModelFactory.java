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

package it.unipmn.di.dcs.cloud.middleware.taaroa.model;

import it.unipmn.di.dcs.cloud.core.middleware.model.CloudService;
import it.unipmn.di.dcs.cloud.core.middleware.model.ICloudService;
import it.unipmn.di.dcs.cloud.core.middleware.model.IMachineManagerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.IPhysicalMachine;
import it.unipmn.di.dcs.cloud.core.middleware.model.IRepoManagerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.MachineManagerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.PhysicalMachine;
import it.unipmn.di.dcs.cloud.core.middleware.model.RepoManagerInfo;
import it.unipmn.di.dcs.cloud.core.middleware.model.sched.IServiceHandle;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.IVirtualMachine;
import it.unipmn.di.dcs.cloud.middleware.taaroa.model.sched.VirtualMachine;

/**
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class ModelFactory
{
	/** A constructor. */
	private ModelFactory()
	{
		// empty
	}

	/** Returns the singleton instance of this class. */
	public static ModelFactory Instance()
	{
		return ModelFactoryHolder.Instance;
	}

	/**
	 * Returns an instance implementing the {@code ICloudService} interface.
	 */
	public ICloudService cloudService()
	{
		return new CloudService();
	}

	/**
	 * Returns an instance implementing the {@code IMachineManagerInfo}
	 * interface.
	 */
	public IMachineManagerInfo machineManagerInfo()
	{
		return new MachineManagerInfo();
	}

	/**
	 * Returns an instance implementing the {@code IPhysicalMachine}
	 * interface.
	 */
	public IPhysicalMachine physicalMachine()
	{
		return new PhysicalMachine();
	}

	/**
	 * Returns an instance implementing the {@code IRepoManagerInfo}
	 * interface.
	 */
	public IRepoManagerInfo repoManagerInfo()
	{
		return new RepoManagerInfo();
	}

	/**
	 * Returns an instance implementing the {@code IServiceHandle}
	 * interface.
	 */
	public IServiceHandle serviceHandle()
	{
		return this.virtualMachine();
	}

	/**
	 * Returns an instance implementing the {@code IVirtualMachine}
	 * interface.
	 */
	public IVirtualMachine virtualMachine()
	{
		return new VirtualMachine();
	}

	/**
	 * Returns an instance implementing the {@code IVirtualMachine}
	 * interface with the given information about a running service.
	 */
	public IVirtualMachine virtualMachine(IServiceHandle shnd)
	{
		return new VirtualMachine(shnd);
	}

	/**
	 * Holder class for the singleton instance.
	 */
	private static class ModelFactoryHolder
	{
		public static final ModelFactory Instance = new ModelFactory();
	}
}
