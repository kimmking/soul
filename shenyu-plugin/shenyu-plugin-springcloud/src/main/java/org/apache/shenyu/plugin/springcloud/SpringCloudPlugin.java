/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.plugin.springcloud;

import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.common.dto.RuleData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.dto.convert.rule.impl.SpringCloudRuleHandle;
import org.apache.shenyu.common.dto.convert.selector.SpringCloudSelectorHandle;
import org.apache.shenyu.common.enums.PluginEnum;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.plugin.api.result.ShenyuResultEnum;
import org.apache.shenyu.plugin.base.utils.FallbackUtils;
import org.apache.shenyu.plugin.api.result.ShenyuResultWrap;
import org.apache.shenyu.plugin.api.ShenyuPluginChain;
import org.apache.shenyu.plugin.base.AbstractShenyuPlugin;
import org.apache.shenyu.plugin.api.context.ShenyuContext;
import org.apache.shenyu.plugin.api.utils.WebFluxResultUtils;
import org.apache.shenyu.plugin.springcloud.cache.SpringCloudRuleHandleCache;
import org.apache.shenyu.plugin.springcloud.cache.SpringCloudSelectorHandleCache;
import org.apache.shenyu.plugin.springcloud.handler.SpringCloudPluginDataHandler;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

/**
 * this is springCloud proxy impl.
 *
 * @author xiaoyu(myth)
 */
public class SpringCloudPlugin extends AbstractShenyuPlugin {

    private final LoadBalancerClient loadBalancer;

    /**
     * Instantiates a new Spring cloud plugin.
     *
     * @param loadBalancer the load balancer
     */
    public SpringCloudPlugin(final LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    protected Mono<Void> doExecute(final ServerWebExchange exchange, final ShenyuPluginChain chain, final SelectorData selector, final RuleData rule) {
        if (Objects.isNull(rule)) {
            return Mono.empty();
        }
        final ShenyuContext shenyuContext = exchange.getAttribute(Constants.CONTEXT);
        assert shenyuContext != null;
        final SpringCloudRuleHandle ruleHandle = SpringCloudRuleHandleCache.getInstance().obtainHandle(SpringCloudPluginDataHandler.getRuleCacheKey(rule));
        final SpringCloudSelectorHandle selectorHandle = SpringCloudSelectorHandleCache.getInstance().obtainHandle(selector.getId());
        if (StringUtils.isBlank(selectorHandle.getServiceId()) || StringUtils.isBlank(ruleHandle.getPath())) {
            Object error = ShenyuResultWrap.error(ShenyuResultEnum.CANNOT_CONFIG_SPRINGCLOUD_SERVICEID.getCode(), ShenyuResultEnum.CANNOT_CONFIG_SPRINGCLOUD_SERVICEID.getMsg(), null);
            return WebFluxResultUtils.result(exchange, error);
        }

        final ServiceInstance serviceInstance = loadBalancer.choose(selectorHandle.getServiceId());
        if (Objects.isNull(serviceInstance)) {
            Object error = ShenyuResultWrap.error(ShenyuResultEnum.SPRINGCLOUD_SERVICEID_IS_ERROR.getCode(), ShenyuResultEnum.SPRINGCLOUD_SERVICEID_IS_ERROR.getMsg(), null);
            return WebFluxResultUtils.result(exchange, error);
        }
        final URI uri = loadBalancer.reconstructURI(serviceInstance, URI.create(shenyuContext.getRealUrl()));

        String realURL = buildRealURL(uri.toASCIIString(), shenyuContext.getHttpMethod(), exchange.getRequest().getURI().getQuery());

        exchange.getAttributes().put(Constants.HTTP_URL, realURL);
        //set time out.
        exchange.getAttributes().put(Constants.HTTP_TIME_OUT, ruleHandle.getTimeout());
        return chain.execute(exchange);
    }

    @Override
    public int getOrder() {
        return PluginEnum.SPRING_CLOUD.getCode();
    }

    @Override
    public String named() {
        return PluginEnum.SPRING_CLOUD.getName();
    }

    /**
     * plugin is execute.
     *
     * @param exchange the current server exchange
     * @return default false.
     */
    @Override
    public Boolean skip(final ServerWebExchange exchange) {
        final ShenyuContext body = exchange.getAttribute(Constants.CONTEXT);
        return !Objects.equals(Objects.requireNonNull(body).getRpcType(), RpcTypeEnum.SPRING_CLOUD.getName());
    }

    @Override
    protected Mono<Void> handleSelectorIsNull(final String pluginName, final ServerWebExchange exchange, final ShenyuPluginChain chain) {
        return FallbackUtils.getNoSelectorResult(pluginName, exchange);
    }

    @Override
    protected Mono<Void> handleRuleIsNull(final String pluginName, final ServerWebExchange exchange, final ShenyuPluginChain chain) {
        return FallbackUtils.getNoRuleResult(pluginName, exchange);
    }

    private String buildRealURL(final String url, final String httpMethod, final String query) {
        if (httpMethod.equals(HttpMethod.GET.name()) && StringUtils.isNotBlank(query)) {
            return url + "?" + query;
        }
        return url;
    }
}
