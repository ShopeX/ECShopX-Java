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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantSettlementApplyListParamValidator {

	private static final String MSG_PAGE = "当前页数为大于0的整数";
	private static final String MSG_PAGE_SIZE = "每页数量为1-50的整数";

	public MerchantSettlementApplyListQueryInput validateAndExtract(Map<String, Object> params) {
		int page = requirePage(MerchantListParamValidator.scalarFrom(params.get("page")));
		int pageSize = requirePageSize(MerchantListParamValidator.scalarFrom(params.get("page_size")));
		Integer createdGte = null;
		Integer createdLte = null;
		Object tsRaw = params.get("time_start");
		if (tsRaw != null) {
			if (tsRaw instanceof Collection<?> c) {
				if (!c.isEmpty()) {
					if (c.size() != 2) {
						throw new ResourceException("time_start 须包含开始与结束两个时间");
					}
					Object[] arr = c.toArray();
					createdGte = requireEpoch(arr[0], "time_start 开始时间无效");
					createdLte = requireEpoch(arr[1], "time_start 结束时间无效");
				}
			} else if (tsRaw instanceof String s && StringUtils.hasText(s)) {
				throw new ResourceException("time_start 须包含开始与结束两个时间");
			} else if (!(tsRaw instanceof Collection<?>)) {
				throw new ResourceException("time_start 须包含开始与结束两个时间");
			}
		}
		String auditStatus = trimOrNull(params, "audit_status");
		String merchantName = trimOrNull(params, "merchant_name");
		String province = trimOrNull(params, "province");
		String city = trimOrNull(params, "city");
		String area = trimOrNull(params, "area");
		String settledType = trimOrNull(params, "settled_type");
		return new MerchantSettlementApplyListQueryInput(
				page,
				pageSize,
				auditStatus,
				merchantName,
				province,
				city,
				area,
				settledType,
				createdGte,
				createdLte);
	}

	private static Integer requireEpoch(Object o, String message) {
		String t = o == null ? "" : String.valueOf(o).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(message);
		}
		Integer sec = MerchantListParamValidator.parseToEpochSecond(t);
		if (sec == null) {
			throw new ResourceException(message);
		}
		return sec;
	}

	private static int requirePage(Object scalar) {
		if (scalar == null) {
			throw new ResourceException(MSG_PAGE);
		}
		String t = String.valueOf(scalar).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(MSG_PAGE);
		}
		try {
			int v;
			if (scalar instanceof Number n) {
				v = n.intValue();
			} else {
				v = Integer.parseInt(t);
			}
			if (v < 1) {
				throw new ResourceException(MSG_PAGE);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(MSG_PAGE);
		}
	}

	private static int requirePageSize(Object scalar) {
		if (scalar == null) {
			throw new ResourceException(MSG_PAGE_SIZE);
		}
		String t = String.valueOf(scalar).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(MSG_PAGE_SIZE);
		}
		try {
			int v;
			if (scalar instanceof Number n) {
				v = n.intValue();
			} else {
				v = Integer.parseInt(t);
			}
			if (v < 1 || v > 50) {
				throw new ResourceException(MSG_PAGE_SIZE);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(MSG_PAGE_SIZE);
		}
	}

	private static String trimOrNull(Map<String, Object> params, String key) {
		Object v = params.get(key);
		Object s = MerchantListParamValidator.scalarFrom(v);
		if (s == null) {
			return null;
		}
		String t = String.valueOf(s).trim();
		return StringUtils.hasText(t) ? t : null;
	}
}
