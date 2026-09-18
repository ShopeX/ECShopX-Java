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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterAddService {

	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final MemberAccountService memberAccountService;
	private final PromoterChangePromoterService promoterChangePromoterService;
	private final PromoterRelRemoveService promoterRelRemoveService;
	private final boolean oemShuyun;

	public PromoterAddService(
			PopularizeSettingSaveService popularizeSettingSaveService,
			MemberAccountService memberAccountService,
			PromoterChangePromoterService promoterChangePromoterService,
			PromoterRelRemoveService promoterRelRemoveService,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.memberAccountService = memberAccountService;
		this.promoterChangePromoterService = promoterChangePromoterService;
		this.promoterRelRemoveService = promoterRelRemoveService;
		this.oemShuyun = oemShuyun;
	}

	public void addPromoter(long companyId, String mobileRaw, String userIdRaw, Map<String, Object> body) {
		String trimMobile = mobileRaw == null ? "" : mobileRaw.trim();
		long userId;
		if (StringUtils.hasText(trimMobile)) {
			userId = memberAccountService.requireUserIdByMobileInCompany(companyId, trimMobile);
		} else {
			userId = parseLongStrict(userIdRaw);
		}

		if (oemShuyun) {
			Map<String, Object> params = extractOemWhitelist(body);
			Map<String, Object> config = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
			String internalOpenIdentity = stringifyConfigFlag(config.get("internalOpenIdentity"), "false");
			if ("true".equalsIgnoreCase(internalOpenIdentity) || "1".equals(internalOpenIdentity)) {
				if (!params.containsKey("identity_id")) {
					throw new ResourceException("推广员身份ID错误");
				}
				long identityId = parseIdentityIdForOem(params.get("identity_id"));
				if (identityId <= 0L) {
					throw new ResourceException("推广员身份ID错误");
				}
			}
			params.put("internalOpenIdentity", internalOpenIdentity);
			promoterChangePromoterService.changePromoter(companyId, userId, true, params);
		} else {
			Map<String, Object> data = promoterChangePromoterService.changePromoter(companyId, userId, true, null);
			Object listRaw = data.get("list");
			if (listRaw instanceof List<?> list && !list.isEmpty()) {
				Object first = list.get(0);
				if (first instanceof Map<?, ?> m) {
					Object pid = m.get("pid");
					if (pidNumberTruthy(pid)) {
						promoterRelRemoveService.relRemove(companyId, userId, 0L);
					}
				}
			}
		}
	}

	private static Map<String, Object> extractOemWhitelist(Map<String, Object> body) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (body == null) {
			return out;
		}
		String[] keys = {"identity_id", "promoter_name", "regions_id", "address", "pid", "pmobile", "pname"};
		for (String k : keys) {
			if (body.containsKey(k)) {
				out.put(k, body.get(k));
			}
		}
		return out;
	}

	private static String stringifyConfigFlag(Object v, String whenMissing) {
		if (v == null) {
			return whenMissing;
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "false";
		}
		return String.valueOf(v).trim();
	}

	private static long parseLongStrict(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ResourceException("参数错误");
		}
		try {
			long id = Long.parseLong(raw.trim());
			if (id <= 0L) {
				throw new ResourceException("参数错误");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("参数错误");
		}
	}

	private static long parseIdentityIdForOem(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean pidNumberTruthy(Object pid) {
		if (pid == null) {
			return false;
		}
		if (pid instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (pid instanceof String s) {
			try {
				return Long.parseLong(s.trim()) != 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return Boolean.TRUE.equals(pid);
	}
}
