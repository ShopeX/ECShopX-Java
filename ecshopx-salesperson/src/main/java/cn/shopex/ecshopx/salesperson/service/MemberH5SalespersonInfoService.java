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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberH5SalespersonInfoService {

	private final WorkWechatRelMapper workWechatRelMapper;
	private final SalespersonGetInfoService salespersonGetInfoService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public MemberH5SalespersonInfoService(WorkWechatRelMapper workWechatRelMapper,
			SalespersonGetInfoService salespersonGetInfoService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.workWechatRelMapper = workWechatRelMapper;
		this.salespersonGetInfoService = salespersonGetInfoService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	public Map<String, Object> getSalespersonInfo(long userId) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("is_show", 0);

		WorkWechatRel rel = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE)
				.last("LIMIT 1"));

		long sid = (rel == null || rel.getSalespersonId() == null) ? 0L : rel.getSalespersonId();
		if (sid == 0L) {
			return result;
		}

		Map<String, Object> detail = salespersonGetInfoService.getShoppingGuideDetailForH5(sid);
		result.putAll(detail);

		long companyId;
		try {
			Object rawComp = result.get("company_id");
			if (rawComp == null) {
				companyId = 0L;
			} else {
				companyId = Long.parseLong(String.valueOf(rawComp).trim());
			}
		} catch (NumberFormatException e) {
			companyId = 0L;
		}
		boolean companyOk = companyId > 0L;

		Object distRaw = result.get("distributor_id");
		if (distRaw instanceof Boolean || distRaw == null) {
			result.put("distributor", Collections.emptyList());
		} else if (distRaw instanceof Number n) {
			long d = n.longValue();
			if (d > 0L && companyOk) {
				Object distPayload =
						distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(d));
				result.put("distributor", distPayload);
			} else {
				result.put("distributor", Collections.emptyList());
			}
		} else {
			String s = String.valueOf(distRaw).trim();
			try {
				long d = Long.parseLong(s);
				if (d > 0L && companyOk) {
					Object distPayload =
							distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(d));
					result.put("distributor", distPayload);
				} else {
					result.put("distributor", Collections.emptyList());
				}
			} catch (NumberFormatException e) {
				result.put("distributor", Collections.emptyList());
			}
		}

		result.put("is_friend", rel.getIsFriend() != null && rel.getIsFriend() ? 1 : 0);

		Object d = result.get("distributor");
		boolean nonEmpty = (d instanceof Map && !((Map<?, ?>) d).isEmpty())
				|| (d instanceof Collection && !((Collection<?>) d).isEmpty());
		result.put("is_show", nonEmpty ? 1 : 0);

		return result;
	}

	public Map<String, Object> getSalespersonInfoNologin(long salespersonId) {
		LinkedHashMap<String, Object> detail =
				new LinkedHashMap<>(salespersonGetInfoService.getShoppingGuideDetailForH5(salespersonId));
		if (detail.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("distributor", Collections.emptyList());
			return empty;
		}

		long companyId;
		try {
			Object rawComp = detail.get("company_id");
			if (rawComp == null) {
				companyId = 0L;
			} else {
				companyId = Long.parseLong(String.valueOf(rawComp).trim());
			}
		} catch (NumberFormatException e) {
			companyId = 0L;
		}
		boolean companyOk = companyId > 0L;

		Object distRaw = detail.get("distributor_id");
		if (distRaw instanceof Boolean || distRaw == null) {
			detail.put("distributor", Collections.emptyList());
		} else if (distRaw instanceof Number n) {
			long d = n.longValue();
			if (d > 0L && companyOk) {
				Object distPayload =
						distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(d));
				detail.put("distributor", distPayload);
			} else {
				detail.put("distributor", Collections.emptyList());
			}
		} else {
			String s = String.valueOf(distRaw).trim();
			try {
				long d = Long.parseLong(s);
				if (d > 0L && companyOk) {
					Object distPayload =
							distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(d));
					detail.put("distributor", distPayload);
				} else {
					detail.put("distributor", Collections.emptyList());
				}
			} catch (NumberFormatException e) {
				detail.put("distributor", Collections.emptyList());
			}
		}

		return detail;
	}
}
