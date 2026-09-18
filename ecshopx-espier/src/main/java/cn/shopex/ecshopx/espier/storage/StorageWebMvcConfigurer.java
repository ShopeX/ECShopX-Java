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

package cn.shopex.ecshopx.espier.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 当 driver=local 时，将本地存储目录映射为 /storage/** 静态资源路径，
 * 使上传后的文件可通过 HTTP 直接访问。
 */
@Configuration
@ConditionalOnProperty(name = "ecshopx.storage.driver", havingValue = "local", matchIfMissing = true)
public class StorageWebMvcConfigurer implements WebMvcConfigurer {

	private final StorageProperties props;

	public StorageWebMvcConfigurer(StorageProperties props) {
		this.props = props;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String root = props.getLocal().getRoot();
		if (root != null && !root.isEmpty()) {
			String location = root.endsWith("/") ? root : root + "/";
			registry.addResourceHandler("/storage/**")
					.addResourceLocations("file:" + location);
		}
	}
}
