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

package cn.shopex.ecshopx.ali.service.alitemplate;

import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.common.promotions.port.AliTemplateMsgSendDispatchPublisher;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ItemStoreGoodsArrivalAliTemplateOrchestrator {

	private static final String SCENE = "goodsArrivalNotice";

	private final ItemCompanyIdResolver itemCompanyIdResolver;
	private final SubscribeNoticeMapper subscribeNoticeMapper;
	private final MembersSubscribeNoticeItemNamePort membersSubscribeNoticeItemNamePort;
	private final AliTemplateMsgSendDispatchPublisher aliTemplateMsgSendDispatchPublisher;

	public void dispatchIfApplicable(long itemId, int store, long distributorId) {
		if (store <= 0 || itemId <= 0L) {
			return;
		}
		Optional<Long> companyIdOpt = itemCompanyIdResolver.findCompanyIdByItemId(itemId);
		if (companyIdOpt.isEmpty()) {
			return;
		}
		long companyId = companyIdOpt.get();
		int dist = (int) Math.min(Math.max(distributorId, 0L), Integer.MAX_VALUE);
		List<SubscribeNotice> pending =
				subscribeNoticeMapper.selectList(
						new LambdaQueryWrapper<SubscribeNotice>()
								.eq(SubscribeNotice::getCompanyId, companyId)
								.eq(SubscribeNotice::getRelId, itemId)
								.eq(SubscribeNotice::getSubType, "goods")
								.eq(SubscribeNotice::getSubStatus, "NO")
								.eq(SubscribeNotice::getDistributorId, dist)
								.eq(SubscribeNotice::getSource, "alipay"));
		if (pending.isEmpty()) {
			return;
		}
		String itemName =
				membersSubscribeNoticeItemNamePort.resolveItemName(companyId, itemId, "zh-CN");
		String displayName = truncateByCodePoints(itemName, 20);
		Map<String, Object> dataFields = new LinkedHashMap<>();
		dataFields.put("item_name", displayName);
		dataFields.put("notice", "您关注的商品已到货，欢迎选购");
		for (SubscribeNotice row : pending) {
			if (row.getOpenId() == null || !StringUtils.hasText(row.getOpenId())) {
				continue;
			}
			Map<String, Object> send = new LinkedHashMap<>();
			send.put("scenes_name", SCENE);
			send.put("company_id", companyId);
			send.put("to_user_id", row.getOpenId().trim());
			send.put("data", dataFields);
			send.put("page_query_str", "id=" + itemId);
			aliTemplateMsgSendDispatchPublisher.publish(send, false);
		}
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
