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
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageStatisticsIncrementService;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	/** 测试环境排查 ECX-11089 用；稳定后可视情况降为 debug 或删除 */
	private static final Logger log = LoggerFactory.getLogger(NormalOrderFinishBrokerageCoordinator.class);

	private static final String LOG_TAG = "[popularize-trade-finish]";

	private static final String BROKERAGE_FIRST_LEVEL = "first_level";
	private static final String ORDER_SOURCE = "order";

	private final PlatformTransactionManager platformTransactionManager;
	private final BrokerageMapper brokerageMapper;
	private final PromoterMapper promoterMapper;
	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;
	private final UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher;
	private final BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	public NormalOrderFinishBrokerageCoordinator(
			PlatformTransactionManager platformTransactionManager,
			BrokerageMapper brokerageMapper,
			PromoterMapper promoterMapper,
			PopularizeSettingSaveService popularizeSettingSaveService,
			PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService,
			UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher,
			BrokeragePlanCloseTimeService brokeragePlanCloseTimeService) {
		this.platformTransactionManager = platformTransactionManager;
		this.brokerageMapper = brokerageMapper;
		this.promoterMapper = promoterMapper;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.popularizeBrokerageStatisticsIncrementService = popularizeBrokerageStatisticsIncrementService;
		this.upgradePromoterGradeJobDispatchPublisher = upgradePromoterGradeJobDispatchPublisher;
		this.brokeragePlanCloseTimeService = brokeragePlanCloseTimeService;
	}

	@Override
	public void onNormalOrderFinishBrokerage(long companyId, long orderId, NormalOrderBrokerageFinishInput order) {
		if (companyId <= 0L || orderId <= 0L || order == null) {
			log.info("{} skip invalid_args companyId={} orderId={} orderNull={}", LOG_TAG, companyId, orderId, order == null);
			return;
		}
		log.info(
				"{} enter companyId={} orderId={} buyerUserId={}",
				LOG_TAG,
				companyId,
				orderId,
				order.userId());
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionTemplate tx = new TransactionTemplate(platformTransactionManager, def);
		tx.executeWithoutResult(
				status -> {
					String openLiteral = popularizeSettingSaveService.getOpenPopularizeLiteral(companyId).trim();
					if (!"true".equalsIgnoreCase(openLiteral)) {
						log.info("{} skip open_popularize_off companyId={} orderId={} isOpen={}", LOG_TAG, companyId, orderId, openLiteral);
						return;
					}
					String orderIdStr = String.valueOf(orderId);
					long existing =
							brokerageMapper.selectCount(
									new LambdaQueryWrapper<Brokerage>()
											.eq(Brokerage::getCompanyId, companyId)
											.eq(Brokerage::getOrderId, orderIdStr));
					if (existing > 0L) {
						log.info("{} skip idempotent_existing companyId={} orderId={} count={}", LOG_TAG, companyId, orderId, existing);
						return;
					}
					Long buyerUserId = order.userId();
					if (buyerUserId == null || buyerUserId <= 0L) {
						log.info("{} skip no_buyer_user_id companyId={} orderId={}", LOG_TAG, companyId, orderId);
						return;
					}
					Promoter buyerPromoter =
							promoterMapper.selectOne(
									new LambdaQueryWrapper<Promoter>()
											.eq(Promoter::getCompanyId, companyId)
											.eq(Promoter::getUserId, buyerUserId)
											.last("LIMIT 1"));
					if (buyerPromoter == null || buyerPromoter.getPid() == null || buyerPromoter.getPid() <= 0L) {
						log.info(
								"{} skip buyer_not_promoter_or_no_pid companyId={} orderId={} buyerUserId={}",
								LOG_TAG,
								companyId,
								orderId,
								buyerUserId);
						return;
					}
					long buyerPid = buyerPromoter.getPid();
					Promoter parentPromoter = promoterMapper.selectById(buyerPid);
					if (parentPromoter == null
							|| parentPromoter.getCompanyId() == null
							|| parentPromoter.getCompanyId().longValue() != companyId
							|| parentPromoter.getUserId() == null
							|| parentPromoter.getUserId() <= 0L
							|| parentPromoter.getIsPromoter() == null
							|| parentPromoter.getIsPromoter() != 1) {
						log.info(
								"{} skip parent_invalid companyId={} orderId={} buyerPid={} parentNull={} parentUserId={} parentIsPromoter={}",
								LOG_TAG,
								companyId,
								orderId,
								buyerPid,
								parentPromoter == null,
								parentPromoter != null ? parentPromoter.getUserId() : null,
								parentPromoter != null ? parentPromoter.getIsPromoter() : null);
						return;
					}
					long parentUserId = parentPromoter.getUserId();
					Map<String, Object> cfg = popularizeSettingSaveService.getMergedPopularizeConfig(companyId);
					long orderBaseMinor = resolveOrderBaseMinorForRatio(cfg, order);
					double ratioPercent = resolveFirstLevelRatioPercent(cfg);
					int rebateMinor = (int) Math.min(Integer.MAX_VALUE, Math.round(orderBaseMinor * ratioPercent / 100.0d));
					if (rebateMinor <= 0) {
						log.info(
								"{} skip rebate_zero companyId={} orderId={} orderBaseMinor={} ratioPercent={}",
								LOG_TAG,
								companyId,
								orderId,
								orderBaseMinor,
								ratioPercent);
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
					row.setPlanCloseTime(brokeragePlanCloseTimeService.computePlanCloseTimeEpochSec());
					row.setCreated(nowSec);
					row.setUpdated(nowSec);
					brokerageMapper.insert(row);
					long priceMinor = orderBaseMinor;
					long rebateMinorLong = rebateMinor;
					popularizeBrokerageStatisticsIncrementService.add(
							Map.of(
									"item_total_price",
									priceMinor,
									"rebate_total",
									rebateMinorLong,
									"no_close_rebate",
									rebateMinorLong),
							parentUserId,
							companyId);
					markBuyerPromoterPurchasedIfNeeded(companyId, buyerUserId, buyerPromoter);
					upgradePromoterGradeJobDispatchPublisher.enqueueUpgradePromoterGrade(companyId, parentUserId);
					log.info(
							"{} done companyId={} orderId={} parentUserId={} buyUserId={} rebateMinor={} priceMinor={}",
							LOG_TAG,
							companyId,
							orderId,
							parentUserId,
							buyerUserId,
							rebateMinor,
							priceMinor);
				});
	}

	private void markBuyerPromoterPurchasedIfNeeded(
			long companyId, long buyerUserId, Promoter buyerPromoter) {
		Integer isBuy = buyerPromoter.getIsBuy();
		if (isBuy != null && isBuy == 1) {
			return;
		}
		UpdateWrapper<Promoter> uw = new UpdateWrapper<>();
		uw.eq("company_id", companyId)
				.eq("user_id", buyerUserId)
				.and(w -> w.isNull("is_buy").or().eq("is_buy", 0))
				.set("is_buy", 1);
		promoterMapper.update(null, uw);
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
