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

import cn.shopex.ecshopx.common.cron.hfpay.HfPayQry008Port;
import cn.shopex.ecshopx.common.port.hfpay.HfpayDistributorWithdrawSuccessEventPublishPort;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayCashRecordService {

	/**
	 * $limit，仅用于计算 $fileNum = ceil(count/limit)；单页条数须为 $fileNum（与 PHP getLists 第四参一致），禁止误用本常量作每页 size。
	 */
	private static final int LIMIT_FOR_FILE_NUM = 500;

	private final HfpayCashRecordMapper cashRecordMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayQry008Port qry008Port;
	private final StringRedisTemplate companysRedisTemplate;
	private final HfpayDistributorWithdrawSuccessEventPublishPort hfpayDistributorWithdrawSuccessEventPublishPort;
	private final ZoneId businessZoneId;
	private final Clock clock;

	public HfpayCashRecordService(
			HfpayCashRecordMapper cashRecordMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayQry008Port qry008Port,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			HfpayDistributorWithdrawSuccessEventPublishPort hfpayDistributorWithdrawSuccessEventPublishPort,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp,
			ObjectProvider<Clock> clockProvider) {
		this.cashRecordMapper = cashRecordMapper;
		this.paymentSettingService = paymentSettingService;
		this.qry008Port = qry008Port;
		this.companysRedisTemplate = companysRedisTemplate;
		this.hfpayDistributorWithdrawSuccessEventPublishPort = hfpayDistributorWithdrawSuccessEventPublishPort;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
		Clock resolved = clockProvider.getIfAvailable(() -> Clock.system(this.businessZoneId));
		this.clock = resolved != null ? resolved : Clock.system(this.businessZoneId);
	}

	/**
	 * 轮询处理中汇付取现状态。无数据与跑完均返回 true。非 per-row try：遇异常向上抛出。 未配置 {@code ecshopx.hfpay.business-zone-id} 时使用 JVM 默认时区作为业务“当前时间”来源，与 PHP 依赖应用默认时区可能不完全一致。
	 */
	public boolean scheduleCheckStatus() {
		ZonedDateTime nowZ = ZonedDateTime.now(clock);
		LocalDateTime cutoff = nowZ.toLocalDateTime().minusDays(2);

		long count = cashRecordMapper.selectCount(pendingFilter(cutoff));
		if (count == 0L) {
			return true;
		}

		int limit = LIMIT_FOR_FILE_NUM;
		int fileNum = (int) Math.ceil(count / (double) limit);

		for (int page = 1; page <= fileNum; page++) {
			Page<HfpayCashRecord> p = new Page<>(page, fileNum, false);
			Page<HfpayCashRecord> result = cashRecordMapper.selectPage(p, pendingFilter(cutoff));
			List<HfpayCashRecord> list = result.getRecords();
			for (HfpayCashRecord value : list) {
				processOneRow(value, nowZ.toEpochSecond());
			}
		}
		return true;
	}

	private static LambdaQueryWrapper<HfpayCashRecord> pendingFilter(LocalDateTime cutoff) {
		LambdaQueryWrapper<HfpayCashRecord> w = new LambdaQueryWrapper<>();
		w.eq(HfpayCashRecord::getCashStatus, 1).le(HfpayCashRecord::getCreatedAt, cutoff);
		return w;
	}

	private void processOneRow(HfpayCashRecord value, long nowSec) {
		long id = value.getHfpayCashRecordId() == null ? 0L : value.getHfpayCashRecordId();
		if (id <= 0L) {
			return;
		}
		String key = "hfpay:" + id;
		long lockExpireSec = nowSec + 10L;
		String lockStr = String.valueOf(lockExpireSec);
		Boolean got = companysRedisTemplate.opsForValue().setIfAbsent(key, lockStr);
		if (!Boolean.TRUE.equals(got)) {
			String existing = companysRedisTemplate.opsForValue().get(key);
			if (existing != null) {
				try {
					long v = Long.parseLong(existing.trim());
					if (v < nowSec) {
						companysRedisTemplate.delete(key);
					}
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
			return;
		}
		companysRedisTemplate.expire(key, Duration.ofSeconds(10));

		if (value.getCompanyId() == null) {
			return;
		}
		Map<String, Object> setting = paymentSettingService.loadForCompany(value.getCompanyId());
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", 10);
		payload.put("mer_cust_id", merCustId);
		payload.put("order_id", value.getOrderId());
		payload.put("order_date", value.getHfOrderDate() == null ? "" : value.getHfOrderDate());
		payload.put("trans_type", "15");

		Map<String, Object> result = qry008Port.qry008(setting, payload);

		String respCode = result.get("resp_code") == null ? "" : String.valueOf(result.get("resp_code")).trim();
		Integer newCash = null;
		if ("C00000".equals(respCode)) {
			String transStat = result.get("trans_stat") == null ? "" : String.valueOf(result.get("trans_stat")).trim();
			if (!"S".equals(transStat)) {
				companysRedisTemplate.delete(key);
				return;
			}
			newCash = 2;
		} else if (!"C00001".equals(respCode) && !"C00002".equals(respCode)) {
			newCash = 3;
		}

		if (newCash == null) {
			return;
		}

		Object transAmtRaw = result.get("trans_amt");
		int realFen;
		try {
			BigDecimal yuan =
					transAmtRaw == null
							? BigDecimal.ZERO
							: new BigDecimal(String.valueOf(transAmtRaw).trim());
			realFen = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
		} catch (Exception e) {
			realFen = 0;
		}
		String respDesc = result.get("resp_desc") == null ? "" : String.valueOf(result.get("resp_desc"));

		UpdateWrapper<HfpayCashRecord> u = new UpdateWrapper<>();
		u.eq("hfpay_cash_record_id", id)
				.set("real_trans_amt", realFen)
				.set("cash_status", newCash)
				.set("resp_code", respCode)
				.set("resp_desc", respDesc.isEmpty() ? null : respDesc);
		cashRecordMapper.update(null, u);

		if (newCash == 2) {
			long distributorId = value.getDistributorId() == null ? 0L : value.getDistributorId();
			int transAmtFen = value.getTransAmt() == null ? 0 : value.getTransAmt();
			String orderId = value.getOrderId() == null ? "" : value.getOrderId();
			hfpayDistributorWithdrawSuccessEventPublishPort.publishSyncAfterScheduleWithdrawSuccess(
					id, value.getCompanyId(), distributorId, transAmtFen, orderId);
		}
	}
}
