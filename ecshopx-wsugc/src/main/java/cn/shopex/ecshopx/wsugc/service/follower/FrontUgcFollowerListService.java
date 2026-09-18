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

package cn.shopex.ecshopx.wsugc.service.follower;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Follower;
import cn.shopex.ecshopx.wsugc.mapper.FollowerMapper;
import cn.shopex.ecshopx.wsugc.service.setting.UgcOfficialUserInfoReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcFollowerListService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcFollowerListService.class);

	private final FollowerMapper followerMapper;
	private final MemberAccountService memberAccountService;
	private final UgcOfficialUserInfoReadService ugcOfficialUserInfoReadService;

	public FrontUgcFollowerListService(
			FollowerMapper followerMapper,
			MemberAccountService memberAccountService,
			UgcOfficialUserInfoReadService ugcOfficialUserInfoReadService) {
		this.followerMapper = followerMapper;
		this.memberAccountService = memberAccountService;
		this.ugcOfficialUserInfoReadService = ugcOfficialUserInfoReadService;
	}

	public Map<String, Object> buildList(
			Long listUserId, long companyId, String userType, int pageNo, int pageSize) {
		LambdaQueryWrapper<Follower> w = new LambdaQueryWrapper<>();
		w.eq(Follower::getDisabled, false).eq(Follower::getCompanyId, companyId);
		if ("user".equals(userType)) {
			if (listUserId == null) {
				w.isNull(Follower::getUserId);
			} else {
				w.eq(Follower::getUserId, listUserId);
			}
		} else {
			if (listUserId == null) {
				w.isNull(Follower::getFollowerUserId);
			} else {
				w.eq(Follower::getFollowerUserId, listUserId);
			}
		}
		w.orderByDesc(Follower::getCreated);

		Page<Follower> page = new Page<>(pageNo, pageSize);
		followerMapper.selectPage(page, w);

		List<Map<String, Object>> list = new ArrayList<>();
		List<Follower> records = page.getRecords() != null ? page.getRecords() : List.of();

		for (Follower v : records) {
			long memberUserId;
			if ("user".equals(userType)) {
				Long fu = v.getFollowerUserId();
				memberUserId = fu != null ? fu : 0L;
			} else {
				Long uid = v.getUserId();
				memberUserId = uid != null ? uid : 0L;
			}

			if (memberUserId > 0L) {
				Map<String, Object> filter = new LinkedHashMap<>();
				filter.put("user_id", memberUserId);
				Long rowCompany = v.getCompanyId();
				filter.put("company_id", rowCompany != null ? rowCompany : 0L);
				Map<String, Object> userInfo = memberAccountService.getWechatUserInfo(filter);
				if (userInfo.isEmpty() || !userInfo.containsKey("nickname")) {
					throw new ResourceException("用户信息不完整");
				}
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("user_id", memberUserId);
				row.put("nickname", stringVal(userInfo.get("nickname")));
				row.put("headimgurl", stringVal(userInfo.get("headimgurl")));
				row.put("mutal_follow", computeMutualFollow(v.getFollowerUserId(), v.getUserId()));
				list.add(row);
			} else {
				long cid = v.getCompanyId() != null ? v.getCompanyId() : 0L;
				Map<String, String> official = ugcOfficialUserInfoReadService.loadOfficialDisplay(cid);
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("user_id", 0L);
				row.put("nickname", official.getOrDefault("official.nickname", ""));
				row.put("headimgurl", official.getOrDefault("official.headerimgurl", ""));
				row.put("mutal_follow", computeMutualFollow(v.getFollowerUserId(), v.getUserId()));
				list.add(row);
			}
		}

		long totalCount = page.getTotal() - (records.size() - list.size());

		TreeMap<String, Object> pager = new TreeMap<>();
		pager.put("count", totalCount);
		pager.put("page_no", pageNo);
		pager.put("page_size", pageSize);

		TreeMap<String, Object> body = new TreeMap<>();
		body.put("list", list);
		body.put("pager", pager);
		body.put("total_count", totalCount);
		return body;
	}

	public int computeMutualFollowForMessage(long fromUserId, long toUserId) {
		return computeMutualFollow(fromUserId, toUserId);
	}

	private int computeMutualFollow(Long followerUserId, Long userId) {
		long fu = followerUserId != null ? followerUserId : 0L;
		long u = userId != null ? userId : 0L;
		return computeMutualFollow(fu, u);
	}

	private int computeMutualFollow(long followerUserId, long userId) {
		long fu = followerUserId;
		long u = userId;
		LambdaQueryWrapper<Follower> w1 = new LambdaQueryWrapper<>();
		w1.eq(Follower::getUserId, u)
				.eq(Follower::getFollowerUserId, fu)
				.eq(Follower::getDisabled, false);
		LambdaQueryWrapper<Follower> w2 = new LambdaQueryWrapper<>();
		w2.eq(Follower::getUserId, fu)
				.eq(Follower::getFollowerUserId, u)
				.eq(Follower::getDisabled, false);
		long c1 = followerMapper.selectCount(w1);
		long c2 = followerMapper.selectCount(w2);
		int out = (c1 == 1L && c2 == 1L) ? 1 : 0;
		if (log.isDebugEnabled()) {
			log.debug("mutual follow followerUserId={} userId={} -> {}", fu, u, out);
		}
		return out;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}
}
