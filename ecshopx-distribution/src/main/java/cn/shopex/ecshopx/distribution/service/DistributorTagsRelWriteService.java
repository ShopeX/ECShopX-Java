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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorRelTags;
import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorRelTagsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DistributorTagsRelWriteService {

	private final DistributorRelTagsMapper distributorRelTagsMapper;
	private final DistributorMapper distributorMapper;
	private final DistributorTagsMapper distributorTagsMapper;
	private final TransactionTemplate transactionTemplate;

	public DistributorTagsRelWriteService(
			DistributorRelTagsMapper distributorRelTagsMapper,
			DistributorMapper distributorMapper,
			DistributorTagsMapper distributorTagsMapper,
			PlatformTransactionManager transactionManager) {
		this.distributorRelTagsMapper = distributorRelTagsMapper;
		this.distributorMapper = distributorMapper;
		this.distributorTagsMapper = distributorTagsMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * 按公司收窄店铺与标签 id 后，在事务内删除 distributor_rel_tags 中所有 (distributorId, tagId) 组合。
	 * companyId &lt; 1 或任一侧列表为空时立即返回（不启事务、不访问 DB）。
	 */
	public void deleteRelTags(long companyId, List<Long> distributorIds, List<Long> tagIds) {
		if (companyId < 1
				|| distributorIds == null
				|| distributorIds.isEmpty()
				|| tagIds == null
				|| tagIds.isEmpty()) {
			return;
		}
		List<Distributor> distRows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.in(Distributor::getDistributorId, distributorIds)
								.select(Distributor::getDistributorId));
		List<Long> validDistributorIds = new ArrayList<>(distRows.size());
		for (Distributor d : distRows) {
			if (d.getDistributorId() != null) {
				validDistributorIds.add(d.getDistributorId());
			}
		}
		List<DistributorTags> tagRows =
				distributorTagsMapper.selectList(
						new LambdaQueryWrapper<DistributorTags>()
								.eq(DistributorTags::getCompanyId, companyId)
								.in(DistributorTags::getTagId, tagIds)
								.select(DistributorTags::getTagId));
		List<Long> validTagIds = new ArrayList<>(tagRows.size());
		for (DistributorTags t : tagRows) {
			if (t.getTagId() != null) {
				validTagIds.add(t.getTagId());
			}
		}
		if (validDistributorIds.isEmpty() || validTagIds.isEmpty()) {
			return;
		}
		transactionTemplate.executeWithoutResult(
				status -> {
					for (Long distributorId : validDistributorIds) {
						for (Long tagId : validTagIds) {
							LambdaQueryWrapper<DistributorRelTags> w =
									new LambdaQueryWrapper<DistributorRelTags>()
											.eq(DistributorRelTags::getCompanyId, companyId)
											.eq(DistributorRelTags::getDistributorId, distributorId)
											.eq(DistributorRelTags::getTagId, tagId);
							distributorRelTagsMapper.delete(w);
						}
					}
				});
	}

	public boolean applyRelTags(long companyId, DistributorTagsRelInputNormalizer.NormalizedRelTagInput input) {
		if (input.distributorIdIsArray() && input.tagIdsIsArray()) {
			applyBranchA(companyId, input.distributorIds(), input.tagIdsAsList());
			return true;
		}
		if (!input.distributorIdIsArray()) {
			List<Long> tagList =
					input.tagIdsIsArray() ? input.tagIdsAsList() : List.of(input.scalarTagId());
			transactionTemplate.executeWithoutResult(
					status -> applyBranchB(companyId, input.scalarDistributorId(), tagList));
			return true;
		}
		transactionTemplate.executeWithoutResult(
				status -> applyBranchC(companyId, input.scalarTagId(), input.distributorIds()));
		return true;
	}

	private void applyBranchA(long companyId, List<Long> distributorIds, List<Long> tagIds) {
		for (Long distributorId : distributorIds) {
			for (Long tagId : tagIds) {
				LambdaQueryWrapper<DistributorRelTags> w =
						new LambdaQueryWrapper<DistributorRelTags>()
								.eq(DistributorRelTags::getCompanyId, companyId)
								.eq(DistributorRelTags::getDistributorId, distributorId)
								.eq(DistributorRelTags::getTagId, tagId);
				if (distributorRelTagsMapper.selectCount(w) == 0) {
					DistributorRelTags row = new DistributorRelTags();
					row.setCompanyId(companyId);
					row.setDistributorId(distributorId);
					row.setTagId(tagId);
					distributorRelTagsMapper.insert(row);
				}
			}
		}
	}

	private void applyBranchB(long companyId, long distributorId, List<Long> tagIds) {
		LambdaQueryWrapper<DistributorRelTags> w =
				new LambdaQueryWrapper<DistributorRelTags>()
						.eq(DistributorRelTags::getCompanyId, companyId)
						.eq(DistributorRelTags::getDistributorId, distributorId);
		if (distributorRelTagsMapper.selectCount(w) > 0) {
			distributorRelTagsMapper.delete(w);
		}
		if (tagIds != null) {
			for (Long tagId : tagIds) {
				if (tagId == null) {
					continue;
				}
				DistributorRelTags row = new DistributorRelTags();
				row.setCompanyId(companyId);
				row.setDistributorId(distributorId);
				row.setTagId(tagId);
				distributorRelTagsMapper.insert(row);
			}
		}
	}

	private void applyBranchC(long companyId, long tagId, List<Long> distributorIds) {
		LambdaQueryWrapper<DistributorRelTags> w =
				new LambdaQueryWrapper<DistributorRelTags>()
						.eq(DistributorRelTags::getCompanyId, companyId)
						.eq(DistributorRelTags::getTagId, tagId);
		if (distributorRelTagsMapper.selectCount(w) > 0) {
			distributorRelTagsMapper.delete(w);
		}
		for (Long distributorId : distributorIds) {
			DistributorRelTags row = new DistributorRelTags();
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setTagId(tagId);
			distributorRelTagsMapper.insert(row);
		}
	}
}
