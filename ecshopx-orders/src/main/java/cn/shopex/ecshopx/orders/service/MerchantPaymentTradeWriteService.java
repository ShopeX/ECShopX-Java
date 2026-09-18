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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeStatusWritePort;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantPaymentTradeWriteService implements CronMerchantPaymentTradeStatusWritePort {

	private static final long START_EPOCH_2012_01_01 = 1325347200L;

	private final MerchantPaymentTradeMapper merchantPaymentTradeMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public MerchantPaymentTradeWriteService(
			MerchantPaymentTradeMapper merchantPaymentTradeMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.merchantPaymentTradeMapper = merchantPaymentTradeMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public String genMerchantTradeId(long userId) {
		long now = Instant.now().getEpochSecond();
		long day = (now - START_EPOCH_2012_01_01) / 86400L;

		ZoneId z = ZoneId.systemDefault();
		long startOfDay = LocalDate.now(z).atStartOfDay(z).toEpochSecond();
		long minute = (now - startOfDay) / 90L;

		String dateKey = LocalDate.now(z).format(DateTimeFormatter.BASIC_ISO_DATE);
		String minuteField = String.valueOf(minute);
		int step = ThreadLocalRandom.current().nextInt(1, 10);
		Long redisId = companysRedisTemplate.opsForHash().increment(dateKey, minuteField, step);
		companysRedisTemplate.expire(dateKey, java.time.Duration.ofSeconds(86400));

		long safeRedisId = redisId != null ? redisId : 0L;
		String dayStr = Long.toString(day);
		String minutePadded = padLeft(Long.toString(minute), 3, '0');
		String redisPadded = padLeft(Long.toString(safeRedisId), 5, '0');
		String userPadded = padLeft(Long.toString(Math.floorMod(userId, 10000)), 4, '0');
		return dayStr + minutePadded + redisPadded + userPadded;
	}

	private static String padLeft(String s, int minLen, char pad) {
		if (s.length() >= minLen) {
			return s;
		}
		StringBuilder sb = new StringBuilder(minLen);
		for (int i = s.length(); i < minLen; i++) {
			sb.append(pad);
		}
		sb.append(s);
		return sb.toString();
	}

	public MerchantPaymentTrade create(
			long companyId,
			String wxaAppid,
			String mchid,
			Map<String, Object> paymentData) {
		long userId = longValue(paymentData.get("user_id"));
		long amount = longValue(paymentData.get("amount"));

		String merchantTradeId = genMerchantTradeId(userId);
		int ts = (int) Instant.now().getEpochSecond();

		MerchantPaymentTrade row = new MerchantPaymentTrade();
		row.setMerchantTradeId(merchantTradeId);
		row.setCompanyId(companyId);
		row.setRelSceneId(asString(paymentData.get("rel_scene_id")));
		row.setRelSceneName(asString(paymentData.get("rel_scene_name")));
		row.setReUserName(asString(paymentData.get("re_user_name")));
		row.setMobile(asString(paymentData.get("mobile")));
		row.setAmount(amount);
		row.setUserId(userId);
		row.setOpenId(asString(paymentData.get("open_id")));
		row.setPaymentDesc(asString(paymentData.get("payment_desc")));
		row.setSpbillCreateIp(asString(paymentData.get("spbill_create_ip")));
		row.setPaymentAction("WECHAT");
		row.setCheckName("NO_CHECK");
		row.setMchid(mchid);
		row.setMchAppid(wxaAppid);
		row.setStatus("NOT_PAY");
		row.setCurPayFee(Long.toString(amount));
		row.setCreateTime(ts);
		row.setUpdateTime(ts);

		merchantPaymentTradeMapper.insert(row);
		return row;
	}

	@Override
	public void updateStatusOnly(long companyId, String merchantTradeId, String newStatus) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", newStatus);
		updateStatus(companyId, merchantTradeId, result);
	}

	public void updateStatus(long companyId, String merchantTradeId, Map<String, Object> result) {
		if (result == null || result.isEmpty()) {
			return;
		}
		int ts = (int) Instant.now().getEpochSecond();
		LambdaUpdateWrapper<MerchantPaymentTrade> uw = new LambdaUpdateWrapper<>();
		uw.eq(MerchantPaymentTrade::getMerchantTradeId, merchantTradeId).eq(MerchantPaymentTrade::getCompanyId, companyId);

		Object st = result.get("status");
		if (st != null) {
			uw.set(MerchantPaymentTrade::getStatus, st.toString());
		}
		if (result.containsKey("payment_no")) {
			uw.set(MerchantPaymentTrade::getPaymentNo, asNullableString(result.get("payment_no")));
		}
		if (result.containsKey("payment_time")) {
			uw.set(MerchantPaymentTrade::getPaymentTime, asNullableString(result.get("payment_time")));
		}
		if (result.containsKey("error_code")) {
			uw.set(MerchantPaymentTrade::getErrorCode, asNullableString(result.get("error_code")));
		}
		if (result.containsKey("error_desc")) {
			uw.set(MerchantPaymentTrade::getErrorDesc, asNullableString(result.get("error_desc")));
		}
		uw.set(MerchantPaymentTrade::getUpdateTime, ts);
		merchantPaymentTradeMapper.update(null, uw);
	}

	private static String asString(Object o) {
		return o == null ? null : o.toString();
	}

	private static String asNullableString(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString();
		return StringUtils.hasText(s) ? s : null;
	}

	private static long longValue(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		return Long.parseLong(o.toString().trim());
	}
}
