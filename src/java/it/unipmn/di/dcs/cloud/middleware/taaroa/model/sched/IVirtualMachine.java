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

/**
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public interface IVirtualMachine extends IServiceHandle
{
	void setId(int value);

	void setServiceId(int value);

	void setPhysicalMachineId(int value);

//	@Deprecated
//	void setPhysicalHost(String value);

	void setLocalId(String value);

	String getLocalId();

	void setVirtualHost(String value);

//	String getVirtualHost();

	void setProbingPort(int value);

//	int getProbingPort();

	void setRequestedMem(int value);

	int getRequestedMem();

	void setRequestedCpu(float value);

	float getRequestedCpu();

	void setStatus(ExecutionStatus value);
}
