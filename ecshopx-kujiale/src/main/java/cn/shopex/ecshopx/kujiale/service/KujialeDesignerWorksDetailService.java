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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorks;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksLevel;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksPic;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksRelTags;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksLevelMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksPicMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksRelTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KujialeDesignerWorksDetailService {

	private static final Logger log = LoggerFactory.getLogger(KujialeDesignerWorksDetailService.class);

	private static final String KUJIALE_WWW = "www.kujiale.com";
	private static final String KUJIALE_PANO53 = "pano53.p.kujiale.com";

	private final KujialeDesignerWorksMapper worksMapper;
	private final KujialeDesignerWorksLevelMapper levelMapper;
	private final KujialeDesignerWorksPicMapper picMapper;
	private final KujialeDesignerWorksRelTagsMapper relTagsMapper;

	public KujialeDesignerWorksDetailService(
			KujialeDesignerWorksMapper worksMapper,
			KujialeDesignerWorksLevelMapper levelMapper,
			KujialeDesignerWorksPicMapper picMapper,
			KujialeDesignerWorksRelTagsMapper relTagsMapper) {
		this.worksMapper = worksMapper;
		this.levelMapper = levelMapper;
		this.picMapper = picMapper;
		this.relTagsMapper = relTagsMapper;
	}

	public Object getDesignerPicByPicId(String picId) {
		KujialeDesignerWorksPic row =
				picMapper.selectOne(
						new LambdaQueryWrapper<KujialeDesignerWorksPic>()
								.eq(KujialeDesignerWorksPic::getPicId, picId)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}
		return picToSnakeMap(row);
	}

	public Map<String, Object> getDetail(String designId, String planId) {
		Map<String, Object> returnValue = new LinkedHashMap<>();
		returnValue.put("basicInfo", new ArrayList<>());
		returnValue.put("levelinfo", Collections.emptyList());
		returnValue.put("picinfo", Collections.emptyList());

		KujialeDesignerWorks works = worksMapper.selectOne(
				new LambdaQueryWrapper<KujialeDesignerWorks>()
						.eq(KujialeDesignerWorks::getDesignId, designId)
						.eq(KujialeDesignerWorks::getPlanId, planId));
		if (works == null) {
			return returnValue;
		}

		Map<String, Object> basicInfo = worksToSnakeMap(works);
		returnValue.put("basicInfo", basicInfo);

		List<KujialeDesignerWorksLevel> levelRows = levelMapper.selectList(
				new LambdaQueryWrapper<KujialeDesignerWorksLevel>()
						.eq(KujialeDesignerWorksLevel::getDesignId, designId)
						.eq(KujialeDesignerWorksLevel::getPlanId, planId)
						.orderByAsc(KujialeDesignerWorksLevel::getId));
		if (!levelRows.isEmpty()) {
			List<Map<String, Object>> levelinfo = new ArrayList<>();
			for (KujialeDesignerWorksLevel row : levelRows) {
				levelinfo.add(levelToSnakeMap(row));
			}
			returnValue.put("levelinfo", levelinfo);
		}

		List<KujialeDesignerWorksPic> picRows = picMapper.selectList(
				new LambdaQueryWrapper<KujialeDesignerWorksPic>()
						.eq(KujialeDesignerWorksPic::getDesignId, designId)
						.eq(KujialeDesignerWorksPic::getPlanId, planId)
						.orderByAsc(KujialeDesignerWorksPic::getId));
		if (!picRows.isEmpty()) {
			List<Map<String, Object>> picinfo = new ArrayList<>();
			for (KujialeDesignerWorksPic row : picRows) {
				picinfo.add(picToSnakeMap(row));
			}
			returnValue.put("picinfo", picinfo);
			returnValue.put("web_view_url", Boolean.FALSE);

			for (KujialeDesignerWorksPic v : picRows) {
				if (StringUtils.hasText(v.getPanoLink()) && picTypeTruthy(v.getPicType())) {
					String panoLink = v.getPanoLink().replace(KUJIALE_WWW, KUJIALE_PANO53);
					picinfo.get(0).put("pano_link", panoLink);
					basicInfo.put("design_pano_url", panoLink);
				}
			}
		}

		basicInfo.put("design_mesh_url", "");
		Object panoUrl = basicInfo.get("design_pano_url");
		if (panoUrl != null && StringUtils.hasText(panoUrl.toString())) {
			basicInfo.put(
					"design_mesh_url",
					"https://pano572.p.kujiale.com/design/" + works.getDesignId() + "/show");
		}

		List<KujialeDesignerWorksRelTags> tagRows = relTagsMapper.selectList(
				new LambdaQueryWrapper<KujialeDesignerWorksRelTags>()
						.eq(KujialeDesignerWorksRelTags::getDesignId, works.getDesignId())
						.orderByAsc(KujialeDesignerWorksRelTags::getId));
		List<Map<String, Object>> taginfo = new ArrayList<>();
		if (!tagRows.isEmpty()) {
			for (KujialeDesignerWorksRelTags row : tagRows) {
				taginfo.add(relTagToSnakeMap(row));
			}
		}
		returnValue.put("taginfo", taginfo);

		if (hasNonEmptyFilter(designId, planId)) {
			try {
				incrementViewCount(designId, planId);
			} catch (Exception e) {
				log.error("更新view_count失败: {}", e.getMessage());
			}
		}

		return returnValue;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateViewCountReturningRow(String designId, String planId) {
		KujialeDesignerWorks row =
				worksMapper.selectOne(
						new LambdaQueryWrapper<KujialeDesignerWorks>()
								.eq(KujialeDesignerWorks::getDesignId, designId)
								.eq(KujialeDesignerWorks::getPlanId, planId));
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		int cur = row.getViewCount() != null ? row.getViewCount() : 0;
		row.setViewCount(cur + 1);
		worksMapper.updateById(row);
		return worksToSnakeMap(row);
	}

	private static boolean hasNonEmptyFilter(String designId, String planId) {
		return StringUtils.hasText(designId) && StringUtils.hasText(planId);
	}

	private void incrementViewCount(String designId, String planId) {
		KujialeDesignerWorks row = worksMapper.selectOne(
				new LambdaQueryWrapper<KujialeDesignerWorks>()
						.eq(KujialeDesignerWorks::getDesignId, designId)
						.eq(KujialeDesignerWorks::getPlanId, planId));
		if (row == null) {
			return;
		}
		int cur = row.getViewCount() != null ? row.getViewCount() : 0;
		row.setViewCount(cur + 1);
		worksMapper.updateById(row);
	}

	private static boolean picTypeTruthy(String picType) {
		if (picType == null || picType.isEmpty()) {
			return false;
		}
		String t = picType.trim();
		return !"0".equals(t);
	}

	private static Map<String, Object> worksToSnakeMap(KujialeDesignerWorks w) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", w.getId());
		m.put("design_name", w.getDesignName());
		m.put("cover_pic", w.getCoverPic());
		m.put("is_origin", w.getIsOrigin());
		m.put("is_excellent", w.getIsExcellent());
		m.put("is_real_excellent", w.getIsRealExcellent());
		m.put("is_top", w.getIsTop());
		m.put("design_id", w.getDesignId());
		m.put("plan_id", w.getPlanId());
		m.put("comm_name", w.getCommName());
		m.put("city", w.getCity());
		m.put("name", w.getName());
		m.put("tag_id", w.getTagId());
		m.put("design_pano_url", w.getDesignPanoUrl());
		m.put("user_avatar", w.getUserAvatar());
		m.put("email", w.getEmail());
		m.put("user_name", w.getUserName());
		m.put("user_id", w.getUserId());
		m.put("organization_id", w.getOrganizationId());
		m.put("created", w.getCreated());
		m.put("updated", w.getUpdated());
		m.put("view_count", w.getViewCount());
		m.put("like_count", w.getLikeCount());
		m.put("ku_created", w.getKuCreated());
		return m;
	}

	private static Map<String, Object> levelToSnakeMap(KujialeDesignerWorksLevel r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.getId());
		m.put("design_id", r.getDesignId());
		m.put("plan_id", r.getPlanId());
		m.put("spec_name", r.getSpecName());
		m.put("src_area", r.getSrcArea());
		m.put("area", r.getArea());
		m.put("real_area", r.getRealArea());
		m.put("plan_pic", r.getPlanPic());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}

	private static Map<String, Object> picToSnakeMap(KujialeDesignerWorksPic r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.getId());
		m.put("pic_id", r.getPicId());
		m.put("pic_type", r.getPicType());
		m.put("pic_detail_type", r.getPicDetailType());
		m.put("room_name", r.getRoomName());
		m.put("img", r.getImg());
		m.put("pano_link", r.getPanoLink());
		m.put("design_id", r.getDesignId());
		m.put("plan_id", r.getPlanId());
		m.put("level", r.getLevel());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}

	private static Map<String, Object> relTagToSnakeMap(KujialeDesignerWorksRelTags r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.getId());
		m.put("tag_category_id", r.getTagCategoryId());
		m.put("tag_category_name", r.getTagCategoryName());
		m.put("tag_id", r.getTagId());
		m.put("tag_name", r.getTagName());
		m.put("design_id", r.getDesignId());
		m.put("plan_id", r.getPlanId());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}
}
