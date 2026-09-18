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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VipGradeOrderReceiveService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final ObjectMapper objectMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final StringRedisTemplate companysRedisTemplate;

	public VipGradeOrderReceiveService(
			VipGradeMapper vipGradeMapper,
			VipGradeOrderMapper vipGradeOrderMapper,
			VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService,
			NormalOrderNumericIdService normalOrderNumericIdService,
			ObjectMapper objectMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.vipGradeOrderMemberRelationApplyService = vipGradeOrderMemberRelationApplyService;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.objectMapper = objectMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void receiveAdminCustomDelay(long companyId, long userId, String mobilePlain, long vipGradeId, int day) {
		if (day <= 0) {
			throw new BadRequestException("延期天数无效");
		}
		String mobile = mobilePlain != null ? mobilePlain : "";
		receiveSingleDelay(companyId, userId, mobile, vipGradeId, day, "admin");
	}

	public void receiveMemberCard(long companyId, long userId, String mobilePlain, String vipGradeAddDayJson) {
		if (vipGradeAddDayJson == null || vipGradeAddDayJson.isBlank()) {
			throw new BadRequestException("vipGradeAddDay 格式错误");
		}
		final JsonNode root;
		try {
			root = objectMapper.readTree(vipGradeAddDayJson);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("vipGradeAddDay 格式错误");
		}
		if (root == null || root.isNull()) {
			throw new BadRequestException("vipGradeAddDay 格式错误");
		}
		if (root.isArray()) {
			for (JsonNode row : root) {
				if (row.isObject()) {
					processRow(companyId, userId, mobilePlain, (ObjectNode) row);
				}
			}
		} else if (root.isObject()) {
			for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
				JsonNode row = it.next().getValue();
				if (row.isObject()) {
					processRow(companyId, userId, mobilePlain, (ObjectNode) row);
				}
			}
		} else {
			throw new BadRequestException("vipGradeAddDay 格式错误");
		}
	}

	private void processRow(long companyId, long userId, String mobilePlain, ObjectNode row) {
		int day = parseDay(row);
		if (day <= 0) {
			return;
		}
		long vipGradeId = row.path("vip_grade_id").asLong(0L);
		receiveSingleDelay(companyId, userId, mobilePlain, vipGradeId, day, "admin");
	}

	private static int parseDay(ObjectNode row) {
		JsonNode d = row.get("day");
		if (d == null || d.isNull()) {
			return 0;
		}
		if (d.isNumber()) {
			return d.asInt();
		}
		if (d.isTextual()) {
			try {
				return Integer.parseInt(d.asText().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private void receiveSingleDelay(
			long companyId, long userId, String mobilePlain, long vipGradeId, int day, String sourceType) {
		if (vipGradeId > Integer.MAX_VALUE || vipGradeId < Integer.MIN_VALUE) {
			throw new BadRequestException("vip_grade_id 超出允许范围");
		}
		int vipGradeIdInt = (int) vipGradeId;

		VipGrade grade = vipGradeMapper.selectOne(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId)
				.eq(VipGrade::getVipGradeId, (long) vipGradeIdInt));
		if (grade == null) {
			throw new ResourceException("没有该会员卡");
		}
		if (Boolean.TRUE.equals(grade.getIsDisabled())) {
			throw new ResourceException("会员卡已禁用");
		}

		int discount = VipGradeGradeDiscountParser.parseDiscountFromPrivilegesJson(grade.getPrivileges(), objectMapper);

		Map<String, Object> customCardType = new HashMap<>(4);
		customCardType.put("day", day);
		customCardType.put("desc", "后台手动赠送" + day + "天");
		customCardType.put("name", "custom");
		customCardType.put("price", 0);
		String cardTypeJson;
		try {
			cardTypeJson = objectMapper.writeValueAsString(customCardType);
		} catch (JsonProcessingException e) {
			throw new ResourceException("没有该会员卡");
		}

		Integer price = 0;

		long orderId = normalOrderNumericIdService.generate(userId);
		int nowSec = (int) Instant.now().getEpochSecond();

		String mobileStored;
		if (mobilePlain != null && !mobilePlain.isBlank()) {
			mobileStored = sensitiveFieldEncryptor.encrypt(mobilePlain.trim());
		} else {
			mobileStored = "";
		}

		VipGradeOrder order = new VipGradeOrder();
		order.setOrderId(orderId);
		order.setVipGradeId(vipGradeIdInt);
		order.setLvType(grade.getLvType());
		order.setCompanyId((int) companyId);
		order.setUserId(userId);
		order.setMobile(mobileStored);
		order.setTitle(grade.getGradeName());
		order.setPrice(price);
		order.setCardType(cardTypeJson);
		order.setDiscount(discount);
		order.setOrderStatus("DONE");
		order.setSourceType(sourceType);
		order.setCreated(nowSec);
		order.setUpdated(nowSec);

		vipGradeOrderMapper.insert(order);

		if (!"sale".equals(sourceType)) {
			Map<String, Object> data =
					vipGradeOrderMemberRelationApplyService.addMemberVipGrade(companyId, userId, orderId, false);
			String dateYmd = DateTimeFormatter.BASIC_ISO_DATE.format(Instant.now().atZone(SHANGHAI));
			String vipType = String.valueOf(data.get("vip_type"));
			String redisKey = "MemberCard:" + (int) companyId + ":" + vipType + ":" + dateYmd;
			companysRedisTemplate.opsForSet().add(redisKey, String.valueOf(userId));
		}
	}

}
