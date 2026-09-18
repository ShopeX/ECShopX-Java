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

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.aliyunsms.MassTaskSendSmsResult;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替代真实 {@code SendSms}；返回 Code=OK 与固定 BizId，供 {@code aliyunsms_record} 多行同 biz_id 对齐 PHP。
 */
@Slf4j
public class NoopAliyunsmsMassTaskSendClient implements AliyunsmsMassTaskSendClient {

	public static final String MOCK_BIZ_ID = "cron-mock-biz";

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public MassTaskSendSmsResult sendMassSms(
			String accessKeyId,
			String accessKeySecret,
			String phoneNumbers,
			String signName,
			String templateCode,
			String templateParamJson) {
		int n = callCount.incrementAndGet();
		int phonePieces =
				phoneNumbers == null || phoneNumbers.isBlank()
						? 0
						: (int) Arrays.stream(phoneNumbers.split(",")).filter(s -> !s.isBlank()).count();
		log.info(
				"[cron-mock][aliyunsms-mass-task-send] called#{}, args={}",
				n,
				Arrays.toString(
						new Object[] {
							signName,
							templateCode,
							phonePieces,
							templateParamJson != null && !templateParamJson.isBlank()
						}));
		return new MassTaskSendSmsResult("OK", "OK", MOCK_BIZ_ID);
	}
}
