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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.domain.TradeRateReply;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateReplyMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeRateAdminReplyService {

	private final TradeRateMapper tradeRateMapper;
	private final TradeRateReplyMapper tradeRateReplyMapper;

	public TradeRateAdminReplyService(
			TradeRateMapper tradeRateMapper, TradeRateReplyMapper tradeRateReplyMapper) {
		this.tradeRateMapper = tradeRateMapper;
		this.tradeRateReplyMapper = tradeRateReplyMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> replyTradeRate(long companyId, long operatorId, long rateId, String content) {
		int contentLen = content.getBytes(StandardCharsets.UTF_8).length;

		LambdaUpdateWrapper<TradeRate> uw = new LambdaUpdateWrapper<>();
		uw.eq(TradeRate::getCompanyId, companyId).eq(TradeRate::getRateId, rateId);
		TradeRate patch = new TradeRate();
		patch.setIsReply(true);
		int updated = tradeRateMapper.update(patch, uw);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		TradeRateReply row = new TradeRateReply();
		row.setCompanyId(companyId);
		row.setRateId(rateId);
		row.setContent(content);
		row.setContentLen(contentLen);
		row.setRole("seller");
		row.setOperatorId(operatorId);
		row.setUserId(null);
		row.setCreated(now);
		row.setUpdated(now);

		tradeRateReplyMapper.insert(row);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("reply_id", row.getReplyId());
		data.put("rate_id", row.getRateId());
		data.put("company_id", row.getCompanyId());
		data.put("user_id", row.getUserId());
		data.put("operator_id", row.getOperatorId());
		data.put("content", row.getContent());
		data.put("content_len", row.getContentLen());
		data.put("role", "seller");
		data.put("created", row.getCreated());
		data.put("unionid", row.getUnionid());
		return data;
	}
}
