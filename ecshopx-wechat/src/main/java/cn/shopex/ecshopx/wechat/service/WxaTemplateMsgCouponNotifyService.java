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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaTemplateMsgCouponNotifyService {

	private static final Logger log = LoggerFactory.getLogger(WxaTemplateMsgCouponNotifyService.class);

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";

	private final WeappMapper weappMapper;
	private final WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;
	private final WxaCardUsedTemplateSendPort templateSendPort;

	public WxaTemplateMsgCouponNotifyService(
			WeappMapper weappMapper,
			WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort,
			WxaCardUsedTemplateSendPort templateSendPort) {
		this.weappMapper = weappMapper;
		this.wxaMemberOpenIdLookupPort = wxaMemberOpenIdLookupPort;
		this.templateSendPort = templateSendPort;
	}

	public void sendCardUsed(long companyId, long userId, Map<String, Object> userCardInfo) {
		try {
			doSendCardUsed(companyId, userId, userCardInfo);
		} catch (RuntimeException e) {
			log.debug("coupon used template notify skipped: {}", e.getMessage());
		}
	}

	public void sendReceiveCardSuccess(long companyId, long userId, Map<String, Object> cardSnapshot) {
		try {
			doSendReceiveCardSuccess(companyId, userId, cardSnapshot);
		} catch (RuntimeException e) {
			log.debug("receive card template notify skipped: {}", e.getMessage());
		}
	}

	private void doSendCardUsed(long companyId, long userId, Map<String, Object> userCardInfo) {
		String wxaAppid = resolveWxaAppId(companyId);
		if (!StringUtils.hasText(wxaAppid)) {
			return;
		}
		String openid = wxaMemberOpenIdLookupPort.resolveOpenId(userId, wxaAppid);
		if (!StringUtils.hasText(openid)) {
			return;
		}

		String cardType = stringVal(userCardInfo.get("card_type"));
		String remarks;
		if ("gift".equals(cardType)) {
			remarks = "无限制";
		} else {
			int least = intVal(userCardInfo.get("least_cost"));
			double yuan = least > 0 ? least / 100.0 : 0.01;
			remarks = "满" + yuan + "元可用";
		}

		String amount;
		if ("cash".equals(cardType)) {
			amount = (intVal(userCardInfo.get("reduce_cost")) / 100.0) + "元";
		} else if ("discount".equals(cardType)) {
			int d = intVal(userCardInfo.get("discount"));
			amount = ((100 - d) / 10.0) + "折";
		} else if ("gift".equals(cardType) || "new_gift".equals(cardType)) {
			amount = stringVal(userCardInfo.get("gift"));
			if (!StringUtils.hasText(amount)) {
				amount = "";
			}
		} else {
			amount = "";
		}

		int statusVal = intVal(userCardInfo.get("status"));
		String statusText = statusVal == 2 ? "已使用" : "已到账";

		int begin = intVal(userCardInfo.get("begin_date"));
		int end = intVal(userCardInfo.get("end_date"));
		ZoneId z = ZoneId.systemDefault();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(z);
		DateTimeFormatter secFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss").withZone(z);
		String activeDate = dayFmt.format(Instant.ofEpochSecond(begin)) + " - " + dayFmt.format(Instant.ofEpochSecond(end));
		String activeDateTime = secFmt.format(Instant.ofEpochSecond(end));

		Map<String, String> keywordData = new HashMap<>();
		keywordData.put("title", stringVal(userCardInfo.get("title")));
		keywordData.put("used_action", "至小程序商城购物可使用");
		keywordData.put("amount", amount);
		keywordData.put("status", statusText);
		keywordData.put("active_date", activeDate);
		keywordData.put("activedate", activeDateTime);
		keywordData.put("remarks", remarks);

		try {
			templateSendPort.send(companyId, userId, "cardUsedSucc", keywordData);
		} catch (RuntimeException e) {
			log.debug("coupon used template port failed: {}", e.getMessage());
		}
	}

	private void doSendReceiveCardSuccess(long companyId, long userId, Map<String, Object> cardSnapshot) {
		String wxaAppid = resolveWxaAppId(companyId);
		if (!StringUtils.hasText(wxaAppid)) {
			return;
		}
		String openid = wxaMemberOpenIdLookupPort.resolveOpenId(userId, wxaAppid);
		if (!StringUtils.hasText(openid)) {
			return;
		}

		String cardType = stringVal(cardSnapshot.get("card_type"));
		String remarks;
		if ("gift".equals(cardType)) {
			remarks = "无限制";
		} else {
			int least = intVal(cardSnapshot.get("least_cost"));
			double yuan = least > 0 ? least / 100.0 : 0.01;
			remarks = "满" + yuan + "元可用";
		}

		String amount;
		if ("cash".equals(cardType)) {
			amount = (intVal(cardSnapshot.get("reduce_cost")) / 100.0) + "元";
		} else if ("discount".equals(cardType)) {
			int d = intVal(cardSnapshot.get("discount"));
			amount = ((100 - d) / 10.0) + "折";
		} else if ("gift".equals(cardType) || "new_gift".equals(cardType)) {
			amount = stringVal(cardSnapshot.get("gift"));
			if (!StringUtils.hasText(amount)) {
				amount = "";
			}
		} else {
			amount = "";
		}

		String statusText = "已到账";

		int begin = intVal(cardSnapshot.get("begin_date"));
		int end = intVal(cardSnapshot.get("end_date"));
		ZoneId z = ZoneId.systemDefault();
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(z);
		DateTimeFormatter secFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss").withZone(z);
		String activeDate = dayFmt.format(Instant.ofEpochSecond(begin)) + " - " + dayFmt.format(Instant.ofEpochSecond(end));
		String activeDateTime = secFmt.format(Instant.ofEpochSecond(end));

		Map<String, String> keywordData = new HashMap<>();
		keywordData.put("title", stringVal(cardSnapshot.get("title")));
		keywordData.put("used_action", "至小程序商城购物可使用");
		keywordData.put("amount", amount);
		keywordData.put("status", statusText);
		keywordData.put("active_date", activeDate);
		keywordData.put("activedate", activeDateTime);
		keywordData.put("remarks", remarks);

		try {
			templateSendPort.send(companyId, userId, "userGetCardSucc", keywordData);
		} catch (RuntimeException e) {
			log.debug("receive card template port failed: {}", e.getMessage());
		}
	}

	private String resolveWxaAppId(long companyId) {
		Weapp row = weappMapper.selectOne(new LambdaQueryWrapper<Weapp>()
				.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getTemplateName, TEMPLATE_NAME_YYKWEISHOP)
				.last("LIMIT 1"));
		return row == null ? "" : stringVal(row.getAuthorizerAppid());
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
