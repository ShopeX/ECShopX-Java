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

package cn.shopex.ecshopx.popularize.service.brokerage;

import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coordinates normal-order finish brokerage persistence and downstream grade job enqueue within a single
 * transaction boundary.
 */
@Service
public class NormalOrderFinishBrokerageCoordinator
		implements cn.shopex.ecshopx.common.service.brokerage.NormalOrderFinishBrokerageCoordinator {

	private static final String BROKERAGE_FIRST_LEVEL = "first_level";
	private static final String ORDER_SOURCE = "order";

	private final PlatformTransactionManager platformTransactionManager;
	private final BrokerageMapper brokerageMapper;
	private final PromoterMapper promoterMapper;
	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher;

	public NormalOrderFinishBrokerageCoordinator(
			PlatformTransactionManager platformTransactionManager,
			BrokerageMapper brokerageMapper,
			PromoterMapper promoterMapper,
			PopularizeSettingSaveService popularizeSettingSaveService,
			UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher) {
		this.platformTransactionManager = platformTransactionManager;
		this.brokerageMapper = brokerageMapper;
		this.promoterMapper = promoterMapper;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.upgradePromoterGradeJobDispatchPublisher = upgradePromoterGradeJobDispatchPublisher;
	}

	@Override
	public void onNormalOrderFinishBrokerage(long companyId, long orderId, NormalOrderBrokerageFinishInput order) {
		if (companyId <= 0L || orderId <= 0L || order == null) {
			return;
		}
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionTemplate tx = new TransactionTemplate(platformTransactionManager, def);
		tx.executeWithoutResult(
				status -> {
					if (!"true".equalsIgnoreCase(
							popularizeSettingSaveService.getOpenPopularizeLiteral(companyId).trim())) {
						return;
					}
					String orderIdStr = String.valueOf(orderId);
					long existing =
							brokerageMapper.selectCount(
									new LambdaQueryWrapper<Brokerage>()
											.eq(Brokerage::getCompanyId, companyId)
											.eq(Brokerage::getOrderId, orderIdStr));
					if (existing > 0L) {
						return;
					}
					Long buyerUserId = order.userId();
					if (buyerUserId == null || buyerUserId <= 0L) {
						return;
					}
					Promoter buyerPromoter =
							promoterMapper.selectOne(
									new LambdaQueryWrapper<Promoter>()
											.eq(Promoter::getCompanyId, companyId)
											.eq(Promoter::getUserId, buyerUserId)
											.last("LIMIT 1"));
					if (buyerPromoter == null || buyerPromoter.getPid() == null || buyerPromoter.getPid() <= 0L) {
						return;
					}
					long parentUserId = buyerPromoter.getPid();
					Promoter parentPromoter =
							promoterMapper.selectOne(
									new LambdaQueryWrapper<Promoter>()
											.eq(Promoter::getCompanyId, companyId)
											.eq(Promoter::getUserId, parentUserId)
											.eq(Promoter::getIsPromoter, 1)
											.last("LIMIT 1"));
					if (parentPromoter == null) {
						return;
					}
					Map<String, Object> cfg = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
					long orderBaseMinor = resolveOrderBaseMinorForRatio(cfg, order);
					double ratioPercent = resolveFirstLevelRatioPercent(cfg);
					int rebateMinor = (int) Math.min(Integer.MAX_VALUE, Math.round(orderBaseMinor * ratioPercent / 100.0d));
					if (rebateMinor <= 0) {
						return;
					}
					int nowSec = (int) Instant.now().getEpochSecond();
					Brokerage row = new Brokerage();
					row.setBrokerageType(BROKERAGE_FIRST_LEVEL);
					row.setOrderId(orderIdStr);
					row.setUserId(parentUserId);
					row.setBuyUserId(buyerUserId);
					row.setOrderType(safeOrderType(order));
					row.setSource(ORDER_SOURCE);
					row.setCompanyId(companyId);
					row.setPrice(orderBaseMinor > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) orderBaseMinor);
					row.setCommissionType("money");
					row.setRebate(rebateMinor);
					row.setRebatePoint("money");
					row.setDetail("{}");
					row.setIsClose(false);
					row.setCreated(nowSec);
					row.setUpdated(nowSec);
					brokerageMapper.insert(row);
					upgradePromoterGradeJobDispatchPublisher.enqueueUpgradePromoterGrade(companyId, parentUserId);
				});
	}

	private static String safeOrderType(NormalOrderBrokerageFinishInput order) {
		String oc = order.orderClass();
		return oc == null || oc.isBlank() ? "normal" : oc;
	}

	private static long resolveOrderBaseMinorForRatio(Map<String, Object> cfg, NormalOrderBrokerageFinishInput order) {
		Object prObj = cfg.get("popularize_ratio");
		if (!(prObj instanceof Map<?, ?> prRaw)) {
			return parseMoneyStringToMinor(order.totalFee());
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> popularizeRatio = (Map<String, Object>) prRaw;
		String ratioType =
				popularizeRatio.get("type") != null
						? String.valueOf(popularizeRatio.get("type")).trim()
						: "order_money";
		if ("profit".equalsIgnoreCase(ratioType)) {
			Integer commissionFee = order.commissionFee();
			return commissionFee == null ? 0L : commissionFee.longValue();
		}
		return parseMoneyStringToMinor(order.totalFee());
	}

	private static double resolveFirstLevelRatioPercent(Map<String, Object> cfg) {
		Object prObj = cfg.get("popularize_ratio");
		if (!(prObj instanceof Map<?, ?> prMap)) {
			return 0d;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> popularizeRatio = (Map<String, Object>) prMap;
		String ratioType =
				popularizeRatio.get("type") != null
						? String.valueOf(popularizeRatio.get("type")).trim()
						: "order_money";
		Object branchObj = popularizeRatio.get(ratioType);
		if (!(branchObj instanceof Map<?, ?>)) {
			return 0d;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> branch = (Map<String, Object>) branchObj;
		Object flObj = branch.get("first_level");
		if (!(flObj instanceof Map<?, ?>)) {
			return 0d;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> fl = (Map<String, Object>) flObj;
		return toDouble(fl.get("ratio"));
	}

	private static double toDouble(Object v) {
		if (v == null) {
			return 0d;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0d;
		}
	}

	private static long parseMoneyStringToMinor(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return (long) Double.parseDouble(s);
			} catch (NumberFormatException e2) {
				return 0L;
			}
		}
	}
}
