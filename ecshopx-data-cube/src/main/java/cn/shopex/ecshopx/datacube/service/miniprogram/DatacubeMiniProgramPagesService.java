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

package cn.shopex.ecshopx.datacube.service.miniprogram;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DatacubeMiniProgramPagesService {

	private static final List<Map<String, Object>> PAGES_DEFAULT;

	private static final List<Map<String, Object>> PAGES_YYK_MENDIAN;

	static {
		PAGES_DEFAULT = List.of(
				page("pages/index", "首页", Collections.emptyList()),
				page(
						"pages/item/espier-detail",
						"商品详情页",
						List.of(pathParam("id", "商品ID"))));
		PAGES_YYK_MENDIAN = List.of(
				page("pages/index", "首页", Collections.emptyList()),
				page(
						"pages/course_detail",
						"课程详情页",
						List.of(pathParam("id", "课程ID"))));
	}

	private final WeappMapper weappMapper;

	public DatacubeMiniProgramPagesService(WeappMapper weappMapper) {
		this.weappMapper = weappMapper;
	}

	public List<Map<String, Object>> getPages(long companyId, String wxappid) {
		if (wxappid == null || wxappid.trim().isEmpty()) {
			throw new ResourceException("添加来源出错: wxappid 不能为空");
		}
		String trimmed = wxappid.trim();
		Weapp weapp = weappMapper.selectOne(
				new LambdaQueryWrapper<Weapp>()
						.eq(Weapp::getCompanyId, companyId)
						.eq(Weapp::getAuthorizerAppid, trimmed)
						.isNull(Weapp::getDeletedAt));
		if (weapp == null) {
			throw new ResourceException("获取小程序模板出错，请检查后再试");
		}
		String templateName = weapp.getTemplateName();
		List<Map<String, Object>> src;
		if ("yykmendian".equals(templateName)) {
			src = PAGES_YYK_MENDIAN;
		} else if ("yykweishop".equals(templateName)) {
			src = PAGES_DEFAULT;
		} else {
			src = PAGES_DEFAULT;
		}
		return new ArrayList<>(src);
	}

	private static Map<String, Object> page(String page, String label, List<Map<String, String>> pathParams) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("page", page);
		m.put("label", label);
		m.put("pathParams", pathParams);
		return m;
	}

	private static Map<String, String> pathParam(String paramName, String paramLabel) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("param_name", paramName);
		m.put("param_label", paramLabel);
		return m;
	}
}
