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

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@EnableConfigurationProperties(CompanyDomainLookupProperties.class)
public class CompanyDomainLookupService {

	private static final long CACHE_TTL_HOURS = 24L;

	private final CompanysMapper companysMapper;

	private final StringRedisTemplate stringRedisTemplate;

	private final CompanyDomainLookupProperties domainProperties;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public CompanyDomainLookupService(
			CompanysMapper companysMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			CompanyDomainLookupProperties domainProperties) {
		this.companysMapper = companysMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.domainProperties = domainProperties;
	}

	/**
	 * Strips scheme and path segment from a host or URL string (same rules as {@link #resolveAndPutCompanyId}).
	 */
	static String stripToDomainHost(String raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim().replace("http://", "").replace("https://", "");
		int slash = s.indexOf('/');
		if (slash >= 0) {
			s = s.substring(0, slash);
		}
		return s.trim();
	}

	public Optional<Map<String, Object>> findCompanyInfoByDomain(String domain) {
		if (domain == null) {
			return Optional.empty();
		}
		String stripped = stripToDomainHost(domain);
		return resolveStrippedDomainToInfo(stripped);
	}

	public void resolveAndPutCompanyId(Map<String, Object> credentials) {
		Object existing = credentials.get("company_id");
		if (existing != null && !"".equals(String.valueOf(existing).trim())) {
			return;
		}
		Object originObj = credentials.get("origin");
		if (originObj == null || !StringUtils.hasText(String.valueOf(originObj))) {
			return;
		}
		String origin = String.valueOf(originObj).trim();
		String domain = stripToDomainHost(origin);
		if (!StringUtils.hasText(domain)) {
			return;
		}
		resolveStrippedDomainToInfo(domain).ifPresent(info -> putCompanyId(credentials, info));
	}

	/**
	 * Writes {@code saas_domain:{sha1(domainHost)}} with a company snapshot and 24h TTL (same JSON as lookup cache).
	 */
	public void writeSaasDomainCacheForDomainHost(String domainHost, Companys row) {
		if (!StringUtils.hasText(domainHost) || row == null) {
			return;
		}
		String redisKey = "saas_domain:" + sha1Hex(domainHost);
		try {
			Map<String, Object> info = buildInfoMap(row);
			stringRedisTemplate.opsForValue().set(
					redisKey, objectMapper.writeValueAsString(info), CACHE_TTL_HOURS, TimeUnit.HOURS);
		} catch (JsonProcessingException ignored) {
			// skip cache write
		}
	}

	private Optional<Map<String, Object>> resolveStrippedDomainToInfo(String stripped) {
		if (!StringUtils.hasText(stripped)) {
			return Optional.empty();
		}
		String redisKey = "saas_domain:" + sha1Hex(stripped);
		String cached = stringRedisTemplate.opsForValue().get(redisKey);
		if (StringUtils.hasText(cached)) {
			try {
				Map<String, Object> info = objectMapper.readValue(cached, new TypeReference<>() {});
				return Optional.of(info);
			} catch (JsonProcessingException ignored) {
				// fall through to DB
			}
		}
		Companys row = companysMapper.selectOne(new LambdaQueryWrapper<Companys>()
				.eq(Companys::getPcDomain, stripped)
				.last("LIMIT 1"));
		if (row != null) {
			return Optional.of(cacheAfterDbHit(redisKey, row));
		}
		row = companysMapper.selectOne(new LambdaQueryWrapper<Companys>()
				.eq(Companys::getH5Domain, stripped)
				.last("LIMIT 1"));
		if (row != null) {
			return Optional.of(cacheAfterDbHit(redisKey, row));
		}
		Long parsedId = parseCompanyIdFromPattern(stripped);
		if (parsedId != null) {
			row = companysMapper.selectById(parsedId);
			if (row != null) {
				return Optional.of(cacheAfterDbHit(redisKey, row));
			}
		}
		return Optional.empty();
	}

	private Map<String, Object> cacheAfterDbHit(String redisKey, Companys row) {
		try {
			Map<String, Object> info = buildInfoMap(row);
			stringRedisTemplate.opsForValue().set(
					redisKey, objectMapper.writeValueAsString(info), CACHE_TTL_HOURS, TimeUnit.HOURS);
			return info;
		} catch (JsonProcessingException ignored) {
			return buildInfoMap(row);
		}
	}

	private static Map<String, Object> buildInfoMap(Companys row) {
		Map<String, Object> info = new HashMap<>(4);
		info.put("company_id", row.getCompanyId());
		info.put("company_name", row.getCompanyName() != null ? row.getCompanyName() : "");
		info.put("pc_domain", row.getPcDomain() != null ? row.getPcDomain() : "");
		info.put("h5_domain", row.getH5Domain() != null ? row.getH5Domain() : "");
		return info;
	}

	private void putCompanyId(Map<String, Object> credentials, Map<String, Object> info) {
		Object id = info.get("company_id");
		if (id != null) {
			credentials.put("company_id", id);
		}
	}

	private Long parseCompanyIdFromPattern(String domain) {
		String pcSuffix = escapeRegex(domainProperties.getPcDomainSuffix());
		Pattern pc = Pattern.compile("^s(\\d+)" + pcSuffix + "$");
		Matcher m = pc.matcher(domain);
		if (m.matches()) {
			return Long.parseLong(m.group(1));
		}
		String h5Suffix = escapeRegex(domainProperties.getH5DomainSuffix());
		Pattern h5 = Pattern.compile("^m(\\d+)" + h5Suffix + "$");
		m = h5.matcher(domain);
		if (m.matches()) {
			return Long.parseLong(m.group(1));
		}
		return null;
	}

	private static String escapeRegex(String suffix) {
		return Pattern.quote(suffix);
	}

	static String sha1Hex(String domain) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(domain.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
