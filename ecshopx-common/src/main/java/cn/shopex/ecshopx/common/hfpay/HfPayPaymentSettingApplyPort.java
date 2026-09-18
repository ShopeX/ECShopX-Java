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

package cn.shopex.ecshopx.common.hfpay;

import java.util.Map;

/**
 * 汇付支付配置持久化出口：由 ecshopx-hfpay 实现，供 ecshopx-payment 调用以避免 Maven 模块环依赖。
 */
public interface HfPayPaymentSettingApplyPort {

	/**
	 * 将汇付配置写入 Redis（{@code hfPaymentSetting:} + SHA1(companyId)）并同步证书落盘，与读路径 {@code loadForCompany} 一致。
	 *
	 * @param companyId 企业 ID
	 * @param mergedRedisPayload 即将序列化写入 Redis 的完整字段映射（与现有读路径字段名一致）
	 */
	void applySetPaymentSetting(long companyId, Map<String, Object> mergedRedisPayload);

	/**
	 * 管理端按企业读取汇付支付配置（Redis + 可选证书同步），解析或同步失败时不抛 HTTP 业务异常，返回可序列化 Map。
	 */
	Map<String, Object> loadForAdminPaymentSettingGet(long companyId);
}
