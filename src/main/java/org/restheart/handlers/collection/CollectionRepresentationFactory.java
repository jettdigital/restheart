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
package org.restheart.handlers.collection;

import com.mongodb.DBObject;
import org.restheart.Configuration;
import org.restheart.hal.Link;
import org.restheart.hal.Representation;
import org.restheart.handlers.IllegalQueryParamenterException;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.document.DocumentRepresentationFactory;
import org.restheart.utils.ResponseHelper;
import org.restheart.utils.URLUtilis;
import io.undertow.server.HttpServerExchange;
import java.util.List;
import org.bson.types.ObjectId;
import org.restheart.handlers.AbstractRepresentationFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class CollectionRepresentationFactory extends AbstractRepresentationFactory {

    private static final Logger logger = LoggerFactory.getLogger(CollectionRepresentationFactory.class);
    
    public CollectionRepresentationFactory() {
    }

    /**
     *
     * @param exchange
     * @param context
     * @param embeddedData
     * @param size
     * @return
     * @throws IllegalQueryParamenterException
     */
    @Override
    protected Representation getRepresentation(HttpServerExchange exchange, RequestContext context, List<DBObject> embeddedData, long size)
            throws IllegalQueryParamenterException {
        final String requestPath = buildRequestPath(exchange);
        final Representation rep = createRepresentation(exchange, context, requestPath);

        // add the collection properties
        final DBObject collProps = context.getCollectionProps();

        if (collProps != null) {
            rep.addProperties(collProps);
        }

        addSizeAndTotalPagesProperties(size, context, rep);

        addEmbeddedData(embeddedData, rep, requestPath, exchange, context);

        addPaginationLinks(exchange, context, size, rep);

        addLinkTemplatesAndCuries(exchange, context, rep, requestPath);

        return rep;
    }

    private void addEmbeddedData(List<DBObject> embeddedData, final Representation rep, final String requestPath, final HttpServerExchange exchange, final RequestContext context)
            throws IllegalQueryParamenterException {
        if (embeddedData != null) {
            addReturnedProperty(embeddedData, rep);
            if (!embeddedData.isEmpty()) {
                embeddedDocuments(embeddedData, requestPath, exchange, context, rep);
            }
        }
    }

    private void addLinkTemplatesAndCuries(final HttpServerExchange exchange, final RequestContext context, final Representation rep, final String requestPath) {
        // link templates and curies
        if (context.isParentAccessible()) {
            // this can happen due to mongo-mounts mapped URL
            rep.addLink(new Link("rh:db", URLUtilis.getParentPath(requestPath)));
        }
        rep.addLink(new Link("rh:filter", requestPath + "/{?filter}", true));
        rep.addLink(new Link("rh:sort", requestPath + "/{?sort_by}", true));
        rep.addLink(new Link("rh:paging", requestPath + "/{?page}{&pagesize}", true));
        rep.addLink(new Link("rh:countandpaging", requestPath + "/{?page}{&pagesize}&count", true));
        rep.addLink(new Link("rh:indexes", requestPath + "/_indexes"));
        rep.addLink(new Link("rh", "curies", Configuration.RESTHEART_ONLINE_DOC_URL + "/#api-coll-{rel}", true), true);

        ResponseHelper.injectWarnings(rep, exchange, context);
    }

    private void embeddedDocuments(List<DBObject> embeddedData, String requestPath, HttpServerExchange exchange, RequestContext context, Representation rep) throws IllegalQueryParamenterException {
        for (DBObject d : embeddedData) {
            Object _id = d.get("_id");

            if (_id != null && (_id instanceof String || _id instanceof ObjectId)) {
                Representation nrep = DocumentRepresentationFactory.getDocument(requestPath + "/" + _id.toString(), exchange, context, d);

                nrep.addProperty("_type", RequestContext.TYPE.DOCUMENT.name());

                if (d.get("_etag") != null && d.get("_etag") instanceof ObjectId) {
                    d.put("_etag", ((ObjectId) d.get("_etag")).toString()); // represent the etag as a string
                }
                rep.addRepresentation("rh:doc", nrep);
            } else {
                logger.error("collection missing string _id field", d);
            }
        }
    }
}
