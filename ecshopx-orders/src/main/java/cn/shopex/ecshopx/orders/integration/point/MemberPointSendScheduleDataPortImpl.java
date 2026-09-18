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

package cn.shopex.ecshopx.orders.integration.point;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.port.point.MemberPointScheduleItemRow;
import cn.shopex.ecshopx.common.port.point.MemberPointScheduleOrderRow;
import cn.shopex.ecshopx.common.port.point.MemberPointSendScheduleDataPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.repository.OrderItemsRelPointAccessReadRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MemberPointSendScheduleDataPortImpl implements MemberPointSendScheduleDataPort {

	private static final String ORDER_STATUS_DONE = "DONE";
	private static final String DELIVERY_STATUS_DONE = "DONE";

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OrderItemsRelPointAccessReadRepository orderItemsRelPointAccessReadRepository;

	public MemberPointSendScheduleDataPortImpl(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			OrderItemsRelPointAccessReadRepository orderItemsRelPointAccessReadRepository) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.orderItemsRelPointAccessReadRepository = orderItemsRelPointAccessReadRepository;
	}

	@Override
	public long countSendPointPendingOrders() {
		return normalOrdersMapper.selectCount(basePendingWrapper());
	}

	@Override
	public List<MemberPointScheduleOrderRow> listSendPointPendingOrders(int offset, int pageSize) {
		LambdaQueryWrapper<NormalOrders> w = basePendingWrapper();
		w.orderByAsc(NormalOrders::getEndTime);
		w.last("LIMIT " + pageSize + " OFFSET " + offset);
		return normalOrdersMapper.selectList(w).stream().map(this::toRow).toList();
	}

	private LambdaQueryWrapper<NormalOrders> basePendingWrapper() {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getOrderStatus, ORDER_STATUS_DONE);
		w.eq(NormalOrders::getDeliveryStatus, DELIVERY_STATUS_DONE);
		w.in(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL", "FAILS");
		w.eq(NormalOrders::getSendPoint, 0);
		w.gt(NormalOrders::getUserId, 0L);
		return w;
	}

	private MemberPointScheduleOrderRow toRow(NormalOrders o) {
		return new MemberPointScheduleOrderRow(
				o.getOrderId(),
				o.getCompanyId(),
				o.getUserId(),
				o.getEndTime() == null ? 0L : o.getEndTime(),
				o.getGetPointType() == null ? 0 : o.getGetPointType(),
				o.getGetPoints() == null ? 0 : o.getGetPoints(),
				o.getExtraPoints() == null ? 0 : o.getExtraPoints(),
				parseFeeCents(o.getTotalFee()),
				o.getFreightFee() == null ? 0 : o.getFreightFee(),
				o.getPointFee() == null ? 0 : o.getPointFee());
	}

	private static long parseFeeCents(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@Override
	public Set<Long> orderIdsWithOpenAftersales(List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Set.of();
		}
		LambdaQueryWrapper<Aftersales> w = new LambdaQueryWrapper<>();
		w.in(Aftersales::getOrderId, orderIds);
		w.notIn(Aftersales::getAftersalesStatus, 3, 4);
		return aftersalesMapper.selectList(w).stream()
				.map(Aftersales::getOrderId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Override
	public List<MemberPointScheduleItemRow> listLineItemsForOrder(long orderId) {
		LambdaQueryWrapper<NormalOrdersItems> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrdersItems::getOrderId, orderId);
		return normalOrdersItemsMapper.selectList(w).stream()
				.map(
						li ->
								new MemberPointScheduleItemRow(
										li.getItemId() == null ? 0L : li.getItemId(),
										li.getNum() == null ? 0 : li.getNum(),
										li.getPointFee() == null ? 0 : li.getPointFee()))
				.toList();
	}

	@Override
	public int sumReturnPointForSuccessfulRefunds(long orderId) {
		LambdaQueryWrapper<AftersalesRefund> w = new LambdaQueryWrapper<>();
		w.eq(AftersalesRefund::getOrderId, orderId);
		w.in(AftersalesRefund::getRefundStatus, "SUCCESS", "success");
		List<AftersalesRefund> list = aftersalesRefundMapper.selectList(w);
		if (list == null) {
			return 0;
		}
		return list.stream()
				.mapToInt(
						r -> {
							if (r.getReturnPoint() == null) {
								return 0;
							}
							return r.getReturnPoint() > 0 ? r.getReturnPoint() : 0;
						})
				.sum();
	}

	@Override
	public void markOrderSendPointDone(long orderId) {
		NormalOrders patch = new NormalOrders();
		patch.setSendPoint(1);
		normalOrdersMapper.update(
				patch, new LambdaUpdateWrapper<NormalOrders>().eq(NormalOrders::getOrderId, orderId));
	}

	@Override
	public Map<Long, Long> mapItemPointAccess(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		return orderItemsRelPointAccessReadRepository.mapPointByItemId(companyId, itemIds);
	}
}
