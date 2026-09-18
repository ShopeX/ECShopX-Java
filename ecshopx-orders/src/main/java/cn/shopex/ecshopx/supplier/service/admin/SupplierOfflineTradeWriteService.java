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

package cn.shopex.ecshopx.supplier.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierOfflineTradeWriteService {

	private static final Logger log = LoggerFactory.getLogger(SupplierOfflineTradeWriteService.class);
	private static final long TRADE_ID_EPOCH_ORIGIN = 1325347200L;

	private final SupplierOrderMapper supplierOrderMapper;
	private final TradeMapper tradeMapper;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;

	public SupplierOfflineTradeWriteService(
			SupplierOrderMapper supplierOrderMapper,
			TradeMapper tradeMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.tradeMapper = tradeMapper;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
	}

	public void createOfflineTradeAndMarkSuccess(long companyId, long supplierId, long orderId) {
		int supplierIdInt = (int) supplierId;
		SupplierOrder orderInfo = supplierOrderMapper.selectOne(new LambdaQueryWrapper<SupplierOrder>()
				.eq(SupplierOrder::getCompanyId, companyId)
				.eq(SupplierOrder::getSupplierId, supplierIdInt)
				.eq(SupplierOrder::getOrderId, orderId)
				.last("LIMIT 1"));
		if (orderInfo == null) {
			throw new ResourceException("订单不存在");
		}

		int baseCents = parseTotalFeeToCents(orderInfo.getTotalFee());
		int totalFeeStored = baseCents;
		int payFee = baseCents;
		String payType = orderInfo.getPayType() == null ? "" : orderInfo.getPayType();
		Float feeRate = orderInfo.getFeeRate();
		boolean rateBranch =
				(payType.startsWith("wxpay") || payType.startsWith("alipay"))
						&& feeRate != null
						&& feeRate > 0f;
		float curFeeRateVal = 1.0f;
		String curFeeTypeVal = "CNY";
		String curFeeSymbolVal = "￥";
		int curPayFeeVal = payFee;
		if (rateBranch) {
			curFeeRateVal = (float) (Math.round(feeRate.doubleValue() * 10000d) / 10000d);
			curFeeTypeVal = orderInfo.getFeeType() != null ? orderInfo.getFeeType() : "";
			curFeeSymbolVal = orderInfo.getFeeSymbol() != null ? orderInfo.getFeeSymbol() : "";
			int payFeeOrig = payFee;
			int totalFeeOrig = totalFeeStored;
			curPayFeeVal = payFeeOrig;
			payFee = (int) Math.round(payFeeOrig * curFeeRateVal);
			totalFeeStored = (int) Math.round(totalFeeOrig * curFeeRateVal);
		}

		long userId = orderInfo.getUserId() == null ? 0L : orderInfo.getUserId();
		String tradeIdCore = generateTradeIdCore(userId);
		Long distributorId = orderInfo.getDistributorId() == null ? 0L : orderInfo.getDistributorId();
		String dealerIdStr = "0";
		Long merchantIdVal = 0L;
		if (distributorId > 0L) {
			DistributionDistributorPeek dist = distributionDistributorPeekMapper.selectOne(
					new LambdaQueryWrapper<DistributionDistributorPeek>()
							.eq(DistributionDistributorPeek::getCompanyId, companyId)
							.eq(DistributionDistributorPeek::getDistributorId, distributorId)
							.last("LIMIT 1"));
			if (dist == null || dist.getDealerId() == null) {
				throw new ResourceException("未查询到店铺信息");
			}
			dealerIdStr = String.valueOf(dist.getDealerId());
			merchantIdVal = dist.getMerchantId() == null ? 0L : dist.getMerchantId();
		}

		String tradeId = tradeIdCore;
		if (distributorId > 0L) {
			Optional<String> shopCode = findShopCodeForValidDistributor(companyId, distributorId);
			if (shopCode.isPresent()) {
				tradeId = shopCode.get() + tradeIdCore;
			}
		}

		String discountInfoStored = resolveDiscountInfoForInsert(orderInfo.getDiscountInfo());

		Trade trade = new Trade();
		trade.setTradeId(tradeId);
		trade.setOrderId(String.valueOf(orderInfo.getOrderId()));
		trade.setCompanyId(String.valueOf(orderInfo.getCompanyId()));
		trade.setShopId(String.valueOf(orderInfo.getShopId() == null ? 0L : orderInfo.getShopId()));
		if (orderInfo.getDistributorId() == null || orderInfo.getDistributorId() == 0L) {
			trade.setDistributorId("");
		} else {
			trade.setDistributorId(String.valueOf(orderInfo.getDistributorId()));
		}
		trade.setDealerId(dealerIdStr);
		trade.setMerchantId(merchantIdVal);
		trade.setTradeSourceType(orderInfo.getOrderType() == null ? "" : orderInfo.getOrderType());
		trade.setUserId(String.valueOf(orderInfo.getUserId() == null ? 0L : orderInfo.getUserId()));
		trade.setMobile(orderInfo.getReceiverMobile() == null ? "" : orderInfo.getReceiverMobile().trim());
		trade.setOpenId("");
		trade.setWxaAppid("");
		trade.setDiscountFee(orderInfo.getDiscountFee() == null ? 0 : orderInfo.getDiscountFee());
		trade.setDiscountInfo(discountInfoStored);
		trade.setTotalFee(totalFeeStored);
		trade.setPayFee(payFee);
		trade.setFeeType("CNY");
		trade.setTradeState("NOTPAY");
		trade.setPayType(payType);
		trade.setPayChannel("");
		String title = orderInfo.getTitle();
		trade.setBody(title);
		trade.setDetail(title);
		trade.setTimeStart(String.valueOf(System.currentTimeMillis() / 1000L));
		if (rateBranch) {
			trade.setCurFeeRate(curFeeRateVal);
			trade.setCurFeeType(curFeeTypeVal);
			trade.setCurFeeSymbol(curFeeSymbolVal);
			trade.setCurPayFee(curPayFeeVal);
		} else {
			trade.setCurPayFee(payFee);
			trade.setCurFeeType("CNY");
			trade.setCurFeeRate(1.0f);
			trade.setCurFeeSymbol("￥");
		}

		String openIdForWx = trade.getOpenId();
		String mchIdForWx = trade.getMchId();
		if ("wxpay".equals(payType)
				&& (!StringUtils.hasText(openIdForWx) || !StringUtils.hasText(mchIdForWx))) {
			throw new ResourceException("创建交易单失败，请检查参数");
		}

		try {
			tradeMapper.insert(trade);
		} catch (DataAccessException ex) {
			log.error("trade insert failed, companyId={} orderId={}", companyId, orderId, ex);
			throw new ResourceException("创建交易失败");
		}

		markTradeSuccess(tradeId);
	}

	private Optional<String> findShopCodeForValidDistributor(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		DistributionDistributorPeek entity = distributionDistributorPeekMapper.selectOne(
				new LambdaQueryWrapper<DistributionDistributorPeek>()
						.eq(DistributionDistributorPeek::getCompanyId, companyId)
						.eq(DistributionDistributorPeek::getDistributorId, distributorId)
						.eq(DistributionDistributorPeek::getIsValid, "true")
						.last("LIMIT 1"));
		if (entity == null) {
			return Optional.empty();
		}
		String code = entity.getShopCode();
		if (!StringUtils.hasText(code)) {
			return Optional.empty();
		}
		return Optional.of(code.trim());
	}

	private String resolveDiscountInfoForInsert(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String t = raw.trim();
		if (t.startsWith("{") || t.startsWith("[")) {
			return t;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			log.error("discount_info serialization failed", e);
			throw new ResourceException("优惠信息格式错误");
		}
	}

	private static int parseTotalFeeToCents(String totalFee) {
		if (totalFee == null || totalFee.isBlank()) {
			throw new ResourceException("订单金额无效");
		}
		try {
			BigDecimal bd = new BigDecimal(totalFee.trim());
			return bd.setScale(0, RoundingMode.HALF_UP).intValue();
		} catch (ArithmeticException | NumberFormatException ex) {
			throw new ResourceException("订单金额无效");
		}
	}

	private String generateTradeIdCore(long userId) {
		long epochSec = System.currentTimeMillis() / 1000L;
		long day = (epochSec - TRADE_ID_EPOCH_ORIGIN) / 86400L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());
		long todayStartEpoch = z.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		long minute = (epochSec - todayStartEpoch) / 90L;
		String ymd = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int rnd = ThreadLocalRandom.current().nextInt(1, 10);
		Long redisId = redisTemplate.opsForHash().increment(ymd, String.valueOf(minute), rnd);
		if (redisId == null) {
			throw new ResourceException("生成交易单号失败");
		}
		redisTemplate.expire(ymd, Duration.ofSeconds(86400));
		long uidMod = Math.floorMod(userId, 10000L);
		return day + padLeftMin(minute, 3) + padLeftMin(redisId, 5) + padLeftMin(uidMod, 4);
	}

	private static String padLeftMin(long n, int minLen) {
		String s = Long.toString(n);
		if (s.length() >= minLen) {
			return s;
		}
		return "0".repeat(minLen - s.length()) + s;
	}

	private void markTradeSuccess(String tradeId) {
		Trade loaded = tradeMapper.selectById(tradeId);
		if (loaded == null) {
			throw new ResourceException("更新订单不存在");
		}
		if (!"NOTPAY".equals(loaded.getTradeState())) {
			throw new ResourceException("更新已处理，不需要更新");
		}
		String tradeNo = buildTradeNoSuffix(loaded);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, tradeId)
				.eq(Trade::getTradeState, "NOTPAY")
				.set(Trade::getTradeState, "SUCCESS")
				.set(Trade::getTimeExpire, String.valueOf(nowSec))
				.set(Trade::getTradeNo, tradeNo);
		int updated = tradeMapper.update(null, uw);
		if (updated <= 0) {
			log.warn("trade success update affected 0 rows, tradeId={}", tradeId);
			throw new ResourceException("交易单状态更新失败");
		}
	}

	private String buildTradeNoSuffix(Trade trade) {
		String companyId = trade.getCompanyId() == null ? "0" : trade.getCompanyId();
		String distributorId = trade.getDistributorId() == null ? "0" : trade.getDistributorId();
		String orderId = trade.getOrderId() == null ? "" : trade.getOrderId();
		long seq = nextTodayTradeSequence(companyId, distributorId, orderId);
		String md = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		return md + "-" + seq;
	}

	private long nextTodayTradeSequence(String companyId, String distributorId, String orderId) {
		String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		String hKey = "h_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String cKey = "c_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String existing = (String) redisTemplate.opsForHash().get(hKey, orderId);
		if (StringUtils.hasText(existing)) {
			return Long.parseLong(existing);
		}
		Long count = redisTemplate.opsForValue().increment(cKey);
		if (count == null) {
			count = 1L;
		}
		redisTemplate.opsForHash().put(hKey, orderId, String.valueOf(count));
		redisTemplate.expire(hKey, Duration.ofSeconds(86400));
		redisTemplate.expire(cKey, Duration.ofSeconds(86400));
		return count;
	}
}
