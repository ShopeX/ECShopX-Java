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

package cn.shopex.ecshopx.members.service.reltag;

import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberTagRelationMarketingSyncService {

	private static final DateTimeFormatter TIMESTAMP_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public MemberTagRelationMarketingSyncService(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	/**
	 * Posts one batch to the marketing center {@code members.tag.relation.sync} endpoint with a
	 * {@code formBody} containing {@code company_id}, {@code action}, {@code timestamp}, and {@code relations}.
	 */
	public void syncRelationBatchesToShoppingGuide(
			long companyId, String action, List<Map<String, Object>> relations) {
		if (companyId <= 0L || relations == null || relations.isEmpty()) {
			return;
		}
		Map<String, Object> formBody = new LinkedHashMap<>();
		formBody.put("company_id", String.valueOf(companyId));
		formBody.put("action", action == null ? "" : action);
		formBody.put("timestamp", TIMESTAMP_FMT.format(ZonedDateTime.now()));
		formBody.put("relations", relations);
		marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(
				companyId, "members.tag.relation.sync", formBody);
	}
}
