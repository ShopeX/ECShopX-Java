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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTemplateInfoService {

	private final TemplateMapper templateMapper;

	public AliyunsmsTemplateInfoService(TemplateMapper templateMapper) {
		this.templateMapper = templateMapper;
	}

	public Map<String, Object> getInfo(long companyId, Long id) {
		if (id == null) {
			return new LinkedHashMap<>();
		}
		Template row =
				templateMapper.selectOne(
						Wrappers.<Template>lambdaQuery()
								.eq(Template::getId, id)
								.eq(Template::getCompanyId, companyId));
		if (row == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("company_id", row.getCompanyId());
		out.put("template_name", row.getTemplateName());
		out.put("template_type", row.getTemplateType());
		out.put("template_content", row.getTemplateContent());
		out.put("template_code", row.getTemplateCode());
		out.put("remark", row.getRemark());
		out.put("scene_id", row.getSceneId());
		out.put("status", row.getStatus());
		out.put("reason", emptyReasonToNull(row.getReason()));
		out.put("related_sign_name", row.getRelatedSignName());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	private static String emptyReasonToNull(String reason) {
		if (reason == null) {
			return null;
		}
		return reason.isEmpty() ? null : reason;
	}
}
