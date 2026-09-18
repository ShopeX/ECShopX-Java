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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityItemsSortImportRowService {

	private final ActivitiesMapper activitiesMapper;
	private final ActivityItemsMapper activityItemsMapper;

	public EmployeePurchaseActivityItemsSortImportRowService(
			ActivitiesMapper activitiesMapper, ActivityItemsMapper activityItemsMapper) {
		this.activitiesMapper = activitiesMapper;
		this.activityItemsMapper = activityItemsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void acceptRow(long companyId, Map<String, Object> row) {
		long relationId = parseRelationId(row.get("relation_id"));
		String goodsBn = trim(row.get("goods_bn"));
		if (!StringUtils.hasText(goodsBn)) {
			throw new BadRequestException("SPU编码不能为空");
		}
		int sort = parseSort(row.get("sort"));

		Activities activity = requireActivity(companyId, relationId);
		Long goodsId =
				activityItemsMapper.selectGoodsIdByActivityAndGoodsBn(companyId, activity.getId(), goodsBn);
		if (goodsId == null || goodsId <= 0) {
			throw new BadRequestException("内购商品不存在:" + goodsBn);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		activityItemsMapper.update(
				null,
				Wrappers.<ActivityItems>lambdaUpdate()
						.eq(ActivityItems::getCompanyId, companyId)
						.eq(ActivityItems::getActivityId, activity.getId())
						.eq(ActivityItems::getGoodsId, goodsId)
						.set(ActivityItems::getSort, sort)
						.set(ActivityItems::getUpdated, now));
	}

	public void requireActivityForRelation(long companyId, long relationId) {
		requireActivity(companyId, relationId);
	}

	private Activities requireActivity(long companyId, long relationId) {
		if (relationId <= 0) {
			throw new BadRequestException("关联id不能为空");
		}
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, relationId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new BadRequestException("内购活动不存在");
		}
		return activity;
	}

	private static long parseRelationId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("关联id不能为空");
		}
		try {
			long v = new java.math.BigDecimal(String.valueOf(raw).trim()).longValueExact();
			if (v <= 0) {
				throw new BadRequestException("关联id不能为空");
			}
			return v;
		} catch (ArithmeticException | NumberFormatException e) {
			throw new BadRequestException("关联id不能为空");
		}
	}

	private static int parseSort(Object raw) {
		String s = trim(raw);
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			java.math.BigDecimal bd = new java.math.BigDecimal(s);
			if (bd.scale() > 0 && bd.stripTrailingZeros().scale() > 0) {
				throw new BadRequestException("排序值必须为大于等于0的整数");
			}
			long v = bd.longValueExact();
			if (v < 0 || v > 2147483647L) {
				throw new BadRequestException("排序值必须为大于等于0的整数");
			}
			return (int) v;
		} catch (ArithmeticException | NumberFormatException e) {
			throw new BadRequestException("排序值必须为大于等于0的整数");
		}
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
