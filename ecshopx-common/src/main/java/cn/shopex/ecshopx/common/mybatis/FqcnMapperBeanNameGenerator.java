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

package cn.shopex.ecshopx.common.mybatis;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * 多模块存在同名 {@code *Mapper} 接口时，默认 Bean 名（如 {@code statisticsMapper}）会冲突。
 * 使用「全限定类名将 {@code .} 替换为 {@code _}」作为 Bean 名，保证唯一。
 */
public class FqcnMapperBeanNameGenerator implements BeanNameGenerator {

	private final AnnotationBeanNameGenerator defaultGenerator = new AnnotationBeanNameGenerator();

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		String className = definition.getBeanClassName();
		if (className != null && className.endsWith("Mapper")) {
			return className.replace('.', '_');
		}
		return defaultGenerator.generateBeanName(definition, registry);
	}
}
