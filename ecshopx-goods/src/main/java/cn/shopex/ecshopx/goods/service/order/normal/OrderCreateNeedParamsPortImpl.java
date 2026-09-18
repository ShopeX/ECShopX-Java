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

package cn.shopex.ecshopx.goods.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateNeedParamsPortImpl implements OrderCreateNeedParamsPort {

	@Override
	public void checkCreateOrderNeedParamsForTempInfo(Map<String, Object> params, boolean isCreate) {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().putAll(params);
		if (isCreate) {
			validate(p);
		} else {
			validateForTempInfoPreview(p);
		}
	}

	private void validateForTempInfoPreview(NormalOrderCreateParams p) {
		validate(p);
	}

	@Override
	public void validate(NormalOrderCreateParams p) {
		Map<String, Object> m = p.getParams();
		long companyId = longVal(m.get("company_id"), 0L);
		if (companyId <= 0L) {
			throw new ResourceException("企业id必填");
		}
		if (!m.containsKey("user_id")) {
			throw new ResourceException("用户id必填");
		}
		String receipt = stringVal(m.get("receipt_type"));
		if (!StringUtils.hasText(receipt)
				|| "logistics".equals(receipt)
				|| "ziti".equals(receipt)
				|| "dada".equals(receipt)
				|| "merchant".equals(receipt)) {
			if ("normal_groups".equals(stringVal(m.get("order_type")))
					&& longVal(m.get("bargain_id"), 0L) <= 0L) {
				throw new ResourceException("拼团id必填");
			}
			return;
		}
		throw new ResourceException("请选择正确的配送方式");
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
