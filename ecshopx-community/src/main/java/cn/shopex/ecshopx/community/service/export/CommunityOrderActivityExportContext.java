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

package cn.shopex.ecshopx.community.service.export;

import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;

/**
 * 团购订单异步导出任务上下文（HTTP 线程构建，异步线程消费）。
 */
public record CommunityOrderActivityExportContext(
		long companyId,
		long operatorId,
		boolean datapassAllowed,
		CommunityActivityAdminListQuery query,
		Long optionalActivityId) {}
