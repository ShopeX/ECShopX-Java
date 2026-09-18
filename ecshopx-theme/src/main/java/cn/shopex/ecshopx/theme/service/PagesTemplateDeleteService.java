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
import cn.shopex.ecshopx.goods.service.ItemsGroupDelGroupDataService;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagesTemplateDeleteService {

	private static final ZoneId SHANGHAI_SOFT_DELETE_ZONE = ZoneId.of("Asia/Shanghai");

	private final PagesTemplateMapper pagesTemplateMapper;

	private final ItemsGroupDelGroupDataService itemsGroupDelGroupDataService;

	public void delete(long companyId, long pagesTemplateId) {
		PagesTemplate info =
				pagesTemplateMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
								.isNull(PagesTemplate::getDeletedAt));
		if (info == null) {
			throw new ResourceException("未查询到更新数据");
		}
		Integer st = info.getStatus();
		if (st != null && st.intValue() == 1) {
			throw new ResourceException("当前模板为启用状态，无法删除");
		}
		LocalDateTime deletedAt = LocalDate.now(SHANGHAI_SOFT_DELETE_ZONE).atStartOfDay();
		LambdaUpdateWrapper<PagesTemplate> uw =
				new LambdaUpdateWrapper<PagesTemplate>()
						.set(PagesTemplate::getDeletedAt, deletedAt)
						.eq(PagesTemplate::getCompanyId, companyId)
						.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
						.isNull(PagesTemplate::getDeletedAt);
		int rows = pagesTemplateMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		String groupKey = "widget-" + pagesTemplateId;
		itemsGroupDelGroupDataService.delGroupData(groupKey);
	}
}
