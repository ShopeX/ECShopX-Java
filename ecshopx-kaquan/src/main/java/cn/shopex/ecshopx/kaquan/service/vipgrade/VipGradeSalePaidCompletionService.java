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
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VipGradeSalePaidCompletionService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final VipGradeOrderMapper vipGradeOrderMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService;
	private final FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;
	private final StringRedisTemplate companysRedisTemplate;

	public VipGradeSalePaidCompletionService(
			VipGradeOrderMapper vipGradeOrderMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			VipGradeOrderMemberRelationApplyService vipGradeOrderMemberRelationApplyService,
			FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.vipGradeOrderMapper = vipGradeOrderMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.vipGradeOrderMemberRelationApplyService = vipGradeOrderMemberRelationApplyService;
		this.firePromotionsActivityDispatchPublisher = firePromotionsActivityDispatchPublisher;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	/**
	 * For {@code source_type = sale}, transitions NOTPAY→DONE, applies member relations, then publishes async
	 * marketing dispatch for member VIP upgrade.
	 */
	public void completeSaleOrderAfterTradePaid(long companyId, long orderId) {
		VipGradeOrder order = vipGradeOrderMapper.selectOne(new QueryWrapper<VipGradeOrder>()
				.eq("order_id", orderId)
				.eq("company_id", (int) companyId));
		if (order == null) {
			return;
		}
		if (order.getUserId() == null) {
			return;
		}
		if (!"sale".equals(order.getSourceType())) {
			return;
		}
		if ("DONE".equals(order.getOrderStatus())) {
			return;
		}
		if (!"NOTPAY".equals(order.getOrderStatus())) {
			return;
		}

		int nowSec = (int) Instant.now().getEpochSecond();
		int updated = vipGradeOrderMapper.update(
				null,
				new UpdateWrapper<VipGradeOrder>()
						.eq("order_id", orderId)
						.eq("company_id", (int) companyId)
						.eq("order_status", "NOTPAY")
						.eq("source_type", "sale")
						.set("order_status", "DONE")
						.set("updated", nowSec));
		if (updated <= 0) {
			return;
		}

		order = vipGradeOrderMapper.selectById(orderId);
		if (order == null) {
			return;
		}

		long userId = order.getUserId();
		Map<String, Object> redisPayload =
				vipGradeOrderMemberRelationApplyService.addMemberVipGrade(companyId, userId, orderId, false);
		String dateYmd = DateTimeFormatter.BASIC_ISO_DATE.format(Instant.now().atZone(SHANGHAI));
		String vipType = String.valueOf(redisPayload.get("vip_type"));
		String redisKey = "MemberCard:" + (int) companyId + ":" + vipType + ":" + dateYmd;
		companysRedisTemplate.opsForSet().add(redisKey, String.valueOf(userId));

		String encMobile = order.getMobile();
		String mobilePlain = encMobile == null || encMobile.isBlank() ? "" : sensitiveFieldEncryptor.decrypt(encMobile);

		Map<String, Object> memberInfo = new LinkedHashMap<>(8);
		memberInfo.put("vip_grade_type", order.getLvType() == null ? "" : order.getLvType());
		memberInfo.put("user_id", userId);
		memberInfo.put("mobile", mobilePlain);
		memberInfo.put("grade_name", order.getTitle() == null ? "" : order.getTitle());

		firePromotionsActivityDispatchPublisher.publish(companyId, memberInfo, "member_vip_upgrade");
	}
}
