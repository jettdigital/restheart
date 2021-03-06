/*
 * RESTHeart - the data REST API server
 * Copyright (C) 2014 - 2015 SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.security.handlers;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpServerExchange;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.security.impl.AuthTokenIdentityManager;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

/**
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class AuthTokenInvalidationHandler extends PipedHttpHandler {
    private static final Predicate predicate;

    static {
        predicate = Predicates.parse("method[DELETE] and regex[pattern=\"/_authtokens/(.*?)\", value=%U, full-match=true] and equals[%u, \"${1}\"]");
    }

    /**
     * Creates a new instance of SessionTokenInvalidationHandler
     *
     */
    public AuthTokenInvalidationHandler() {
        super(null);
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        if (!predicate.resolve(exchange) || exchange.getSecurityContext() == null || exchange.getSecurityContext().getAuthenticatedAccount() == null){
            ResponseHelper.endExchange(exchange, HttpStatus.SC_UNAUTHORIZED);
        } else {
            AuthTokenIdentityManager.getInstance().getCachedAccounts().invalidate(exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName());
            removeAuthTokens(exchange);
            exchange.setResponseCode(HttpStatus.SC_NO_CONTENT);
            exchange.endExchange();
        }
    }
    
    private void removeAuthTokens(HttpServerExchange exchange) {
        exchange.getResponseHeaders().remove(AuthTokenInjecterHandler.AUTH_TOKEN_HEADER);
        exchange.getResponseHeaders().remove(AuthTokenInjecterHandler.AUTH_TOKEN_VALID_HEADER);
    }
}
