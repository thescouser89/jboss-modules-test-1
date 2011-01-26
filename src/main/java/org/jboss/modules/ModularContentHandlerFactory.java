/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ModularContentHandlerFactory implements ContentHandlerFactory {
    private static final PrivilegedAction<String> URL_MODULES_LIST_ACTION = new PropertyReadAction("jboss.content.handler.modules");

    public ContentHandler createContentHandler(final String mimeType) {
        final SecurityManager sm = System.getSecurityManager();
        final String urlModulesList;
        if (sm != null) {
            urlModulesList = AccessController.doPrivileged(URL_MODULES_LIST_ACTION);
        } else {
            urlModulesList = URL_MODULES_LIST_ACTION.run();
        }
        if (urlModulesList == null) {
            return null;
        }
        int f = 0;
        int i;
        do {
            i = urlModulesList.indexOf('|', f);
            final String moduleId = (i == -1 ? urlModulesList.substring(f) : urlModulesList.substring(f, i)).trim();
            if (moduleId.length() > 0) {
                try {
                    final ModuleIdentifier identifier = ModuleIdentifier.fromString(moduleId);
                    final ServiceLoader<ContentHandlerFactory> loader = Module.getSystemModuleLoader().loadModule(identifier).loadService(ContentHandlerFactory.class);
                    for (ContentHandlerFactory factory : loader) {
                        final ContentHandler handler = factory.createContentHandler(mimeType);
                        if (handler != null) {
                            return handler;
                        }
                    }
                } catch (RuntimeException e) {
                    // skip it
                } catch (ModuleLoadException e) {
                    // skip it
                }
            }
            f = i + 1;
        } while (i != -1);
        return null;
    }
}