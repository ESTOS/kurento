/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract tect service.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public abstract class TestService {

  public static Logger log = LoggerFactory.getLogger(TestService.class);

  public enum TestServiceScope {
    TEST, TESTCLASS, TESTSUITE, EXTERNAL;
  }

  public TestServiceScope scope = TestServiceScope.TEST;

  public abstract TestServiceScope getScope();

  public void start() {
    log.debug("[+] Starting {} (scope={})", this.getClass().getSimpleName(), getScope());
  }

  public void stop() {
    log.debug("[-] Stopping {} (scope={})", this.getClass().getSimpleName(), getScope());
  }

}
