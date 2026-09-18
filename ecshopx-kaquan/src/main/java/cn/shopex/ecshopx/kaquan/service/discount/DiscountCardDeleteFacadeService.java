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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.kaquan.port.CouponDeleteEventDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.WechatRelCard;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.mapper.WechatRelCardMapper;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformCardDeleteClient;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardDeleteFacadeService {

	private static final String ERR_DELETE_VALIDATE = "删除卡券出错.";
	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final WechatRelCardMapper wechatRelCardMapper;
	private final WechatOpenPlatformCardDeleteClient wechatOpenPlatformCardDeleteClient;
	private final CouponDeleteEventDispatchPublisher couponDeleteEventDispatchPublisher;
	private final TransactionTemplate transactionTemplate;

	public DiscountCardDeleteFacadeService(
			UserDiscountMapper userDiscountMapper,
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			WechatRelCardMapper wechatRelCardMapper,
			WechatOpenPlatformCardDeleteClient wechatOpenPlatformCardDeleteClient,
			CouponDeleteEventDispatchPublisher couponDeleteEventDispatchPublisher,
			PlatformTransactionManager platformTransactionManager) {
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.wechatRelCardMapper = wechatRelCardMapper;
		this.wechatOpenPlatformCardDeleteClient = wechatOpenPlatformCardDeleteClient;
		this.couponDeleteEventDispatchPublisher = couponDeleteEventDispatchPublisher;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void deleteDiscountCard(Map<String, Object> merged, long companyId, String authorizerAppid) {
		deleteDiscountCard(merged, companyId, authorizerAppid, true);
	}

	/**
	 * @param checkUserReceived when {@code true} (API-style callers), refuses delete if any {@code user_discount}
	 *        row exists for the card; when {@code false} (DM-style callers), skips that guard so delete may run
	 *        even when the card was issued to members.
	 */
	public void deleteDiscountCard(
			Map<String, Object> merged, long companyId, String authorizerAppid, boolean checkUserReceived) {
		long cardId = parseAndValidateCardId(merged.get("card_id"));
		if (checkUserReceived) {
			long received = userDiscountMapper.selectCount(
					Wrappers.<UserDiscount>lambdaQuery().eq(UserDiscount::getCardId, cardId));
			if (received > 0L) {
				throw new ResourceException("删除优惠券失败,已有会员领取");
			}
		}
		final long publishCardId = cardId;
		final long publishCompanyId = companyId;
		final String publishAuthorizerAppid = authorizerAppid;
		transactionTemplate.executeWithoutResult(status -> {
			int mainDeleted = discountCardsMapper.delete(Wrappers.<DiscountCards>lambdaQuery()
					.eq(DiscountCards::getCompanyId, companyId)
					.eq(DiscountCards::getCardId, cardId));
			if (mainDeleted > 0) {
				relItemsMapper.delete(Wrappers.<RelItems>lambdaQuery()
						.eq(RelItems::getCompanyId, companyId)
						.eq(RelItems::getCardId, cardId));
			}
			final int committedMainDeleted = mainDeleted;
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					if (committedMainDeleted > 0 && StringUtils.hasText(publishAuthorizerAppid)) {
						deleteWechatMappingIfPresent(publishCompanyId, publishCardId, publishAuthorizerAppid);
					}
					couponDeleteEventDispatchPublisher.publish(publishCardId, publishCompanyId);
				}
			});
		});
	}

	private long parseAndValidateCardId(Object raw) {
		String s = raw == null ? "" : String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(ERR_DELETE_VALIDATE, Map.of("card_id", List.of("validation.required")));
		}
		if (!INTEGER_STRING.matcher(s).matches()) {
			throw new BadRequestException(ERR_DELETE_VALIDATE, Map.of("card_id", List.of("validation.integer")));
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(ERR_DELETE_VALIDATE, Map.of("card_id", List.of("validation.integer")));
		}
	}

	private void deleteWechatMappingIfPresent(long companyId, long cardId, String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			return;
		}
		WechatRelCard row = wechatRelCardMapper.selectOne(Wrappers.<WechatRelCard>lambdaQuery()
				.eq(WechatRelCard::getCompanyId, companyId)
				.eq(WechatRelCard::getCardId, cardId)
				.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		String wechatCardId = row.getWechatCardId();
		wechatRelCardMapper.deleteById(cardId);
		wechatOpenPlatformCardDeleteClient.deleteCard(authorizerAppid, wechatCardId);
	}
}
