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
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.mapper.WeappSettingMapper;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PagesTemplateStoreSyncApplyService {

	private final PagesTemplateMapper pagesTemplateMapper;

	private final WeappSettingMapper weappSettingMapper;

	private final WeappSettingParamsI18nReader weappSettingParamsI18nReader;

	public PagesTemplateStoreSyncApplyService(
			PagesTemplateMapper pagesTemplateMapper,
			WeappSettingMapper weappSettingMapper,
			WeappSettingParamsI18nReader weappSettingParamsI18nReader) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.weappSettingMapper = weappSettingMapper;
		this.weappSettingParamsI18nReader = weappSettingParamsI18nReader;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyOneDistributor(
			long companyId,
			long distributorId,
			int enforceSyncStatus,
			String templateTitle,
			String templatePic,
			String weappPagesFixed,
			Integer elementEditStatus,
			long headquartersPagesTemplateId,
			String templateName,
			String localeTag) {
		int distInt = safeIntDistributor(distributorId);
		if (enforceSyncStatus == 1) {
			pagesTemplateMapper.update(
					null,
					new LambdaUpdateWrapper<PagesTemplate>()
							.eq(PagesTemplate::getCompanyId, companyId)
							.eq(PagesTemplate::getDistributorId, distInt)
							.eq(PagesTemplate::getStatus, 1)
							.set(PagesTemplate::getStatus, 2)
							.set(
									PagesTemplate::getTemplateStatusModifyTime,
									(int) (System.currentTimeMillis() / 1000L)));
		}

		PagesTemplate created = new PagesTemplate();
		created.setCompanyId(companyId);
		created.setDistributorId(distInt);
		created.setTemplateTitle(templateTitle);
		created.setTemplatePic(templatePic);
		created.setTemplateType(1);
		created.setWeappPages(weappPagesFixed);
		created.setElementEditStatus(elementEditStatus);
		created.setTemplateName(templateName);
		created.setStatus(enforceSyncStatus == 1 ? 1 : 2);
		if (enforceSyncStatus == 1) {
			created.setTemplateStatusModifyTime((int) (System.currentTimeMillis() / 1000L));
		} else {
			created.setTemplateStatusModifyTime(null);
		}

		pagesTemplateMapper.insert(created);
		Long newPagesTemplateId = created.getPagesTemplateId();
		if (newPagesTemplateId == null) {
			throw new ResourceException("同步失败，门店模板主键未回填");
		}

		String pageName = "index";
		String version = "v1.0.2";
		int hqTemplateIdForWeapp = Math.toIntExact(headquartersPagesTemplateId);
		int newTemplateIdForWeapp = Math.toIntExact(newPagesTemplateId);

		LambdaQueryWrapper<WeappSetting> w =
				new LambdaQueryWrapper<WeappSetting>()
						.eq(WeappSetting::getCompanyId, companyId)
						.eq(WeappSetting::getTemplateName, templateName)
						.eq(WeappSetting::getPageName, pageName)
						.eq(WeappSetting::getVersion, version)
						.eq(WeappSetting::getPagesTemplateId, hqTemplateIdForWeapp)
						.orderByAsc(WeappSetting::getSortBy)
						.orderByAsc(WeappSetting::getId);

		List<WeappSetting> rows = weappSettingMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = rows.stream().map(WeappSetting::getId).toList();
		Map<Long, String> langParamsByRowId =
				weappSettingParamsI18nReader.findParamsByLocale(companyId, ids, localeTag);

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
			if (Objects.equals(elementEditStatus, 2)) {
				applyNoEditToConfig(paramsMap);
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

	private static void applyNoEditToConfig(LinkedHashMap<String, Object> paramsMap) {
		Object cfg = paramsMap.get("config");
		LinkedHashMap<String, Object> configInner;
		if (cfg instanceof Map<?, ?> m) {
			configInner = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				configInner.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else if (cfg == null) {
			configInner = new LinkedHashMap<>();
		} else {
			throw new ResourceException("装修配置格式无效");
		}
		configInner.put("no_edit", Boolean.TRUE);
		paramsMap.put("config", configInner);
	}

	private static int safeIntDistributor(long distributorId) {
		return (int) Math.min(Integer.MAX_VALUE, distributorId);
	}
}
