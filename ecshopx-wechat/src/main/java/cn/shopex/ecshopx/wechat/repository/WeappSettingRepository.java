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

package cn.shopex.ecshopx.wechat.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.mapper.WeappSettingMapper;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class WeappSettingRepository {

	private final WeappSettingMapper mapper;

	public WeappSettingRepository(WeappSettingMapper mapper) {
		this.mapper = mapper;
	}

	public List<WeappSetting> listByCompanyTemplateAndPageName(long companyId, String templateName, String pageName) {
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId)
				.eq(WeappSetting::getTemplateName, templateName)
				.eq(WeappSetting::getPageName, pageName)
				.orderByAsc(WeappSetting::getSortBy)
				.orderByAsc(WeappSetting::getId);
		return mapper.selectList(w);
	}

	public List<WeappSetting> listForAdminPageParams(
			long companyId,
			String templateName,
			String pageName,
			String version,
			String configNameFilterOrNull) {
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId)
				.eq(WeappSetting::getTemplateName, templateName)
				.eq(WeappSetting::getPageName, pageName)
				.eq(WeappSetting::getVersion, version);
		if (configNameFilterOrNull != null) {
			w.eq(WeappSetting::getName, configNameFilterOrNull);
		}
		w.orderByAsc(WeappSetting::getSortBy).orderByAsc(WeappSetting::getId);
		return mapper.selectList(w);
	}

	public List<WeappSetting> listForMemberCenterPages(
			long companyId, String templateName, String version, List<String> pageNames) {
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId)
				.eq(WeappSetting::getTemplateName, templateName)
				.eq(WeappSetting::getVersion, version)
				.in(WeappSetting::getPageName, pageNames)
				.orderByAsc(WeappSetting::getSortBy)
				.orderByAsc(WeappSetting::getId);
		return mapper.selectList(w);
	}

	public List<Long> listIdsByCompanyTemplatePageNameVersion(
			long companyId, String templateName, String pageName, String version) {
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId)
				.eq(WeappSetting::getTemplateName, templateName)
				.eq(WeappSetting::getPageName, pageName)
				.eq(WeappSetting::getVersion, version)
				.orderByAsc(WeappSetting::getSortBy)
				.orderByAsc(WeappSetting::getId);
		return mapper.selectList(w).stream().map(WeappSetting::getId).collect(Collectors.toList());
	}

	public void updateRowByIdWithSerializedParams(
			long id,
			long companyId,
			String templateName,
			String pageName,
			String version,
			String name,
			Map<String, Object> params,
			int sortBy,
			int pagesTemplateId) {
		WeappSetting existing = mapper.selectById(id);
		if (existing == null
				|| !Objects.equals(existing.getCompanyId(), companyId)
				|| !Objects.equals(existing.getTemplateName(), templateName)
				|| !Objects.equals(existing.getPageName(), pageName)
				|| !Objects.equals(existing.getVersion(), version)) {
			throw new ResourceException("配置不存在");
		}
		String serialized = WeappSettingLegacySerializeCodec.serialize(params);
		existing.setName(name);
		existing.setPageName(pageName);
		existing.setTemplateName(templateName);
		existing.setVersion(version);
		existing.setSortBy(sortBy);
		existing.setPagesTemplateId(pagesTemplateId);
		existing.setParams(serialized);
		mapper.updateById(existing);
	}

	public void deleteByIdsForCompany(List<Long> ids, long companyId) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId).in(WeappSetting::getId, ids);
		mapper.delete(w);
	}

	public int deleteHardByCompanyTemplateAndPageName(long companyId, String templateName, String pageName) {
		LambdaQueryWrapper<WeappSetting> w = new LambdaQueryWrapper<>();
		w.eq(WeappSetting::getCompanyId, companyId)
				.eq(WeappSetting::getTemplateName, templateName)
				.eq(WeappSetting::getPageName, pageName);
		return mapper.delete(w);
	}

	public void updateParamsById(long companyId, Long id, String serializedParams) {
		LambdaUpdateWrapper<WeappSetting> uw = new LambdaUpdateWrapper<>();
		uw.set(WeappSetting::getParams, serializedParams).eq(WeappSetting::getCompanyId, companyId);
		if (id == null) {
			uw.isNull(WeappSetting::getId);
		} else {
			uw.eq(WeappSetting::getId, id);
		}
		mapper.update(null, uw);
	}

	public void insertWithParamsIdRewrite(
			long companyId,
			String templateName,
			String pageName,
			String configName,
			Map<String, Object> params,
			String version,
			Integer pagesTemplateId,
			Integer sortBy) {
		String paramsSerialized = WeappSettingLegacySerializeCodec.serialize(params);
		WeappSetting entity = new WeappSetting();
		entity.setCompanyId(companyId);
		entity.setTemplateName(templateName);
		entity.setPageName(pageName);
		entity.setName(configName);
		entity.setVersion(version != null && !version.isBlank() ? version : "v1.0.0");
		entity.setParams(paramsSerialized);
		entity.setPagesTemplateId(pagesTemplateId != null ? pagesTemplateId : 0);
		entity.setSortBy(sortBy != null ? sortBy : 0);
		mapper.insert(entity);
		params.put("id", entity.getId());
		entity.setParams(WeappSettingLegacySerializeCodec.serialize(params));
		mapper.updateById(entity);
	}
}
