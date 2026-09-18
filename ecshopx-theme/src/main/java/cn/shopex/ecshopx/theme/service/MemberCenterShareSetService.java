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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.MemberCenterShareSetRequest;
import cn.shopex.ecshopx.theme.domain.ThemeMemberCenterShare;
import cn.shopex.ecshopx.theme.mapper.ThemeMemberCenterShareMapper;
import cn.shopex.ecshopx.theme.support.ThemeMemberCenterShareColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MemberCenterShareSetService {

	private final ThemeMemberCenterShareMapper themeMemberCenterShareMapper;

	private final ThemeMemberCenterShareColumnNamesDataMapper themeMemberCenterShareColumnNamesDataMapper;

	public Object getInfo(long companyId) {
		LambdaQueryWrapper<ThemeMemberCenterShare> w = new LambdaQueryWrapper<>();
		w.eq(ThemeMemberCenterShare::getCompanyId, companyId);
		ThemeMemberCenterShare row = themeMemberCenterShareMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		return themeMemberCenterShareColumnNamesDataMapper.toColumnNamesData(row);
	}

	public Map<String, Object> set(long companyId, MemberCenterShareSetRequest request) {
		String picWechat = request.getSharePicWechatapp() == null ? "" : request.getSharePicWechatapp();
		String picH5 = request.getSharePicH5() == null ? "" : request.getSharePicH5();

		LambdaQueryWrapper<ThemeMemberCenterShare> w = new LambdaQueryWrapper<>();
		w.eq(ThemeMemberCenterShare::getCompanyId, companyId);
		ThemeMemberCenterShare existing = themeMemberCenterShareMapper.selectOne(w);

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			ThemeMemberCenterShare entity = new ThemeMemberCenterShare();
			entity.setCompanyId(companyId);
			entity.setShareTitle(request.getShareTitle());
			entity.setShareDescription(request.getShareDescription());
			entity.setSharePicWechatapp(picWechat);
			entity.setSharePicH5(picH5);
			entity.setCreated(now);
			entity.setUpdated(now);
			themeMemberCenterShareMapper.insert(entity);
			return themeMemberCenterShareColumnNamesDataMapper.toColumnNamesData(entity);
		}

		existing.setShareTitle(request.getShareTitle());
		existing.setShareDescription(request.getShareDescription());
		existing.setSharePicWechatapp(picWechat);
		existing.setSharePicH5(picH5);
		existing.setUpdated(now);
		int rows = themeMemberCenterShareMapper.updateById(existing);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return themeMemberCenterShareColumnNamesDataMapper.toColumnNamesData(existing);
	}
}
