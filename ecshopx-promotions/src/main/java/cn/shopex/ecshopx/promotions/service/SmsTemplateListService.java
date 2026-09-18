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
import cn.shopex.ecshopx.promotions.domain.SmsTemplate;
import cn.shopex.ecshopx.promotions.mapper.SmsTemplateMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRegistry;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRow;
import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsTemplateListService {

	private final Environment environment;
	private final CompanysMapper companysMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final SmsTemplateMapper smsTemplateMapper;
	private final SmsDefaultTemplateRegistry defaultTemplateRegistry;

	public SmsTemplateListService(
			Environment environment,
			CompanysMapper companysMapper,
			PrismCoreHttpClient prismCoreHttpClient,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			SmsTemplateMapper smsTemplateMapper,
			SmsDefaultTemplateRegistry defaultTemplateRegistry) {
		this.environment = environment;
		this.companysMapper = companysMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.smsTemplateMapper = smsTemplateMapper;
		this.defaultTemplateRegistry = defaultTemplateRegistry;
	}

	public Map<String, Object> getSmsTemplateList(long companyId) {
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

		LambdaQueryWrapper<SmsTemplate> w =
				new LambdaQueryWrapper<SmsTemplate>()
						.eq(SmsTemplate::getCompanyId, companyId)
						.orderByDesc(SmsTemplate::getCreated);
		Page<SmsTemplate> page = new Page<>(1, 100);
		smsTemplateMapper.selectPage(page, w);

		LinkedHashMap<String, SmsTemplate> byTmplName = new LinkedHashMap<>();
		for (SmsTemplate r : page.getRecords()) {
			byTmplName.put(r.getTmplName(), r);
		}

		LinkedHashMap<String, SmsDefaultTemplateRow> defaults =
				defaultTemplateRegistry.orderedDefaultTemplateRows();

		LinkedHashMap<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
		for (Map.Entry<String, SmsDefaultTemplateRow> e : defaults.entrySet()) {
			String tmplName = e.getKey();
			SmsDefaultTemplateRow def = e.getValue();
			String tmplType = def.tmplType();
			List<Map<String, Object>> bucket = grouped.computeIfAbsent(tmplType, k -> new ArrayList<>());
			if (byTmplName.containsKey(tmplName)) {
				SmsTemplate row = byTmplName.get(tmplName);
				bucket.add(dbRowToMap(row));
			} else {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("content", def.content());
				m.put("tmpl_type", def.tmplType());
				m.put("sms_type", def.smsType());
				m.put("tmpl_name", def.tmplName());
				m.put("send_time_desc", def.sendTimeDesc());
				Boolean bi = def.builtinIsOpen();
				if (bi != null && Boolean.TRUE.equals(bi)) {
					m.put("is_open", "true");
				} else if (bi != null && Boolean.FALSE.equals(bi)) {
					m.put("is_open", Boolean.FALSE);
				} else {
					m.put("is_open", null);
				}
				bucket.add(m);
			}
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("list", grouped);
		return data;
	}

	private LinkedHashMap<String, Object> dbRowToMap(SmsTemplate row) {
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
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
		return body;
	}
}
