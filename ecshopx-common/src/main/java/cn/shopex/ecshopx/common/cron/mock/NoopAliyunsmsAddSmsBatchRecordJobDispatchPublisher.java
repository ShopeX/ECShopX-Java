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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下覆盖真实 {@link AliyunsmsAddSmsBatchRecordJobDispatchPublisher}，避免入队与落库链。
 */
@Slf4j
public class NoopAliyunsmsAddSmsBatchRecordJobDispatchPublisher implements AliyunsmsAddSmsBatchRecordJobDispatchPublisher {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void publish(
			long companyId,
			int taskId,
			List<String> mobilesPlain,
			int sceneId,
			String templateCode,
			String templateType,
			String smsContent,
			int status,
			String bizId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][add-sms-batch-record-job] called#{}, companyId={}, taskId={}, mobiles={}",
				n,
				companyId,
				taskId,
				mobilesPlain == null ? 0 : mobilesPlain.size());
	}
}
