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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagMemberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WechatFansTagsBatchSetService {

	private final OfficialAccountUserTagMemberService officialAccountUserTagMemberService;
	private final WechatFansMapper wechatFansMapper;
	private final WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;

	public WechatFansTagsBatchSetService(
			OfficialAccountUserTagMemberService officialAccountUserTagMemberService,
			WechatFansMapper wechatFansMapper,
			WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper) {
		this.officialAccountUserTagMemberService = officialAccountUserTagMemberService;
		this.wechatFansMapper = wechatFansMapper;
		this.wechatFansBindWechatTagMapper = wechatFansBindWechatTagMapper;
	}

	/**
	 * 单用户批量打标签接口的短路探测：在 openId + companyId + authorizer 列条件下无粉丝行时返回 true。
	 */
	public boolean isFanAbsentForBatchSetTriple(
			String openId, String authorizerAppidFromJwtNullable, long companyId) {
		LambdaQueryWrapper<WechatFans> w = Wrappers.lambdaQuery();
		w.eq(WechatFans::getOpenId, openId).eq(WechatFans::getCompanyId, companyId);
		if (authorizerAppidFromJwtNullable != null && !authorizerAppidFromJwtNullable.isBlank()) {
			w.eq(WechatFans::getAuthorizerAppid, authorizerAppidFromJwtNullable);
		} else {
			w.and(x -> x.isNull(WechatFans::getAuthorizerAppid).or().eq(WechatFans::getAuthorizerAppid, ""));
		}
		return wechatFansMapper.selectOne(w) == null;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean batchSetUserTags(
			String authorizerAppid, long companyId, List<String> openIds, List<Long> tagIds) {
		if (authorizerAppid == null || authorizerAppid.isBlank()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		if (openIds == null || openIds.isEmpty()) {
			throw new BadRequestException("用户标识必填", 411);
		}
		if (tagIds == null) {
			throw new BadRequestException("标签参数格式错误", 411);
		}
		if (openIds.size() > 1) {
			batchTagUsersMulti(authorizerAppid, companyId, openIds, tagIds);
			return true;
		}
		if (openIds.size() == 1) {
			batchSetUserTagsSingle(authorizerAppid, companyId, openIds.get(0), tagIds);
			return true;
		}
		throw new BadRequestException("用户标识必填", 411);
	}

	private void batchTagUsersMulti(
			String authorizerAppid, long companyId, List<String> openIds, List<Long> tagIds) {
		if (!tagIds.isEmpty()) {
			for (Long tagId : tagIds) {
				officialAccountUserTagMemberService.tagUsers(authorizerAppid, openIds, tagId);
			}
		}
		for (String openId : openIds) {
			WechatFans fan = findFan(authorizerAppid, companyId, openId);
			if (fan == null) {
				continue;
			}
			if (!Boolean.TRUE.equals(fan.getSubscribed())) {
				continue;
			}
			List<Long> oldTags = listBindTagIds(authorizerAppid, companyId, openId);
			if (!oldTags.isEmpty()) {
				deleteAllBinds(authorizerAppid, companyId, openId);
			}
			LinkedHashSet<Long> merged = new LinkedHashSet<>();
			merged.addAll(oldTags);
			merged.addAll(tagIds);
			syncFanTagsInTransaction(fan, merged, authorizerAppid, companyId, openId);
		}
	}

	private void batchSetUserTagsSingle(
			String authorizerAppid, long companyId, String openId, List<Long> tagIds) {
		WechatFans fan = findFan(authorizerAppid, companyId, openId);
		if (fan == null) {
			return;
		}
		List<Long> oldTags = listBindTagIds(authorizerAppid, companyId, openId);
		Set<Long> tagIdSetForDiff = new LinkedHashSet<>(tagIds);
		List<Long> delTags = new ArrayList<>();
		for (Long tid : oldTags) {
			if (!tagIdSetForDiff.contains(tid)) {
				delTags.add(tid);
			}
		}
		for (Long tid : delTags) {
			officialAccountUserTagMemberService.untagUsers(authorizerAppid, List.of(openId), tid);
		}
		deleteAllBinds(authorizerAppid, companyId, openId);
		for (Long tagId : tagIds) {
			officialAccountUserTagMemberService.tagUsers(authorizerAppid, List.of(openId), tagId);
		}
		String tagidsCsv = tagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		long nowSec = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<WechatFans> uw = new LambdaUpdateWrapper<WechatFans>()
				.eq(WechatFans::getOpenId, openId)
				.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
				.eq(WechatFans::getCompanyId, companyId)
				.set(WechatFans::getTagids, tagidsCsv)
				.set(WechatFans::getUpdated, nowSec);
		int rows = wechatFansMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("满足条件的用户不存在");
		}
	}

	private void syncFanTagsInTransaction(
			WechatFans fan,
			LinkedHashSet<Long> mergedTagIds,
			String authorizerAppid,
			long companyId,
			String openId) {
		Objects.requireNonNull(fan);
		deleteAllBinds(authorizerAppid, companyId, openId);
		for (Long tagId : mergedTagIds) {
			WechatFansBindWechatTag row = new WechatFansBindWechatTag();
			row.setTagId(tagId);
			row.setOpenId(openId);
			row.setCompanyId(companyId);
			row.setAuthorizerAppid(authorizerAppid);
			wechatFansBindWechatTagMapper.insert(row);
		}
		String tagidsCsv = mergedTagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
		long nowSec = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<WechatFans> uw = new LambdaUpdateWrapper<WechatFans>()
				.eq(WechatFans::getOpenId, openId)
				.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
				.eq(WechatFans::getCompanyId, companyId)
				.set(WechatFans::getTagids, tagidsCsv)
				.set(WechatFans::getUpdated, nowSec);
		int rows = wechatFansMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("满足条件的用户不存在");
		}
	}

	private WechatFans findFan(String authorizerAppid, long companyId, String openId) {
		return wechatFansMapper.selectOne(
				new LambdaQueryWrapper<WechatFans>()
						.eq(WechatFans::getOpenId, openId)
						.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
						.eq(WechatFans::getCompanyId, companyId));
	}

	private List<Long> listBindTagIds(String authorizerAppid, long companyId, String openId) {
		return wechatFansBindWechatTagMapper
				.selectList(
						new LambdaQueryWrapper<WechatFansBindWechatTag>()
								.eq(WechatFansBindWechatTag::getOpenId, openId)
								.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppid)
								.eq(WechatFansBindWechatTag::getCompanyId, companyId))
				.stream()
				.map(WechatFansBindWechatTag::getTagId)
				.toList();
	}

	private void deleteAllBinds(String authorizerAppid, long companyId, String openId) {
		wechatFansBindWechatTagMapper.delete(
				new LambdaQueryWrapper<WechatFansBindWechatTag>()
						.eq(WechatFansBindWechatTag::getOpenId, openId)
						.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppid)
						.eq(WechatFansBindWechatTag::getCompanyId, companyId));
	}
}
