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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.orders.domain.OrderItemsProfit;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.OrderItemsProfitMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class OrderProfitService {

	private static final int PAGE_SIZE = 100;
	private static final long PROFIT_STATUS_FROZEN = 1L;
	private static final long PROFIT_STATUS_SETTLED = 2L;

	private final OrderProfitMapper orderProfitMapper;
	private final OrderItemsProfitMapper orderItemsProfitMapper;
	private final TransactionTemplate transactionTemplate;

	public OrderProfitService(
			OrderProfitMapper orderProfitMapper,
			OrderItemsProfitMapper orderItemsProfitMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.orderProfitMapper = orderProfitMapper;
		this.orderItemsProfitMapper = orderItemsProfitMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public int scheduleSettleProfit() {
		long nowEpoch = Instant.now().getEpochSecond();
		long totalCount = orderProfitMapper.countPendingFrozenProfitWithNormalOrderJoin(nowEpoch);
		if (totalCount == 0) {
			return 0;
		}
		int processed = 0;
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		for (int page = 1; page <= totalPage; page++) {
			int offset = (page - 1) * PAGE_SIZE;
			List<OrderProfit> list = orderProfitMapper.selectPageForSettle(nowEpoch, offset, PAGE_SIZE);
			for (OrderProfit row : list) {
				try {
					transactionTemplate.executeWithoutResult(status -> settleOneRow(row));
					processed++;
				} catch (Exception e) {
					log.debug("定时执行导购分销佣金结算失败=>{}", e.getMessage());
					log.debug("定时执行导购分销佣金结算失败参数=>{}", row);
				}
			}
		}
		return processed;
	}

	public Optional<OrderProfit> findForTradeFinishProfit(long orderId, long companyId, long userId) {
		OrderProfit row =
				orderProfitMapper.selectOne(
						new LambdaQueryWrapper<OrderProfit>()
								.eq(OrderProfit::getOrderId, orderId)
								.eq(OrderProfit::getCompanyId, companyId)
								.eq(OrderProfit::getUserId, userId)
								.last("LIMIT 1"));
		return Optional.ofNullable(row);
	}

	public void markTradeFinishProfitClosed(long orderId, long companyId, long userId, long planCloseEpochSeconds) {
		transactionTemplate.executeWithoutResult(
				status -> {
					LambdaUpdateWrapper<OrderProfit> head = new LambdaUpdateWrapper<>();
					head.eq(OrderProfit::getOrderId, orderId)
							.eq(OrderProfit::getCompanyId, companyId)
							.eq(OrderProfit::getUserId, userId)
							.set(OrderProfit::getOrderProfitStatus, PROFIT_STATUS_FROZEN)
							.set(OrderProfit::getPlanCloseTime, planCloseEpochSeconds);
					orderProfitMapper.update(null, head);

					LambdaUpdateWrapper<OrderItemsProfit> items = new LambdaUpdateWrapper<>();
					items.eq(OrderItemsProfit::getOrderId, orderId)
							.eq(OrderItemsProfit::getCompanyId, companyId)
							.eq(OrderItemsProfit::getUserId, userId)
							.set(OrderItemsProfit::getOrderProfitStatus, PROFIT_STATUS_FROZEN);
					orderItemsProfitMapper.update(null, items);
				});
	}

	private void settleOneRow(OrderProfit row) {
		LambdaUpdateWrapper<OrderProfit> head = new LambdaUpdateWrapper<>();
		head.eq(OrderProfit::getId, row.getId())
				.eq(OrderProfit::getOrderProfitStatus, PROFIT_STATUS_FROZEN)
				.set(OrderProfit::getOrderProfitStatus, PROFIT_STATUS_SETTLED);
		orderProfitMapper.update(null, head);

		LambdaUpdateWrapper<OrderItemsProfit> items = new LambdaUpdateWrapper<>();
		items.eq(OrderItemsProfit::getOrderId, row.getOrderId())
				.eq(OrderItemsProfit::getOrderProfitStatus, PROFIT_STATUS_FROZEN)
				.set(OrderItemsProfit::getOrderProfitStatus, PROFIT_STATUS_SETTLED);
		orderItemsProfitMapper.update(null, items);
	}
}
