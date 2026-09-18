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

package cn.shopex.ecshopx.espier.cron.testcfg;

import cn.shopex.ecshopx.common.cron.EspierExportHistoryZipFileRemover;
import cn.shopex.ecshopx.common.cron.EspierScheduledUploadSourceFileRemover;
import cn.shopex.ecshopx.common.cron.mock.NoopEspierExportHistoryZipFileRemover;
import cn.shopex.ecshopx.common.cron.mock.NoopEspierScheduledUploadSourceFileRemover;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖；仅 spring.profiles.active 含 test-cron 时生效。
 * 用 Noop 替换源文件删除端口，避免阶段 4 快照对比时真实删 COS/OSS/本地盘。
 */
@Profile("test-cron")
@Configuration("espierCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖 {@link cn.shopex.ecshopx.espier.cron.support.EspierScheduledUploadSourceFileRemoverImpl}，避免 test-cron 下真实删存储。
	 */
	@Bean
	@Primary
	public EspierScheduledUploadSourceFileRemover noopEspierScheduledUploadSourceFileRemover() {
		return new NoopEspierScheduledUploadSourceFileRemover();
	}

	/**
	 * 覆盖 {@link cn.shopex.ecshopx.espier.cron.support.EspierExportHistoryZipFileRemoverImpl}，避免 test-cron 下真实删历史 zip。
	 */
	@Bean
	@Primary
	public EspierExportHistoryZipFileRemover noopEspierExportHistoryZipFileRemover() {
		return new NoopEspierExportHistoryZipFileRemover();
	}
}
