/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.thrift.pool;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

public interface Pool<T> {

	/**
	 * Retrieves an object from the pool
	 * 
	 * @return An object from the pool
	 * @throws PoolLimitException
	 *             If the maximum number of allowed instances have already been
	 *             created
	 * @throws KurentoMediaFrameworkException
	 */
	public T acquire() throws PoolLimitException,
			KurentoMediaFrameworkException;

	/**
	 * Returns an object to the pool
	 * 
	 * @param obj
	 * @throws KurentoMediaFrameworkException
	 *             In case of an internal exception in the pool
	 */
	public void release(T obj);
}
