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

package cn.shopex.ecshopx.companys.service.merchant;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupProperties;
import cn.shopex.ecshopx.merchant.port.CompanyDomainInfoRead;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysCompanyDomainInfoRead implements CompanyDomainInfoRead {

	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final CompanysMapper companysMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final CompanyDomainLookupProperties domainLookupProperties;
	private final ObjectMapper objectMapper;

	public CompanysCompanyDomainInfoRead(
			CompanysMapper companysMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			CompanyDomainLookupProperties domainLookupProperties,
			ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.domainLookupProperties = domainLookupProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> getDomainInfo(long companyId) {
		String defaultH5 = "m" + companyId + domainLookupProperties.getH5DomainSuffix();
		String defaultPc = "s" + companyId + domainLookupProperties.getPcDomainSuffix();

		String redisKey = "domainSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(redisKey);
		if (StringUtils.hasText(raw)) {
			try {
				Map<String, Object> decoded =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (decoded != null) {
					LinkedHashMap<String, Object> merged = new LinkedHashMap<>(decoded);
					merged.put("h5_default_domain", defaultH5);
					merged.put("pc_default_domain", defaultPc);
					return merged;
				}
			} catch (JsonProcessingException ignored) {
				// fall through to DB
			}
		}

		Companys row = companysMapper.selectById(companyId);
		LinkedHashMap<String, Object> base =
				row != null ? toCompanyDataMap(row) : new LinkedHashMap<>();
		base.put("h5_default_domain", defaultH5);
		base.put("pc_default_domain", defaultPc);
		return base;
	}

	private static LinkedHashMap<String, Object> toCompanyDataMap(Companys row) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("company_id", row.getCompanyId());
		map.put("company_name", row.getCompanyName());
		map.put("eid", row.getEid());
		map.put("passport_uid", row.getPassportUid());
		map.put("company_admin_operator_id", row.getCompanyAdminOperatorId());
		map.put("industry", row.getIndustry());
		map.put("created", row.getCreated());
		if (row.getCreated() != null) {
			map.put(
					"created_date",
					LocalDateTime.ofInstant(
									Instant.ofEpochSecond(row.getCreated().longValue()),
									ZoneId.systemDefault())
							.format(DATE_TIME));
		} else {
			map.put("created_date", null);
		}
		map.put("expiredAt", row.getExpiredAt());
		if (row.getExpiredAt() != null) {
			map.put(
					"expiredAt_date",
					LocalDateTime.ofInstant(
									Instant.ofEpochSecond(row.getExpiredAt()),
									ZoneId.systemDefault())
							.format(DATE_TIME));
		} else {
			map.put("expiredAt_date", null);
		}
		map.put("is_disabled", row.getIsDisabled());
		map.put("third_params", row.getThirdParams());
		map.put("salesman_limit", row.getSalesmanLimit());
		map.put("is_open_pc_template", row.getIsOpenPcTemplate());
		map.put("is_open_domain_setting", row.getIsOpenDomainSetting());
		map.put("h5_domain", row.getH5Domain());
		map.put("pc_domain", row.getPcDomain());
		map.put("menu_type", row.getMenuType());
		return map;
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
