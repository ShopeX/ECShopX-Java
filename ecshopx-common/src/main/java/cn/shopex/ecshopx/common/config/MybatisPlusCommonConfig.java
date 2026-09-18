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

package cn.shopex.ecshopx.common.config;

import cn.shopex.ecshopx.common.mybatis.metadata.MpMetadataAnnotationHandler;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件等公共配置。Mapper 扫描在主应用类 {@code cn.shopex.ecshopx.EcshopxApplication} 的 {@code @MapperScan}。
 */
@Configuration
public class MybatisPlusCommonConfig {

	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
		return interceptor;
	}

	@Bean
	public MybatisPlusPropertiesCustomizer mpMetadataAnnotationHandlerCustomizer() {
		return properties -> {
			GlobalConfig globalConfig = properties.getGlobalConfig();
			if (globalConfig == null) {
				globalConfig = new GlobalConfig();
				properties.setGlobalConfig(globalConfig);
			}
			globalConfig.setAnnotationHandler(new MpMetadataAnnotationHandler());
		};
	}
}
