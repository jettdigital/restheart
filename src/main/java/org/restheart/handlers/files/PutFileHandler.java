/*
 * RESTHeart - the data REST API server
 * Copyright (C) SoftInstigate Srl
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
package org.restheart.handlers.files;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.undertow.server.HttpServerExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maurizio Turatti <maurizio@softinstigate.com>
 */
public class PutFileHandler extends PipedHttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutFileHandler.class);

    public PutFileHandler() {
        super(null);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        DBObject content = context.getContent();

        if (content == null) {
            content = new BasicDBObject();
        }

        // cannot PUT an array
        if (content instanceof BasicDBList) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "data cannot be an array");
            return;
        }

        String id = context.getDocumentId();

        if (content.get("_id") == null) {
            content.put("_id", getId(id));
        } else if (!content.get("_id").equals(id)) {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "_id in content body is different than id in URL");
            return;
        }

        //ObjectId etag = RequestHelper.getWriteEtag(exchange);
        // TODO
        int httpStatus = saveBinaryFile(exchange);

        // send the warnings if any (and in case no_content change the return code to ok
        if (context.getWarnings() != null && !context.getWarnings().isEmpty()) {
            sendWarnings(httpStatus, exchange, context);
        } else {
            exchange.setResponseCode(httpStatus);
        }

        exchange.endExchange();
    }

    private int saveBinaryFile(HttpServerExchange exchange) throws IOException {
        LOGGER.debug("@@@ Saving file");
        int httpStatus;
        //exchange.getRequestHeaders();
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line);
        }
        System.out.println(stringBuilder.toString());
        httpStatus = 201;
        return httpStatus;
    }

}
