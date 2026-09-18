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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreatePagesAdPlaceRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesAdPlaceRelTagItemRequest;
import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.domain.PagesAdPlaceRelDistributors;
import cn.shopex.ecshopx.theme.domain.PagesAdPlaceRelMemberTags;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelDistributorsMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelMemberTagsMapper;
import cn.shopex.ecshopx.theme.support.PagesAdPlaceColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class PagesAdPlaceUpdateService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	private final PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper;

	private final PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper;

	private final PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper;

	public PagesAdPlaceUpdateService(
			PagesAdPlaceMapper pagesAdPlaceMapper,
			PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper,
			PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper,
			PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
		this.pagesAdPlaceRelMemberTagsMapper = pagesAdPlaceRelMemberTagsMapper;
		this.pagesAdPlaceRelDistributorsMapper = pagesAdPlaceRelDistributorsMapper;
		this.pagesAdPlaceColumnNamesDataMapper = pagesAdPlaceColumnNamesDataMapper;
	}

	public Map<String, Object> update(
			long companyId,
			long adPlaceId,
			long sourceIdFilter,
			List<Long> distributorIdsForRel,
			CreatePagesAdPlaceRequest request) {
		try {
			List<Long> ids =
					distributorIdsForRel == null ? List.of() : new ArrayList<>(distributorIdsForRel);
			ids.removeIf(Objects::isNull);
			ids.removeIf(v -> v == 0L);
			boolean hasDistributors = !ids.isEmpty();
			int useBound = hasDistributors ? 1 : 0;

			LambdaQueryWrapper<PagesAdPlace> loadQ = new LambdaQueryWrapper<>();
			loadQ.eq(PagesAdPlace::getCompanyId, companyId)
					.eq(PagesAdPlace::getId, adPlaceId)
					.eq(PagesAdPlace::getSourceId, sourceIdFilter);
			PagesAdPlace entity = pagesAdPlaceMapper.selectOne(loadQ);
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}

			if (request.getRegionauthId() != null) {
				entity.setRegionauthId(request.getRegionauthId());
			}
			if (request.getAdType() != null) {
				entity.setAdType(request.getAdType());
			}
			if (request.getName() != null) {
				entity.setName(request.getName());
			}
			if (request.getPages() != null) {
				entity.setPages(request.getPages());
			}
			if (request.getStartTime() != null) {
				entity.setStartTime(request.getStartTime());
			}
			if (request.getEndTime() != null) {
				entity.setEndTime(request.getEndTime());
			}
			if (request.getSetting() != null) {
				entity.setSetting(request.getSetting());
			}
			if (request.getAutoPlay() != null) {
				entity.setAutoPlay(request.getAutoPlay());
			}
			if (request.getPlayInterval() != null) {
				entity.setPlayInterval(request.getPlayInterval());
			}
			if (request.getAutoClose() != null) {
				entity.setAutoClose(request.getAutoClose());
			}
			if (request.getCloseDelay() != null) {
				entity.setCloseDelay(request.getCloseDelay());
			}
			if (request.getAuditRemark() != null) {
				entity.setAuditRemark(request.getAuditRemark());
			}
			if (request.getSort() != null) {
				entity.setSort(request.getSort());
			}
			if (request.getTrackingCode() != null) {
				entity.setTrackingCode(request.getTrackingCode());
			}

			entity.setUseBound(useBound);
			entity.setAuditStatus("submitting");

			int now = (int) (System.currentTimeMillis() / 1000L);
			entity.setUpdated(now);

			int affected = pagesAdPlaceMapper.updateById(entity);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}

			LambdaQueryWrapper<PagesAdPlaceRelMemberTags> delTags = new LambdaQueryWrapper<>();
			delTags.eq(PagesAdPlaceRelMemberTags::getCompanyId, companyId)
					.eq(PagesAdPlaceRelMemberTags::getAdPlaceId, adPlaceId);
			pagesAdPlaceRelMemberTagsMapper.delete(delTags);

			LambdaQueryWrapper<PagesAdPlaceRelDistributors> delDist = new LambdaQueryWrapper<>();
			delDist.eq(PagesAdPlaceRelDistributors::getCompanyId, companyId)
					.eq(PagesAdPlaceRelDistributors::getAdPlaceId, adPlaceId);
			pagesAdPlaceRelDistributorsMapper.delete(delDist);

			if (request.getRelTags() != null && !request.getRelTags().isEmpty()) {
				for (PagesAdPlaceRelTagItemRequest t : request.getRelTags()) {
					PagesAdPlaceRelMemberTags row = new PagesAdPlaceRelMemberTags();
					row.setCompanyId(companyId);
					row.setAdPlaceId(adPlaceId);
					row.setTagId(t.getTagId());
					pagesAdPlaceRelMemberTagsMapper.insert(row);
				}
			}

			if (hasDistributors) {
				for (Long did : ids) {
					PagesAdPlaceRelDistributors row = new PagesAdPlaceRelDistributors();
					row.setCompanyId(companyId);
					row.setAdPlaceId(adPlaceId);
					row.setDistributorId(did);
					pagesAdPlaceRelDistributorsMapper.insert(row);
				}
			}

			LambdaQueryWrapper<PagesAdPlace> reloadQ = new LambdaQueryWrapper<>();
			reloadQ.eq(PagesAdPlace::getCompanyId, companyId)
					.eq(PagesAdPlace::getId, adPlaceId)
					.eq(PagesAdPlace::getSourceId, sourceIdFilter);
			PagesAdPlace refreshed = pagesAdPlaceMapper.selectOne(reloadQ);
			if (refreshed == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return pagesAdPlaceColumnNamesDataMapper.toColumnNamesData(refreshed);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}
}
