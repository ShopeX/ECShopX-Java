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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TrackViewNumService {

	private static final String PREFIX = "datecube_tracklog";

	private final MemberAccountService memberAccountService;
	private final StringRedisTemplate datacubeStringRedisTemplate;
	private final UvBloomFilterService uvBloomFilterService;

	public TrackViewNumService(
			MemberAccountService memberAccountService,
			@Qualifier("datacubeStringRedisTemplate") StringRedisTemplate datacubeStringRedisTemplate,
			UvBloomFilterService uvBloomFilterService) {
		this.memberAccountService = memberAccountService;
		this.datacubeStringRedisTemplate = datacubeStringRedisTemplate;
		this.uvBloomFilterService = uvBloomFilterService;
	}

	public void addViewNum(long companyId, String monitorId, String sourceId, String openId) {
		if (companyId <= 0) {
			return;
		}
		if (!StringUtils.hasText(monitorId) || !StringUtils.hasText(sourceId)) {
			return;
		}

		String ymd = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
		String cid = Long.toString(companyId);
		String pvKey = PREFIX + ":viewnum:" + cid + "|" + monitorId + ":page_view:" + sourceId;
		String uvKey = PREFIX + ":viewnum:" + cid + "|" + monitorId + ":unique_visitor:" + sourceId;
		String mvKey = PREFIX + ":viewnum:" + cid + "|" + monitorId + ":member_visitor:" + sourceId;

		datacubeStringRedisTemplate.opsForHash().increment(pvKey, ymd, 1L);

		if (uvBloomFilterService.checkAndAdd(monitorId, sourceId, openId)) {
			datacubeStringRedisTemplate.opsForHash().increment(uvKey, ymd, 1L);
			Map<String, Object> userInfo = memberAccountService.getWechatSimpleUser(
					Map.of("open_id", openId, "company_id", companyId));
			if (!userInfo.isEmpty()) {
				datacubeStringRedisTemplate.opsForHash().increment(mvKey, ymd, 1L);
			}
		}
	}
}
