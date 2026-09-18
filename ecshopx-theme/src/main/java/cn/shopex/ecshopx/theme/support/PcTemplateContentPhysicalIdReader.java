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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PcTemplateContentPhysicalIdReader {

	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;

	public List<Long> listIdsByCompanyAndTemplate(long companyId, long themePcTemplateId) {
		LambdaQueryWrapper<ThemePcTemplateContent> w =
				new LambdaQueryWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getCompanyId, companyId)
						.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId)
						.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId);
		List<ThemePcTemplateContent> rows = themePcTemplateContentMapper.selectList(w);
		List<Long> ids = new ArrayList<>(rows.size());
		for (ThemePcTemplateContent row : rows) {
			Long id = row.getThemePcTemplateContentId();
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}
}
