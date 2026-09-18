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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayTradeRefundSnippetQueryService {

	private final AftersalesRefundMapper aftersalesRefundMapper;

	public AdapayTradeRefundSnippetQueryService(AftersalesRefundMapper aftersalesRefundMapper) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
	}

	public List<Map<String, Object>> listRefundSnippets(long companyId, String tradeId) {
		List<AftersalesRefund> rows = aftersalesRefundMapper.selectList(new LambdaQueryWrapper<AftersalesRefund>()
				.eq(AftersalesRefund::getCompanyId, companyId)
				.eq(AftersalesRefund::getTradeId, tradeId));
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (AftersalesRefund r : rows) {
			Map<String, Object> m = new LinkedHashMap<>(4);
			m.put("refund_bn", r.getRefundBn());
			m.put("order_id", r.getOrderId());
			m.put("refunded_fee", r.getRefundedFee() != null ? r.getRefundedFee() : 0);
			m.put("create_time", r.getCreateTime());
			out.add(m);
		}
		return out;
	}
}
