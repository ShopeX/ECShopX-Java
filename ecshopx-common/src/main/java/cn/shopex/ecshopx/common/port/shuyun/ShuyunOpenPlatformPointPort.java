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
 * 数云开放平台积分出站（D9）。point 模块可注入；默认实现可为空操作。
 */
public interface ShuyunOpenPlatformPointPort {

	/** 租户是否已启用且具备出站资格（含 access_token）。 */
	boolean isOpenPlatformPointEnabled(long companyId);

	/**
	 * 调用 point.change。
	 *
	 * @param plus true=增加 false=扣减
	 * @return true 网关成功
	 */
	boolean changePoint(
			long companyId,
			long userId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			Map<String, Object> otherParams);

	/**
	 * point.changelog.search；失败返回 null。
	 *
	 * @return Map 含 totals/pageNum/pageSize/list，或 null
	 */
	Map<String, Object> searchChangelog(
			long companyId, long userId, long regDistributorId, int pageNo, int pageSize);

	/**
	 * enhance.member.query.detail 取可用积分（validPoint → pointAsserts）；失败返回 null。
	 */
	Long queryValidPoint(long companyId, long userId);
}
