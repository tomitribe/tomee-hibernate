/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomee.webapp.installer;

import org.apache.tomee.installer.Installer;
import org.apache.tomee.installer.Paths;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Runner {
    private final File openejbWarDir;
    private String catalinaHome = System.getProperty("catalina.home");
    private String catalinaBase = System.getProperty("catalina.base");
    private String serverXmlFile = System.getProperty("catalina.base") + "/conf/server.xml";

    public Runner(File openejbWarDir) {
        this.openejbWarDir = openejbWarDir;
    }

    public void setCatalinaHome(String catalinaHome) {
        this.catalinaHome = catalinaHome;
    }

    public void setCatalinaBaseDir(String catalinaBase) {
        this.catalinaBase = catalinaBase;
    }

    public void setServerXmlFile(String serverXmlFile) {
        this.serverXmlFile = serverXmlFile;
    }

    private void setAlerts(String key, List<String> messages, List<Map<String, String>> result) {
        if (messages == null) {
            return;
        }
        for (String message : messages) {
            result.add(Common.build(key, message));
        }
    }

    public List<Map<String, String>> execute() {
        final Paths paths = new Paths(openejbWarDir);
        final Installer installer = new Installer(paths);
        final List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        if (org.apache.tomee.installer.Status.NONE.equals(installer.getStatus())) {
            paths.reset();
            installer.reset();
            paths.setCatalinaHomeDir(this.catalinaHome);
            paths.setCatalinaBaseDir(this.catalinaBase);
            paths.setServerXmlFile(this.serverXmlFile);
            if (paths.verify()) {
                installer.installAll();
            }
        }
        result.add(Common.build("status", String.valueOf(installer.getStatus())));
        setAlerts("errors", installer.getAlerts().getErrors(), result);
        setAlerts("warnings", installer.getAlerts().getWarnings(), result);
        setAlerts("infos", installer.getAlerts().getInfos(), result);
        {
            boolean hasHome = false;
            boolean doesHomeExist = false;
            boolean isHomeDirectory = false;
            boolean hasLibDirectory = false;
            final String homePath = System.getProperty("openejb.home");
            if (homePath != null) {
                hasHome = true;
                final File homeDir = new File(homePath);
                doesHomeExist = homeDir.exists();
                if (homeDir.exists()) {
                    isHomeDirectory = homeDir.isDirectory();
                    final File libDir = new File(homeDir, "lib");
                    hasLibDirectory = libDir.exists();
                }
            }
            result.add(Common.build("hasHome", String.valueOf(hasHome)));
            result.add(Common.build("doesHomeExist", String.valueOf(doesHomeExist)));
            result.add(Common.build("isHomeDirectory", String.valueOf(isHomeDirectory)));
            result.add(Common.build("hasLibDirectory", String.valueOf(hasLibDirectory)));
        }
        {
            boolean wereTheOpenEJBClassesInstalled = false;
            boolean wereTheEjbClassesInstalled = false;
            boolean wasOpenEJBStarted = false;
            boolean canILookupAnything = false;
            try {
                final ClassLoader myLoader = this.getClass().getClassLoader();
                Class.forName("org.apache.openejb.OpenEJB", true, myLoader);
                wereTheOpenEJBClassesInstalled = true;
            } catch (Exception e) {
                // noop
            }
            try {
                Class.forName("javax.ejb.EJBHome", true, this.getClass().getClassLoader());
                wereTheEjbClassesInstalled = true;
            } catch (Exception e) {
                // noop
            }
            try {
                final Class openejb = Class.forName("org.apache.openejb.OpenEJB", true, this.getClass().getClassLoader());
                final Method isInitialized = openejb.getDeclaredMethod("isInitialized");
                wasOpenEJBStarted = (Boolean) isInitialized.invoke(openejb);
            } catch (Exception e) {
                // noop
            }
            try {
                final Properties p = new Properties();
                p.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.openejb.core.LocalInitialContextFactory");
                p.put("openejb.loader", "embed");
                final InitialContext ctx = new InitialContext(p);
                final Object obj = ctx.lookup("");
                if (obj.getClass().getName().equals("org.apache.openejb.core.ivm.naming.IvmContext")) {
                    canILookupAnything = true;
                }
            } catch (Exception e) {
                // noop
            }
            result.add(Common.build("wereTheOpenEJBClassesInstalled", String.valueOf(wereTheOpenEJBClassesInstalled)));
            result.add(Common.build("wereTheEjbClassesInstalled", String.valueOf(wereTheEjbClassesInstalled)));
            result.add(Common.build("wasOpenEJBStarted", String.valueOf(wasOpenEJBStarted)));
            result.add(Common.build("canILookupAnything", String.valueOf(canILookupAnything)));
        }
        return result;
    }
}
