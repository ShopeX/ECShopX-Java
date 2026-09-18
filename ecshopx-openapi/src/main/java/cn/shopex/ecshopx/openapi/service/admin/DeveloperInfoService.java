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

package cn.shopex.ecshopx.openapi.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.companys.CompanyOpenapiIdentityReadPort;
import cn.shopex.ecshopx.openapi.domain.OpenapiDeveloper;
import cn.shopex.ecshopx.openapi.mapper.OpenapiDeveloperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeveloperInfoService {

	private final OpenapiDeveloperMapper openapiDeveloperMapper;
	private final CompanyOpenapiIdentityReadPort companyOpenapiIdentityReadPort;

	@Value("${common.external-baseuri:}")
	private String externalBaseUri;

	@Value("${common.openapi-rand-salt:}")
	private String openapiRandSalt;

	public DeveloperInfoService(
			OpenapiDeveloperMapper openapiDeveloperMapper,
			CompanyOpenapiIdentityReadPort companyOpenapiIdentityReadPort) {
		this.openapiDeveloperMapper = openapiDeveloperMapper;
		this.companyOpenapiIdentityReadPort = companyOpenapiIdentityReadPort;
	}

	public void update(long companyId, Map<String, Object> params) {
		Object appKeyRaw = params.get("app_key");
		String appKey = appKeyRaw == null ? "" : String.valueOf(appKeyRaw).trim();
		if (!StringUtils.hasText(appKey)) {
			throw new BadRequestException("app_key不能为空");
		}
		String appSecret;
		if (!params.containsKey("app_secret")) {
			OpenapiDeveloper existing =
					openapiDeveloperMapper.selectOne(
							new LambdaQueryWrapper<OpenapiDeveloper>()
									.eq(OpenapiDeveloper::getCompanyId, companyId)
									.last("LIMIT 1"));
			appSecret =
					existing != null && existing.getAppSecret() != null ? existing.getAppSecret() : "";
		} else {
			Object secretRaw = params.get("app_secret");
			appSecret = secretRaw == null ? "" : String.valueOf(secretRaw);
		}
		long conflict =
				openapiDeveloperMapper.selectCount(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.ne(OpenapiDeveloper::getCompanyId, companyId)
								.eq(OpenapiDeveloper::getAppKey, appKey));
		if (conflict > 0) {
			throw new ResourceException("app_key已存在");
		}
		OpenapiDeveloper cur =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (cur == null) {
			throw new ResourceException("未查询到更新数据");
		}
		cur.setAppKey(appKey);
		cur.setAppSecret(appSecret);
		LambdaUpdateWrapper<OpenapiDeveloper> w =
				new LambdaUpdateWrapper<OpenapiDeveloper>().eq(OpenapiDeveloper::getCompanyId, companyId);
		int n = openapiDeveloperMapper.update(cur, w);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public Map<String, Object> info(long companyId) {
		OpenapiDeveloper row =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row != null) {
			return toResponseMap(row);
		}
		CompanyOpenapiIdentityReadPort.Identity company =
				companyOpenapiIdentityReadPort.findByCompanyId(companyId).orElse(null);
		String passportUid =
				(company != null && StringUtils.hasText(company.passportUid()))
						? company.passportUid()
						: (Instant.now().getEpochSecond() + String.valueOf(companyId));
		String eid =
				(company != null && StringUtils.hasText(company.eid()))
						? company.eid()
						: md5Hex(passportUid);
		String appKey = md5Hex(passportUid).substring(8, 24);
		String appSecret =
				md5Hex(eid + (openapiRandSalt == null ? "" : openapiRandSalt));
		String extUri = StringUtils.hasText(externalBaseUri) ? externalBaseUri : "";
		String externalAppKey = appKey;
		String externalAppSecret = appSecret;
		applyCredentials(companyId, appKey, appSecret, extUri, externalAppKey, externalAppSecret);
		OpenapiDeveloper finalRow =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (finalRow == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toResponseMap(finalRow);
	}

	private void applyCredentials(
			long companyId,
			String appKey,
			String appSecret,
			String externalBaseUri,
			String externalAppKey,
			String externalAppSecret) {
		long conflict =
				openapiDeveloperMapper.selectCount(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.ne(OpenapiDeveloper::getCompanyId, companyId)
								.eq(OpenapiDeveloper::getAppKey, appKey));
		if (conflict > 0) {
			throw new ResourceException("app_key已存在");
		}
		OpenapiDeveloper cur =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (cur == null) {
			OpenapiDeveloper ins = new OpenapiDeveloper();
			ins.setCompanyId(companyId);
			ins.setAppKey(appKey);
			ins.setAppSecret(appSecret);
			ins.setExternalBaseUri(externalBaseUri);
			ins.setExternalAppKey(externalAppKey);
			ins.setExternalAppSecret(externalAppSecret);
			openapiDeveloperMapper.insert(ins);
		} else {
			cur.setAppKey(appKey);
			cur.setAppSecret(appSecret);
			cur.setExternalBaseUri(externalBaseUri);
			cur.setExternalAppKey(externalAppKey);
			cur.setExternalAppSecret(externalAppSecret);
			LambdaUpdateWrapper<OpenapiDeveloper> w =
					new LambdaUpdateWrapper<OpenapiDeveloper>()
							.eq(OpenapiDeveloper::getCompanyId, companyId);
			int n = openapiDeveloperMapper.update(cur, w);
			if (n == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}
	}

	private Map<String, Object> toResponseMap(OpenapiDeveloper finalRow) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("developer_id", finalRow.getDeveloperId());
		m.put("company_id", finalRow.getCompanyId());
		m.put("app_key", finalRow.getAppKey());
		m.put("app_secret", finalRow.getAppSecret());
		m.put(
				"external_base_uri",
				Optional.ofNullable(finalRow.getExternalBaseUri()).orElse(""));
		m.put("external_app_key", finalRow.getExternalAppKey());
		m.put("external_app_secret", finalRow.getExternalAppSecret());
		return m;
	}

	private static String md5Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
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
}
