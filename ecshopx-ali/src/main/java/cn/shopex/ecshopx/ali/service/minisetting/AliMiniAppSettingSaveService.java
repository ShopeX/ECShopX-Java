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

package cn.shopex.ecshopx.ali.service.minisetting;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import cn.shopex.ecshopx.ali.mapper.AliMiniAppSettingMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliMiniAppSettingSaveService {

	private static final String CACHE_KEY_PATTERN = "CACHE:ALI:MINI:APP:%s";

	private final AliMiniAppSettingMapper mapper;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public AliMiniAppSettingSaveService(
			AliMiniAppSettingMapper mapper,
			ObjectMapper objectMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void save(long companyId, Map<String, Object> params) {
		String authorizerAppid = stringParam(params, "authorizer_appid");
		if (companyId <= 0 || !StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("参数错误，company_id或authorizer_appid不能为空");
		}

		String merchantPrivateKey = requiredStringFromParams(params, "merchant_private_key");
		String apiSignMethod = requiredStringFromParams(params, "api_sign_method");

		Object rawSettingId = params.get("setting_id");
		AliMiniAppSetting companyData =
				mapper.selectOne(Wrappers.<AliMiniAppSetting>lambdaQuery().eq(AliMiniAppSetting::getCompanyId, companyId));

		Long effectiveSettingId;
		if (companyData != null) {
			if (AliMiniAppSettingIdValidator.isEmptySettingId(rawSettingId)) {
				effectiveSettingId = companyData.getSettingId();
			} else {
				Long reqId = parseNonEmptySettingId(rawSettingId);
				if (!Objects.equals(reqId, companyData.getSettingId())) {
					throw new ResourceException("当前账号没有该小程序的配置权限");
				}
				effectiveSettingId = reqId;
			}
		} else {
			if (AliMiniAppSettingIdValidator.isEmptySettingId(rawSettingId)) {
				effectiveSettingId = null;
			} else {
				effectiveSettingId = parseNonEmptySettingId(rawSettingId);
			}
		}

		Long dup = mapper.selectCount(Wrappers.<AliMiniAppSetting>lambdaQuery()
				.eq(AliMiniAppSetting::getAuthorizerAppid, authorizerAppid)
				.ne(AliMiniAppSetting::getCompanyId, companyId));
		if (dup != null && dup > 0) {
			throw new ResourceException("当前小程序appId已被绑定，不能重复绑定");
		}

		long now = System.currentTimeMillis() / 1000;

		if (AliMiniAppSettingIdValidator.isEmptySettingId(effectiveSettingId)) {
			AliMiniAppSetting entity = new AliMiniAppSetting();
			entity.setCompanyId(companyId);
			entity.setAuthorizerAppid(authorizerAppid);
			entity.setMerchantPrivateKey(merchantPrivateKey);
			entity.setApiSignMethod(apiSignMethod);
			applyOptionalStrings(entity, params);
			entity.setCreated(now);
			entity.setUpdated(now);
			mapper.insert(entity);
			return;
		}

		AliMiniAppSetting existing = mapper.selectById(effectiveSettingId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		existing.setAuthorizerAppid(authorizerAppid);
		existing.setMerchantPrivateKey(merchantPrivateKey);
		existing.setApiSignMethod(apiSignMethod);
		applyOptionalStrings(existing, params);
		existing.setUpdated(now);
		mapper.updateById(existing);

		try {
			Map<String, Object> row = AliMiniAppSettingRowMapper.toSnakeRow(existing);
			String json = objectMapper.writeValueAsString(row);
			String key = String.format(CACHE_KEY_PATTERN, existing.getCompanyId());
			companysRedisTemplate.opsForValue().set(key, json);
		} catch (JsonProcessingException e) {
			throw new ResourceException("缓存写入失败");
		}
	}

	private static void applyOptionalStrings(AliMiniAppSetting entity, Map<String, Object> params) {
		if (params.containsKey("alipay_cert_path")) {
			entity.setAlipayCertPath(stringValue(params.get("alipay_cert_path")));
		}
		if (params.containsKey("alipay_root_cert_path")) {
			entity.setAlipayRootCertPath(stringValue(params.get("alipay_root_cert_path")));
		}
		if (params.containsKey("merchant_cert_path")) {
			entity.setMerchantCertPath(stringValue(params.get("merchant_cert_path")));
		}
		if (params.containsKey("alipay_public_key")) {
			entity.setAlipayPublicKey(stringValue(params.get("alipay_public_key")));
		}
		if (params.containsKey("notify_url")) {
			entity.setNotifyUrl(stringValue(params.get("notify_url")));
		}
		if (params.containsKey("encrypt_key")) {
			entity.setEncryptKey(stringValue(params.get("encrypt_key")));
		}
	}

	private static String stringValue(Object o) {
		if (o == null) {
			return null;
		}
		return String.valueOf(o);
	}

	private static String stringParam(Map<String, Object> params, String key) {
		Object v = params.get(key);
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static String requiredStringFromParams(Map<String, Object> params, String key) {
		Object v = params.get(key);
		return v == null ? "" : String.valueOf(v);
	}

	private static Long parseNonEmptySettingId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("setting_id 格式错误");
		}
	}
}
