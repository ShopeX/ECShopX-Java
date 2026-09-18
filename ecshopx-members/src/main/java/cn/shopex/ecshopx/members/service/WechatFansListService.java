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

import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import cn.shopex.ecshopx.members.mapper.WechatTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatFansListService {

	private record TagFansPage(int totalCount, List<WechatFans> records) {}

	private final WechatFansMapper wechatFansMapper;
	private final WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;
	private final WechatTagsMapper wechatTagsMapper;

	public WechatFansListService(
			WechatFansMapper wechatFansMapper,
			WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper,
			WechatTagsMapper wechatTagsMapper) {
		this.wechatFansMapper = wechatFansMapper;
		this.wechatFansBindWechatTagMapper = wechatFansBindWechatTagMapper;
		this.wechatTagsMapper = wechatTagsMapper;
	}

	public Map<String, Object> getWxFansList(
			String authorizerAppid,
			Long companyId,
			int page,
			int pageSize,
			String nickname,
			String remark,
			String tagIdParam,
			String subscribedParam) {

		List<Map<String, Object>> tagRows = wechatTagsMapper.selectTagsWithBindCount(authorizerAppid, companyId);
		Map<String, Map<String, Object>> tagById = new LinkedHashMap<>();
		for (Map<String, Object> row : tagRows) {
			Object tid = row.get("tag_id");
			String key = String.valueOf(tid);
			Map<String, Object> tagMap = new LinkedHashMap<>();
			tagMap.put("tag_id", tid);
			tagMap.put("tag_name", row.get("tag_name"));
			tagMap.put("total", row.get("total"));
			tagById.put(key, tagMap);
		}

		boolean tagBranch =
				StringUtils.hasText(tagIdParam) && !"0".equals(tagIdParam);

		int totalCount;
		List<WechatFans> records;

		if (tagBranch) {
			Long tagId;
			try {
				tagId = Long.parseLong(tagIdParam.trim());
			} catch (NumberFormatException e) {
				Map<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0);
				empty.put("list", Collections.emptyList());
				return empty;
			}

			TagFansPage tagFansPage =
					loadSubscribedFansPageByNumericTagId(tagId, companyId, authorizerAppid, page, pageSize);
			totalCount = tagFansPage.totalCount();
			records = tagFansPage.records();
		} else {
			Boolean subscribedFilter = parseSubscribedFilter(subscribedParam);
			LambdaQueryWrapper<WechatFans> wrapper = new LambdaQueryWrapper<>();
			wrapper
					.eq(WechatFans::getCompanyId, companyId)
					.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
					.eq(WechatFans::getSubscribed, subscribedFilter);
			if (StringUtils.hasText(nickname)) {
				String escaped = escapeLike(nickname);
				wrapper.and(w -> w.like(WechatFans::getNickname, "%" + escaped + "%"));
			}
			if (StringUtils.hasText(remark)) {
				String escaped = escapeLike(remark);
				wrapper.and(w -> w.like(WechatFans::getRemark, "%" + escaped + "%"));
			}
			totalCount = Math.toIntExact(wechatFansMapper.selectCount(wrapper));
			wrapper.orderByDesc(WechatFans::getCreated);
			if (totalCount == 0) {
				records = Collections.emptyList();
			} else {
				Page<WechatFans> mpPage = new Page<>(page, pageSize);
				mpPage.setSearchCount(false);
				wechatFansMapper.selectPage(mpPage, wrapper);
				records = mpPage.getRecords();
			}
		}

		List<LinkedHashMap<String, Object>> listRows =
				records.stream().map(this::toFanRow).collect(Collectors.toList());
		enrichTagNamesForRows(listRows, tagById);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listRows);
		return out;
	}

	public Map<String, Object> getWxFansOfTag(
			String authorizerAppid, Long companyId, int page, int pageSize, String tagIdParam) {
		long tagIdNumeric;
		try {
			tagIdNumeric = Long.parseLong(tagIdParam.trim());
		} catch (NumberFormatException e) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", 0);
			out.put("list", Collections.emptyList());
			return out;
		}
		TagFansPage pageResult =
				loadSubscribedFansPageByNumericTagId(tagIdNumeric, companyId, authorizerAppid, page, pageSize);
		List<LinkedHashMap<String, Object>> listRows =
				pageResult.records().stream()
						.map(this::toFanRowForTagFansEndpoint)
						.collect(Collectors.toList());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", pageResult.totalCount());
		out.put("list", listRows);
		return out;
	}

	private TagFansPage loadSubscribedFansPageByNumericTagId(
			long tagId, Long companyId, String authorizerAppid, int page, int pageSize) {
		LambdaQueryWrapper<WechatFansBindWechatTag> bindWrapper = new LambdaQueryWrapper<>();
		bindWrapper
				.eq(WechatFansBindWechatTag::getTagId, tagId)
				.eq(WechatFansBindWechatTag::getCompanyId, companyId)
				.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppid)
				.select(WechatFansBindWechatTag::getOpenId);
		List<WechatFansBindWechatTag> binds = wechatFansBindWechatTagMapper.selectList(bindWrapper);
		Set<String> openIdSet = new LinkedHashSet<>();
		for (WechatFansBindWechatTag b : binds) {
			if (b.getOpenId() != null) {
				openIdSet.add(b.getOpenId());
			}
		}
		if (openIdSet.isEmpty()) {
			return new TagFansPage(0, Collections.emptyList());
		}
		LambdaQueryWrapper<WechatFans> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.in(WechatFans::getOpenId, openIdSet)
				.eq(WechatFans::getCompanyId, companyId)
				.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
				.eq(WechatFans::getSubscribed, Boolean.TRUE);
		int totalCount = Math.toIntExact(wechatFansMapper.selectCount(wrapper));
		wrapper.orderByDesc(WechatFans::getCreated);
		if (totalCount == 0) {
			return new TagFansPage(totalCount, Collections.emptyList());
		}
		Page<WechatFans> mpPage = new Page<>(page, pageSize);
		mpPage.setSearchCount(false);
		wechatFansMapper.selectPage(mpPage, wrapper);
		return new TagFansPage(totalCount, mpPage.getRecords());
	}

	private LinkedHashMap<String, Object> toFanRowForTagFansEndpoint(WechatFans entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", entity.getCompanyId());
		row.put("authorizer_appid", entity.getAuthorizerAppid());
		row.put("subscribed", entity.getSubscribed());
		row.put("open_id", entity.getOpenId());
		row.put("nickname", entity.getNickname());
		row.put("sex", entity.getSex());
		row.put("city", entity.getCity());
		row.put("country", entity.getCountry());
		row.put("province", entity.getProvince());
		row.put("language", entity.getLanguage());
		row.put("headimgurl", entity.getHeadimgurl());
		row.put("subscribe_time", entity.getSubscribeTime());
		row.put("unionid", entity.getUnionid());
		row.put("remark", entity.getRemark());
		row.put("groupid", entity.getGroupid());
		row.put("tagids", entity.getTagids() == null ? "" : entity.getTagids());
		row.put("tagpop", entity.getTagpop());
		row.put("remarkpop", entity.getRemarkpop());
		return row;
	}

	private static Boolean parseSubscribedFilter(String subscribedParam) {
		if (subscribedParam == null) {
			return Boolean.TRUE;
		}
		String s = subscribedParam.trim();
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return Boolean.FALSE;
		}
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return Boolean.TRUE;
		}
		return Boolean.TRUE;
	}

	private static String escapeLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private LinkedHashMap<String, Object> toFanRow(WechatFans entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", entity.getCompanyId());
		row.put("authorizer_appid", entity.getAuthorizerAppid());
		row.put("subscribed", entity.getSubscribed());
		row.put("open_id", entity.getOpenId());
		row.put("nickname", entity.getNickname());
		row.put("sex", entity.getSex());
		row.put("city", entity.getCity());
		row.put("country", entity.getCountry());
		row.put("province", entity.getProvince());
		row.put("language", entity.getLanguage());
		row.put("headimgurl", entity.getHeadimgurl());
		row.put("subscribe_time", entity.getSubscribeTime());
		row.put("unionid", entity.getUnionid());
		row.put("remark", entity.getRemark());
		row.put("groupid", entity.getGroupid());
		row.put("tagpop", entity.getTagpop());
		row.put("remarkpop", entity.getRemarkpop());
		String tagidsCsv = entity.getTagids() == null ? "" : entity.getTagids();
		row.put("tagids_csv", tagidsCsv);
		return row;
	}

	private void enrichTagNamesForRows(
			List<LinkedHashMap<String, Object>> rows, Map<String, Map<String, Object>> tagById) {
		for (LinkedHashMap<String, Object> row : rows) {
			enrichTagNamesForRow(row, tagById);
		}
	}

	private void enrichTagNamesForRow(
			LinkedHashMap<String, Object> row, Map<String, Map<String, Object>> tagById) {
		Object rawObj = row.get("tagids_csv");
		String raw = rawObj == null ? "" : String.valueOf(rawObj);

		List<Map<String, Object>> tags = new ArrayList<>();
		List<String> tagids = new ArrayList<>();
		if (StringUtils.hasText(raw)) {
			String[] parts = raw.split(",", -1);
			for (String part : parts) {
				String token = part.trim();
				if (token.isEmpty()) {
					continue;
				}
				Map<String, Object> tagInfo = tagById.get(token);
				if (tagInfo != null) {
					tags.add(tagInfo);
					tagids.add(token);
				}
			}
		}
		row.put("tags", tags);
		row.put("tagids", tagids);
		row.remove("tagids_csv");
	}
}
