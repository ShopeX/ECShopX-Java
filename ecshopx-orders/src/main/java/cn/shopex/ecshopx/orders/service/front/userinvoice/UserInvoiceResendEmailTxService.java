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

package cn.shopex.ecshopx.orders.service.front.userinvoice;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInvoiceResendEmailTxService {

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;

	public UserInvoiceResendEmailTxService(
			OrderInvoiceMapper orderInvoiceMapper, OrderInvoiceLogMapper orderInvoiceLogMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyEmailChangeAndLog(
			long invoiceId,
			OrderInvoice row,
			String confirmEmail,
			long nowSec,
			String operatorContentJson) {
		int ts = (int) nowSec;
		LambdaUpdateWrapper<OrderInvoice> uw =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.set(OrderInvoice::getEmail, confirmEmail)
						.set(OrderInvoice::getUpdateTime, ts);
		orderInvoiceMapper.update(null, uw);

		OrderInvoiceLog logRow = new OrderInvoiceLog();
		logRow.setInvoiceId(invoiceId);
		logRow.setOperatorType("admin");
		logRow.setOperatorId(1L);
		logRow.setUserId(row.getUserId());
		logRow.setOperatorContent(operatorContentJson);
		logRow.setCreateTime(ts);
		logRow.setUpdateTime(ts);
		orderInvoiceLogMapper.insert(logRow);
	}
}
