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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesAdPlaceAuditRequest;
import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceMapper;
import cn.shopex.ecshopx.theme.support.PagesAdPlaceColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PagesAdPlaceAuditService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	private final PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper;

	public PagesAdPlaceAuditService(
			PagesAdPlaceMapper pagesAdPlaceMapper,
			PagesAdPlaceColumnNamesDataMapper pagesAdPlaceColumnNamesDataMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
		this.pagesAdPlaceColumnNamesDataMapper = pagesAdPlaceColumnNamesDataMapper;
	}

	public Map<String, Object> audit(
			long companyId, long adPlaceId, Long sourceIdFilter, PagesAdPlaceAuditRequest body) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<PagesAdPlace> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(PagesAdPlace::getCompanyId, companyId).eq(PagesAdPlace::getId, adPlaceId);
		if (sourceIdFilter == null) {
			updateWrapper.isNull(PagesAdPlace::getSourceId);
		} else {
			updateWrapper.eq(PagesAdPlace::getSourceId, sourceIdFilter);
		}
		updateWrapper.set(PagesAdPlace::getAuditStatus, body.getAuditStatus().trim());
		if ("rejected".equals(body.getAuditStatus())) {
			updateWrapper.set(PagesAdPlace::getAuditRemark, body.getAuditRemark().trim());
		} else if ("approved".equals(body.getAuditStatus()) && StringUtils.hasText(body.getAuditRemark())) {
			updateWrapper.set(PagesAdPlace::getAuditRemark, body.getAuditRemark().trim());
		}
		updateWrapper.set(PagesAdPlace::getUpdated, now);

		int rows = pagesAdPlaceMapper.update(null, updateWrapper);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		LambdaQueryWrapper<PagesAdPlace> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PagesAdPlace::getCompanyId, companyId).eq(PagesAdPlace::getId, adPlaceId);
		if (sourceIdFilter == null) {
			queryWrapper.isNull(PagesAdPlace::getSourceId);
		} else {
			queryWrapper.eq(PagesAdPlace::getSourceId, sourceIdFilter);
		}
		PagesAdPlace entity = pagesAdPlaceMapper.selectOne(queryWrapper);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return pagesAdPlaceColumnNamesDataMapper.toColumnNamesData(entity);
	}
}
