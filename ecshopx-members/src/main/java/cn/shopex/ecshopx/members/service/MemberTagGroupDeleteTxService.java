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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTagGroupRel;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupRelMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTagGroupDeleteTxService {

	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagGroupRelMapper memberTagGroupRelMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagGroupMapper memberTagGroupMapper;

	public MemberTagGroupDeleteTxService(
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagGroupRelMapper memberTagGroupRelMapper,
			MemberTagsMapper memberTagsMapper,
			MemberTagGroupMapper memberTagGroupMapper) {
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagGroupRelMapper = memberTagGroupRelMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagGroupMapper = memberTagGroupMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteTagGroupInTransaction(long companyId, long distributorId, long groupId, List<Long> tagIds) {
		try {
			if (tagIds != null) {
				for (Long tagId : tagIds) {
					if (tagId == null || tagId <= 0) {
						continue;
					}
					LambdaQueryWrapper<MemberRelTags> relW = new LambdaQueryWrapper<MemberRelTags>()
							.eq(MemberRelTags::getCompanyId, companyId)
							.eq(MemberRelTags::getTagId, tagId);
					memberRelTagsMapper.delete(relW);

					LambdaQueryWrapper<MemberTagGroupRel> grpRelW = new LambdaQueryWrapper<MemberTagGroupRel>()
							.eq(MemberTagGroupRel::getGroupId, groupId)
							.eq(MemberTagGroupRel::getCompanyId, companyId)
							.eq(MemberTagGroupRel::getDistributorId, distributorId)
							.eq(MemberTagGroupRel::getTagId, tagId);
					memberTagGroupRelMapper.delete(grpRelW);

					LambdaQueryWrapper<MemberTagGroupRel> cntW = new LambdaQueryWrapper<MemberTagGroupRel>()
							.eq(MemberTagGroupRel::getCompanyId, companyId)
							.eq(MemberTagGroupRel::getDistributorId, distributorId)
							.eq(MemberTagGroupRel::getTagId, tagId);
					Long otherBoxed = memberTagGroupRelMapper.selectCount(cntW);
					long other = otherBoxed == null ? 0L : otherBoxed.longValue();
					if (other == 0) {
						LambdaQueryWrapper<MemberTags> delTag = new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getTagId, tagId)
								.eq(MemberTags::getCompanyId, companyId)
								.eq(MemberTags::getDistributorId, distributorId);
						memberTagsMapper.delete(delTag);
					}
				}
			}
			memberTagGroupMapper.deleteById(groupId);
		} catch (DataAccessException e) {
			throw MemberTagsCreateService.mapDataAccess(e);
		}
	}
}
