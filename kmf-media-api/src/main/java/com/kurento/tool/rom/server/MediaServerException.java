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
package com.kurento.tool.rom.server;

import com.kurento.kmf.common.exception.KurentoSystemException;

/**
 * This exception represents errors that take place in KMS, while operating with
 * pipelines and media elements
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.2.1
 * 
 */
public class MediaServerException extends KurentoSystemException {

	private static final long serialVersionUID = -4925041543188451274L;

	public MediaServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public MediaServerException(String message) {
		super(message);
	}

	public MediaServerException(Throwable cause) {
		super(cause);
	}

}
