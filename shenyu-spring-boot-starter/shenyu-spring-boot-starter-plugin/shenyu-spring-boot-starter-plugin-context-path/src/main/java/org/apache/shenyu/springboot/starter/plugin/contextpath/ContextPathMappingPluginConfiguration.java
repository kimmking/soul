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

package org.apache.shenyu.springboot.starter.plugin.contextpath;

import org.apache.shenyu.plugin.api.ShenyuPlugin;
import org.apache.shenyu.plugin.base.handler.PluginDataHandler;
import org.apache.shenyu.plugin.context.path.ContextPathMappingPlugin;
import org.apache.shenyu.plugin.context.path.handler.ContextPathMappingPluginDataHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ContextPathMappingPluginConfiguration.
 *
 * @author zhanglei
 */
@Configuration
public class ContextPathMappingPluginConfiguration {

    /**
     * Context path mapping plugin.
     *
     * @return the soul plugin
     */
    @Bean
    public ShenyuPlugin contextPathMappingPlugin() {
        return new ContextPathMappingPlugin();
    }

    /**
     * Context path mapping plugin data handler plugin data handler.
     *
     * @return the plugin data handler
     */
    @Bean
    public PluginDataHandler contextPathMappingPluginDataHandler() {
        return new ContextPathMappingPluginDataHandler();
    }
}
