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

package cn.shopex.ecshopx.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务迁移「阶段 0」：xxl-job 执行器，向 admin 自动注册；业务 handler 见 {@code cn.shopex.ecshopx.cron}。
 */
@Configuration
public class XxlJobConfig {

	@Value("${xxl.job.admin.addresses}")
	private String adminAddresses;

	@Value("${xxl.job.accessToken:}")
	private String accessToken;

	@Value("${xxl.job.executor.appname}")
	private String appname;

	@Value("${xxl.job.executor.address:}")
	private String address;

	@Value("${xxl.job.executor.ip:}")
	private String ip;

	@Value("${xxl.job.executor.port}")
	private int port;

	@Value("${xxl.job.executor.logpath:}")
	private String logPath;

	@Value("${xxl.job.executor.logretentiondays:7}")
	private int logRetentionDays;

	@Bean
	public XxlJobSpringExecutor xxlJobExecutor() {
		XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
		executor.setAdminAddresses(adminAddresses);
		executor.setAppname(appname);
		executor.setAddress(address);
		executor.setIp(ip);
		executor.setPort(port);
		executor.setAccessToken(accessToken);
		executor.setLogPath(logPath);
		executor.setLogRetentionDays(logRetentionDays);
		return executor;
	}
}
