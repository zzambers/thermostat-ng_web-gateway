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

package com.redhat.thermostat.gateway.service.commands.socket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import javax.websocket.PongMessage;
import javax.websocket.Session;

import com.redhat.thermostat.gateway.common.core.auth.basic.RoleAwareUser;
import com.redhat.thermostat.gateway.service.commands.channel.ClientAgentCommunication;
import com.redhat.thermostat.gateway.service.commands.channel.CommunicationsRegistry;
import com.redhat.thermostat.gateway.service.commands.channel.model.Message;
import com.redhat.thermostat.gateway.service.commands.channel.model.WebSocketResponse;

class CommandChannelAgentSocket extends CommandChannelSocket {

    private static final long SOCKET_SESSION_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    private static final String UNKNOWN_PAYLOAD = "UNKNOWN";
    private static final String AGENT_PROVIDER_PREFIX = "thermostat-commands-provider-";

    CommandChannelAgentSocket(String id, Session session) {
        super(id, session);
        // Be sure to have the socket timeout the same as on the
        // agent side. Otherwise the agent socket might
        // time out. Note that the time period upon
        // which we will send ping messages is relative to the
        // configured socket timeout. It must be strictly less than
        // the used socket timeout on both sides. This ensures that
        // the agent socket does not close on us and is kept alive.
        session.setMaxIdleTimeout(SOCKET_SESSION_IDLE_TIMEOUT);
    }

    @Override
    public void onConnect() throws IOException {
        super.onConnect();
        // NOTE:  There is a slight window where the agent has connected
        //        and signaled so by sending back the "connected" event,
        //        yet, the agent socket is not yet ready in the registry.
        //        Server side is busy doing the role checks. If a client
        //        connects in that window it will get an error back,
        //        believing that the agent it wants to talk to has not
        //        connected.
        AgentSocketsRegistry reg = AgentSocketsRegistry.getInstance(SOCKET_SESSION_IDLE_TIMEOUT);
        reg.addSocket(agentId, this.session);
    }

    @Override
    public void onClose(int closeCode, String reason) {
        super.onClose(closeCode, reason);
        AgentSocketsRegistry reg = AgentSocketsRegistry.getInstance(SOCKET_SESSION_IDLE_TIMEOUT);
        reg.removeSocket(agentId);
    }

    @Override
    public void onPongMessage(PongMessage message) {
        if (Debug.isOn()) {
            String payload = extractPayload(message);
            System.err.println("Server: Got pong message <<" + payload + ">>");
        }
    }

    private String extractPayload(PongMessage message) {
        ByteBuffer payload = message.getApplicationData();
        int limit = payload.limit();
        int position = payload.position();
        int length = limit - position;
        byte[] buf = new byte[length];
        for (int i = 0; position < limit; position++, i++) {
            buf[i] = payload.get(position);
        }
        try {
            return new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("ERROR: Failed to extract payload from pong message: " + e.getMessage());
            return UNKNOWN_PAYLOAD;
        }
    }

    @Override
    protected void performCommunication(Message msg) throws IOException {
        if (!(msg instanceof WebSocketResponse)) {
            throw new IllegalStateException("Unknown message of type: " + msg.getClass().getName());
        }
        WebSocketResponse resp = (WebSocketResponse)msg;
        String commId = agentId + resp.getSequenceId();
        ClientAgentCommunication comm = CommunicationsRegistry.get(commId);
        comm.responseReceived(resp);
    }

    @Override
    protected String getSequence() {
        return null; // Agent connections don't have sequence numbers there is only one
    }

    @Override
    protected boolean checkRoles() {
        // FIXME: relies on RoleAwareUser - i.e. specific auth scheme.
        RoleAwareUser user = (RoleAwareUser) session.getUserPrincipal();
        String roleToCheck = AGENT_PROVIDER_PREFIX + agentId;
        if (!user.isUserInRole(roleToCheck)) {
            return false;
        }
        return true;
    }
}
