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

package cn.shopex.ecshopx;

import cn.shopex.ecshopx.common.mybatis.FqcnMapperBeanNameGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 多模块下若存在同名 {@code *Mapper} 接口，须在 {@code @MapperScan} 上指定 {@link FqcnMapperBeanNameGenerator}，
 * 避免默认短 Bean 名冲突。将 {@code @MapperScan} 放在主类上，使 {@code MapperScannerRegistrar} 在解析主配置阶段
 * 即注册 {@code MapperFactoryBean}，从而满足 MyBatis-Plus 的 {@code @ConditionalOnMissingBean(MapperFactoryBean)}，
 * 不再启用其自动 Mapper 扫描（与 Spring Boot + MyBatis 常见写法一致，无需占位 {@code MapperScannerConfigurer}）。
 */
@SpringBootApplication(scanBasePackages = "cn.shopex.ecshopx")
@MapperScan(
		basePackages = "cn.shopex.ecshopx.**.mapper",
		nameGenerator = FqcnMapperBeanNameGenerator.class,
		sqlSessionFactoryRef = "sqlSessionFactory",
		lazyInitialization = "true")
public class EcshopxApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcshopxApplication.class, args);
	}

}
