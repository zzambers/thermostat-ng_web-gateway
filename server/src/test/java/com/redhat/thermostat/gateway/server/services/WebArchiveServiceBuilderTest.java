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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.gateway.common.core.config.Configuration;
import com.redhat.thermostat.gateway.common.core.config.ConfigurationFactory;

public class WebArchiveServiceBuilderTest {
    @Test
    public void testBuildWebArchiveCoreServices() {
        ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
        when(configurationFactory.createServiceConfiguration(anyString())).thenReturn(mock(Configuration.class));

        WebArchiveServiceBuilder serviceBuilder = new WebArchiveServiceBuilder(configurationFactory);
        Configuration configuration = mock(Configuration.class);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("/context0", "serviceName0");
        configMap.put("/context1", "serviceName1");

        when(configuration.asMap()).thenReturn(configMap);
        serviceBuilder.setConfiguration(configuration);

        List<CoreService> services = serviceBuilder.build();
        assertEquals(2, services.size());

        assertTrue(services.get(0) instanceof WebArchiveCoreService);
        assertTrue(services.get(1) instanceof WebArchiveCoreService);
    }


}
