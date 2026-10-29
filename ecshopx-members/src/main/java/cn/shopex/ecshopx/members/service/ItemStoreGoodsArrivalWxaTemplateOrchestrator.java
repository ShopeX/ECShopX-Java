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

import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.common.port.members.GoodsArrivalWxaSubscribeSendPort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns with PHP {@code SendTemplateMsgListener} wechat branch for {@code goodsArrivalNotice}.
 */
@Service
public class ItemStoreGoodsArrivalWxaTemplateOrchestrator {

	private static final Logger log =
			LoggerFactory.getLogger(ItemStoreGoodsArrivalWxaTemplateOrchestrator.class);

	private static final String SCENE = "goodsArrivalNotice";
	private static final String TEMPLATE_YYKWEISHOP = "yykweishop";
	private static final String REDIS_KEY_PREFIX = "arrivalNoticeSub";
	private static final String NOTICE_TEXT = "亲！您想要的商品已经到货，可以购买啦！";

	private final ItemCompanyIdResolver itemCompanyIdResolver;
	private final GoodsArrivalSubscribeMatchService goodsArrivalSubscribeMatchService;
	private final MembersSubscribeNoticeItemNamePort membersSubscribeNoticeItemNamePort;
	private final GoodsArrivalWxaSubscribeSendPort goodsArrivalWxaSubscribeSendPort;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public ItemStoreGoodsArrivalWxaTemplateOrchestrator(
			ItemCompanyIdResolver itemCompanyIdResolver,
			GoodsArrivalSubscribeMatchService goodsArrivalSubscribeMatchService,
			MembersSubscribeNoticeItemNamePort membersSubscribeNoticeItemNamePort,
			GoodsArrivalWxaSubscribeSendPort goodsArrivalWxaSubscribeSendPort,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.itemCompanyIdResolver = itemCompanyIdResolver;
		this.goodsArrivalSubscribeMatchService = goodsArrivalSubscribeMatchService;
		this.membersSubscribeNoticeItemNamePort = membersSubscribeNoticeItemNamePort;
		this.goodsArrivalWxaSubscribeSendPort = goodsArrivalWxaSubscribeSendPort;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void dispatchIfApplicable(long itemId, int store, long distributorId) {
		try {
			if (store <= 0 || itemId <= 0L) {
				return;
			}
			Optional<Long> companyIdOpt = itemCompanyIdResolver.findCompanyIdByItemId(itemId);
			if (companyIdOpt.isEmpty()) {
				return;
			}
			long companyId = companyIdOpt.get();
			int dist = GoodsArrivalSubscribeMatchService.normalizeDistributorId(distributorId);
			List<SubscribeNotice> pending =
					goodsArrivalSubscribeMatchService.listPending(companyId, itemId, dist, "wechat");
			if (pending.isEmpty()) {
				return;
			}
			String fallbackAppid =
					weappAuthorizerAppidRepository
							.findAuthorizerAppid(companyId, TEMPLATE_YYKWEISHOP)
							.orElse("");
			String itemName =
					membersSubscribeNoticeItemNamePort.resolveItemName(companyId, itemId, "zh-CN");
			String displayName = truncateByCodePoints(itemName, 20);
			Map<String, Object> dataFields = new LinkedHashMap<>();
			dataFields.put("item_name", displayName);
			dataFields.put("notice", NOTICE_TEXT);

			for (SubscribeNotice row : pending) {
				if (!GoodsArrivalSubscribeMatchService.hasOpenId(row)) {
					continue;
				}
				int rowDist = row.getDistributorId() == null ? 0 : row.getDistributorId();
				String redisKey = buildArrivalNoticeRedisKey(companyId, itemId, rowDist);
				String appid = resolveAppid(redisKey, row.getUserId(), fallbackAppid);
				if (!StringUtils.hasText(appid)) {
					continue;
				}
				Map<String, Object> send = new LinkedHashMap<>();
				send.put("scenes_name", SCENE);
				send.put("company_id", companyId);
				send.put("appid", appid);
				send.put("openid", row.getOpenId().trim());
				send.put("data", dataFields);
				send.put("page_query_str", "id=" + itemId);
				goodsArrivalWxaSubscribeSendPort.send(send, true);
			}
		} catch (RuntimeException e) {
			log.debug("goodsArrivalNotice wxa send skipped: {}", e.toString());
		}
	}

	private String resolveAppid(String redisKey, Long userId, String fallbackAppid) {
		if (userId != null) {
			Object hv = sharedStringRedisTemplate.opsForHash().get(redisKey, String.valueOf(userId));
			if (hv != null) {
				String raw = hv instanceof String s ? s : String.valueOf(hv);
				try {
					JsonNode node = objectMapper.readTree(raw);
					String fromRedis = node.path("wxa_appid").asText("");
					if (StringUtils.hasText(fromRedis)) {
						return fromRedis.trim();
					}
				} catch (Exception ignored) {
					// fall through
				}
			}
		}
		return fallbackAppid == null ? "" : fallbackAppid.trim();
	}

	private static String buildArrivalNoticeRedisKey(long companyId, long itemId, int distributorId) {
		String cid = String.valueOf(companyId);
		String iid = String.valueOf(itemId);
		if (distributorId > 0) {
			return REDIS_KEY_PREFIX + ":" + cid + ":" + iid + ":" + distributorId;
		}
		return REDIS_KEY_PREFIX + ":" + cid + ":" + iid;
	}

	private static String truncateByCodePoints(String s, int maxCodePoints) {
		if (s == null || s.isEmpty() || maxCodePoints <= 0) {
			return s == null ? "" : s;
		}
		int count = 0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			if (count >= maxCodePoints) {
				break;
			}
			sb.appendCodePoint(cp);
			count++;
			i += Character.charCount(cp);
		}
		return sb.toString();
	}
}
