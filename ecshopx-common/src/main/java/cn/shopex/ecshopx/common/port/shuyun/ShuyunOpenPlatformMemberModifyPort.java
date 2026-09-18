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

package cn.shopex.ecshopx.common.port.shuyun;

import java.util.Map;

/**
 * 数云开放平台会员资料 modify（D7）。members 注入；默认 NoOp。
 */
public interface ShuyunOpenPlatformMemberModifyPort {

	boolean isOpenPlatformMemberEnabled(long companyId);

	/**
	 * 映射 username/birthday/sex 后出站；无变更字段返回 false（跳过）。
	 *
	 * @return true 网关成功或无需同步；失败抛 ResourceException
	 */
	boolean modifyIfNeeded(long companyId, long userId, Map<String, Object> memberFields);
}
