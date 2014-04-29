/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
public class ClientWithValidation extends Client {

	private boolean isProtocolOpen = true;

	/**
	 * @param prot
	 */
	public ClientWithValidation(TProtocol prot) {
		super(prot);
	}

	/**
	 * @param iprot
	 * @param oprot
	 */
	public ClientWithValidation(TProtocol iprot, TProtocol oprot) {
		super(iprot, oprot);
	}

	@Override
	public String invokeJsonRpc(String request) throws TException {
		try {
			return super.invokeJsonRpc(request);
		} catch (TTransportException e) {
			isProtocolOpen = false;
			throw e;
		}
	}

	public boolean isValid() {
		return isProtocolOpen;
	}
}
