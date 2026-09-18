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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaAuthorizerAppIdByTemplateService {

	private final WeappMapper weappMapper;
	private final MessageSource messageSource;

	public WxaAuthorizerAppIdByTemplateService(WeappMapper weappMapper, MessageSource messageSource) {
		this.weappMapper = weappMapper;
		this.messageSource = messageSource;
	}

	public String requireAuthorizerAppid(long companyId, String templateName) {
		String tn = templateName == null ? "" : templateName.trim();
		if (!StringUtils.hasText(tn)) {
			String msg =
					messageSource.getMessage(
							"promotions.liverooms.get_list_error", null, LocaleContextHolder.getLocale());
			throw new ResourceException(
					msg, Map.of("template_name", List.of("validation.required_without")));
		}

		Weapp row = findWeappRow(companyId, tn);
		if (row == null || !StringUtils.hasText(row.getAuthorizerAppid())) {
			throw new ResourceException("未绑定小程序,稍后配置");
		}
		return row.getAuthorizerAppid().trim();
	}

	public String requireAuthorizerAppidForPcLogin(long companyId) {
		Weapp row = findWeappRow(companyId, "yykweishop");
		String appid = row == null ? null : row.getAuthorizerAppid();
		if (appid == null || !StringUtils.hasText(appid)) {
			throw new ResourceException("没有开通小程序");
		}
		return appid.trim();
	}

	private Weapp findWeappRow(long companyId, String templateName) {
		String tn = templateName == null ? "" : templateName.trim();
		if (!StringUtils.hasText(tn)) {
			return null;
		}
		LambdaQueryWrapper<Weapp> w = new LambdaQueryWrapper<>();
		w.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getTemplateName, tn)
				.isNull(Weapp::getDeletedAt)
				.last("LIMIT 1");
		return weappMapper.selectOne(w);
	}
}
