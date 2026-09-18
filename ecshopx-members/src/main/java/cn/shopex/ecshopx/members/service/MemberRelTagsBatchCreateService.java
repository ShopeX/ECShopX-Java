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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.service.reltag.MemberTagRelationPushCoordinator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class MemberRelTagsBatchCreateService {

	private static final Logger log = LoggerFactory.getLogger(MemberRelTagsBatchCreateService.class);

	private final MemberRelTagsMapper memberRelTagsMapper;
	private final JdbcTemplate jdbcTemplate;
	private final MemberTagRelationPushCoordinator memberTagRelationPushCoordinator;

	public MemberRelTagsBatchCreateService(
			MemberRelTagsMapper memberRelTagsMapper,
			JdbcTemplate jdbcTemplate,
			MemberTagRelationPushCoordinator memberTagRelationPushCoordinator) {
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.memberTagRelationPushCoordinator = memberTagRelationPushCoordinator;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createRelTags(List<Long> userIds, List<Long> tagIds, long companyId) {
		for (Long userId : userIds) {
			for (Long tagId : tagIds) {
				long exists =
						memberRelTagsMapper.selectCount(
								new LambdaQueryWrapper<MemberRelTags>()
										.eq(MemberRelTags::getTagId, tagId)
										.eq(MemberRelTags::getCompanyId, companyId)
										.eq(MemberRelTags::getUserId, userId));
				if (exists > 0) {
					continue;
				}
				MemberRelTags row = new MemberRelTags();
				row.setTagId(tagId);
				row.setCompanyId(companyId);
				row.setUserId(userId);
				memberRelTagsMapper.insertMemberRelTag(row);
				jdbcTemplate.update(
						"UPDATE members_tags SET self_tag_count = self_tag_count + 1 WHERE tag_id = ? AND company_id = ?",
						tagId,
						companyId);
			}
		}
		schedulePushAfterCommit(userIds, tagIds, companyId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void createRelTagsByUserId(long userId, List<Long> tagIds, long companyId) {
		LambdaQueryWrapper<MemberRelTags> w =
				new LambdaQueryWrapper<MemberRelTags>()
						.eq(MemberRelTags::getUserId, userId)
						.eq(MemberRelTags::getCompanyId, companyId);
		try {
			long existing = memberRelTagsMapper.selectCount(w);
			if (existing > 0) {
				memberRelTagsMapper.delete(w);
			}
			if (tagIds == null || tagIds.isEmpty()) {
				return;
			}
			LinkedHashSet<Long> distinct = new LinkedHashSet<>(tagIds);
			for (Long tagId : distinct) {
				MemberRelTags row = new MemberRelTags();
				row.setTagId(tagId);
				row.setCompanyId(companyId);
				row.setUserId(userId);
				memberRelTagsMapper.insertMemberRelTag(row);
				jdbcTemplate.update(
						"UPDATE members_tags SET self_tag_count = self_tag_count + 1 WHERE tag_id = ? AND company_id = ?",
						tagId,
						companyId);
			}
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void createRelTagsByTagId(List<Long> userIds, long tagId, long companyId) {
		LambdaQueryWrapper<MemberRelTags> w0 =
				new LambdaQueryWrapper<MemberRelTags>()
						.eq(MemberRelTags::getTagId, tagId)
						.eq(MemberRelTags::getCompanyId, companyId);
		try {
			if (memberRelTagsMapper.selectCount(w0) > 0) {
				memberRelTagsMapper.delete(w0);
				jdbcTemplate.update(
						"UPDATE members_tags SET self_tag_count = self_tag_count - 1 WHERE tag_id = ? AND company_id = ?",
						tagId,
						companyId);
			}
			if (userIds == null || userIds.isEmpty()) {
				return;
			}
			LinkedHashSet<Long> distinctUsers = new LinkedHashSet<>();
			for (Long uid : userIds) {
				if (uid != null && uid > 0) {
					distinctUsers.add(uid);
				}
			}
			for (Long userId : distinctUsers) {
				MemberRelTags row = new MemberRelTags();
				row.setTagId(tagId);
				row.setCompanyId(companyId);
				row.setUserId(userId);
				memberRelTagsMapper.insertMemberRelTag(row);
				jdbcTemplate.update(
						"UPDATE members_tags SET self_tag_count = self_tag_count + 1 WHERE tag_id = ? AND company_id = ?",
						tagId,
						companyId);
			}
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
	}

	public void deleteRelTagsByUserIdNoPush(long userId, long companyId) {
		List<MemberRelTags> relations =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getUserId, userId)
								.eq(MemberRelTags::getCompanyId, companyId));
		if (relations.isEmpty()) {
			return;
		}
		for (MemberRelTags relation : relations) {
			jdbcTemplate.update(
					"UPDATE members_tags SET self_tag_count = self_tag_count - 1 WHERE tag_id = ? AND company_id = ?",
					relation.getTagId(),
					companyId);
		}
		memberRelTagsMapper.delete(
				new LambdaQueryWrapper<MemberRelTags>()
						.eq(MemberRelTags::getUserId, userId)
						.eq(MemberRelTags::getCompanyId, companyId));
	}

	@Transactional(rollbackFor = Exception.class)
	public void replaceMemberTagsInboundNoPush(long userId, List<Long> tagIds, long companyId) {
		deleteRelTagsByUserIdNoPush(userId, companyId);
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> distinct = new LinkedHashSet<>(tagIds);
		for (Long tagId : distinct) {
			MemberRelTags row = new MemberRelTags();
			row.setTagId(tagId);
			row.setCompanyId(companyId);
			row.setUserId(userId);
			memberRelTagsMapper.insertMemberRelTag(row);
			jdbcTemplate.update(
					"UPDATE members_tags SET self_tag_count = self_tag_count + 1 WHERE tag_id = ? AND company_id = ?",
					tagId,
					companyId);
		}
		log.info(
				"[createRelTags] 跳过推送到导购平台 company_id={} user_ids=[{}] tag_ids={}",
				companyId,
				userId,
				distinct);
	}

	@Transactional(rollbackFor = Exception.class)
	public void userRelTagDeleteNoPush(long companyId, List<Long> userIds, List<Long> tagIds) {
		if (userIds == null || userIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
			return;
		}
		try {
			for (Long userId : userIds) {
				if (userId == null || userId <= 0) {
					continue;
				}
				for (Long tagId : tagIds) {
					if (tagId == null || tagId <= 0) {
						continue;
					}
					memberRelTagsMapper.delete(
							new LambdaQueryWrapper<MemberRelTags>()
									.eq(MemberRelTags::getCompanyId, companyId)
									.eq(MemberRelTags::getUserId, userId)
									.eq(MemberRelTags::getTagId, tagId));
					jdbcTemplate.update(
							"UPDATE members_tags SET self_tag_count = self_tag_count - 1 WHERE tag_id = ? AND company_id = ?",
							tagId,
							companyId);
				}
			}
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void delRelMemberTag(long companyId, long userId, long tagId) {
		try {
			jdbcTemplate.update(
					"UPDATE members_tags SET self_tag_count = self_tag_count - 1 WHERE tag_id = ? AND company_id = ?",
					tagId,
					companyId);
			memberRelTagsMapper.delete(
					new LambdaQueryWrapper<MemberRelTags>()
							.eq(MemberRelTags::getTagId, tagId)
							.eq(MemberRelTags::getCompanyId, companyId)
							.eq(MemberRelTags::getUserId, userId));
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		scheduleRemovePushAfterCommit(companyId, userId, tagId);
	}

	private static ResourceException mapDataAccess(DataAccessException e) {
		Throwable c = e.getMostSpecificCause();
		String msg = c != null ? c.getMessage() : null;
		return new ResourceException(StringUtils.hasText(msg) ? msg : "操作失败");
	}

	private void schedulePushAfterCommit(List<Long> userIds, List<Long> tagIds, long companyId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					runPushPlaceholderWarn(userIds, tagIds, companyId);
				}
			});
		} else {
			runPushPlaceholderWarn(userIds, tagIds, companyId);
		}
	}

	private void runPushPlaceholderWarn(List<Long> userIds, List<Long> tagIds, long companyId) {
		try {
			memberTagRelationPushCoordinator.afterCommitPushAdd(companyId, userIds, tagIds);
		} catch (Exception e) {
			log.warn(
					"[createRelTags] 推送导购平台失败 company_id={} user_ids={} tag_ids={} error={}",
					companyId,
					userIds,
					tagIds,
					e.getMessage());
		}
	}

	private void scheduleRemovePushAfterCommit(long companyId, long userId, long tagId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					runPushRemovePlaceholderWarn(companyId, userId, tagId);
				}
			});
		} else {
			runPushRemovePlaceholderWarn(companyId, userId, tagId);
		}
	}

	private void runPushRemovePlaceholderWarn(long companyId, long userId, long tagId) {
		try {
			memberTagRelationPushCoordinator.afterCommitPushRemove(companyId, userId, tagId);
		} catch (Exception e) {
			log.warn(
					"[delRelMemberTag] 推送导购平台失败 company_id={} user_id={} tag_id={} error={}",
					companyId,
					userId,
					tagId,
					e.getMessage());
		}
	}
}
