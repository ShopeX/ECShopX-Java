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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappMemberMobileChangeService {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3456789][0-9]{9}$");

	private final ObjectMapper objectMapper;

	private final StringRedisTemplate membersRedis;

	private final MembersMapper membersMapper;

	private final MemberOperateLogMapper memberOperateLogMapper;

	@Value("${members.update-mobile.ttl-seconds:2592000}")
	private int cooldownTtlSeconds;

	public WxappMemberMobileChangeService(
			ObjectMapper objectMapper,
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			MembersMapper membersMapper,
			MemberOperateLogMapper memberOperateLogMapper) {
		this.objectMapper = objectMapper;
		this.membersRedis = membersRedis;
		this.membersMapper = membersMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
	}

	public Map<String, Object> updateMemberMobile(
			long companyId,
			long userId,
			String oldMobile,
			String oldRegionMobile,
			String oldCountryCode,
			String newMobile,
			String newRegionMobile,
			String newCountryCode,
			String smsCode) {
		String cacheName = "updateMemberMobileCD:" + userId;
		String fullKey = cacheName + ":" + sha1CompanyId(companyId);
		String existing = membersRedis.opsForValue().get(fullKey);
		if (existing != null) {
			throw new ResourceException("手机号每30天只可修改一次");
		}

		if (!CN_MOBILE.matcher(newMobile).matches()) {
			throw new ResourceException("请输入合法的手机号");
		}

		String smsKey = "member-update:company" + companyId + ":" + newMobile;
		String know = membersRedis.opsForValue().get(smsKey);
		String smsNorm = (smsCode == null || smsCode.isEmpty()) ? null : smsCode;
		String knowNorm = (know == null || know.isEmpty()) ? null : know;
		if (!Objects.equals(smsNorm, knowNorm)) {
			throw new ResourceException("短信验证码错误");
		}

		LambdaQueryWrapper<Members> selfQ = new LambdaQueryWrapper<>();
		selfQ.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId).last("LIMIT 1");
		Members current = membersMapper.selectOne(selfQ);
		if (current == null) {
			throw new ResourceException("更新的用户不存在！");
		}

		String newEnc = LegacyFixedMobileEncrypt.fixedEncryptMobile(newMobile);
		LambdaQueryWrapper<Members> dupQ = new LambdaQueryWrapper<>();
		dupQ.eq(Members::getCompanyId, companyId).eq(Members::getMobile, newEnc).last("LIMIT 1");
		Members occupant = membersMapper.selectOne(dupQ);
		if (occupant != null) {
			throw new ResourceException("用户手机号已经存在");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
		uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
		uw.set(Members::getMobile, newEnc);
		uw.set(Members::getRegionMobile, newRegionMobile);
		uw.set(Members::getMobileCountryCode, newCountryCode);
		uw.set(Members::getUpdated, nowSec);
		int affected = membersMapper.update(null, uw);
		if (affected == 0) {
			throw new ResourceException("更新的用户不存在！");
		}

		Members row =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("更新的用户不存在！");
		}

		LinkedHashMap<String, Object> data = MemberAccountService.mapMembersTable(row);
		data.put("mobile", newMobile);

		Map<String, String> oldDataMap = new LinkedHashMap<>();
		oldDataMap.put("mobile", oldMobile);
		oldDataMap.put("region_mobile", oldRegionMobile);
		oldDataMap.put("country_code", oldCountryCode);
		Map<String, String> newDataMap = new LinkedHashMap<>();
		newDataMap.put("mobile", newMobile);
		newDataMap.put("region_mobile", newRegionMobile);
		newDataMap.put("country_code", newCountryCode);

		String oldJson;
		String newJson;
		try {
			oldJson = objectMapper.writeValueAsString(oldDataMap);
			newJson = objectMapper.writeValueAsString(newDataMap);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}

		if (!data.isEmpty()) {
			MemberOperateLog log = new MemberOperateLog();
			log.setCompanyId(companyId);
			log.setUserId(userId);
			log.setOperateType("mobile");
			log.setRemarks("");
			log.setOldData(oldJson);
			log.setNewData(newJson);
			log.setOperater("用户在云店自行修改");
			log.setCreated(nowSec);
			log.setUpdated(nowSec);
			memberOperateLogMapper.insert(log);
		}

		membersRedis.opsForValue().set(fullKey, "1", Duration.ofSeconds(cooldownTtlSeconds));

		return data;
	}

	private static String sha1CompanyId(long companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(String.valueOf(companyId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
