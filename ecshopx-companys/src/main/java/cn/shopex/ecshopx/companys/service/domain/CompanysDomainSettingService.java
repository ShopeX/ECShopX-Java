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

package cn.shopex.ecshopx.companys.service.domain;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.SetDomainSettingData;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CompanysDomainSettingService {

	private static final DateTimeFormatter COMPANY_DATA_DATE =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final CompanysMapper companysMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final CompanyDomainLookupService companyDomainLookupService;
	private final CompanyDomainLookupProperties domainProperties;
	private final ObjectMapper objectMapper;

	public CompanysDomainSettingService(
			CompanysMapper companysMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			CompanyDomainLookupService companyDomainLookupService,
			CompanyDomainLookupProperties domainProperties,
			ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.companyDomainLookupService = companyDomainLookupService;
		this.domainProperties = domainProperties;
		this.objectMapper = objectMapper;
	}

	public LinkedHashMap<String, Object> getDomainSetting(long companyId) {
		LinkedHashMap<String, Object> defaultDomain = new LinkedHashMap<>();
		defaultDomain.put(
				"h5_default_domain",
				"m" + companyId + domainProperties.getH5DomainSuffix());
		defaultDomain.put(
				"pc_default_domain",
				"s" + companyId + domainProperties.getPcDomainSuffix());

		String key = "domainSetting:" + CompanyDomainLookupService.sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw != null && StringUtils.hasText(raw.trim())) {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					LinkedHashMap<String, Object> out = new LinkedHashMap<>();
					out.putAll(parsed);
					out.putAll(defaultDomain);
					return out;
				}
			} catch (JsonProcessingException ignored) {
				// treat as cache miss
			}
		}

		Companys row = companysMapper.selectById(companyId);
		LinkedHashMap<String, Object> base =
				row == null ? new LinkedHashMap<>() : toCompanyDataMap(row);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(base);
		merged.putAll(defaultDomain);
		return merged;
	}

	private LinkedHashMap<String, Object> toCompanyDataMap(Companys row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", row.getCompanyId());
		m.put("company_name", row.getCompanyName());
		m.put("eid", row.getEid());
		m.put("passport_uid", row.getPassportUid());
		m.put("company_admin_operator_id", row.getCompanyAdminOperatorId());
		m.put("industry", row.getIndustry());
		m.put("created", row.getCreated());
		if (row.getCreated() != null) {
			m.put(
					"created_date",
					Instant.ofEpochSecond(row.getCreated().longValue())
							.atZone(ZoneId.systemDefault())
							.format(COMPANY_DATA_DATE));
		} else {
			m.put("created_date", "");
		}
		m.put("expiredAt", row.getExpiredAt());
		if (row.getExpiredAt() != null) {
			m.put(
					"expiredAt_date",
					Instant.ofEpochSecond(row.getExpiredAt())
							.atZone(ZoneId.systemDefault())
							.format(COMPANY_DATA_DATE));
		} else {
			m.put("expiredAt_date", "");
		}
		m.put("is_disabled", Boolean.TRUE.equals(row.getIsDisabled()));
		m.put("third_params", row.getThirdParams());
		m.put("salesman_limit", row.getSalesmanLimit());
		m.put("is_open_pc_template", row.getIsOpenPcTemplate());
		m.put("is_open_domain_setting", row.getIsOpenDomainSetting());
		m.put("h5_domain", row.getH5Domain());
		m.put("pc_domain", row.getPcDomain());
		m.put("menu_type", row.getMenuType());
		return m;
	}

	public SetDomainSettingData setDomainSetting(long companyId, Map<String, Object> mergedInput) {
		LinkedHashMap<String, String> data = new LinkedHashMap<>();
		putStrippedDomainIfPresent(mergedInput, "pc_domain", data);
		putStrippedDomainIfPresent(mergedInput, "h5_domain", data);

		for (Map.Entry<String, String> e : data.entrySet()) {
			String domain = e.getValue();
			if (!StringUtils.hasText(domain)) {
				continue;
			}
			Optional<Map<String, Object>> occ = companyDomainLookupService.findCompanyInfoByDomain(domain);
			if (occ.isPresent()) {
				long otherId = parseCompanyId(occ.get().get("company_id"));
				if (otherId > 0L && otherId != companyId) {
					throw new ResourceException("域名被占用：" + domain);
				}
			}
		}

		Companys row = companysMapper.selectById(companyId);
		if (row == null) {
			throw new ResourceException("企业账号为" + companyId + "不存在！");
		}

		SetDomainSettingData out = new SetDomainSettingData();
		out.setCompanyId(companyId);

		if (data.isEmpty()) {
			return out;
		}

		String oldPc = row.getPcDomain() != null ? row.getPcDomain() : "";
		String oldH5 = row.getH5Domain() != null ? row.getH5Domain() : "";

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Companys> w = new LambdaUpdateWrapper<Companys>()
				.eq(Companys::getCompanyId, companyId)
				.set(Companys::getUpdated, now);
		if (data.containsKey("pc_domain")) {
			w.set(Companys::getPcDomain, data.get("pc_domain"));
		}
		if (data.containsKey("h5_domain")) {
			w.set(Companys::getH5Domain, data.get("h5_domain"));
		}
		int n = companysMapper.update(null, w);
		if (n == 0) {
			throw new ResourceException("企业账号为" + companyId + "不存在！");
		}

		mergeDomainSettingRedis(companyId, data);
		syncSaasDomainKeys(data, oldPc, oldH5, companyId);

		if (data.containsKey("pc_domain")) {
			out.setPcDomain(data.get("pc_domain"));
		}
		if (data.containsKey("h5_domain")) {
			out.setH5Domain(data.get("h5_domain"));
		}
		return out;
	}

	private static void putStrippedDomainIfPresent(
			Map<String, Object> mergedInput, String key, LinkedHashMap<String, String> data) {
		if (mergedInput == null
				|| !mergedInput.containsKey(key)
				|| mergedInput.get(key) == null) {
			return;
		}
		Object raw = mergedInput.get(key);
		String stripped = CompanyDomainLookupService.stripToDomainHost(String.valueOf(raw));
		data.put(key, stripped);
	}

	private static long parseCompanyId(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void mergeDomainSettingRedis(long companyId, Map<String, String> data) {
		String key = "domainSetting:" + CompanyDomainLookupService.sha1Hex(String.valueOf(companyId));
		String existing = companysRedisTemplate.opsForValue().get(key);
		LinkedHashMap<String, Object> bucket = new LinkedHashMap<>();
		if (StringUtils.hasText(existing)) {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(existing, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					bucket.putAll(parsed);
				}
			} catch (JsonProcessingException ignored) {
				// replace with fresh map
			}
		}
		for (Map.Entry<String, String> e : data.entrySet()) {
			bucket.put(e.getKey(), e.getValue());
		}
		try {
			companysRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(bucket));
		} catch (JsonProcessingException e) {
			throw new ResourceException("域名配置缓存写入失败");
		}
	}

	private void syncSaasDomainKeys(
			Map<String, String> data, String oldPc, String oldH5, long companyId) {
		Companys refreshed = companysMapper.selectById(companyId);
		if (refreshed == null) {
			return;
		}
		if (data.containsKey("pc_domain")) {
			if (StringUtils.hasText(oldPc)) {
				companysRedisTemplate.delete("saas_domain:" + CompanyDomainLookupService.sha1Hex(oldPc));
			}
			String newPc = data.get("pc_domain");
			if (StringUtils.hasText(newPc)) {
				companyDomainLookupService.writeSaasDomainCacheForDomainHost(newPc, refreshed);
			}
		}
		if (data.containsKey("h5_domain")) {
			if (StringUtils.hasText(oldH5)) {
				companysRedisTemplate.delete("saas_domain:" + CompanyDomainLookupService.sha1Hex(oldH5));
			}
			String newH5 = data.get("h5_domain");
			if (StringUtils.hasText(newH5)) {
				companyDomainLookupService.writeSaasDomainCacheForDomainHost(newH5, refreshed);
			}
		}
	}
}
