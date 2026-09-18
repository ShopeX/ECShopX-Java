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

package cn.shopex.ecshopx.common.openapi;

public interface OpenapiJurisdictionSysuserPort {

	SysuserResult syncSysuser(long companyId, OpenapiJurisdictionSysuserInput input);

	record OpenapiJurisdictionSysuserInput(
			String shopexId,
			String mobile,
			String operatorType,
			String loginName,
			String username,
			String password,
			String eid,
			String passportUid) {}

	sealed interface SysuserResult permits SysuserSuccess, SysuserBusinessFail, SysuserLegacyZeroCode {}

	record SysuserSuccess() implements SysuserResult {}

	/** 配置错误 / 帐号已存在 / 保存失败 → 外层 E0001 + 嵌套 data */
	record SysuserBusinessFail(String dataMessage) implements SysuserResult {}

	/** open 账号已开通 / update 无记录 → E0000 + data=null，由 Handler throw */
	record SysuserLegacyZeroCode(String message) implements SysuserResult {}
}
