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

package cn.shopex.ecshopx.hfpay.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.hfpay.domain.HfpayTradeRecord;
import cn.shopex.ecshopx.hfpay.dto.HfpayProfitBrokerageRow;
import cn.shopex.ecshopx.hfpay.dto.HfpayProfitOrderContextRow;
import cn.shopex.ecshopx.hfpay.dto.HfpayRefundLedgerContextRow;
import cn.shopex.ecshopx.hfpay.dto.TradePayGateRow;
import cn.shopex.ecshopx.hfpay.mapper.HfpayTradeRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HfpayTradeRecordService {

	private static final int TRADE_TYPE_SUB = 0;

	private static final int TRADE_TYPE_ADD = 1;

	private static final DateTimeFormatter ORDER_DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayTradeRecordMapper tradeRecordMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final ZoneId businessZoneId;

	public HfpayTradeRecordService(
			HfpayTradeRecordMapper tradeRecordMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.tradeRecordMapper = tradeRecordMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	/**
	 * 提现成功记账：与取现轮询成功终态监听器配套，单笔插入、独立小事务。
	 *
	 * @param companyId 公司 id
	 * @param distributorId 分销商 id
	 * @param outcomeFen 支出金额（分）
	 * @param orderId 订单号
	 */
	@Transactional(rollbackFor = Exception.class)
	public void withdraw(long companyId, long distributorId, int outcomeFen, String orderId) {
		int nowSec = (int) Instant.now().getEpochSecond();
		LocalDateTime now = LocalDateTime.now();
		HfpayTradeRecord row = new HfpayTradeRecord();
		row.setTradeId(nextTradeId());
		row.setCompanyId(Math.toIntExact(companyId));
		row.setDistributorId(String.valueOf(distributorId));
		row.setOuterOrderId(orderId);
		row.setFormUserId(String.valueOf(distributorId));
		row.setTargetUserId("2");
		row.setTradeTime(String.valueOf(nowSec));
		row.setTradeType(TRADE_TYPE_SUB);
		row.setFinType("500");
		row.setIncome(0);
		row.setOutcome(outcomeFen);
		row.setMessage("提现");
		row.setIsClean(1);
		row.setCleanTime(nowSec);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		tradeRecordMapper.insert(row);
	}

	/**
	 * 订单支付成功且支付方式为汇付时写入收入侧记账流水；其它支付方式或查无成功交易单则直接返回。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void paySuccess(String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		TradePayGateRow trade = tradeRecordMapper.selectLatestSuccessTradeForHfpayPaySuccess(orderId.trim());
		if (trade == null || !StringUtils.hasText(trade.getPayType())) {
			return;
		}
		if (!"hfpay".equalsIgnoreCase(trade.getPayType().trim())) {
			return;
		}
		if (tradeRecordMapper.countPaySuccessFinRecordsByOuterOrderId(orderId.trim()) > 0L) {
			return;
		}
		if (!StringUtils.hasText(trade.getCompanyId())) {
			return;
		}
		int companyId;
		try {
			companyId = Integer.parseInt(trade.getCompanyId().trim());
		} catch (NumberFormatException e) {
			return;
		}
		int incomeFen = trade.getPayFee() == null || trade.getPayFee() < 0 ? 0 : trade.getPayFee();
		int nowSec = (int) Instant.now().getEpochSecond();
		LocalDateTime now = LocalDateTime.now();
		HfpayTradeRecord row = new HfpayTradeRecord();
		row.setTradeId(nextTradeId());
		row.setCompanyId(companyId);
		row.setDistributorId(
				StringUtils.hasText(trade.getDistributorId()) ? trade.getDistributorId().trim() : "0");
		row.setOuterOrderId(orderId.trim());
		row.setFormUserId(StringUtils.hasText(trade.getUserId()) ? trade.getUserId().trim() : "0");
		row.setTargetUserId("2");
		row.setTradeTime(String.valueOf(nowSec));
		row.setTradeType(TRADE_TYPE_SUB);
		row.setFinType("100");
		row.setIncome(incomeFen);
		row.setOutcome(0);
		row.setMessage("支付");
		row.setIsClean(0);
		row.setCleanTime(0);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		tradeRecordMapper.insert(row);
	}

	/**
	 * 分账确认后分销佣金退回记账，并将本单汇付记账流水标为已结算。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void profit(String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		String oid = orderId.trim();
		HfpayProfitOrderContextRow ctx = tradeRecordMapper.selectProfitOrderContext(oid);
		if (ctx == null || ctx.getCompanyId() == null) {
			return;
		}
		String payType = ctx.getPayType() == null ? "" : ctx.getPayType().trim();
		if (!"hfpay".equalsIgnoreCase(payType)) {
			return;
		}
		int companyId = ctx.getCompanyId().intValue();
		String distributorId = StringUtils.hasText(ctx.getDistributorId()) ? ctx.getDistributorId().trim() : "0";
		int isProfitsharing = ctx.getIsProfitsharing() == null ? 0 : ctx.getIsProfitsharing();
		Integer isDistribution = ctx.getIsDistribution();

		List<HfpayTradeRecord> batch = new ArrayList<>();
		if (isDistribution != null && isDistribution == 1) {
			List<HfpayProfitBrokerageRow> brokerageRows =
					tradeRecordMapper.selectProfitBrokerageRows(ctx.getCompanyId(), oid);
			if (brokerageRows != null) {
				for (HfpayProfitBrokerageRow br : brokerageRows) {
					if (br.getRebate() == null || br.getRebate() > 0) {
						continue;
					}
					if (!StringUtils.hasText(br.getUserId())) {
						continue;
					}
					String finType = isProfitsharing == 1 ? "411" : "401";
					int absRebate = Math.abs(br.getRebate());
					String uid = br.getUserId().trim();
					String seriesTradeId = nextTradeId();
					int nowSec = (int) Instant.now().getEpochSecond();
					LocalDateTime now = LocalDateTime.now();
					String formFirst = "401".equals(finType) ? distributorId : String.valueOf(companyId);
					String targetFirst = uid;
					batch.add(buildProfitLedgerLine(
							seriesTradeId,
							companyId,
							distributorId,
							oid,
							formFirst,
							targetFirst,
							TRADE_TYPE_ADD,
							finType,
							absRebate,
							0,
							"收到分销员佣金退回",
							nowSec,
							now));
					String formSecond = uid;
					String targetSecond = "401".equals(finType) ? distributorId : String.valueOf(companyId);
					batch.add(buildProfitLedgerLine(
							seriesTradeId,
							companyId,
							distributorId,
							oid,
							formSecond,
							targetSecond,
							TRADE_TYPE_SUB,
							finType,
							0,
							absRebate,
							"分销员佣金退回至商户",
							nowSec,
							now));
				}
			}
		}
		for (HfpayTradeRecord row : batch) {
			tradeRecordMapper.insert(row);
		}
		long existing = tradeRecordMapper.selectCount(new LambdaQueryWrapper<HfpayTradeRecord>()
				.eq(HfpayTradeRecord::getCompanyId, companyId)
				.eq(HfpayTradeRecord::getOuterOrderId, oid));
		if (existing > 0L) {
			int cleanSec = (int) Instant.now().getEpochSecond();
			tradeRecordMapper.update(
					null,
					new LambdaUpdateWrapper<HfpayTradeRecord>()
							.eq(HfpayTradeRecord::getCompanyId, companyId)
							.eq(HfpayTradeRecord::getOuterOrderId, oid)
							.set(HfpayTradeRecord::getIsClean, 1)
							.set(HfpayTradeRecord::getCleanTime, cleanSec));
		}
	}

	private HfpayTradeRecord buildProfitLedgerLine(
			String tradeIdStr,
			int companyId,
			String distributorId,
			String outerOrderId,
			String formUserId,
			String targetUserId,
			int tradeType,
			String finType,
			int income,
			int outcome,
			String message,
			int nowSec,
			LocalDateTime now) {
		HfpayTradeRecord rec = new HfpayTradeRecord();
		rec.setTradeId(tradeIdStr);
		rec.setCompanyId(companyId);
		rec.setDistributorId(distributorId);
		rec.setOuterOrderId(outerOrderId);
		rec.setFormUserId(formUserId);
		rec.setTargetUserId(targetUserId);
		rec.setTradeTime(String.valueOf(nowSec));
		rec.setTradeType(tradeType);
		rec.setFinType(finType);
		rec.setIncome(income);
		rec.setOutcome(outcome);
		rec.setMessage(message);
		rec.setIsClean(0);
		rec.setCleanTime(0);
		rec.setCreatedAt(now);
		rec.setUpdatedAt(now);
		return rec;
	}

	/**
	 * 退款成功记账：与网关成功后的同步 listener 配套；幂等键为 outer_order_id + 退款单号 + 退款科目集合。
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean refundSuccess(String orderId, long refundBn) {
		if (!StringUtils.hasText(orderId)) {
			return true;
		}
		String oid = orderId.trim();
		HfpayRefundLedgerContextRow row = tradeRecordMapper.selectRefundLedgerContext(oid, refundBn);
		if (row == null) {
			return true;
		}
		if (row.getRefundOrderId() != null && !oid.equals(String.valueOf(row.getRefundOrderId()))) {
			throw new BadRequestException("退款单与订单不匹配");
		}
		String orderPay = row.getOrderPayType() == null ? "" : row.getOrderPayType().trim();
		if (!"hfpay".equalsIgnoreCase(orderPay)) {
			return true;
		}
		String refundPay = row.getRefundPayType() == null ? "" : row.getRefundPayType().trim();
		if (!"hfpay".equalsIgnoreCase(refundPay)) {
			return true;
		}
		if (tradeRecordMapper.countRefundSeriesLedgerByOuterOrderAndRefund(oid, refundBn) > 0L) {
			return true;
		}
		int refundFen = row.getRefundFeeFen() == null || row.getRefundFeeFen() < 0 ? 0 : row.getRefundFeeFen();
		if (refundFen <= 0) {
			return true;
		}
		if (row.getCompanyId() == null) {
			throw new BadRequestException("公司信息缺失");
		}
		int companyId = row.getCompanyId().intValue();
		String distributorId = pickDistributorId(row);
		String companyKey = String.valueOf(companyId);
		int ps = row.getIsProfitsharing() == null ? 0 : row.getIsProfitsharing();
		int nowSec = (int) Instant.now().getEpochSecond();
		LocalDateTime now = LocalDateTime.now();
		String seriesTradeId = String.valueOf(refundBn);

		if (ps == 1) {
			insertRefundLine(
					seriesTradeId,
					companyId,
					distributorId,
					oid,
					companyKey,
					"3",
					TRADE_TYPE_SUB,
					"620",
					0,
					refundFen,
					"货款退还至消费者",
					nowSec,
					now);
		} else if (ps == 2) {
			insertRefundLine(
					seriesTradeId,
					companyId,
					distributorId,
					oid,
					distributorId,
					companyKey,
					TRADE_TYPE_SUB,
					"600",
					0,
					refundFen,
					"退款，货款退回平台",
					nowSec,
					now);
			insertRefundLine(
					seriesTradeId,
					companyId,
					distributorId,
					oid,
					companyKey,
					distributorId,
					TRADE_TYPE_ADD,
					"600",
					refundFen,
					0,
					"收到商户货款退回",
					nowSec,
					now);
			insertRefundLine(
					seriesTradeId,
					companyId,
					distributorId,
					oid,
					companyKey,
					"3",
					TRADE_TYPE_SUB,
					"610",
					0,
					refundFen,
					"货款退还至消费者",
					nowSec,
					now);
		}

		return true;
	}

	private void insertRefundLine(
			String tradeIdStr,
			int companyId,
			String distributorId,
			String outerOrderId,
			String formUserId,
			String targetUserId,
			int tradeType,
			String finType,
			int income,
			int outcome,
			String message,
			int nowSec,
			LocalDateTime now) {
		HfpayTradeRecord rec = new HfpayTradeRecord();
		rec.setTradeId(tradeIdStr);
		rec.setCompanyId(companyId);
		rec.setDistributorId(distributorId);
		rec.setOuterOrderId(outerOrderId);
		rec.setFormUserId(formUserId);
		rec.setTargetUserId(targetUserId);
		rec.setTradeTime(String.valueOf(nowSec));
		rec.setTradeType(tradeType);
		rec.setFinType(finType);
		rec.setIncome(income);
		rec.setOutcome(outcome);
		rec.setMessage(message);
		rec.setIsClean(0);
		rec.setCleanTime(0);
		rec.setCreatedAt(now);
		rec.setUpdatedAt(now);
		tradeRecordMapper.insert(rec);
	}

	private static String pickDistributorId(HfpayRefundLedgerContextRow row) {
		if (StringUtils.hasText(row.getTradeDistributorId())) {
			return row.getTradeDistributorId().trim();
		}
		return "0";
	}

	private String nextTradeId() {
		Long redisId = companysRedisTemplate.opsForValue().increment("hfpay_trade_record_trade_id");
		if (redisId == null) {
			redisId = 0L;
		}
		ZonedDateTime endOfDay = LocalDate.now(businessZoneId).atTime(23, 59, 59).atZone(businessZoneId);
		ZonedDateTime znow = ZonedDateTime.now(businessZoneId);
		long seconds = Duration.between(znow, endOfDay).getSeconds();
		if (seconds < 1L) {
			seconds = 1L;
		}
		companysRedisTemplate.expire("hfpay_trade_record_trade_id", Duration.ofSeconds(seconds));
		String ymd = LocalDate.now(businessZoneId).format(ORDER_DAY);
		return "T" + ymd + String.format("%09d", redisId);
	}
}
