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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateAddRequest;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.support.PagesTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class PagesTemplateAddService {

	private final PagesTemplateSetMapper pagesTemplateSetMapper;

	private final PagesTemplateMapper pagesTemplateMapper;

	private final PagesTemplateRowMapper pagesTemplateRowMapper;

	public PagesTemplateAddService(
			PagesTemplateSetMapper pagesTemplateSetMapper,
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateRowMapper pagesTemplateRowMapper) {
		this.pagesTemplateSetMapper = pagesTemplateSetMapper;
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateRowMapper = pagesTemplateRowMapper;
	}

	public Map<String, Object> add(long companyId, PagesTemplateAddRequest req) {
		String wp = req.getWeappPages();
		if (wp == null || !StringUtils.hasText(wp.trim())) {
			wp = "index";
		} else {
			wp = wp.trim();
		}
		int distributorId = req.getDistributorId() == null ? 0 : req.getDistributorId();
		if (distributorId > 0 && "index".equals(wp)) {
			wp = "distributor_index";
		}

		String title = req.getTemplateTitle();
		if (title == null || !StringUtils.hasText(title.trim())) {
			throw new BadRequestException("缺少模板名称");
		}
		String name = req.getTemplateName();
		if (name == null || !StringUtils.hasText(name.trim())) {
			throw new BadRequestException("缺少模板展示类型");
		}
		title = title.trim();
		name = name.trim();

		long regionauthId = req.getRegionauthId() == null ? 0L : req.getRegionauthId();
		int templateType = distributorId == 0 ? 0 : 2;

		PagesTemplateSet existingSet =
				pagesTemplateSetMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplateSet>()
								.eq(PagesTemplateSet::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (existingSet == null) {
			PagesTemplateSet set = new PagesTemplateSet();
			set.setCompanyId(companyId);
			set.setIndexType(1);
			pagesTemplateSetMapper.insert(set);
		}

		Long count =
				pagesTemplateMapper.selectCount(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.isNull(PagesTemplate::getDeletedAt));
		boolean firstForCompany = count == null || count == 0L;

		PagesTemplate entity = new PagesTemplate();
		entity.setCompanyId(companyId);
		entity.setRegionauthId(regionauthId);
		entity.setDistributorId(distributorId);
		entity.setTemplateTitle(title);
		entity.setTemplateName(name);
		entity.setTemplatePic(req.getTemplatePic());
		entity.setTemplateType(templateType);
		entity.setWeappPages(wp);
		if (firstForCompany) {
			entity.setStatus(1);
			entity.setTemplateStatusModifyTime((int) (System.currentTimeMillis() / 1000L));
		}

		int inserted = pagesTemplateMapper.insert(entity);
		if (inserted != 1) {
			throw new ResourceException("未查询到更新数据");
		}
		return pagesTemplateRowMapper.toRowMap(entity);
	}
}
