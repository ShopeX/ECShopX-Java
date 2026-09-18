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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.service.AftersalesShopPartialCancelCreateService;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNormalOrderPartialCancelService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final AftersalesShopPartialCancelCreateService aftersalesShopPartialCancelCreateService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher;

	public AdminNormalOrderPartialCancelService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			SupplierOrderMapper supplierOrderMapper,
			AftersalesShopPartialCancelCreateService aftersalesShopPartialCancelCreateService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.aftersalesShopPartialCancelCreateService = aftersalesShopPartialCancelCreateService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.sendAfterSaleWaitDealNoticeJobDispatchPublisher = sendAfterSaleWaitDealNoticeJobDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> execute(
			long companyId,
			String operatorType,
			long operatorId,
			long supplierId,
			long userId,
			long orderId,
			String reasonText) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getUserId, userId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		if (!"PAYED".equalsIgnoreCase(safe(order.getOrderStatus()))) {
			throw new ResourceException("订单状态不能申请取消");
		}
		if (!"PARTAIL".equalsIgnoreCase(safe(order.getDeliveryStatus()))) {
			throw new ResourceException("非部分发货订单不能取消");
		}
		if (order.getType() != null
				&& order.getType() == 1
				&& "approved".equalsIgnoreCase(safe(order.getAuditStatus()))) {
			throw new ResourceException("订单已审核成功，不能取消");
		}

		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		List<Map<String, Object>> detail = new ArrayList<>();
		for (NormalOrdersItems it : items) {
			if (supplierId > 0) {
				long sid = it.getSupplierId() == null ? 0L : it.getSupplierId();
				if (sid != supplierId) {
					continue;
				}
			}
			int n = it.getNum() == null ? 0 : it.getNum();
			int delivered = it.getDeliveryItemNum() == null ? 0 : it.getDeliveryItemNum();
			int cancelNum = n - delivered;
			if (cancelNum > 0) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", it.getId());
				row.put("num", cancelNum);
				detail.add(row);
			}
		}
		if (detail.isEmpty()) {
			throw new ResourceException("没有可退款商品");
		}

		Map<String, Object> result =
				aftersalesShopPartialCancelCreateService.createForShopPartialCancel(
						companyId,
						orderId,
						userId,
						supplierId,
						operatorType,
						operatorId,
						reasonText,
						detail);

		long aftersalesBn = longVal(result.get("aftersales_bn"));
		sendAfterSaleWaitDealNoticeJobDispatchPublisher.publish(companyId, aftersalesBn);
		for (Map<String, Object> d : detail) {
			long itemId = longVal(d.get("id"));
			int addNum = intVal(d.get("num"));
			NormalOrdersItems row =
					normalOrdersItemsMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getId, itemId)
									.last("LIMIT 1"));
			if (row != null) {
				int existing = row.getCancelItemNum() == null ? 0 : row.getCancelItemNum();
				LambdaUpdateWrapper<NormalOrdersItems> iu = new LambdaUpdateWrapper<>();
				iu.eq(NormalOrdersItems::getId, itemId).set(NormalOrdersItems::getCancelItemNum, existing + addNum);
				normalOrdersItemsMapper.update(null, iu);
			}
		}

		List<NormalOrdersItems> itemsAfter =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		String orderStatusWait = "WAIT_BUYER_CONFIRM";
		for (NormalOrdersItems it : itemsAfter) {
			int n = it.getNum() == null ? 0 : it.getNum();
			int delivered = it.getDeliveryItemNum() == null ? 0 : it.getDeliveryItemNum();
			int cancelled = it.getCancelItemNum() == null ? 0 : it.getCancelItemNum();
			if (delivered + cancelled < n) {
				orderStatusWait = "";
				break;
			}
		}
		if ("WAIT_BUYER_CONFIRM".equals(orderStatusWait)) {
			Map<String, Object> setting = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
			int finishDays = 7;
			Object ot = setting.get("order_finish_time");
			if (ot instanceof Number n) {
				finishDays = n.intValue();
			} else if (ot != null) {
				try {
					finishDays = Integer.parseInt(String.valueOf(ot).trim());
				} catch (NumberFormatException ignored) {
					finishDays = 7;
				}
			}
			int finishSec = finishDays * 24 * 3600;
			int now = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
			ou.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, orderId);
			if (supplierId > 0) {
				ou.eq(NormalOrders::getSupplierId, supplierId);
			}
			ou.set(NormalOrders::getAutoFinishTime, String.valueOf(now + finishSec))
					.set(NormalOrders::getOrderStatus, "WAIT_BUYER_CONFIRM");
			normalOrdersMapper.update(null, ou);
		}

		if (supplierId > 0) {
			String supOrderStatus = "CANCEL";
			for (NormalOrdersItems it : items) {
				if ((it.getSupplierId() == null ? 0L : it.getSupplierId()) != supplierId) {
					continue;
				}
				int delivered = it.getDeliveryItemNum() == null ? 0 : it.getDeliveryItemNum();
				if (delivered > 0) {
					supOrderStatus = "WAIT_BUYER_CONFIRM";
					break;
				}
			}
			LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
			su.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderId)
					.eq(SupplierOrder::getSupplierId, supplierId)
					.set(SupplierOrder::getOrderStatus, supOrderStatus);
			supplierOrderMapper.update(null, su);
		}

		boolean auto =
				Boolean.TRUE.equals(orderValiditySettingRedisReadService.readPlatformSetting(companyId).get("auto_aftersales"));
		if (auto && aftersalesBn > 0) {
			int rf = intVal(result.get("refund_fee"));
			int rp = intVal(result.get("refund_point"));
			aftersalesShopPartialCancelCreateService.autoApproveOnlyRefund(
					companyId,
					aftersalesBn,
					rf,
					rp,
					0,
					operatorType,
					operatorId,
					userId);
		}

		return result;
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
