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

package cn.shopex.ecshopx.companys.service.openapi;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.openapi.domain.OpenapiDeveloper;
import cn.shopex.ecshopx.openapi.mapper.OpenapiDeveloperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Loads or materializes OpenAPI developer credentials for a company (app key / secret used for outbound callbacks).
 */
@Service
public class OpenapiDeveloperBootstrapService {

	private final OpenapiDeveloperMapper openapiDeveloperMapper;
	private final CompanysMapper companysMapper;

	@Value("${common.external-baseuri:}")
	private String externalBaseUri;

	@Value("${common.openapi-rand-salt:ecshopx}")
	private String openapiRandSalt;

	public OpenapiDeveloperBootstrapService(
			OpenapiDeveloperMapper openapiDeveloperMapper, CompanysMapper companysMapper) {
		this.openapiDeveloperMapper = openapiDeveloperMapper;
		this.companysMapper = companysMapper;
	}

	public record DeveloperCredentials(String appKey, String appSecret) {}

	public DeveloperCredentials loadOrCreateForCompany(long companyId) {
		OpenapiDeveloper existing =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (existing != null && StringUtils.hasText(existing.getAppKey())) {
			return new DeveloperCredentials(existing.getAppKey(), existing.getAppSecret());
		}
		Companys company = companysMapper.selectById(companyId);
		if (company == null) {
			throw new IllegalStateException("company not found: " + companyId);
		}
		String passportUid =
				StringUtils.hasText(company.getPassportUid())
						? company.getPassportUid()
						: (System.currentTimeMillis() / 1000L + String.valueOf(companyId));
		String eid =
				StringUtils.hasText(company.getEid()) ? company.getEid() : md5Hex(passportUid);
		if (!StringUtils.hasText(company.getPassportUid()) || !StringUtils.hasText(company.getEid())) {
			LambdaUpdateWrapper<Companys> u = new LambdaUpdateWrapper<Companys>().eq(Companys::getCompanyId, companyId);
			if (!StringUtils.hasText(company.getPassportUid())) {
				u.set(Companys::getPassportUid, passportUid);
			}
			if (!StringUtils.hasText(company.getEid())) {
				u.set(Companys::getEid, eid);
			}
			companysMapper.update(null, u);
		}
		String appKey = md5Hex(passportUid).substring(8, 24);
		String appSecret = md5Hex(eid + openapiRandSalt);
		long other =
				openapiDeveloperMapper.selectCount(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.ne(OpenapiDeveloper::getCompanyId, companyId)
								.eq(OpenapiDeveloper::getAppKey, appKey));
		if (other > 0) {
			appKey = md5Hex(passportUid + "_" + companyId).substring(8, 24);
		}
		String extUri = StringUtils.hasText(externalBaseUri) ? externalBaseUri : "";
		if (existing == null) {
			OpenapiDeveloper ins = new OpenapiDeveloper();
			ins.setCompanyId(companyId);
			ins.setAppKey(appKey);
			ins.setAppSecret(appSecret);
			ins.setExternalBaseUri(extUri);
			ins.setExternalAppKey(appKey);
			ins.setExternalAppSecret(appSecret);
			openapiDeveloperMapper.insert(ins);
		} else {
			existing.setAppKey(appKey);
			existing.setAppSecret(appSecret);
			existing.setExternalBaseUri(extUri);
			existing.setExternalAppKey(appKey);
			existing.setExternalAppSecret(appSecret);
			openapiDeveloperMapper.updateById(existing);
		}
		return new DeveloperCredentials(appKey, appSecret);
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
