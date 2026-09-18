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

package cn.shopex.ecshopx.promotions.service.bargain;

import cn.shopex.ecshopx.common.dispatch.BargainFinishSendSmsNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.BargainLog;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainLogMapper;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserBargainCreateBargainLogService {

	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final BargainLogMapper bargainLogMapper;
	private final UserBargainsMapper userBargainsMapper;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;
	private final BargainFinishSendSmsNoticeJobDispatchPublisher bargainFinishSendSmsNoticeJobDispatchPublisher;

	public UserBargainCreateBargainLogService(
			BargainPromotionsMapper bargainPromotionsMapper,
			BargainLogMapper bargainLogMapper,
			UserBargainsMapper userBargainsMapper,
			MessageSource messageSource,
			ObjectMapper objectMapper,
			BargainFinishSendSmsNoticeJobDispatchPublisher bargainFinishSendSmsNoticeJobDispatchPublisher) {
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.bargainLogMapper = bargainLogMapper;
		this.userBargainsMapper = userBargainsMapper;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
		this.bargainFinishSendSmsNoticeJobDispatchPublisher = bargainFinishSendSmsNoticeJobDispatchPublisher;
	}

	public Map<String, Object> createBargainLog(Map<String, Object> mergedParams, Locale locale) {
		long companyId = ((Number) mergedParams.get("company_id")).longValue();

		List<String> parts = new ArrayList<>();

		Object rawBargainId = mergedParams.get("bargain_id");
		String bargainText = rawBargainId == null ? "" : String.valueOf(rawBargainId).trim();
		long bargainId = 0L;
		if (bargainText.isEmpty()) {
			parts.add(
					messageSource.getMessage("promotions.bargain.bargain_activity_id_required", null, locale));
		} else {
			try {
				bargainId = Long.parseLong(bargainText);
				if (bargainId <= 0L) {
					parts.add(messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale));
				}
			} catch (NumberFormatException e) {
				parts.add(messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale));
			}
		}

		Object rawUserId = mergedParams.get("user_id");
		String userText = rawUserId == null ? "" : String.valueOf(rawUserId).trim();
		long targetUserId = 0L;
		if (userText.isEmpty()) {
			parts.add(messageSource.getMessage("promotions.bargain.bargain_user_id_required", null, locale));
		} else {
			try {
				targetUserId = Long.parseLong(userText);
				if (targetUserId <= 0L) {
					parts.add(
							messageSource.getMessage("promotions.bargain.bargain_user_id_required", null, locale));
				}
			} catch (NumberFormatException e) {
				parts.add(messageSource.getMessage("promotions.bargain.bargain_user_id_required", null, locale));
			}
		}

		Object rawOpenId = mergedParams.get("open_id");
		String openId = rawOpenId == null ? "" : String.valueOf(rawOpenId).trim();
		if (openId.isEmpty()) {
			parts.add(messageSource.getMessage("promotions.bargain.wechat_openid_required", null, locale));
		}

		if (!parts.isEmpty()) {
			String joined = String.join("，", parts);
			joined = trimTrailingCommaSeparators(joined);
			throw new ResourceException(joined);
		}

		LambdaQueryWrapper<BargainPromotions> pw =
				new LambdaQueryWrapper<BargainPromotions>()
						.eq(BargainPromotions::getBargainId, bargainId)
						.eq(BargainPromotions::getCompanyId, companyId)
						.last("LIMIT 1");
		BargainPromotions promotion = bargainPromotionsMapper.selectOne(pw);
		if (promotion == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_id",
							new Object[] {bargainId},
							locale));
		}

		long now = Instant.now().getEpochSecond();
		if (promotion.getEndTime() != null && promotion.getEndTime() < now) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.activity_expired_expect_next", null, locale));
		}

		LambdaQueryWrapper<BargainLog> logW =
				new LambdaQueryWrapper<BargainLog>()
						.eq(BargainLog::getCompanyId, companyId)
						.eq(BargainLog::getOpenId, openId)
						.eq(BargainLog::getUserId, targetUserId)
						.eq(BargainLog::getBargainId, bargainId)
						.last("LIMIT 1");
		if (bargainLogMapper.selectOne(logW) != null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.already_helped", null, locale));
		}

		LambdaQueryWrapper<UserBargains> ubW =
				new LambdaQueryWrapper<UserBargains>()
						.eq(UserBargains::getCompanyId, companyId)
						.eq(UserBargains::getUserId, targetUserId)
						.eq(UserBargains::getBargainId, bargainId)
						.last("LIMIT 1");
		UserBargains userBargain = userBargainsMapper.selectOne(ubW);
		if (userBargain == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.user_bargain_help_not_found", null, locale));
		}
		if (Boolean.TRUE.equals(userBargain.getIsOrdered())) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.user_bargain_help_ended_participate_next", null, locale));
		}

		int mkt = nz(promotion.getMktPrice());
		int price = nz(promotion.getPrice());
		int totalCutdownAmount = mkt - price;
		int cutdownSoFar = nz(userBargain.getCutdownAmount());
		if (totalCutdownAmount <= cutdownSoFar) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_expect_next", null, locale));
		}

		String json = userBargain.getCutpriceRange();
		if (json == null || json.isBlank()) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}
		List<Map<String, Object>> slots;
		try {
			slots =
					objectMapper.readValue(
							json, new TypeReference<List<Map<String, Object>>>() {});
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}
		if (slots == null) {
			slots = new ArrayList<>();
		}
		Map<String, Object> chosen = null;
		int cutdownNum = 0;
		for (Map<String, Object> slot : slots) {
			if (slot == null) {
				continue;
			}
			if (!isSlotUsed(slot.get("used"))) {
				chosen = slot;
				cutdownNum = parsePositiveInt(slot.get("cut"), messageSource);
				slot.put("used", Integer.valueOf(1));
				break;
			}
		}
		if (chosen == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}

		int nowInt = (int) Math.min(now, Integer.MAX_VALUE);
		BargainLog log = new BargainLog();
		log.setCompanyId(companyId);
		log.setAuthorizerAppid(Objects.toString(mergedParams.get("authorizer_appid"), ""));
		log.setWxaAppid(Objects.toString(mergedParams.get("wxa_appid"), ""));
		log.setBargainId(bargainId);
		log.setUserId(targetUserId);
		log.setOpenId(openId);
		log.setNickname(readTrimmedNullable(mergedParams.get("nickname")));
		log.setHeadimgurl(readTrimmedNullable(mergedParams.get("headimgurl")));
		log.setCutdownNum(cutdownNum);
		log.setCreated(nowInt);
		log.setUpdated(nowInt);
		bargainLogMapper.insert(log);

		userBargain = userBargainsMapper.selectOne(ubW);
		if (userBargain == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.user_bargain_help_not_found", null, locale));
		}

		int totalCutdown = nz(userBargain.getCutdownAmount()) + cutdownNum;
		userBargain.setCutdownAmount(totalCutdown);
		try {
			userBargain.setCutpriceRange(objectMapper.writeValueAsString(slots));
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}
		userBargain.setUpdated(nowInt);

		LambdaUpdateWrapper<UserBargains> uw =
				new LambdaUpdateWrapper<UserBargains>()
						.eq(UserBargains::getCompanyId, companyId)
						.eq(UserBargains::getUserId, targetUserId)
						.eq(UserBargains::getBargainId, bargainId);
		int rows;
		try {
			rows = userBargainsMapper.update(userBargain, uw);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}
		if (rows != 1) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("bargain_log_id", log.getBargainLogId());
		row.put("company_id", log.getCompanyId());
		row.put("authorizer_appid", log.getAuthorizerAppid());
		row.put("wxa_appid", log.getWxaAppid());
		row.put("bargain_id", log.getBargainId());
		row.put("user_id", log.getUserId());
		row.put("open_id", log.getOpenId());
		row.put("nickname", log.getNickname());
		row.put("headimgurl", log.getHeadimgurl());
		row.put("cutdown_num", log.getCutdownNum());
		row.put("created", log.getCreated());
		row.put("updated", log.getUpdated());

		boolean ordered = Boolean.TRUE.equals(userBargain.getIsOrdered());
		int umkt = nz(userBargain.getMktPrice());
		int floor = nz(userBargain.getPrice());
		final UserBargains snapshotForNotify = userBargain;
		final Long promotionEndTimeForNotify = promotion.getEndTime();
		final long bargainOwnerUserId = targetUserId;
		if (!ordered && (umkt - floor - totalCutdown) <= 0) {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(
						new TransactionSynchronization() {
							@Override
							public void afterCommit() {
								bargainFinishSendSmsNoticeJobDispatchPublisher.publishBargainFinishSendSmsNotice(
										companyId,
										bargainOwnerUserId,
										bargainSnapshotFieldsForNotify(snapshotForNotify),
										promotionEndTimeForNotify,
										locale);
							}
						});
			} else {
				bargainFinishSendSmsNoticeJobDispatchPublisher.publishBargainFinishSendSmsNotice(
						companyId,
						bargainOwnerUserId,
						bargainSnapshotFieldsForNotify(snapshotForNotify),
						promotionEndTimeForNotify,
						locale);
			}
		}

		return row;
	}

	private static Map<String, Object> bargainSnapshotFieldsForNotify(UserBargains snapshot) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_name", Objects.toString(snapshot.getItemName(), ""));
		m.put("price", snapshot.getPrice() == null ? Integer.valueOf(0) : snapshot.getPrice());
		return m;
	}

	private static String readTrimmedNullable(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}

	private static int parsePositiveInt(Object raw, MessageSource messageSource) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (raw == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.cut_invalid", null, locale));
		}
		int value;
		try {
			if (raw instanceof Number n) {
				value = n.intValue();
			} else {
				String s = String.valueOf(raw).trim();
				if (s.isEmpty()) {
					throw new ResourceException(
							messageSource.getMessage("promotions.bargain.cut_invalid", null, locale));
				}
				value = Integer.parseInt(s);
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.cut_invalid", null, locale));
		}
		if (value <= 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.cut_invalid", null, locale));
		}
		return value;
	}

	private static String trimTrailingCommaSeparators(String joined) {
		String s = joined;
		while (!s.isEmpty()) {
			char last = s.charAt(s.length() - 1);
			if (last == '\uFF0C' || last == ',') {
				s = s.substring(0, s.length() - 1);
			} else {
				break;
			}
		}
		return s;
	}

	private static boolean isSlotUsed(Object usedVal) {
		if (usedVal == null) {
			return false;
		}
		if (usedVal instanceof Boolean b) {
			return b.booleanValue();
		}
		if (usedVal instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(usedVal).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}
}
