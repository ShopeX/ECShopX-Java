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

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySendDetailsResult;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AliyunsmsQuerySendDetailsClient} 的 Noop；test-cron 下替代真实 Dysms 调用。默认不返回 sendStatus，避免误写库。
 *
 * <p>日志<strong>故意不打印</strong> {@code mobile}（敏感字段；service 层已 decrypt 为明文）；仅记录 {@code companyId / bizId / sendDate}
 * 即可还原调用路径。
 */
@Slf4j
public class NoopAliyunsmsQuerySendDetailsClient implements AliyunsmsQuerySendDetailsClient {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public QuerySendDetailsResult querySendDetails(long companyId, String mobile, String bizId, String sendDate) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][aliyunsms-query-send-details] called#{}, args={}",
				n,
				Arrays.toString(new Object[] { companyId, bizId, sendDate }));
		return QuerySendDetailsResult.noDetail();
	}
}
