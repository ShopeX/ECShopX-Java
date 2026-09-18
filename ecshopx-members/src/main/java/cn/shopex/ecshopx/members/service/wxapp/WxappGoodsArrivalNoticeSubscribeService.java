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

package cn.shopex.ecshopx.members.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappGoodsArrivalNoticeSubscribeService {

	private static final String REDIS_KEY_PREFIX = "arrivalNoticeSub";

	private static final Pattern POSITIVE_ITEM_ID = Pattern.compile("^[1-9]\\d*$");

	private final SubscribeNoticeMapper subscribeNoticeMapper;

	private final MembersSubscribeNoticeItemNamePort itemNamePort;

	private final StringRedisTemplate sharedStringRedisTemplate;

	private final ObjectMapper subscribeJsonMapper = new ObjectMapper();

	public WxappGoodsArrivalNoticeSubscribeService(
			SubscribeNoticeMapper subscribeNoticeMapper,
			MembersSubscribeNoticeItemNamePort itemNamePort,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.subscribeNoticeMapper = subscribeNoticeMapper;
		this.itemNamePort = itemNamePort;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public Object itemsSubscribe(
			long companyId,
			long userId,
			String openId,
			Object wxaAppid,
			String source,
			int distributorId,
			long itemId,
			String acceptLanguageHeader) {
		SubscribeNotice existing =
				subscribeNoticeMapper.selectOne(buildRowFilter(userId, companyId, itemId, source, distributorId));
		if (existing != null) {
			subscribeNoticeMapper.delete(buildRowFilter(userId, companyId, itemId, source, distributorId));
		} else {
			if (!itemNamePort.itemExists(companyId, itemId)) {
				throw new ResourceException("商品不存在");
			}
			String remarks = itemNamePort.resolveItemName(companyId, itemId, acceptLanguageHeader);
			long now = Instant.now().getEpochSecond();
			SubscribeNotice row = new SubscribeNotice();
			row.setCompanyId(companyId);
			row.setUserId(userId);
			row.setOpenId(openId);
			row.setSource(source);
			row.setRelId(itemId);
			row.setSubType("goods");
			row.setRemarks(remarks);
			row.setSubStatus("NO");
			row.setDistributorId(distributorId);
			row.setCreated(now);
			row.setUpdated(now);
			subscribeNoticeMapper.insert(row);
		}

		String redisKey = buildArrivalNoticeRedisKey(source, companyId, itemId, distributorId);
		String userField = String.valueOf(userId);
		boolean inRedis = Boolean.TRUE.equals(sharedStringRedisTemplate.opsForHash().hasKey(redisKey, userField));
		if (inRedis) {
			sharedStringRedisTemplate.opsForHash().delete(redisKey, userField);
		} else {
			String json = buildSubscribeJson(openId, wxaAppid);
			sharedStringRedisTemplate.opsForHash().put(redisKey, userField, json);
		}

		SubscribeNotice info =
				subscribeNoticeMapper.selectOne(buildRowFilter(userId, companyId, itemId, source, distributorId));
		if (info == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, String> out = new LinkedHashMap<>(3);
		out.put("user_id", String.valueOf(info.getUserId()));
		out.put("item_id", String.valueOf(info.getRelId()));
		out.put("item_name", info.getRemarks() == null ? "" : info.getRemarks());
		return out;
	}

	public Object isSubscribe(
			long companyId,
			long userId,
			String source,
			int distributorId,
			String itemIdPathRaw,
			String acceptLanguageHeader) {
		String redisKey = buildArrivalNoticeRedisKey(source, companyId, itemIdPathRaw, distributorId);
		String field = String.valueOf(userId);
		Object hv = sharedStringRedisTemplate.opsForHash().get(redisKey, field);
		if (hv == null) {
			return Collections.emptyList();
		}
		String raw = hv instanceof String s ? s : String.valueOf(hv);
		if (raw.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, String> out = new LinkedHashMap<>(3);
		out.put("user_id", String.valueOf(userId));
		out.put("item_id", itemIdPathRaw == null ? "" : itemIdPathRaw);
		String itemName = "";
		if (itemIdPathRaw != null && POSITIVE_ITEM_ID.matcher(itemIdPathRaw).matches()) {
			try {
				long parsedLong = Long.parseLong(itemIdPathRaw);
				String resolved = itemNamePort.resolveItemName(companyId, parsedLong, acceptLanguageHeader);
				itemName = resolved == null ? "" : resolved;
			} catch (NumberFormatException e) {
				itemName = "";
			}
		}
		out.put("item_name", itemName);
		return out;
	}

	private static LambdaQueryWrapper<SubscribeNotice> buildRowFilter(
			long userId, long companyId, long itemId, String source, int distributorId) {
		return new LambdaQueryWrapper<SubscribeNotice>()
				.eq(SubscribeNotice::getUserId, userId)
				.eq(SubscribeNotice::getCompanyId, companyId)
				.eq(SubscribeNotice::getRelId, itemId)
				.eq(SubscribeNotice::getSubType, "goods")
				.eq(SubscribeNotice::getSubStatus, "NO")
				.eq(SubscribeNotice::getSource, source)
				.eq(SubscribeNotice::getDistributorId, distributorId);
	}

	private String buildArrivalNoticeRedisKey(String source, long companyId, long itemId, int distributorId) {
		String cid = String.valueOf(companyId);
		String iid = String.valueOf(itemId);
		if ("wechat".equals(source)) {
			if (distributorId > 0) {
				return REDIS_KEY_PREFIX + ":" + cid + ":" + iid + ":" + distributorId;
			}
			return REDIS_KEY_PREFIX + ":" + cid + ":" + iid;
		}
		if (distributorId > 0) {
			return REDIS_KEY_PREFIX + ":" + source + ":" + cid + ":" + iid + ":" + distributorId;
		}
		return REDIS_KEY_PREFIX + ":" + source + ":" + cid + ":" + iid;
	}

	private String buildArrivalNoticeRedisKey(
			String source, long companyId, String itemIdPathRaw, int distributorId) {
		String cid = String.valueOf(companyId);
		String iid = itemIdPathRaw == null ? "" : itemIdPathRaw;
		if ("wechat".equals(source)) {
			if (distributorId > 0) {
				return REDIS_KEY_PREFIX + ":" + cid + ":" + iid + ":" + distributorId;
			}
			return REDIS_KEY_PREFIX + ":" + cid + ":" + iid;
		}
		if (distributorId > 0) {
			return REDIS_KEY_PREFIX + ":" + source + ":" + cid + ":" + iid + ":" + distributorId;
		}
		return REDIS_KEY_PREFIX + ":" + source + ":" + cid + ":" + iid;
	}

	private String buildSubscribeJson(String openId, Object wxaAppidRaw) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>(2);
		payload.put("open_id", openId);
		payload.put("wxa_appid", wxaAppidRaw);
		try {
			return subscribeJsonMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
