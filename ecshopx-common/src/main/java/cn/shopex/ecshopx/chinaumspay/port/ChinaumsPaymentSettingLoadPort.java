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

package cn.shopex.ecshopx.chinaumspay.port;

import java.util.Map;

/**
 * 与 PHP {@code ChinaumsPayService::getPaymentSetting(companyId, subKey)} 对齐的 Redis
 * 读配置；生产实现读 companys Redis，test-cron 下可替换为 Noop。
 */
public interface ChinaumsPaymentSettingLoadPort {

	/**
	 * @param subKey 空串表示主配置；否则如 {@code distributor_123}、{@code dealer_456}（不含 key 前缀）
	 */
	Map<String, Object> load(long companyId, String subKey);
}
