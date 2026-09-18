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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.cron.hfpay.HfPayQry008Port;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 将 {@link HfPayQry008Port} 委托给 {@link HfPayAcouJsonPostClient#qry008}；test-cron 下由 {@code CronTestMockConfig} 以 {@code @Primary} Noop 覆盖。
 */
@Service
@RequiredArgsConstructor
public class HfPayQry008PortAcouAdapter implements HfPayQry008Port {

	private final HfPayAcouJsonPostClient acouJsonPostClient;

	@Override
	public Map<String, Object> qry008(Map<String, Object> setting, Map<String, Object> payload) {
		return acouJsonPostClient.qry008(setting, payload);
	}
}
