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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.WechatRelCard;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.WechatRelCardMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UploadWechatCardWorker {

	private static final Logger log = LoggerFactory.getLogger(UploadWechatCardWorker.class);

	private final DiscountCardsMapper discountCardsMapper;
	private final WechatRelCardMapper wechatRelCardMapper;
	private final WxShopsSettingForWechatCardService wxShopsSettingForWechatCardService;
	private final UploadWechatCardPayloadBuilder payloadBuilder;

	public UploadWechatCardWorker(DiscountCardsMapper discountCardsMapper,
			WechatRelCardMapper wechatRelCardMapper,
			WxShopsSettingForWechatCardService wxShopsSettingForWechatCardService,
			UploadWechatCardPayloadBuilder payloadBuilder) {
		this.discountCardsMapper = discountCardsMapper;
		this.wechatRelCardMapper = wechatRelCardMapper;
		this.wxShopsSettingForWechatCardService = wxShopsSettingForWechatCardService;
		this.payloadBuilder = payloadBuilder;
	}

	public void execute(String authorizerAppId, long companyId, List<Long> cardIds) {
		List<DiscountCards> lists = discountCardsMapper.selectList(
				Wrappers.<DiscountCards>lambdaQuery()
						.eq(DiscountCards::getCompanyId, companyId)
						.in(DiscountCards::getCardId, cardIds));
		Map<String, Object> shopdata = wxShopsSettingForWechatCardService.load(companyId);
		for (DiscountCards card : lists) {
			try {
				processOne(authorizerAppId, companyId, card, shopdata);
			} catch (Exception e) {
				log.warn("upload wechat card skip card_id={} company_id={}: {}",
						card.getCardId(), companyId, e.getMessage());
			}
		}
	}

	private void processOne(String authorizerAppId, long companyId, DiscountCards card, Map<String, Object> shopdata) {
		WechatRelCard existing = wechatRelCardMapper.selectOne(
				Wrappers.<WechatRelCard>lambdaQuery()
						.eq(WechatRelCard::getCompanyId, companyId)
						.eq(WechatRelCard::getCardId, card.getCardId()));
		if (existing != null) {
			return;
		}
		String wechatCardId = payloadBuilder.buildAndCreate(authorizerAppId, card, shopdata);
		int now = (int) (System.currentTimeMillis() / 1000L);
		WechatRelCard row = new WechatRelCard();
		row.setCardId(card.getCardId());
		row.setCompanyId(companyId);
		row.setWechatCardId(wechatCardId);
		row.setCreated(now);
		row.setUpdated(now);
		wechatRelCardMapper.insert(row);
	}
}
