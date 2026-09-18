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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.SalemanCustomerComplaint;
import cn.shopex.ecshopx.salesperson.mapper.SalemanCustomerComplaintMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SalemanCustomerComplaintReplyService {

	private final SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;
	private final ObjectMapper objectMapper;

	public SalemanCustomerComplaintReplyService(SalemanCustomerComplaintMapper salemanCustomerComplaintMapper,
			ObjectMapper objectMapper) {
		this.salemanCustomerComplaintMapper = salemanCustomerComplaintMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> replySalemanCustomerComplaints(int complaintId, String replyContentTrimmed,
			Map<String, Object> operatorJwtUserData) {
		int now = (int) Instant.now().getEpochSecond();

		int replyOperatorId = parseOperatorIdLoose(operatorJwtUserData.get("operator_id"));
		String replyOperatorName = stringField(operatorJwtUserData.get("username"));
		String replyOperatorMobile = stringField(operatorJwtUserData.get("mobile"));

		SalemanCustomerComplaint existing = salemanCustomerComplaintMapper.selectById(complaintId);
		List<Map<String, Object>> history = new ArrayList<>();
		if (existing != null && StringUtils.hasText(existing.getReplyContent())) {
			try {
				List<Map<String, Object>> parsed = objectMapper.readValue(existing.getReplyContent(),
						new TypeReference<List<Map<String, Object>>>() {
						});
				if (parsed == null) {
					throw new ResourceException("客诉历史回复格式异常");
				}
				history = new ArrayList<>(parsed);
			} catch (JsonProcessingException e) {
				throw new ResourceException("客诉历史回复格式异常");
			}
		}

		Map<String, Object> tmp = new LinkedHashMap<>();
		tmp.put("reply_operator_id", replyOperatorId);
		tmp.put("reply_operator_name", replyOperatorName);
		tmp.put("reply_operator_mobile", replyOperatorMobile);
		tmp.put("reply_time", now);
		tmp.put("reply_content", replyContentTrimmed);
		history.add(tmp);

		String newReplyContentJson;
		try {
			newReplyContentJson = objectMapper.writeValueAsString(history);
		} catch (JsonProcessingException e) {
			throw new ResourceException("客诉历史回复格式异常");
		}

		SalemanCustomerComplaint row = salemanCustomerComplaintMapper.selectById(complaintId);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		row.setReplyContent(newReplyContentJson);
		row.setReplyStatus(true);
		row.setReplyTime(now);
		row.setUpdated(now);
		row.setReplyOperatorId(replyOperatorId);
		row.setReplyOperatorName(replyOperatorName);
		row.setReplyOperatorMobile(replyOperatorMobile);

		int affected = salemanCustomerComplaintMapper.updateById(row);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		SalemanCustomerComplaint refreshed = salemanCustomerComplaintMapper.selectById(complaintId);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return SalemanCustomerComplaintSwgRowConverter.toRow(refreshed);
	}

	private static String stringField(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int parseOperatorIdLoose(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
