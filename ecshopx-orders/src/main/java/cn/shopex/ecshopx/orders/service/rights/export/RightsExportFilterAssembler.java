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

package cn.shopex.ecshopx.orders.service.rights.export;

import cn.shopex.ecshopx.members.service.admin.MembersShopRelIntersectUserIdsService;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RightsExportFilterAssembler {

	public record Assembly(LinkedHashMap<String, Object> filter, boolean emptyShopNoMembers) {}

	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final MembersShopRelIntersectUserIdsService membersShopRelIntersectUserIdsService;

	public RightsExportFilterAssembler(
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			MembersShopRelIntersectUserIdsService membersShopRelIntersectUserIdsService) {
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.membersShopRelIntersectUserIdsService = membersShopRelIntersectUserIdsService;
	}

	public Assembly assemble(
			long companyId,
			HttpServletRequest request,
			String mobileParam,
			String userIdParam,
			String validParam,
			String dateBegin,
			String dateEnd,
			String rightsFrom,
			String orderId,
			String shopId) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		if (isNonZeroIntegerString(mobileParam)) {
			String plain = mobileParam.trim();
			Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, plain);
			if (uid != null && uid > 0L) {
				filter.put("user_id", uid);
			} else {
				try {
					filter.put("mobile", Long.parseLong(plain));
				} catch (NumberFormatException e) {
					filter.put("mobile", plain);
				}
			}
		}

		String userIdInput = request.getParameter("user_id");
		if (userIdInput == null) {
			userIdInput = userIdParam;
		}
		if (StringUtils.hasText(userIdInput)) {
			List<Long> multi = parseCommaLongs(userIdInput);
			if (multi.size() > 1) {
				filter.put("user_id", multi);
			} else if (multi.size() == 1) {
				filter.put("user_id", multi.get(0));
			} else {
				filter.put("user_id", userIdInput.trim());
			}
		}

		if (request.getParameterMap().containsKey("valid")) {
			int v = 0;
			if (StringUtils.hasText(validParam)) {
				try {
					v = Integer.parseInt(validParam.trim());
				} catch (NumberFormatException ignored) {
				}
			}
			filter.put("valid", v);
		}

		if (dateBeginTruthy(dateBegin)) {
			String endRaw = dateEnd == null ? "" : dateEnd;
			filter.put("datetime", List.of(dateBegin.trim(), endRaw.trim()));
		}

		if (StringUtils.hasText(rightsFrom) && !"0".equals(rightsFrom.trim())) {
			filter.put("rights_from", rightsFrom.trim());
		}

		String orderRaw = request.getParameter("order_id");
		if (orderRaw == null) {
			orderRaw = orderId;
		}
		if (StringUtils.hasText(orderRaw) && !"0".equals(orderRaw.trim())) {
			filter.put("order_id", orderRaw.trim());
		}

		List<Long> shopIds = parseShopIds(shopId);
		if (!shopIds.isEmpty()) {
			List<Long> restrict = extractRestrictUserIds(filter.get("user_id"));
			List<Long> intersect =
					membersShopRelIntersectUserIdsService.listUserIdsBelongingToAllShops(
							companyId, shopIds, restrict);
			if (intersect.isEmpty()) {
				return new Assembly(filter, true);
			}
			filter.put("user_id", intersect);
		}

		return new Assembly(filter, false);
	}

	private static boolean dateBeginTruthy(String dateBegin) {
		if (!StringUtils.hasText(dateBegin)) {
			return false;
		}
		String t = dateBegin.trim();
		return !"0".equals(t);
	}

	private static boolean isNonZeroIntegerString(String mobileParam) {
		if (!StringUtils.hasText(mobileParam)) {
			return false;
		}
		try {
			long v = Long.parseLong(mobileParam.trim());
			return v != 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static List<Long> parseShopIds(String shopId) {
		if (!StringUtils.hasText(shopId)) {
			return List.of();
		}
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (String part : shopId.split(",")) {
			String t = part.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				long v = Long.parseLong(t);
				if (v != 0L) {
					set.add(v);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return new ArrayList<>(set);
	}

	private static List<Long> parseCommaLongs(String raw) {
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		for (String part : raw.split(",")) {
			String t = part.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				long v = Long.parseLong(t);
				if (v > 0L) {
					out.add(v);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return new ArrayList<>(out);
	}

	private static List<Long> extractRestrictUserIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? List.of(v) : List.of();
		}
		if (raw instanceof Collection<?> c) {
			LinkedHashSet<Long> set = new LinkedHashSet<>();
			for (Object o : c) {
				if (o instanceof Number nn) {
					long v = nn.longValue();
					if (v > 0L) {
						set.add(v);
					}
				} else if (o != null && StringUtils.hasText(o.toString())) {
					try {
						long v = Long.parseLong(o.toString().trim());
						if (v > 0L) {
							set.add(v);
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return new ArrayList<>(set);
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			return parseCommaLongs(s);
		}
		return List.of();
	}
}
