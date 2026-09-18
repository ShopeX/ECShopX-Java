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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyDemoLicenseService {

	private final CompanysMapper companysMapper;
	private final ResourcesMapper resourcesMapper;
	private final CompanyActivatePersistenceService companyActivatePersistenceService;
	private final CompanysLicensePayloadEncryptor licensePayloadEncryptor;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public CompanyDemoLicenseService(
			CompanysMapper companysMapper,
			ResourcesMapper resourcesMapper,
			CompanyActivatePersistenceService companyActivatePersistenceService,
			CompanysLicensePayloadEncryptor licensePayloadEncryptor,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.resourcesMapper = resourcesMapper;
		this.companyActivatePersistenceService = companyActivatePersistenceService;
		this.licensePayloadEncryptor = licensePayloadEncryptor;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createDemoCompanyLicense(Map<String, Object> params) {
		if (params.get("company_id") == null || params.get("expired_at") == null) {
			throw new ResourceException("初始化商城授权信息有误");
		}
		long companyId = toLong(params.get("company_id"));
		Companys company = companysMapper.selectById(companyId);
		if (company == null) {
			throw new ResourceException("无相关企业信息");
		}
		String activeCodePlain = "demo123";
		LinkedHashMap<String, Object> full = new LinkedHashMap<>(params);
		full.put("available_days", 15);
		full.put("shop_num", 1);
		full.put("source", "demo");
		full.put("resource_name", "1店版");
		full.put("left_shop_num", 1);
		full.put("active_status", "active");
		full.put("active_at", System.currentTimeMillis() / 1000L);
		full.put("code", activeCodePlain);
		full.put("active_code", licensePayloadEncryptor.encryptActiveCodePlain(activeCodePlain));

		Map<String, Object> result = companyActivatePersistenceService.activateAfterLicense(full);
		writePostCommitSideEffects(companyId, full, result);
		return result;
	}

	private void writePostCommitSideEffects(long companyId, LinkedHashMap<String, Object> fullParams, Map<String, Object> result) {
		Resources latest = selectLatestValidResource(companyId);
		if (latest != null) {
			LinkedHashMap<String, Object> res = new LinkedHashMap<>();
			res.put("company_id", companyId);
			res.put("expired_at", latest.getExpiredAt());
			res.put("resouce_id", latest.getResourceId());
			res.put("source", latest.getSource() != null ? latest.getSource() : "");
			try {
				String json = objectMapper.writeValueAsString(res);
				companysRedisTemplate.opsForValue().set(companyActivateRedisKey(companyId), json);
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		}
		Object rid = result.get("resource_id");
		if (rid != null && StringUtils.hasText(rid.toString()) && !"0".equals(rid.toString())) {
			saveLicenseRedis(fullParams, companyId);
		}
	}

	private void saveLicenseRedis(LinkedHashMap<String, Object> params, long companyId) {
		String redisKey = "AuthorizeActivation:" + sha1Hex(String.valueOf(companyId));
		String field = str(params.get("active_code"));
		String value = licensePayloadEncryptor.encryptParamsMap(params);
		companysRedisTemplate.opsForHash().put(redisKey, field, value);
	}

	private Resources selectLatestValidResource(long companyId) {
		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<Resources> q = new LambdaQueryWrapper<>();
		q.eq(Resources::getCompanyId, companyId)
				.gt(Resources::getExpiredAt, nowSec)
				.orderByDesc(Resources::getExpiredAt)
				.last("LIMIT 1");
		return resourcesMapper.selectOne(q);
	}

	private static String companyActivateRedisKey(long companyId) {
		return "companyActivateInfo:" + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
