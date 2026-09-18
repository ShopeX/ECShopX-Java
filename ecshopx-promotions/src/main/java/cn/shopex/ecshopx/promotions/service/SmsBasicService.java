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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsBasicService {

	private final CompanysMapper companysMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;

	public SmsBasicService(
			CompanysMapper companysMapper,
			PrismCoreHttpClient prismCoreHttpClient,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSmsBasic(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		String passportUid;
		if (c == null || c.getPassportUid() == null || !StringUtils.hasText(c.getPassportUid().trim())) {
			passportUid = null;
		} else {
			passportUid = c.getPassportUid().trim();
		}
		ShopexPrismSmsSignClient client =
				new ShopexPrismSmsSignClient(prismCoreHttpClient, prismRedisTemplate, objectMapper, companyId, passportUid);
		try {
			Object remainder = client.getSmsRemainder();
			String buyUrl = client.getSmsBuyUrl();
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("sms_remainder", remainder);
			data.put("sms_buy_url", buyUrl);
			return data;
		} catch (ForbiddenException ex) {
			throw new ResourceException(ex.getMessage(), 403);
		}
	}
}
