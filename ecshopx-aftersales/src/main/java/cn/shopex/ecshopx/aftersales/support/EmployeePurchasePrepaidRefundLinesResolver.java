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

package cn.shopex.ecshopx.aftersales.support;

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 从售后明细解析企业购预充点 SKU 限购还点行。 */
@Component
public class EmployeePurchasePrepaidRefundLinesResolver {

	private final AftersalesDetailMapper aftersalesDetailMapper;

	public EmployeePurchasePrepaidRefundLinesResolver(AftersalesDetailMapper aftersalesDetailMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
	}

	public List<EmployeePurchasePrepaidAftersalesRestorePort.RefundLine> resolve(
			long companyId, AftersalesRefund refund) {
		if (refund == null) {
			return List.of();
		}
		Long aftersalesBn = refund.getAftersalesBn();
		if (aftersalesBn == null || aftersalesBn <= 0L) {
			return List.of();
		}
		List<AftersalesDetail> details =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
		if (details == null || details.isEmpty()) {
			return List.of();
		}
		List<EmployeePurchasePrepaidAftersalesRestorePort.RefundLine> lines = new ArrayList<>();
		for (AftersalesDetail d : details) {
			long itemId = d.getItemId() == null ? 0L : d.getItemId();
			int num = d.getRefundedNum() != null && d.getRefundedNum() > 0
					? d.getRefundedNum()
					: (d.getNum() == null ? 0 : d.getNum());
			int itemFee = d.getRefundFee() == null ? 0 : d.getRefundFee();
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			lines.add(new EmployeePurchasePrepaidAftersalesRestorePort.RefundLine(itemId, num, itemFee));
		}
		return lines;
	}
}
