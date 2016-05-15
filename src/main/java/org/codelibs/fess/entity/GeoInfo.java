/*
 * Copyright 2012-2016 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.exception.InvalidQueryException;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.StreamUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.lastaflute.core.message.UserMessages;

public class GeoInfo {

    private QueryBuilder builder;

    public GeoInfo(HttpServletRequest request) {

        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final String[] geoFields = fessConfig.getQueryGeoFieldsAsArray();
        final Map<String, List<QueryBuilder>> geoMap = new HashMap<>();

        StreamUtil
                .of(request.getParameterMap())
                .filter(e -> e.getKey().startsWith("geo.") && e.getKey().endsWith(".point"))
                .forEach(
                        e -> {
                            final String key = e.getKey();
                            for (String geoField : geoFields) {
                                if (key.startsWith("geo." + geoField + ".")) {
                                    String distanceKey = key.replaceFirst(".point$", ".distance");
                                    final String distance = request.getParameter(distanceKey);
                                    if (StringUtil.isNotBlank(distance)) {
                                        StreamUtil.of(e.getValue()).forEach(
                                                pt -> {
                                                    List<QueryBuilder> list = geoMap.get(geoField);
                                                    if (list == null) {
                                                        list = new ArrayList<>();
                                                        geoMap.put(geoField, list);
                                                    }
                                                    String[] values = pt.split(",");
                                                    if (values.length == 2) {
                                                        try {
                                                            double lat = Double.parseDouble(values[0]);
                                                            double lon = Double.parseDouble(values[1]);
                                                            list.add(QueryBuilders.geoDistanceQuery(geoField).distance(distance).lat(lat)
                                                                    .lon(lon));
                                                        } catch (Exception ex) {
                                                            throw new InvalidQueryException(messages -> messages
                                                                    .addErrorsInvalidQueryUnknown(UserMessages.GLOBAL_PROPERTY_KEY), ex
                                                                    .getLocalizedMessage());
                                                        }
                                                    } else {
                                                        throw new InvalidQueryException(messages -> messages
                                                                .addErrorsInvalidQueryUnknown(UserMessages.GLOBAL_PROPERTY_KEY),
                                                                "Invalid geo point: " + pt);
                                                    }
                                                });
                                    }
                                    break;
                                }
                            }
                        });

        QueryBuilder[] queryBuilders = geoMap.values().stream().map(list -> {
            if (list.size() == 1) {
                return list.get(0);
            } else if (list.size() > 1) {
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                list.forEach(q -> boolQuery.should(q));
                return boolQuery;
            }
            return null;
        }).filter(q -> q != null).toArray(n -> new QueryBuilder[n]);

        if (queryBuilders.length == 1) {
            builder = queryBuilders[0];
        } else if (queryBuilders.length > 1) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            StreamUtil.of(queryBuilders).forEach(q -> boolQuery.must(q));
            builder = boolQuery;
        }

    }

    public QueryBuilder toQueryBuilder() {
        return builder;
    }

}
