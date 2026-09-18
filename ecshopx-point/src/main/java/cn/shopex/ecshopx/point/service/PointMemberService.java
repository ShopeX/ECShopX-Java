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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.port.point.MemberPointScheduleItemRow;
import cn.shopex.ecshopx.common.port.point.MemberPointScheduleOrderRow;
import cn.shopex.ecshopx.common.port.point.MemberPointSendScheduleDataPort;
import cn.shopex.ecshopx.common.port.point.PointMemberRuleReadPort;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PointMemberService {

	private static final Logger log = LoggerFactory.getLogger(PointMemberService.class);

	private static final int PAGE_SIZE = 100;
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final MemberPointSendScheduleDataPort scheduleDataPort;
	private final PointMemberRuleReadPort pointMemberRuleReadPort;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final PointMemberLogMapper pointMemberLogMapper;

	public PointMemberService(
			MemberPointSendScheduleDataPort scheduleDataPort,
			PointMemberRuleReadPort pointMemberRuleReadPort,
			PointMemberAddPointService pointMemberAddPointService,
			PointMemberLogMapper pointMemberLogMapper) {
		this.scheduleDataPort = scheduleDataPort;
		this.pointMemberRuleReadPort = pointMemberRuleReadPort;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.pointMemberLogMapper = pointMemberLogMapper;
	}

	/**
	 * 同步跑完与 PHP 队列消费 SendMemberPoint 等价的批处理，并回写成功订单 send_point=1。
	 */
	public ScheduleSendMemberPointResult scheduleSendMemberPoint() {
		long timeSec = Instant.now().getEpochSecond();
		long totalCount = scheduleDataPort.countSendPointPendingOrders();
		if (totalCount == 0) {
			return new ScheduleSendMemberPointResult(0);
		}
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		List<Long> successOrderIds = new ArrayList<>();
		MonthEpochRange month = currentMonthEpochRange();
		for (int page = 0; page < totalPage; page++) {
			List<MemberPointScheduleOrderRow> rows =
					scheduleDataPort.listSendPointPendingOrders(page * PAGE_SIZE, PAGE_SIZE);
			Map<Long, Map<String, Object>> rules = new HashMap<>();
			for (long cid : rows.stream().map(MemberPointScheduleOrderRow::companyId).distinct().toList()) {
				rules.put(cid, pointMemberRuleReadPort.getPointRule(cid));
			}
			List<Long> orderIds = rows.stream().map(MemberPointScheduleOrderRow::orderId).toList();
			Set<Long> blocking = scheduleDataPort.orderIdsWithOpenAftersales(orderIds);
			for (MemberPointScheduleOrderRow row : rows) {
				Map<String, Object> rule = rules.get(row.companyId());
				if (rule == null) {
					continue;
				}
				if (!passesOpenAndEndTimeRule(rule, row, timeSec)) {
					continue;
				}
				String mark;
				int point;
				if (blocking.contains(row.orderId())) {
					point = 0;
					mark = "存在售后状态的订单，无法获取积分";
					tryAddBonus(row, point, mark, successOrderIds);
					continue;
				}
				int pointTotal = incomeSumForOrderBonusMonth(row.userId(), row.companyId(), month);
				mark = "订单获取积分";
				if (row.getPointType() == 1) {
					int ret = scheduleDataPort.sumReturnPointForSuccessfulRefunds(row.orderId());
					point = Math.max(0, row.getPoints() + row.extraPoints() - ret);
				} else if (isAccessItems(rule)) {
					point = computePointsByItems(row, rule);
				} else {
					point = computePointsByOrder(row, rule);
				}
				int gainLimit = intFromObject(rule.get("gain_limit"), 9_999_999);
				if (pointTotal + point >= gainLimit) {
					int minpoint = gainLimit - pointTotal;
					mark = "应增加" + point + "积分，本月订单获取积分达到限度";
					point = Math.max(0, minpoint);
				}
				tryAddBonus(row, point, mark, successOrderIds);
			}
		}
		for (long orderId : successOrderIds) {
			scheduleDataPort.markOrderSendPointDone(orderId);
		}
		return new ScheduleSendMemberPointResult(successOrderIds.size());
	}

	private void tryAddBonus(
			MemberPointScheduleOrderRow row, int point, String mark, List<Long> successOrderIds) {
		try {
			pointMemberAddPointService.addPointForNormalOrderBonus(
					row.userId(), row.companyId(), point, row.orderId(), mark);
			successOrderIds.add(row.orderId());
		} catch (Exception e) {
			log.debug("积分增加失败:{} -> {}", row.orderId(), e.getMessage());
		}
	}

	private int incomeSumForOrderBonusMonth(
			long userId, long companyId, MonthEpochRange month) {
		Integer v =
				pointMemberLogMapper.selectSumIncomeOrderBonusInRange(
						userId, companyId, month.start(), month.end());
		return v == null ? 0 : v;
	}

	private boolean passesOpenAndEndTimeRule(Map<String, Object> rule, MemberPointScheduleOrderRow row, long timeSec) {
		if (!ruleFlagTrue(rule.get("isOpenMemberPoint"))) {
			return false;
		}
		int gainDays = intFromObject(rule.get("gain_time"), 7);
		long threshold = timeSec - 86400L * gainDays;
		return row.endTime() > 0 && row.endTime() <= threshold;
	}

	private int computePointsByItems(MemberPointScheduleOrderRow row, @SuppressWarnings("unused") Map<String, Object> rule) {
		List<MemberPointScheduleItemRow> lines = scheduleDataPort.listLineItemsForOrder(row.orderId());
		if (lines.isEmpty()) {
			return 0;
		}
		List<Long> itemIds = lines.stream().map(MemberPointScheduleItemRow::itemId).filter(id -> id > 0L).toList();
		Map<Long, Long> access = scheduleDataPort.mapItemPointAccess(row.companyId(), itemIds);
		int sum = 0;
		for (MemberPointScheduleItemRow li : lines) {
			if (li.itemId() <= 0 || li.num() <= 0) {
				continue;
			}
			Long p = access.get(li.itemId());
			if (p == null) {
				continue;
			}
			long add = p * (long) li.num();
			if (add > 0) {
				sum = (int) Math.min((long) Integer.MAX_VALUE, (long) sum + add);
			}
		}
		return sum;
	}

	private int computePointsByOrder(MemberPointScheduleOrderRow row, Map<String, Object> rule) {
		BigDecimal gain = gainPointBd(rule);
		if (ruleFlagTrue(rule.get("include_freight"))) {
			BigDecimal totalYuan = BigDecimal.valueOf(row.totalFeeCents()).divide(HUNDRED, 8, RoundingMode.HALF_UP);
			return toIntPoints(gain.multiply(totalYuan));
		}
		List<MemberPointScheduleItemRow> lines = scheduleDataPort.listLineItemsForOrder(row.orderId());
		int sumItemPointFee = 0;
		for (MemberPointScheduleItemRow li : lines) {
			sumItemPointFee += li.pointFee();
		}
		int pointFreightFee = 0;
		int orderPf = row.pointFee();
		int orderFf = row.freightFee();
		if (orderPf > 0 && orderFf > 0) {
			pointFreightFee = orderPf - sumItemPointFee;
		}
		long totalC = row.totalFeeCents();
		long effectiveCents = totalC - ((long) orderFf - pointFreightFee);
		if (effectiveCents < 0) {
			effectiveCents = 0;
		}
		BigDecimal baseYuan = BigDecimal.valueOf(effectiveCents).divide(HUNDRED, 8, RoundingMode.HALF_UP);
		return toIntPoints(gain.multiply(baseYuan));
	}

	private static int toIntPoints(BigDecimal v) {
		if (v.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		return v.setScale(0, RoundingMode.DOWN).intValue();
	}

	private static boolean isAccessItems(Map<String, Object> rule) {
		Object a = rule.get("access");
		return a != null && "items".equalsIgnoreCase(String.valueOf(a).trim());
	}

	private static boolean ruleFlagTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(String.valueOf(v).trim()) || "1".equals(String.valueOf(v).trim());
	}

	private static int intFromObject(Object o, int defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static BigDecimal gainPointBd(Map<String, Object> rule) {
		Object g = rule.get("gain_point");
		if (g == null) {
			return BigDecimal.ONE;
		}
		if (g instanceof BigDecimal b) {
			return b;
		}
		if (g instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(g).trim());
		} catch (Exception e) {
			return BigDecimal.ONE;
		}
	}

	private static MonthEpochRange currentMonthEpochRange() {
		ZoneId z = ZoneId.systemDefault();
		YearMonth ym = YearMonth.now(z);
		int start = (int) ym.atDay(1).atStartOfDay(z).toEpochSecond();
		int end = (int) ym.atEndOfMonth().atTime(23, 59, 59).atZone(z).toEpochSecond();
		return new MonthEpochRange(start, end);
	}

	private record MonthEpochRange(int start, int end) {}
}
