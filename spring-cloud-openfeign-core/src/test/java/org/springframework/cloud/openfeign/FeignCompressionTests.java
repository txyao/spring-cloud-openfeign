/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import java.util.Map;

import feign.Client;
import feign.RequestInterceptor;
import feign.httpclient.ApacheHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.openfeign.encoding.FeignAcceptGzipEncodingAutoConfiguration;
import org.springframework.cloud.openfeign.encoding.FeignAcceptGzipEncodingInterceptor;
import org.springframework.cloud.openfeign.encoding.FeignContentGzipEncodingAutoConfiguration;
import org.springframework.cloud.openfeign.encoding.FeignContentGzipEncodingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 * @author Biju Kunjummen
 */
class FeignCompressionTests {

	@Test
	void testInterceptors() {
		new ApplicationContextRunner()
				.withPropertyValues("feign.compression.response.enabled=true", "feign.compression.request.enabled=true",
						"feign.okhttp.enabled=false")
				.withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class,
						FeignContentGzipEncodingAutoConfiguration.class, FeignAcceptGzipEncodingAutoConfiguration.class,
						HttpClientConfiguration.class, PlainConfig.class))
				.run(context -> {
					FeignContext feignContext = context.getBean(FeignContext.class);
					Map<String, RequestInterceptor> interceptors = feignContext.getInstances("foo",
							RequestInterceptor.class);
					assertThat(interceptors.size()).isEqualTo(2);
					assertThat(interceptors.get("feignAcceptGzipEncodingInterceptor"))
							.isInstanceOf(FeignAcceptGzipEncodingInterceptor.class);
					assertThat(interceptors.get("feignContentGzipEncodingInterceptor"))
							.isInstanceOf(FeignContentGzipEncodingInterceptor.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	protected static class PlainConfig {

		@Autowired
		private Client client;

		@Bean
		public ApacheHttpClient client() {
			/*
			 * We know our client is an AppacheHttpClient because we disabled the OK HTTP
			 * client. FeignAcceptGzipEncodingAutoConfiguration won't load unless there is
			 * a bean of type ApacheHttpClient (not Client) in this test because the bean
			 * is not yet created and so the application context doesnt know that the
			 * Client bean is actually an instance of ApacheHttpClient, therefore
			 * FeignAcceptGzipEncodingAutoConfiguration will not be loaded. We just create
			 * a bean here of type ApacheHttpClient so that the configuration will be
			 * loaded correctly.
			 */
			return (ApacheHttpClient) this.client;
		}

	}

}
