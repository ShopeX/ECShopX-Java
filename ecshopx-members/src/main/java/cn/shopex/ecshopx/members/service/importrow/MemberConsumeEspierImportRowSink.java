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

package cn.shopex.ecshopx.members.service.importrow;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MemberConsumeEspierImportRowSink implements EspierImportRowSink {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersMapper membersMapper;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final StringRedisTemplate redis;

	public MemberConsumeEspierImportRowSink(
			MembersMapper membersMapper,
			MemberTotalConsumptionReadService memberTotalConsumptionReadService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.membersMapper = membersMapper;
		this.memberTotalConsumptionReadService = memberTotalConsumptionReadService;
		this.redis = redis;
	}

	@Override
	public String supportedFileType() {
		return "member_consume";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		String mobile = str(row.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("手机号必填");
		}
		if (!MOBILE_PATTERN.matcher(mobile.trim()).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
		Object consObj = row.get("consumption");
		if (consObj == null || !StringUtils.hasText(String.valueOf(consObj).trim())) {
			throw new BadRequestException("消费额必填");
		}
		BigDecimal consumption;
		try {
			consumption = new BigDecimal(String.valueOf(consObj).trim()).setScale(0, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			throw new BadRequestException("消费额格式错误");
		}
		long delta = consumption.longValue();
		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile.trim());
		Members m =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getMobile, mobileStored)
								.last("LIMIT 1"));
		if (m == null) {
			throw new BadRequestException("手机号不存在");
		}
		long userId = m.getUserId();
		BigDecimal cur = memberTotalConsumptionReadService.getTotalConsumption(userId);
		BigDecimal next = cur.add(BigDecimal.valueOf(delta));
		if (next.compareTo(BigDecimal.ZERO) > 0) {
			String key = redisKey(userId);
			redis.opsForValue().increment(key, delta);
		} else {
			redis.opsForValue().set(redisKey(userId), "0");
		}
	}

	private static String redisKey(long userId) {
		return "totalConsumption:" + sha1HexLowerUtf8(String.valueOf(userId));
	}

	private static String sha1HexLowerUtf8(String s) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
