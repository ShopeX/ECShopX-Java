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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOfflineBenefitCouponGrantPort;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefit;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按影子权益 {@code local_card_id} 调用卡券发券。对齐 PHP {@code ShuyunOfflineBenefitKaquanIssuer}。
 */
@Component
public class OfflineBenefitKaquanIssuer implements OfflineBenefitItemIssuer {

	public static final String SOURCE_FROM = "数云线下权益发放";

	private static final Logger log = LoggerFactory.getLogger(OfflineBenefitKaquanIssuer.class);

	private final ShuyunOfflineBenefitMapper benefitMapper;
	private final ShuyunOfflineBenefitCouponGrantPort couponGrantPort;
	private final OfflineBenefitIssuingMemberResolver memberResolver;

	public OfflineBenefitKaquanIssuer(
			ShuyunOfflineBenefitMapper benefitMapper,
			ShuyunOfflineBenefitCouponGrantPort couponGrantPort,
			OfflineBenefitIssuingMemberResolver memberResolver) {
		this.benefitMapper = benefitMapper;
		this.couponGrantPort = couponGrantPort;
		this.memberResolver = memberResolver;
	}

	@Override
	public OfflineBenefitIssueResult issue(
			ShuyunOfflineBenefitSendBatch batch, ShuyunOfflineBenefitSendItem item) {
		long companyId = batch.getCompanyId() == null ? 0L : batch.getCompanyId();
		ShuyunOfflineBenefit benefit =
				benefitMapper.selectOne(
						new LambdaQueryWrapper<ShuyunOfflineBenefit>()
								.eq(ShuyunOfflineBenefit::getCompanyId, companyId)
								.eq(ShuyunOfflineBenefit::getBenefitId, batch.getBenefitId())
								.last("LIMIT 1"));
		if (benefit == null) {
			return OfflineBenefitIssueResult.fail("权益档案不存在");
		}
		Long cardId = benefit.getLocalCardId();
		if (cardId == null || cardId <= 0L) {
			return OfflineBenefitIssueResult.fail("未配置本地券模板(local_card_id)");
		}
		Long userId = memberResolver.resolveLocalUserId(companyId, item.getCustomerId());
		if (userId == null) {
			return OfflineBenefitIssueResult.fail("未找到会员");
		}
		try {
			Map<String, Object> row =
					couponGrantPort.grantByCardTemplate(companyId, cardId, userId, SOURCE_FROM);
			Object codeObj = row == null ? null : row.get("code");
			String code = codeObj == null ? "" : String.valueOf(codeObj).trim();
			if (!StringUtils.hasText(code)) {
				return OfflineBenefitIssueResult.fail("发券未返回券码");
			}
			return OfflineBenefitIssueResult.ok(code, userId);
		} catch (ResourceException e) {
			return OfflineBenefitIssueResult.fail(e.getMessage());
		} catch (Exception e) {
			log.error(
					"Shuyun offline benefit Kaquan issue failed companyId={} benefitId={} cardId={} userId={} err={}",
					companyId,
					batch.getBenefitId(),
					cardId,
					userId,
					e.getMessage());
			return OfflineBenefitIssueResult.fail("发券异常");
		}
	}
}
