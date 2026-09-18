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

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AliyunsmsGetSmsSignClient} 的 Noop；test-cron 下替代真实 Dysms 调用。默认不返回 signStatus，避免误写库。
 */
@Slf4j
public class NoopAliyunsmsGetSmsSignClient implements AliyunsmsGetSmsSignClient {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public GetSmsSignResult getSmsSign(long companyId, String signName) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][aliyunsms-get-sms-sign] called#{}, args={}", n, Arrays.toString(new Object[] { companyId, signName }));
		return GetSmsSignResult.noSignStatus();
	}
}
