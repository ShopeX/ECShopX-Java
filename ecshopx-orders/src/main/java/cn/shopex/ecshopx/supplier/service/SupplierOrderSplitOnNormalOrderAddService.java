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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelSupplier;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelSupplierMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierOrderSplitOnNormalOrderAddService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper;
	private final SupplierOrderMapper supplierOrderMapper;

	public SupplierOrderSplitOnNormalOrderAddService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersRelSupplierMapper normalOrdersRelSupplierMapper,
			SupplierOrderMapper supplierOrderMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersRelSupplierMapper = normalOrdersRelSupplierMapper;
		this.supplierOrderMapper = supplierOrderMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void split(long companyId, long orderId) {
		NormalOrders parent =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (parent == null) {
			throw new ResourceException("订单不存在");
		}

		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));

		List<NormalOrdersRelSupplier> rels =
				normalOrdersRelSupplierMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersRelSupplier>()
								.eq(NormalOrdersRelSupplier::getCompanyId, companyId)
								.eq(NormalOrdersRelSupplier::getOrderId, orderId));

		Set<Integer> supplierIds = new LinkedHashSet<>();
		for (NormalOrdersItems line : items) {
			int sid = line.getSupplierId() == null ? 0 : line.getSupplierId();
			if (sid > 0) {
				supplierIds.add(sid);
			}
		}
		for (NormalOrdersRelSupplier rel : rels) {
			int sid = rel.getSupplierId() == null ? 0 : rel.getSupplierId();
			if (sid > 0) {
				supplierIds.add(sid);
			}
		}
		if (supplierIds.isEmpty()) {
			return;
		}

		Map<Integer, Integer> freightBySupplier = new LinkedHashMap<>();
		for (NormalOrdersRelSupplier rel : rels) {
			int sid = rel.getSupplierId() == null ? 0 : rel.getSupplierId();
			if (sid > 0) {
				int ff = rel.getFreightFee() == null ? 0 : rel.getFreightFee();
				freightBySupplier.merge(sid, ff, Integer::sum);
			}
		}

		long userId = parent.getUserId() == null ? 0L : parent.getUserId();
		int now = (int) (System.currentTimeMillis() / 1000L);

		for (Integer supplierId : supplierIds) {
			long existing =
					supplierOrderMapper.selectCount(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getUserId, userId)
									.eq(SupplierOrder::getSupplierId, supplierId));
			if (existing > 0L) {
				continue;
			}

			List<NormalOrdersItems> linesForSupplier = new ArrayList<>();
			for (NormalOrdersItems line : items) {
				int sid = line.getSupplierId() == null ? 0 : line.getSupplierId();
				if (sid == supplierId) {
					linesForSupplier.add(line);
				}
			}

			int itemFeeSum = 0;
			int totalFeeSum = 0;
			int commissionSum = 0;
			int costSum = 0;
			long marketSum = 0L;
			String lineTitle = "";
			long maxAct = 0L;
			for (NormalOrdersItems line : linesForSupplier) {
				itemFeeSum += intOrZero(line.getItemFee());
				totalFeeSum += intOrZero(line.getTotalFee());
				commissionSum += intOrZero(line.getCommissionFee());
				costSum += intOrZero(line.getCostPrice()) * Math.max(1, intOrZero(line.getNum()));
				int mp = intOrZero(line.getMarketPrice());
				int num = Math.max(1, intOrZero(line.getNum()));
				marketSum += (long) mp * (long) num;
				if (lineTitle.isBlank() && line.getItemName() != null && !line.getItemName().isBlank()) {
					lineTitle = line.getItemName().trim();
				}
				long act = line.getActId() == null ? 0L : line.getActId();
				if (act > maxAct) {
					maxAct = act;
				}
			}

			int relFreight = freightBySupplier.getOrDefault(supplierId, 0);
			String title = lineTitle.isBlank() ? safe(parent.getTitle()) : lineTitle;

			SupplierOrder row = new SupplierOrder();
			row.setCompanyId(companyId);
			row.setOrderId(orderId);
			row.setTitle(title);
			row.setShopId(parent.getShopId() == null ? 0L : parent.getShopId());
			row.setCostFee(costSum);
			row.setCommissionFee(commissionSum);
			row.setUserId(userId);
			row.setActId(maxAct);
			row.setMobile(safe(parent.getMobile()));
			row.setOrderClass(safe(parent.getOrderClass()));
			row.setFreightFee(relFreight);
			row.setItemFee(String.valueOf(itemFeeSum));
			row.setTotalFee(String.valueOf(Math.max(0, totalFeeSum + relFreight)));
			row.setMarketFee(String.valueOf(Math.max(0L, marketSum)));
			row.setDistributorId(parent.getDistributorId() == null ? 0L : parent.getDistributorId());
			row.setReceiptType(safe(parent.getReceiptType()));
			row.setReceiverName(parent.getReceiverName());
			row.setReceiverMobile(parent.getReceiverMobile());
			row.setReceiverZip(parent.getReceiverZip());
			row.setReceiverState(parent.getReceiverState());
			row.setReceiverCity(parent.getReceiverCity());
			row.setReceiverDistrict(parent.getReceiverDistrict());
			row.setReceiverAddress(parent.getReceiverAddress());
			row.setZitiStatus("NOTZITI");
			row.setOrderStatus("NOTPAY");
			row.setPayStatus("NOTPAY");
			row.setOrderSource(safe(parent.getOrderSource()));
			row.setOrderType(safe(parent.getOrderType()));
			row.setIsDistribution(Boolean.TRUE.equals(parent.getIsDistribution()));
			row.setMemberDiscount(parent.getMemberDiscount() == null ? 0 : parent.getMemberDiscount());
			row.setCouponDiscount(parent.getCouponDiscount() == null ? 0 : parent.getCouponDiscount());
			row.setDiscountFee(parent.getDiscountFee() == null ? 0 : parent.getDiscountFee());
			row.setDiscountInfo(parent.getDiscountInfo() == null ? "[]" : parent.getDiscountInfo());
			row.setFeeType(safe(parent.getFeeType()));
			row.setFeeRate(parent.getFeeRate() == null ? 1.0f : parent.getFeeRate());
			row.setFeeSymbol(safe(parent.getFeeSymbol()));
			row.setPayType(safe(parent.getPayType()));
			row.setRemark(safe(parent.getRemark()));
			row.setPoint(parent.getPoint() == null ? 0 : parent.getPoint());
			row.setPointUse(parent.getPointUse() == null ? 0 : parent.getPointUse());
			row.setPointFee(parent.getPointFee() == null ? 0 : parent.getPointFee());
			row.setOperatorId(parent.getOperatorId() == null ? 0 : parent.getOperatorId());
			row.setSourceFrom(safe(parent.getSourceFrom()));
			row.setSupplierId(supplierId);
			row.setCreateTime(now);
			row.setUpdateTime(now);

			supplierOrderMapper.insert(row);
		}
	}

	private static int intOrZero(Integer v) {
		return v == null ? 0 : v;
	}

	private static String safe(String v) {
		return v == null ? "" : v;
	}
}
