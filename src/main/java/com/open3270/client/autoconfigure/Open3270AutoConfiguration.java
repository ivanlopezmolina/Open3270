/*
 * MIT License
 *
 * Copyright (c) 2026 ivanlopezmolina
 *
 * Originally based on Open3270 - A C# implementation of the TN3270/TN3270E protocol
 * Original authors: Michael Warriner and contributors (c) 2004-2020
 * Original project: https://github.com/Open3270/Open3270
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.open3270.client.autoconfigure;

import com.open3270.client.ConnectionConfig;
import com.open3270.client.TNEmulator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot autoconfiguration for Open3270.
 * Registers a {@link TNEmulator} prototype-scoped bean when
 * {@code open3270.host} is configured.
 */
@AutoConfiguration
@ConditionalOnClass(TNEmulator.class)
@EnableConfigurationProperties(Open3270Properties.class)
public class Open3270AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectionConfig open3270ConnectionConfig(Open3270Properties props) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHostName(props.getHost() != null ? props.getHost() : "");
        config.setHostPort(props.getPort());
        config.setTermType(props.getTermType());
        config.setUseSSL(props.isUseSsl());
        config.setHostLU(props.getLu());
        config.setDefaultTimeout(props.getConnectTimeoutMs());
        config.setRefuseTN3270E(props.isRefuseTn3270e());
        return config;
    }
}
