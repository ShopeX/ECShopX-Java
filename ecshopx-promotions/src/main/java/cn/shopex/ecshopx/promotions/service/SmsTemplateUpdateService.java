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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.SmsTemplate;
import cn.shopex.ecshopx.promotions.mapper.SmsTemplateMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRegistry;
import cn.shopex.ecshopx.promotions.service.sms.SmsDefaultTemplateRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsTemplateUpdateService {

	private final SmsTemplateMapper smsTemplateMapper;
	private final SmsDefaultTemplateRegistry defaultTemplateRegistry;
	private final ObjectMapper objectMapper;
	private final boolean oemShuyun;

	public SmsTemplateUpdateService(
			SmsTemplateMapper smsTemplateMapper,
			SmsDefaultTemplateRegistry defaultTemplateRegistry,
			ObjectMapper objectMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.smsTemplateMapper = smsTemplateMapper;
		this.defaultTemplateRegistry = defaultTemplateRegistry;
		this.objectMapper = objectMapper;
		this.oemShuyun = oemShuyun;
	}

	public void updateSmsTemplate(
			long companyId,
			String templateName,
			String isOpenForDb,
			Optional<String> contentOptional) {
		if (contentOptional.isPresent()) {
			String c = contentOptional.get();
			int len = c.codePointCount(0, c.length());
			if (len < 1 || len > 500) {
				throw new BadRequestException("模板内容有效长度1-500个字符");
			}
		}

		SmsTemplate row =
				smsTemplateMapper.selectOne(
						new LambdaQueryWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, templateName)
								.last("LIMIT 1"));

		Optional<SmsDefaultTemplateRow> defOpt = defaultTemplateRegistry.getByName(templateName);
		SmsDefaultTemplateRow def = defOpt.orElse(null);

		if (row != null) {
			if (oemShuyun) {
				if (def == null || def.sendTimeDesc() == null) {
					throw new ResourceException("模板配置异常");
				}
				String jsonDesc = serializeSendTimeDesc(def.sendTimeDesc());
				LambdaUpdateWrapper<SmsTemplate> uw =
						new LambdaUpdateWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, templateName)
								.set(SmsTemplate::getIsOpen, isOpenForDb)
								.set(SmsTemplate::getSendTimeDesc, jsonDesc);
				if (contentOptional.isPresent()) {
					uw.set(SmsTemplate::getContent, contentOptional.get());
				}
				smsTemplateMapper.update(null, uw);
			} else {
				LambdaUpdateWrapper<SmsTemplate> uw =
						new LambdaUpdateWrapper<SmsTemplate>()
								.eq(SmsTemplate::getCompanyId, companyId)
								.eq(SmsTemplate::getTmplName, templateName)
								.set(SmsTemplate::getIsOpen, isOpenForDb);
				if (contentOptional.isPresent()) {
					uw.set(SmsTemplate::getContent, contentOptional.get());
				}
				smsTemplateMapper.update(null, uw);
			}
			return;
		}

		if (def == null) {
			throw new ResourceException("未知短信模板");
		}

		SmsTemplate entity = new SmsTemplate();
		entity.setCompanyId(companyId);
		entity.setSmsType(def.smsType());
		entity.setTmplType(def.tmplType());
		entity.setTmplName(def.tmplName());
		entity.setContent(contentOptional.orElse(def.content()));
		entity.setIsOpen(isOpenForDb);
		if (def.sendTimeDesc() == null) {
			entity.setSendTimeDesc(null);
		} else {
			entity.setSendTimeDesc(serializeSendTimeDesc(def.sendTimeDesc()));
		}
		int nowEpoch = (int) Instant.now().getEpochSecond();
		entity.setCreated(nowEpoch);
		entity.setUpdated(null);
		smsTemplateMapper.insert(entity);
	}

	private String serializeSendTimeDesc(Object sendTimeDesc) {
		try {
			return objectMapper.writeValueAsString(sendTimeDesc);
		} catch (JsonProcessingException e) {
			throw new ResourceException("模板配置异常");
		}
	}
}
