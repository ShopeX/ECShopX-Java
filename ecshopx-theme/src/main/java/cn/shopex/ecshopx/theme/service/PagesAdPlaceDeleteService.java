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
import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.domain.PagesAdPlaceRelDistributors;
import cn.shopex.ecshopx.theme.domain.PagesAdPlaceRelMemberTags;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelDistributorsMapper;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceRelMemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class PagesAdPlaceDeleteService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	private final PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper;

	private final PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper;

	public PagesAdPlaceDeleteService(
			PagesAdPlaceMapper pagesAdPlaceMapper,
			PagesAdPlaceRelMemberTagsMapper pagesAdPlaceRelMemberTagsMapper,
			PagesAdPlaceRelDistributorsMapper pagesAdPlaceRelDistributorsMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
		this.pagesAdPlaceRelMemberTagsMapper = pagesAdPlaceRelMemberTagsMapper;
		this.pagesAdPlaceRelDistributorsMapper = pagesAdPlaceRelDistributorsMapper;
	}

	public void delete(long companyId, long adPlaceId, Long sourceIdFilter) {
		try {
			LambdaQueryWrapper<PagesAdPlaceRelMemberTags> delTags = new LambdaQueryWrapper<>();
			delTags.eq(PagesAdPlaceRelMemberTags::getCompanyId, companyId)
					.eq(PagesAdPlaceRelMemberTags::getAdPlaceId, adPlaceId);
			pagesAdPlaceRelMemberTagsMapper.delete(delTags);

			LambdaQueryWrapper<PagesAdPlaceRelDistributors> delDist = new LambdaQueryWrapper<>();
			delDist.eq(PagesAdPlaceRelDistributors::getCompanyId, companyId)
					.eq(PagesAdPlaceRelDistributors::getAdPlaceId, adPlaceId);
			pagesAdPlaceRelDistributorsMapper.delete(delDist);

			LambdaQueryWrapper<PagesAdPlace> delMain = new LambdaQueryWrapper<>();
			delMain.eq(PagesAdPlace::getCompanyId, companyId).eq(PagesAdPlace::getId, adPlaceId);
			if (sourceIdFilter == null) {
				delMain.isNull(PagesAdPlace::getSourceId);
			} else {
				delMain.eq(PagesAdPlace::getSourceId, sourceIdFilter);
			}
			pagesAdPlaceMapper.delete(delMain);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}
}
