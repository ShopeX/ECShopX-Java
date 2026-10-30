/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.espier.autoconfigure;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractResourceBasedMessageSource;

/**
 * Registers {@code espier-messages} bundles on the shared {@code messageSource} bean
 * (same pattern as theme-messages), so upload tips resolve across locales.
 */
@AutoConfiguration
@AutoConfigureAfter(MessageSourceAutoConfiguration.class)
public class EspierMessageSourceAutoConfiguration {

	private static final String ESPIER_MESSAGES_BASENAME = "espier-messages";

	@Bean
	public static BeanPostProcessor espierMessagesBasenameAppender() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (!"messageSource".equals(beanName)
						|| !(bean instanceof AbstractResourceBasedMessageSource ms)) {
					return bean;
				}
				if (ms.getBasenameSet().contains(ESPIER_MESSAGES_BASENAME)) {
					return bean;
				}
				ms.addBasenames(ESPIER_MESSAGES_BASENAME);
				return bean;
			}
		};
	}
}
