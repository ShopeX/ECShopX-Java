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

package cn.shopex.ecshopx.comments.service;

import cn.shopex.ecshopx.comments.domain.ShopComments;
import cn.shopex.ecshopx.comments.mapper.ShopCommentsMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopCommentListService {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(APP_ZONE);

	private final ShopCommentsMapper shopCommentsMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public ShopCommentListService(
			ShopCommentsMapper shopCommentsMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.shopCommentsMapper = shopCommentsMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> list(long companyId, Map<String, Object> filterParams, int pageNo, int pageSize) {
		LambdaQueryWrapper<ShopComments> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ShopComments::getCompanyId, String.valueOf(companyId));
		if (filterParams != null) {
			if (filterParams.containsKey("hid")) {
				boolean hidVal = normalizeHidFilter(filterParams.get("hid"));
				wrapper.eq(ShopComments::getHid, hidVal);
			}
			if (filterParams.containsKey("shop_id")) {
				Object sid = filterParams.get("shop_id");
				wrapper.eq(ShopComments::getShopId, sid != null ? sid.toString() : "");
			}
		}
		wrapper.orderByDesc(ShopComments::getStuck).orderByDesc(ShopComments::getCreated);

		Page<ShopComments> page = new Page<>(pageNo, pageSize);
		shopCommentsMapper.selectPage(page, wrapper);
		int totalCount = (int) page.getTotal();
		List<ShopComments> records = page.getRecords();

		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (ShopComments row : records) {
			list.add(ShopCommentResponseMaps.toListItemMap(row, objectMapper));
		}

		if (list.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalCount);
			out.put("list", list);
			return out;
		}

		for (Map<String, Object> rowMap : list) {
			Map<String, Object> userFilter = new HashMap<>();
			userFilter.put("user_id", rowMap.get("user_id"));
			userFilter.put("company_id", rowMap.get("company_id") != null ? rowMap.get("company_id").toString() : "");
			Map<String, Object> userInfo = memberAccountService.getWechatUserInfo(userFilter);
			if (userInfo == null || userInfo.isEmpty()) {
				throw new ResourceException("用户信息错误");
			}
			rowMap.put("nickname", userInfo.get("nickname"));
			rowMap.put("headimgurl", userInfo.get("headimgurl"));
			Object createdVal = rowMap.get("created");
			rowMap.put("create_date", formatCreateDate(createdVal));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	private static String formatCreateDate(Object createdVal) {
		if (createdVal == null) {
			return "";
		}
		long sec;
		if (createdVal instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(createdVal.toString().trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		return CREATE_DATE_FMT.format(Instant.ofEpochSecond(sec));
	}

	private static boolean normalizeHidFilter(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			String lower = t.toLowerCase(Locale.ROOT);
			if ("1".equals(t) || "true".equals(lower)) {
				return true;
			}
			if ("0".equals(t) || "false".equals(lower)) {
				return false;
			}
			throw new BadRequestException("is_hide 参数无效");
		}
		throw new BadRequestException("is_hide 参数无效");
	}
}
