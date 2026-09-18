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

package cn.shopex.ecshopx.supplier.service.front;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.supplier.constants.SupplierOrderStatusConstants;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.event.SupplierOfflinePaidNotifyEvent;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SupplierBuyerOrderPayStatusServiceImpl implements SupplierBuyerOrderPayStatusService {

	private static final String REDIS_KEY_PREFIX = "supplier:setOrderPayStatus:";

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final StringRedisTemplate stringRedisTemplate;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final MessageSource messageSource;
	private final TransactionTemplate txTemplate;

	public SupplierBuyerOrderPayStatusServiceImpl(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			StringRedisTemplate stringRedisTemplate,
			ApplicationEventPublisher applicationEventPublisher,
			MessageSource messageSource,
			PlatformTransactionManager platformTransactionManager) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.stringRedisTemplate = stringRedisTemplate;
		this.applicationEventPublisher = applicationEventPublisher;
		this.messageSource = messageSource;
		this.txTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@Override
	public Map<String, Object> setOrderPayStatus(long companyId, long userId, String orderIdRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		if (orderIdRaw == null || orderIdRaw.trim().isEmpty()) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.order_id_required", null, locale));
		}
		String trimmedOrderId = orderIdRaw.trim();
		String rateKey = REDIS_KEY_PREFIX + trimmedOrderId;
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateKey))) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.please_wait_3_seconds", null, locale));
		}
		stringRedisTemplate.opsForValue().set(rateKey, "1", Duration.ofSeconds(3));

		long orderId;
		try {
			orderId = Long.parseLong(trimmedOrderId);
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.order_not_exist_or_no_permission", null, locale));
		}

		NormalOrders main = normalOrdersMapper.selectOne(new LambdaQueryWrapper<NormalOrders>()
				.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.eq(NormalOrders::getUserId, userId)
				.last("LIMIT 1"));
		if (main == null) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.order_not_exist_or_no_permission", null, locale));
		}

		String pt = main.getPayType() == null ? "" : main.getPayType().trim();
		boolean isOffline = "offline".equals(pt) || "offline_pay".equals(pt);
		if (!isOffline) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.order_not_offline_payment", null, locale));
		}

		if (!"NOTPAY".equals(main.getOrderStatus())) {
			throw new ResourceException(messageSource.getMessage(
					"supplier.wxapp.set_order_pay_status.order_not_pending_payment", null, locale));
		}

		Map<String, Object> orderInfo = normalOrdersServiceOrderDataAssembler.toServiceOrderData(main);
		Map<String, Object> responseSnapshot = new LinkedHashMap<>(orderInfo);

		try {
			txTemplate.executeWithoutResult(status -> {
				LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.eq(NormalOrders::getUserId, userId)
						.eq(NormalOrders::getOrderStatus, SupplierOrderStatusConstants.NOTPAY)
						.set(NormalOrders::getOrderStatus, SupplierOrderStatusConstants.WAIT_PAID_CONFIRM);
				int uMain = normalOrdersMapper.update(null, uw);
				if (uMain != 1) {
					throw new ResourceException("未查询到更新数据");
				}

				long cnt = supplierOrderMapper.selectCount(new LambdaQueryWrapper<SupplierOrder>()
						.eq(SupplierOrder::getCompanyId, companyId)
						.eq(SupplierOrder::getOrderId, orderId)
						.eq(SupplierOrder::getUserId, userId)
						.eq(SupplierOrder::getOrderStatus, SupplierOrderStatusConstants.NOTPAY));
				if (cnt > 0) {
					int uSub = supplierOrderMapper.update(
							null,
							new LambdaUpdateWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getUserId, userId)
									.eq(SupplierOrder::getOrderStatus, SupplierOrderStatusConstants.NOTPAY)
									.set(SupplierOrder::getOrderStatus, SupplierOrderStatusConstants.WAIT_PAID_CONFIRM));
					if (uSub == 0) {
						throw new ResourceException(messageSource.getMessage(
								"supplier.wxapp.set_order_pay_status.no_update_data_found", null, locale));
					}
				}
			});
		} catch (RuntimeException ex) {
			if (ex instanceof ResourceException) {
				throw ex;
			}
			throw new ResourceException(messageSource.getMessage(
							"supplier.wxapp.set_order_pay_status.error_prefix", null, locale)
					+ ex.getMessage());
		}

		applicationEventPublisher.publishEvent(new SupplierOfflinePaidNotifyEvent(this, companyId, orderId, userId));

		return responseSnapshot;
	}
}
