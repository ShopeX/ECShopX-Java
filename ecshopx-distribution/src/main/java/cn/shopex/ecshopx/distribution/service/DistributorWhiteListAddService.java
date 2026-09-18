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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListAddCommand;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class DistributorWhiteListAddService {

	private final DistributorWhiteListMapper mapper;
	private final DistributorWhiteListMultiLangWriteService distributorWhiteListMultiLangWriteService;

	public DistributorWhiteListAddService(
			DistributorWhiteListMapper mapper,
			DistributorWhiteListMultiLangWriteService distributorWhiteListMultiLangWriteService) {
		this.mapper = mapper;
		this.distributorWhiteListMultiLangWriteService = distributorWhiteListMultiLangWriteService;
	}

	public void addWhiteList(long companyId, DistributorWhiteListAddCommand cmd, String requestLangTag) {
		if (cmd.distributorId() == null) {
			throw new BadRequestException("distributor_id 不能为空");
		}
		List<Long> distributorIds = cmd.distributorId();
		if (!distributorIds.isEmpty()) {
			for (Long dist : distributorIds) {
				if (dist == null || dist <= 0L) {
					throw new BadRequestException("distributor_id 无效");
				}
			}
		}

		String m = cmd.mobile();
		if (m == null || !StringUtils.hasText(m.trim())) {
			throw new BadRequestException("mobile 不能为空");
		}

		Long id = cmd.id();
		if (id != null) {
			if (id <= 0L) {
				throw new BadRequestException("id 无效");
			}
			DistributorWhiteList existing = mapper.selectOne(
					Wrappers.<DistributorWhiteList>lambdaQuery()
							.eq(DistributorWhiteList::getId, id)
							.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException("白名单记录不存在");
			}
			if (existing.getCompanyId() == null || existing.getCompanyId().longValue() != companyId) {
				throw new ForbiddenException("无权操作该白名单记录");
			}
			String mobileOld = existing.getMobile();
			if (mobileOld == null || !StringUtils.hasText(mobileOld.trim())) {
				throw new ResourceException("白名单记录不存在");
			}
			mapper.delete(
					Wrappers.<DistributorWhiteList>lambdaQuery()
							.eq(DistributorWhiteList::getCompanyId, companyId)
							.eq(DistributorWhiteList::getMobile, mobileOld));
		}

		if (distributorIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Long distId : distributorIds) {
			DistributorWhiteList row = new DistributorWhiteList();
			row.setCompanyId(companyId);
			row.setDistributorId(distId);
			row.setMobile(m.trim());
			row.setUsername(cmd.username());
			row.setCreated((long) now);
			row.setUpdated((long) now);
			mapper.insert(row);
			Long newId = row.getId();
			distributorWhiteListMultiLangWriteService.applyAfterInsert(newId, companyId, cmd.username(), requestLangTag);
		}
	}
}
