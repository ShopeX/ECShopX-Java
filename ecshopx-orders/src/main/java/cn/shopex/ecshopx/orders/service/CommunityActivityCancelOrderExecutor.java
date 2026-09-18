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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 慢队列拉取后逐行取消，与 PHP Job 中 try/catch 仅打日志不抛出对齐。
 */
@Service
public class CommunityActivityCancelOrderExecutor {

	private static final Logger log = LoggerFactory.getLogger(CommunityActivityCancelOrderExecutor.class);

	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;

	public CommunityActivityCancelOrderExecutor(AdminNormalOrderFullCancelService adminNormalOrderFullCancelService) {
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
	}

	public void execute(CommunityActivityCancelOrderRow row) {
		if (row == null) {
			return;
		}
		try {
			LinkedHashMap<String, Object> params = new LinkedHashMap<>();
			String reason = row.getCancelReason() == null ? "" : row.getCancelReason();
			params.put("other_reason", reason);
			if (row.getChiefId() != null && row.getChiefId() > 0) {
				params.put("chief_id", row.getChiefId());
			}
			String cancelFrom = StringUtils.hasText(row.getCancelFrom()) ? row.getCancelFrom() : "system";
			adminNormalOrderFullCancelService.execute(
					row.getCompanyId(),
					"shop",
					0L,
					0L,
					row.getUserId(),
					row.getMobile() == null ? "" : row.getMobile(),
					row.getOrderId(),
					reason,
					params,
					cancelFrom);
		} catch (Exception e) {
			log.info("订单取消失败：{}; ROW: {}", e.getMessage(), row);
		}
	}
}
