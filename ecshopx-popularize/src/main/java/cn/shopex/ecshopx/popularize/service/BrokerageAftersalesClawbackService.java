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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCompletedItemQtyReadPort;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 售后退佣冲销，对齐 PHP {@code BrokerageService::brokerageByAftersalse}。
 */
@Service
public class BrokerageAftersalesClawbackService {

	private static final Logger log = LoggerFactory.getLogger(BrokerageAftersalesClawbackService.class);

	private static final String REMARKS_AFTERSALES = "申请售后生成";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final BrokerageMapper brokerageMapper;
	private final PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;
	private final BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;
	private final AftersalesCompletedItemQtyReadPort aftersalesCompletedItemQtyReadPort;

	public BrokerageAftersalesClawbackService(
			BrokerageMapper brokerageMapper,
			PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService,
			BrokeragePlanCloseTimeService brokeragePlanCloseTimeService,
			AftersalesCompletedItemQtyReadPort aftersalesCompletedItemQtyReadPort) {
		this.brokerageMapper = brokerageMapper;
		this.popularizeBrokerageStatisticsIncrementService = popularizeBrokerageStatisticsIncrementService;
		this.brokeragePlanCloseTimeService = brokeragePlanCloseTimeService;
		this.aftersalesCompletedItemQtyReadPort = aftersalesCompletedItemQtyReadPort;
	}

	@Transactional
	public void brokerageByAftersales(long companyId, long orderId, long itemId, int num) {
		if (companyId <= 0L || orderId <= 0L || itemId <= 0L || num <= 0) {
			return;
		}
		String orderIdStr = String.valueOf(orderId);
		List<Brokerage> openRows =
				brokerageMapper.selectList(
						new LambdaQueryWrapper<Brokerage>()
								.eq(Brokerage::getCompanyId, companyId)
								.eq(Brokerage::getOrderId, orderIdStr)
								.eq(Brokerage::getIsClose, false)
								.gt(Brokerage::getRebate, 0));
		if (openRows == null || openRows.isEmpty()) {
			return;
		}

		int completedNum = aftersalesCompletedItemQtyReadPort.sumCompletedNum(companyId, orderId);
		int nowSec = (int) Instant.now().getEpochSecond();
		int planCloseTime = brokeragePlanCloseTimeService.computePlanCloseTimeEpochSec(companyId);

		for (Brokerage row : openRows) {
			Map<String, Object> itemRebate = readItemRebateDetail(row.getDetail(), itemId);
			if (itemRebate == null) {
				log.warn(
						"skip aftersales clawback missing rebate_detail companyId={} orderId={} itemId={} brokerageId={}",
						companyId,
						orderId,
						itemId,
						row.getId());
				continue;
			}
			int itemNum = intVal(itemRebate.get("item_num"));
			int itemRebateMinor = intVal(itemRebate.get("rebate"));
			long itemTotalFee = longVal(itemRebate.get("total_fee"));
			int leftNum = itemNum - completedNum - num;
			if (leftNum < 0) {
				throw new ResourceException("申请售后单数据异常");
			}

			int clawRebate;
			int clawPrice = (int) (-Math.round((double) itemTotalFee / (double) itemNum) * num);
			if (leftNum == 0) {
				int alreadyClawedAbs = absSumNegativeRebate(companyId, orderIdStr, row.getBrokerageType());
				clawRebate = -(itemRebateMinor - alreadyClawedAbs);
			} else {
				clawRebate = (int) (-Math.round((double) itemRebateMinor / (double) itemNum) * num);
			}

			Brokerage neg = new Brokerage();
			neg.setId(null);
			neg.setBrokerageType(row.getBrokerageType());
			neg.setOrderId(row.getOrderId());
			neg.setUserId(row.getUserId());
			neg.setBuyUserId(row.getBuyUserId());
			neg.setOrderType(row.getOrderType());
			neg.setSource(row.getSource());
			neg.setCompanyId(row.getCompanyId());
			neg.setPrice(clawPrice);
			neg.setCommissionType(row.getCommissionType());
			neg.setRebate(clawRebate);
			neg.setRebatePoint(String.valueOf(clawRebate));
			neg.setDetail(detailWithAftersalesRemarks(row.getDetail()));
			neg.setIsClose(false);
			neg.setPlanCloseTime(planCloseTime);
			neg.setCreated(nowSec);
			neg.setUpdated(nowSec);
			brokerageMapper.insert(neg);
			applyStats(neg);
		}
	}

	private int absSumNegativeRebate(long companyId, String orderId, String brokerageType) {
		List<Brokerage> negRows =
				brokerageMapper.selectList(
						new LambdaQueryWrapper<Brokerage>()
								.eq(Brokerage::getCompanyId, companyId)
								.eq(Brokerage::getOrderId, orderId)
								.eq(Brokerage::getBrokerageType, brokerageType)
								.lt(Brokerage::getRebate, 0)
								.select(Brokerage::getRebate));
		int sum = 0;
		if (negRows != null) {
			for (Brokerage r : negRows) {
				if (r != null && r.getRebate() != null) {
					sum += Math.abs(r.getRebate());
				}
			}
		}
		return sum;
	}

	private void applyStats(Brokerage neg) {
		if (neg.getUserId() == null || neg.getCompanyId() == null) {
			return;
		}
		long itemPrice =
				"order".equalsIgnoreCase(nullToEmpty(neg.getSource()))
						? (neg.getPrice() == null ? 0L : neg.getPrice().longValue())
						: 0L;
		String commissionType = nullToEmpty(neg.getCommissionType());
		if ("money".equalsIgnoreCase(commissionType)) {
			long rebate = neg.getRebate() == null ? 0L : neg.getRebate().longValue();
			popularizeBrokerageStatisticsIncrementService.add(
					Map.of(
							"item_total_price",
							itemPrice,
							"rebate_total",
							rebate,
							"no_close_rebate",
							rebate),
					neg.getUserId(),
					neg.getCompanyId());
			return;
		}
		long point = 0L;
		try {
			point = Long.parseLong(nullToEmpty(neg.getRebatePoint()).trim());
		} catch (NumberFormatException ignored) {
			point = 0L;
		}
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of(
						"item_total_price",
						itemPrice,
						"point_total",
						point,
						"no_close_point",
						point),
				neg.getUserId(),
				neg.getCompanyId());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readItemRebateDetail(String detail, long itemId) {
		if (detail == null || detail.isBlank()) {
			return null;
		}
		try {
			Map<String, Object> root =
					OBJECT_MAPPER.readValue(detail, new TypeReference<Map<String, Object>>() {});
			Object rd = root.get("rebate_detail");
			if (!(rd instanceof Map<?, ?> raw)) {
				return null;
			}
			Object item = ((Map<String, Object>) raw).get(String.valueOf(itemId));
			if (!(item instanceof Map<?, ?>)) {
				return null;
			}
			return (Map<String, Object>) item;
		} catch (Exception e) {
			return null;
		}
	}

	private static String detailWithAftersalesRemarks(String detail) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (detail != null && !detail.isBlank()) {
			try {
				Map<String, Object> parsed =
						OBJECT_MAPPER.readValue(detail, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					map.putAll(parsed);
				}
			} catch (Exception ignored) {
				map.put("origin_detail", detail);
			}
		}
		map.put("remarks", REMARKS_AFTERSALES);
		try {
			return OBJECT_MAPPER.writeValueAsString(map);
		} catch (Exception e) {
			return "{\"remarks\":\"" + REMARKS_AFTERSALES + "\"}";
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
