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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FrontActivityDetailService {

	private final ActivitiesMapper activitiesMapper;

	public FrontActivityDetailService(ActivitiesMapper activitiesMapper) {
		this.activitiesMapper = activitiesMapper;
	}

	public Map<String, Object> getActivityDetail(long companyId, long activityId) {
		if (activityId <= 0L) {
			throw new ResourceException("活动ID必填");
		}
		if (companyId <= 0L) {
			throw new ResourceException("公司ID必填");
		}

		Activities activity =
				activitiesMapper.selectOne(
						new LambdaQueryWrapper<Activities>()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("activity_id", String.valueOf(activity.getId()));
		out.put("name", activity.getName());
		out.put("title", activity.getTitle());
		out.put("pages_template_id", activity.getPagesTemplateId() == null ? 0 : activity.getPagesTemplateId());
		out.put("share_pic", activity.getSharePic());
		out.put("pic", activity.getPic());
		out.put("purchase_mode", activity.getPurchaseMode());
		out.put("purchase_mode_desc", PurchaseModeSupport.desc(activity.getPurchaseMode()));
		out.put(
				"if_relative_join",
				PurchaseModeSupport.isPrepaidPoint(activity)
						? 0
						: (Boolean.TRUE.equals(activity.getIfRelativeJoin()) ? 1 : 0));
		return out;
	}
}
