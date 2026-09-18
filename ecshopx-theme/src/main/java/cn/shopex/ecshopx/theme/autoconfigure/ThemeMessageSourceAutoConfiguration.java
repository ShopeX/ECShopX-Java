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

package cn.shopex.ecshopx.theme.autoconfigure;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.context.annotation.Bean;

/**
 * Ensures {@code theme-messages} bundles participate in the context {@code messageSource} bean
 * (not only a separate {@code @Primary} bean), so Bean Validation interpolation and
 * {@link org.springframework.context.ApplicationContext#getMessage} resolve the same keys.
 */
@AutoConfiguration
@AutoConfigureAfter(MessageSourceAutoConfiguration.class)
public class ThemeMessageSourceAutoConfiguration {

	private static final String THEME_MESSAGES_BASENAME = "theme-messages";

	@Bean
	public static BeanPostProcessor themeMessagesBasenameAppender() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (!"messageSource".equals(beanName)
						|| !(bean instanceof AbstractResourceBasedMessageSource ms)) {
					return bean;
				}
				if (ms.getBasenameSet().contains(THEME_MESSAGES_BASENAME)) {
					return bean;
				}
				ms.addBasenames(THEME_MESSAGES_BASENAME);
				return bean;
			}
		};
	}
}
