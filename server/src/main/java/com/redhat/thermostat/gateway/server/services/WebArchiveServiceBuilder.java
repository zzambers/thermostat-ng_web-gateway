/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.gateway.server.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.gateway.common.core.config.Configuration;
import com.redhat.thermostat.gateway.common.core.config.ConfigurationFactory;
import com.redhat.thermostat.gateway.common.core.servlet.GlobalConstants;

class WebArchiveServiceBuilder extends BasicServiceBuilder {

    private static final String SERVICES_FOLDER = "services";
    private static final String CONTEXT_PREFIX = "/";
    private final ConfigurationFactory configFactory;
    private final EnvHelper envHelper;
    private final PathHelper pathHelper;

    public WebArchiveServiceBuilder(ConfigurationFactory configFactory) {
        this(configFactory, new EnvHelper(), new PathHelper());
    }

    WebArchiveServiceBuilder(ConfigurationFactory configFactory, EnvHelper envHelper, PathHelper pathHelper) {
        this.configFactory = configFactory;
        this.envHelper = envHelper;
        this.pathHelper = pathHelper;
    }

    @Override
    public List<CoreService> build() {
        Map<String, Object> configProperties = configuration.asMap();

        List<CoreService> serviceList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : configProperties.entrySet()) {
            String contextPath = entry.getKey();
            String serviceName = getServiceName(contextPath);
            Configuration config = configFactory.createServiceConfiguration(serviceName);
            String warPathCandidate = entry.getValue().toString();
            String warPath = getAbsolutePathForService(warPathCandidate);
            WebArchiveCoreService service = new WebArchiveCoreService(contextPath, warPath, config);
            serviceList.add(service);
        }
        return serviceList;
    }

    // FIXME: Heuristic => contextPath == /<servicename>
    private String getServiceName(String contextPath) {
        return contextPath.substring(CONTEXT_PREFIX.length());
    }

    String getAbsolutePathForService(String path) {
        if (pathHelper.isAbsolute(path)) {
            return path;
        }
        // assume service web archive file is relative to
        // THERMOSTAT_GATEWAY_HOME/services
        String gwHome = envHelper.getEnv(GlobalConstants.GATEWAY_HOME_ENV);
        Path basePath = Paths.get(gwHome, SERVICES_FOLDER, path);
        return basePath.toAbsolutePath().toString();
    }

    static class EnvHelper {

        String getEnv(String var) {
            return System.getenv(var);
        }

    }

    static class PathHelper {

        boolean isAbsolute(String path) {
            Path pathObj = Paths.get(path);
            return pathObj.isAbsolute();
        }

    }

}
