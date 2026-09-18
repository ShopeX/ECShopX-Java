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
import cn.shopex.ecshopx.promotions.domain.SmsIdiograph;
import cn.shopex.ecshopx.promotions.mapper.SmsIdiographMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SmsSignQueryService {

	private final CompanysMapper companysMapper;
	private final SmsIdiographMapper smsIdiographMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final ObjectMapper objectMapper;
	private final Environment environment;
	private final StringRedisTemplate prismRedisTemplate;

	public SmsSignQueryService(
			CompanysMapper companysMapper,
			SmsIdiographMapper smsIdiographMapper,
			PrismCoreHttpClient prismCoreHttpClient,
			ObjectMapper objectMapper,
			Environment environment,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.companysMapper = companysMapper;
		this.smsIdiographMapper = smsIdiographMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.objectMapper = objectMapper;
		this.environment = environment;
		this.prismRedisTemplate = prismRedisTemplate;
	}

	public Map<String, Object> getSmsSign(long companyId) {
		boolean oemShuyun = SmsOemShuyunFlags.isOemShuyun(environment);
		if (!oemShuyun) {
			Companys c = companysMapper.selectById(companyId);
			String passportUid;
			if (c == null || c.getPassportUid() == null) {
				passportUid = null;
			} else {
				String t = c.getPassportUid().trim();
				passportUid = t.isEmpty() ? null : t;
			}
			ShopexPrismSmsSignClient client =
					new ShopexPrismSmsSignClient(
							prismCoreHttpClient, prismRedisTemplate, objectMapper, companyId, passportUid);
			try {
				client.primeSession();
			} catch (ForbiddenException ex) {
				throw new ResourceException(ex.getMessage(), 403);
			}
		}
		LambdaQueryWrapper<SmsIdiograph> w =
				new LambdaQueryWrapper<SmsIdiograph>().eq(SmsIdiograph::getCompanyId, companyId).last("LIMIT 1");
		SmsIdiograph row = smsIdiographMapper.selectOne(w);
		String sign = row == null ? null : row.getIdiograph();
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("sign", sign);
		return data;
	}
}
