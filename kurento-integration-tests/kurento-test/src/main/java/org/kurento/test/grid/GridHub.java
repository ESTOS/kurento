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

package org.kurento.test.grid;

import org.openqa.grid.internal.utils.GridHubConfiguration;
import org.openqa.grid.web.Hub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selenium Grid Hub.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class GridHub {

  public static Logger log = LoggerFactory.getLogger(GridHub.class);

  private static final int DEFAULT_TIMEOUT = 60;

  private String bindIp = "0.0.0.0";
  private int port;
  private int timeout;
  private Hub hub;

  public GridHub(int port) {
    this.port = port;
    this.timeout = DEFAULT_TIMEOUT; // Default timeout
  }

  public void start() throws Exception {
    GridHubConfiguration config = new GridHubConfiguration();
    config.setHost(bindIp);
    config.setPort(this.port);
    config.setTimeout(getTimeout());

    hub = new Hub(config);
    log.info("Starting hub on {}:{}", this.bindIp, this.port);
    hub.start();
  }

  public void stop() throws Exception {
    if (hub != null) {
      hub.stop();
    }
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

}
