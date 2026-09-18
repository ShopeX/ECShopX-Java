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
import cn.shopex.ecshopx.promotions.domain.SmsTemplate;
import cn.shopex.ecshopx.promotions.mapper.SmsIdiographMapper;
import cn.shopex.ecshopx.promotions.mapper.SmsTemplateMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRegistry;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRow;
import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsTemplateDetailService {

	private final Environment environment;
	private final CompanysMapper companysMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final SmsTemplateMapper smsTemplateMapper;
	private final SmsDefaultTemplateRegistry defaultTemplateRegistry;
	private final SmsIdiographMapper smsIdiographMapper;

	public SmsTemplateDetailService(
			Environment environment,
			CompanysMapper companysMapper,
			PrismCoreHttpClient prismCoreHttpClient,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			SmsTemplateMapper smsTemplateMapper,
			SmsDefaultTemplateRegistry defaultTemplateRegistry,
			SmsIdiographMapper smsIdiographMapper) {
		this.environment = environment;
		this.companysMapper = companysMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.smsTemplateMapper = smsTemplateMapper;
		this.defaultTemplateRegistry = defaultTemplateRegistry;
		this.smsIdiographMapper = smsIdiographMapper;
	}

	public Map<String, Object> getSmsTemplateDetail(long companyId, String tmplName) {
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

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		SmsTemplate row =
				smsTemplateMapper.selectOne(
						new LambdaQueryWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, tmplName)
								.last("LIMIT 1"));
		if (row != null) {
			body.put("company_id", row.getCompanyId());
			body.put("sms_type", row.getSmsType());
			body.put("tmpl_type", row.getTmplType());
			body.put("content", row.getContent());
			body.put("is_open", row.getIsOpen());
			body.put("tmpl_name", row.getTmplName());
			String raw = row.getSendTimeDesc();
			if (raw == null || !StringUtils.hasText(raw.trim())) {
				body.put("send_time_desc", null);
			} else {
				try {
					body.put("send_time_desc", objectMapper.readValue(raw.trim(), Object.class));
				} catch (JsonProcessingException e) {
					body.put("send_time_desc", null);
				}
			}
			body.put("created", row.getCreated());
		} else {
			Optional<SmsDefaultTemplateRow> def = defaultTemplateRegistry.getByName(tmplName);
			if (def.isPresent()) {
				SmsDefaultTemplateRow d = def.get();
				body.put("content", d.content());
				body.put("tmpl_type", d.tmplType());
				body.put("sms_type", d.smsType());
				body.put("tmpl_name", d.tmplName());
				body.put("send_time_desc", d.sendTimeDesc());
				body.put("is_open", d.builtinIsOpen());
			}
		}

		SmsIdiograph sig =
				smsIdiographMapper.selectOne(
						new LambdaQueryWrapper<SmsIdiograph>()
								.eq(SmsIdiograph::getCompanyId, companyId)
								.last("LIMIT 1"));
		String sign = sig == null ? null : sig.getIdiograph();
		body.put("sign", sign);
		return body;
	}
}
