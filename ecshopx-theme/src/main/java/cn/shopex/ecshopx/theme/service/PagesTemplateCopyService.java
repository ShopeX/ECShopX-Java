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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.support.PagesTemplateRowMapper;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.mapper.WeappSettingMapper;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class PagesTemplateCopyService {

	private final PagesTemplateMapper pagesTemplateMapper;

	private final WeappSettingMapper weappSettingMapper;

	private final PagesTemplateRowMapper pagesTemplateRowMapper;

	private final WeappSettingParamsI18nReader weappSettingParamsI18nReader;

	public PagesTemplateCopyService(
			PagesTemplateMapper pagesTemplateMapper,
			WeappSettingMapper weappSettingMapper,
			PagesTemplateRowMapper pagesTemplateRowMapper,
			WeappSettingParamsI18nReader weappSettingParamsI18nReader) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.weappSettingMapper = weappSettingMapper;
		this.pagesTemplateRowMapper = pagesTemplateRowMapper;
		this.weappSettingParamsI18nReader = weappSettingParamsI18nReader;
	}

	public Map<String, Object> copy(long companyId, Long sourcePagesTemplateId, String requestLocaleTag) {
		if (sourcePagesTemplateId == null) {
			throw new ResourceException("复制失败，无效的模板信息");
		}
		PagesTemplate source =
				pagesTemplateMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(PagesTemplate::getPagesTemplateId, sourcePagesTemplateId)
								.isNull(PagesTemplate::getDeletedAt)
								.last("LIMIT 1"));
		if (source == null) {
			throw new ResourceException("复制失败，无效的模板信息");
		}

		PagesTemplate created = new PagesTemplate();
		created.setCompanyId(source.getCompanyId());
		created.setRegionauthId(source.getRegionauthId());
		created.setDistributorId(source.getDistributorId());
		created.setTemplateName(source.getTemplateName());
		created.setTemplatePic(source.getTemplatePic());
		created.setTemplateType(source.getTemplateType());
		created.setElementEditStatus(source.getElementEditStatus());
		created.setWeappPages(source.getWeappPages());
		String baseTitle = source.getTemplateTitle();
		created.setTemplateTitle(
				(baseTitle == null ? "" : baseTitle) + "-" + "复制");

		int inserted = pagesTemplateMapper.insert(created);
		if (inserted != 1 || created.getPagesTemplateId() == null) {
			throw new ResourceException("复制失败，无效的模板信息");
		}

		String templateName = created.getTemplateName();
		String pageName = "index";
		String version = "v1.0.2";
		int sourceTemplateIdForWeapp = Math.toIntExact(sourcePagesTemplateId);

		LambdaQueryWrapper<WeappSetting> w =
				new LambdaQueryWrapper<WeappSetting>()
						.eq(WeappSetting::getCompanyId, companyId)
						.eq(WeappSetting::getTemplateName, templateName)
						.eq(WeappSetting::getPageName, pageName)
						.eq(WeappSetting::getVersion, version)
						.eq(WeappSetting::getPagesTemplateId, sourceTemplateIdForWeapp)
						.orderByAsc(WeappSetting::getSortBy)
						.orderByAsc(WeappSetting::getId);

		List<WeappSetting> rows = weappSettingMapper.selectList(w);
		if (rows != null && !rows.isEmpty()) {
			List<Long> ids = rows.stream().map(WeappSetting::getId).toList();
			Map<Long, String> langParamsByRowId =
					weappSettingParamsI18nReader.findParamsByLocale(companyId, ids, requestLocaleTag);
			int newTemplateIdForWeapp = Math.toIntExact(created.getPagesTemplateId());

			for (WeappSetting row : rows) {
				Object configParams = WeappSettingLegacySerializeCodec.decode(row.getParams());
				String langRaw = langParamsByRowId.get(row.getId());
				if (StringUtils.hasText(langRaw)) {
					configParams = WeappSettingLegacySerializeCodec.decode(langRaw);
				}
				if (!(configParams instanceof Map<?, ?> rawMap)) {
					throw new ResourceException("装修配置格式无效");
				}
				LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : rawMap.entrySet()) {
					paramsMap.put(String.valueOf(e.getKey()), e.getValue());
				}

				WeappSetting ins = new WeappSetting();
				ins.setCompanyId(companyId);
				ins.setTemplateName(templateName);
				ins.setPageName(pageName);
				ins.setName(row.getName());
				ins.setVersion(version);
				ins.setPagesTemplateId(newTemplateIdForWeapp);
				ins.setSortBy(row.getSortBy() == null ? 0 : row.getSortBy());
				ins.setParams(WeappSettingLegacySerializeCodec.serialize(paramsMap));
				weappSettingMapper.insert(ins);

				Long newId = ins.getId();
				paramsMap.put("id", newId.intValue());
				ins.setParams(WeappSettingLegacySerializeCodec.serialize(paramsMap));
				weappSettingMapper.updateById(ins);
			}
		}

		return pagesTemplateRowMapper.toRowMap(created);
	}
}
