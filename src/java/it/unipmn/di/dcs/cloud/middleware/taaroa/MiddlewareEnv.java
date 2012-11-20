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
import it.unipmn.di.dcs.cloud.core.middleware.service.IInformationService;
import it.unipmn.di.dcs.cloud.core.middleware.service.IServiceSchedulerService;

import java.util.Properties;

/**
 * Environment for the TAAROA cloud middleware.
 *
 * @author <a href="mailto:marco.guazzone@gmail.com">Marco Guazzone</a>
 */
public class MiddlewareEnv implements IMiddlewareEnv
{
	private Properties props = new Properties();

	/** A constructor. */
	public MiddlewareEnv()
	{
		this.props.clear();
		this.props.setProperty( IMiddlewareEnv.IS_HOST_PROP, "localhost" );
		this.props.setProperty( IMiddlewareEnv.IS_PORT_PROP, "7777" );
		this.props.setProperty( IMiddlewareEnv.SVCSCHED_HOST_PROP, "localhost" );
		this.props.setProperty( IMiddlewareEnv.SVCSCHED_PORT_PROP, "7778" );
	}

	/** A constructor. */
	public MiddlewareEnv(Properties props)
	{
		this.props.putAll( props );
	}

	/**
	 * Reset all the properties and assign the values specified in
	 * the given {@code props}.
	 */
	public void setProperties(Properties props)
	{
		this.props.clear();
		this.props.putAll( props );
	}

	/**
	 * Set the property named with the given {@code name} to the given
	 * {@code value}.
	 */
	public void setProperty(String name, String value)
	{
		this.props.setProperty(name, value);
	}

	/**
	 * Add to the set of all properties the properties specified in
	 * the given {@code props}.
	 */
	public void addProperty(Properties props)
	{
		this.props.putAll( props );
	}

	/** Remove all the properties. */
	public void removeProperties()
	{
		this.props.clear();
	}

	/** Remove the property named with the given {@code name}. */
	public void removeProperty(String name)
	{
		this.props.remove(name);
	}

	//@{ IMiddlewareEnv implementation ////////////////////////////////////////

	public String getProperty(String name)
	{
		return this.props.getProperty(name);
	}

	public String getInformationServiceHost()
	{
		return this.props.getProperty( IMiddlewareEnv.IS_HOST_PROP );
	}

	public int getInformationServicePort()
	{
		return Integer.parseInt( this.props.getProperty( IMiddlewareEnv.IS_PORT_PROP ) );
	}

	public String getServiceSchedulerServiceHost()
	{
		return this.props.getProperty( IMiddlewareEnv.SVCSCHED_HOST_PROP );
	}

	public int getServiceSchedulerServicePort()
	{
		return Integer.parseInt( this.props.getProperty( IMiddlewareEnv.SVCSCHED_PORT_PROP ) );
	}

	//@} IMiddlewareEnv implementation ////////////////////////////////////////
}
