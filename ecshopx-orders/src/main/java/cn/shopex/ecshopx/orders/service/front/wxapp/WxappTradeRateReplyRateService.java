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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
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
public class WxappTradeRateReplyRateService {

	private final TradeRateMapper tradeRateMapper;
	private final TradeRateReplyMapper tradeRateReplyMapper;

	public WxappTradeRateReplyRateService(
			TradeRateMapper tradeRateMapper, TradeRateReplyMapper tradeRateReplyMapper) {
		this.tradeRateMapper = tradeRateMapper;
		this.tradeRateReplyMapper = tradeRateReplyMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> replyRate(Map<String, Object> merged, Map<String, Object> auth) {
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		String unionid =
				auth.get("unionid") == null ? null : String.valueOf(auth.get("unionid")).trim();

		Object rateIdRaw = merged.get("rate_id");
		if (rateIdRaw == null) {
			throw new BadRequestException("参数异常");
		}
		if (rateIdRaw instanceof String s && s.isEmpty()) {
			throw new BadRequestException("参数异常");
		}
		String rateIdParse = String.valueOf(rateIdRaw).trim();
		if (rateIdParse.isEmpty()) {
			throw new BadRequestException("参数异常");
		}
		long rateId;
		try {
			rateId = Long.parseLong(rateIdParse);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数异常");
		}

		Object contentRaw = merged.get("content");
		if (contentRaw == null) {
			throw new BadRequestException("评论不能为空！");
		}
		String content = String.valueOf(contentRaw);
		if (content.isEmpty()) {
			throw new BadRequestException("评论不能为空！");
		}

		int contentLen = content.getBytes(StandardCharsets.UTF_8).length;

		boolean sellerBranch = merged.containsKey("operator_id") && merged.get("operator_id") != null;

		int now = (int) (System.currentTimeMillis() / 1000L);

		TradeRateReply row;
		if (sellerBranch) {
			Object opRaw = merged.get("operator_id");
			long operatorId;
			if (opRaw instanceof Number n) {
				operatorId = n.longValue();
			} else {
				try {
					operatorId = Long.parseLong(String.valueOf(opRaw).trim());
				} catch (NumberFormatException e) {
					throw new BadRequestException("参数异常");
				}
			}

			LambdaUpdateWrapper<TradeRate> uw = new LambdaUpdateWrapper<>();
			uw.eq(TradeRate::getCompanyId, companyId).eq(TradeRate::getRateId, rateId);
			TradeRate patch = new TradeRate();
			patch.setIsReply(true);
			int updated = tradeRateMapper.update(patch, uw);
			if (updated == 0) {
				throw new ResourceException("未查询到更新数据");
			}

			row = new TradeRateReply();
			row.setCompanyId(companyId);
			row.setRateId(rateId);
			row.setContent(content);
			row.setContentLen(contentLen);
			row.setRole("seller");
			row.setOperatorId(operatorId);
			row.setUserId(null);
			row.setUnionid(null);
			row.setCreated(now);
			row.setUpdated(now);
			tradeRateReplyMapper.insert(row);
		} else {
			row = new TradeRateReply();
			row.setCompanyId(companyId);
			row.setRateId(rateId);
			row.setContent(content);
			row.setContentLen(contentLen);
			row.setRole("buyer");
			row.setUserId(userId);
			row.setUnionid(unionid);
			row.setOperatorId(null);
			row.setCreated(now);
			row.setUpdated(now);
			tradeRateReplyMapper.insert(row);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("reply_id", row.getReplyId());
		data.put("rate_id", row.getRateId());
		data.put("company_id", row.getCompanyId());
		data.put("user_id", row.getUserId());
		data.put("operator_id", row.getOperatorId());
		data.put("content", row.getContent());
		data.put("content_len", row.getContentLen());
		data.put("role", row.getRole());
		data.put("created", row.getCreated());
		data.put("unionid", row.getUnionid());
		return data;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
