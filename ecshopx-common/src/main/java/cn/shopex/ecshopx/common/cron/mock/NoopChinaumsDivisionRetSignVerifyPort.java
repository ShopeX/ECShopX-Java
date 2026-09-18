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

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRetSignVerifyPort;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ChinaumsDivisionRetSignVerifyPort} 的 Noop 实现；阶段 4 通过
 * <code>[cron-mock][chinaums-division-ret-sign-verify]</code> 做断言。
 */
@Slf4j
public class NoopChinaumsDivisionRetSignVerifyPort implements ChinaumsDivisionRetSignVerifyPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void verifyDataFileSign(
			long companyId, String localRetFileRelative, String remoteDir, String dataFileNameBase)
			throws IOException {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-ret-sign-verify] called#{}, args={}",
				n,
				Arrays.toString(
						new Object[] {companyId, localRetFileRelative, remoteDir, dataFileNameBase}));
	}
}
