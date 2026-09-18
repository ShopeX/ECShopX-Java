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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.CardPackageItems;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CardPackageCreateService {

	private static final String ERR_LIMIT = "限领次数必需大于0";
	private static final String ERR_CONTENT = "卡券包内容必传";
	private static final String ERR_SELECT_COUPON = "请选择优惠券";
	private static final String ERR_ITEM_PARAM = "参数错误";

	private final DiscountCardsMapper discountCardsMapper;
	private final CardPackageMapper cardPackageMapper;
	private final CardPackageItemsMapper cardPackageItemsMapper;
	private final CardPackageMultiLangWriteService cardPackageMultiLangWriteService;
	private final TransactionTemplate transactionTemplate;

	public CardPackageCreateService(DiscountCardsMapper discountCardsMapper, CardPackageMapper cardPackageMapper,
			CardPackageItemsMapper cardPackageItemsMapper,
			CardPackageMultiLangWriteService cardPackageMultiLangWriteService,
			PlatformTransactionManager platformTransactionManager) {
		this.discountCardsMapper = discountCardsMapper;
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageItemsMapper = cardPackageItemsMapper;
		this.cardPackageMultiLangWriteService = cardPackageMultiLangWriteService;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void create(long companyId, Map<String, Object> inputData) {
		validateActionRules(inputData);
		String title = ((String) inputData.get("title")).trim();
		String packageDescribe = Objects.toString(inputData.get("package_describe"), "");
		int limitCount = resolveLimitCount(inputData);

		@SuppressWarnings("unchecked")
		List<?> packageContent = (List<?>) inputData.get("package_content");
		List<ItemRow> itemRows = checkCreatePackage(companyId, packageContent);

		int now = (int) (System.currentTimeMillis() / 1000L);
		CardPackage entity = new CardPackage();
		entity.setCompanyId(companyId);
		entity.setTitle(title);
		entity.setPackageDescribe(packageDescribe);
		entity.setLimitCount(limitCount);
		entity.setGetNum(0);
		entity.setRowStatus(1);
		entity.setCreated(now);
		entity.setUpdated(now);

		transactionTemplate.executeWithoutResult(status -> {
			cardPackageMapper.insert(entity);
			Long packageId = entity.getPackageId();
			if (packageId == null || packageId <= 0L) {
				throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
			}
			cardPackageMultiLangWriteService.writeAfterCreate(packageId, companyId, title, packageDescribe);
			for (ItemRow row : itemRows) {
				CardPackageItems item = new CardPackageItems();
				item.setPackageId(packageId);
				item.setCompanyId(companyId);
				item.setCardId(row.cardId());
				item.setGiveNum(row.giveNum());
				item.setCreated(now);
				item.setUpdated(now);
				cardPackageItemsMapper.insert(item);
			}
		});
	}

	private void validateActionRules(Map<String, Object> inputData) {
		Object titleRaw = inputData.get("title");
		if (!(titleRaw instanceof String)) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_TITLE_REQUIRED);
		}
		String title = ((String) titleRaw).trim();
		if (title.isEmpty() || title.length() > 10) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_TITLE_REQUIRED);
		}

		Object descRaw = inputData.get("package_describe");
		if (descRaw instanceof String ds && ds.length() > 20) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_DESCRIBE_MAX);
		}

		Object contentRaw = inputData.get("package_content");
		if (!(contentRaw instanceof List<?> list) || list.isEmpty()) {
			throw new BadRequestException(ERR_CONTENT);
		}
	}

	private int resolveLimitCount(Map<String, Object> inputData) {
		if (!inputData.containsKey("limit_count")) {
			return 1;
		}
		Object lim = inputData.get("limit_count");
		if (lim == null) {
			return 1;
		}
		long v = parseLimitCountNumericOrThrow(lim, ERR_LIMIT);
		if (v == 0L) {
			return 1;
		}
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) v;
	}

	private static long parseLimitCountNumericOrThrow(Object lim, String message) {
		if (lim instanceof Number n) {
			return n.longValue();
		}
		if (lim instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(message);
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(message);
			}
		}
		throw new BadRequestException(message);
	}

	private List<ItemRow> checkCreatePackage(long companyId, List<?> packageContent) {
		if (packageContent == null || packageContent.isEmpty()) {
			throw new BadRequestException(ERR_SELECT_COUPON);
		}
		if (packageContent.size() > 20) {
			throw new ResourceException("请选择小于20张优惠券");
		}

		List<Long> orderedCardIds = new ArrayList<>(packageContent.size());
		List<Long> giveNums = new ArrayList<>(packageContent.size());
		for (Object itemObj : packageContent) {
			if (!(itemObj instanceof Map<?, ?> itemMap)) {
				throw new BadRequestException(ERR_ITEM_PARAM);
			}
			long cardId = parseLongRequired(itemMap.get("card_id"), ERR_ITEM_PARAM);
			long giveNum = parseLongRequired(itemMap.get("give_num"), ERR_ITEM_PARAM);
			orderedCardIds.add(cardId);
			giveNums.add(giveNum);
		}

		Set<Long> distinctIds = new LinkedHashSet<>(orderedCardIds);
		if (distinctIds.isEmpty()) {
			throw new BadRequestException(ERR_SELECT_COUPON);
		}

		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.select(DiscountCards::getCardId, DiscountCards::getCompanyId, DiscountCards::getTitle, DiscountCards::getGetLimit)
				.eq(DiscountCards::getCompanyId, companyId)
				.in(DiscountCards::getCardId, distinctIds);
		List<DiscountCards> loaded = discountCardsMapper.selectList(w);
		Map<Long, DiscountCards> cardById = new LinkedHashMap<>();
		for (DiscountCards c : loaded) {
			if (c.getCardId() != null) {
				cardById.put(c.getCardId(), c);
			}
		}

		List<ItemRow> rows = new ArrayList<>(packageContent.size());
		for (int i = 0; i < orderedCardIds.size(); i++) {
			long cardId = orderedCardIds.get(i);
			long giveNum = giveNums.get(i);
			DiscountCards card = cardById.get(cardId);
			if (card == null) {
				throw new ResourceException("优惠券信息不存在");
			}
			int getLimitEffective = card.getGetLimit() == null ? 0 : card.getGetLimit();
			if (giveNum > getLimitEffective) {
				String cardTitle = Objects.toString(card.getTitle(), "");
				throw new ResourceException(cardTitle + "发送数量不能大于领券限制");
			}
			rows.add(new ItemRow(cardId, giveNum));
		}
		return rows;
	}

	private static long parseLongRequired(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(message);
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(message);
			}
		}
		throw new BadRequestException(message);
	}

	private record ItemRow(long cardId, long giveNum) {}
}
