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

/**
 * Utility class for the RawSocket service scheduler.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class RawSocketServiceSchedulerUtil
{
	/**
	 * Converts the execution status code returned by the scheduler into a
	 * value of the ExecutionStatus enumeration.
	 */
	public static ExecutionStatus SchedulerExecStatusToExecutionStatus(int status)
	{
		switch (status)
		{
			case 1:
				return ExecutionStatus.UNSTARTED;
			case 2:
				return ExecutionStatus.READY;
			case 3:
				return ExecutionStatus.STAGING_IN;
			case 4:
				return ExecutionStatus.RUNNING;
			case 5:
				return ExecutionStatus.STAGING_OUT;
			case 6:
				return ExecutionStatus.FINISHED;
			case 7:
				return ExecutionStatus.CANCELLED;
			case 8:
				return ExecutionStatus.FAILED;
			case 9:
				return ExecutionStatus.ABORTED;
			case 0:
			default:
				return ExecutionStatus.UNKNOWN;
		}
	}

	/**
	 * Converts the ExecutionStatus enumeration value into the associated
	 * numeric code understandable by the scheduler.
	 */
	public static int ExecutionStatusToSchedulerExecStatus(ExecutionStatus status)
	{
		switch (status)
		{
			case UNSTARTED:
				return 1;
			case READY:
				return 2;
			case STAGING_IN:
				return 3;
			case RUNNING:
				return 4;
			case STAGING_OUT:
				return 5;
			case FINISHED:
				return 6;
			case CANCELLED:
				return 7;
			case FAILED:
				return 8;
			case ABORTED:
				return 9;
			case UNKNOWN:
			default:
				return 0;
		}
	}
}
