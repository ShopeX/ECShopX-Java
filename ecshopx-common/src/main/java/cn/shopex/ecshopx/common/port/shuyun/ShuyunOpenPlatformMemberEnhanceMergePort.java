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
 * 数云开放平台 enhance 读合并（D8）。Front 读会员注入；默认 NoOp。
 * 失败仅跳过，不阻断本地资料展示。
 */
public interface ShuyunOpenPlatformMemberEnhanceMergePort {

	boolean isOpenPlatformMemberEnabled(long companyId);

	/**
	 * 调用 enhance.member.post，将 name/birthday/gender 合并进 {@code memberInfo}（含 requestFields）。
	 */
	void mergeEnhanceIntoMemberInfo(long companyId, long userId, Map<String, Object> memberInfo);
}
