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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.dto.BindUserSalespersonRelationshipOutcome;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BindUserSalespersonRelationshipService {

	private final WorkWechatRelMapper workWechatRelMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;

	public BindUserSalespersonRelationshipService(
			WorkWechatRelMapper workWechatRelMapper, ShopsRelSalespersonMapper shopsRelSalespersonMapper) {
		this.workWechatRelMapper = workWechatRelMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public BindUserSalespersonRelationshipOutcome bindUserSalespersonRelationship(
			long userId, long companyId, long salespersonId, String unionidOrNull) {
		LambdaQueryWrapper<WorkWechatRel> anyBound = new LambdaQueryWrapper<>();
		anyBound
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE);
		if (workWechatRelMapper.selectCount(anyBound) > 0) {
			return new BindUserSalespersonRelationshipOutcome(true, false);
		}

		LambdaQueryWrapper<WorkWechatRel> alreadyThisSalesperson = new LambdaQueryWrapper<>();
		alreadyThisSalesperson
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getSalespersonId, salespersonId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE);
		if (workWechatRelMapper.selectCount(alreadyThisSalesperson) > 0) {
			return new BindUserSalespersonRelationshipOutcome(true, false);
		}

		LambdaUpdateWrapper<WorkWechatRel> unbindAll = new LambdaUpdateWrapper<>();
		unbindAll
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.set(WorkWechatRel::getIsBind, false);
		workWechatRelMapper.update(null, unbindAll);

		LambdaQueryWrapper<WorkWechatRel> rowKey = new LambdaQueryWrapper<>();
		rowKey
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getSalespersonId, salespersonId)
				.last("LIMIT 1");
		WorkWechatRel existing = workWechatRelMapper.selectOne(rowKey);

		boolean successInData;
		if (existing != null) {
			LambdaUpdateWrapper<WorkWechatRel> bindOne = new LambdaUpdateWrapper<>();
			bindOne
					.eq(WorkWechatRel::getUserId, userId)
					.eq(WorkWechatRel::getCompanyId, companyId)
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.set(WorkWechatRel::getIsBind, true);
			int updated = workWechatRelMapper.update(null, bindOne);
			if (updated == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			successInData = updated > 0;
		} else {
			WorkWechatRel entity = new WorkWechatRel();
			entity.setUserId(userId);
			entity.setCompanyId(companyId);
			entity.setSalespersonId(salespersonId);
			entity.setIsFriend(false);
			entity.setIsBind(true);
			entity.setBoundTime(System.currentTimeMillis() / 1000L);
			if (StringUtils.hasText(unionidOrNull)) {
				entity.setUnionid(unionidOrNull.trim());
			}
			int inserted = workWechatRelMapper.insert(entity);
			successInData = inserted > 0;
		}

		LambdaQueryWrapper<ShopsRelSalesperson> shopRel = new LambdaQueryWrapper<>();
		shopRel
				.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
				.eq(ShopsRelSalesperson::getCompanyId, companyId)
				.eq(ShopsRelSalesperson::getStoreType, "distributor")
				.last("LIMIT 1");
		shopsRelSalespersonMapper.selectOne(shopRel);

		return new BindUserSalespersonRelationshipOutcome(false, successInData);
	}
}
