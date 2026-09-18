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

package cn.shopex.ecshopx.kaquan.service;

import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountService {

	private static final int OUTER_MAX = 5;
	private static final int PAGE_SIZE = 200;

	private final UserDiscountMapper userDiscountMapper;
	private final ExcardInventoryPort excardInventoryPort;
	private final TransactionTemplate perCardTransactionTemplate;

	public UserDiscountService(
			UserDiscountMapper userDiscountMapper,
			ExcardInventoryPort excardInventoryPort,
			PlatformTransactionManager platformTransactionManager) {
		this.userDiscountMapper = userDiscountMapper;
		this.excardInventoryPort = excardInventoryPort;
		this.perCardTransactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.perCardTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * 释放过期兑换券锁定的商品库存，并将卡券恢复为可重新兑换（与 analysis §3 步骤 1~8 一致）。
	 * 行为锁定：analysis §3 第 58~62 行 — minusItemStore 返回值不校验，仍按步骤 6 写回
	 * rel_item_ids / rel_distributor_ids 空串与 status=1（库存回补失败仍更新卡券，避免误判为可「修复」的 bug）。
	 */
	public void scheduleCancelExCard() {
		for (int round = 0; round < OUTER_MAX; round++) {
			int now = (int) (System.currentTimeMillis() / 1000L);
			List<Long> batch = userDiscountMapper.selectExpiredLockedCardIds(now, PAGE_SIZE);
			for (Long rawId : batch) {
				if (rawId == null || rawId <= 0L) {
					continue;
				}
				final long userDiscountId = rawId;
				perCardTransactionTemplate.executeWithoutResult(
						transactionStatus -> processExpiredLockedCardInTransaction(userDiscountId));
			}
			if (batch.size() < PAGE_SIZE) {
				break;
			}
		}
	}

	private void processExpiredLockedCardInTransaction(long id) {
		UserDiscount userCard = userDiscountMapper.selectById(id);
		Objects.requireNonNull(
				userCard, "user discount not found, id=" + id);
		Integer st = userCard.getStatus();
		if (st == null || !st.equals(10)) {
			return;
		}
		long companyId = userCard.getCompanyId() != null ? userCard.getCompanyId() : 0L;
		String relItem = userCard.getRelItemIds() != null ? userCard.getRelItemIds() : "";
		String relDist = userCard.getRelDistributorIds() != null ? userCard.getRelDistributorIds() : "";
		long itemId;
		long distributorId;
		try {
			itemId = parseFirstLongId(relItem);
			distributorId = parseDistributorId(relDist);
		} catch (NumberFormatException ex) {
			throw new IllegalStateException("invalid rel_item_ids/rel_distributor_ids on locked card, id=" + id, ex);
		}
		boolean isTotal = excardInventoryPort.resolveIsTotalStore(companyId, itemId, distributorId);
		// 返回值不分支：analysis §3 第 58~59 行
		excardInventoryPort.minusItemStore(companyId, itemId, -1, distributorId, isTotal);
		UpdateWrapper<UserDiscount> up = new UpdateWrapper<>();
		up.eq("id", id)
				.set("rel_item_ids", "")
				.set("rel_distributor_ids", "")
				.set("status", 1);
		userDiscountMapper.update(null, up);
	}

	/** 与已核销路径一致：锁定态下单 item 存为可解析的长整型字符串。 */
	private static long parseFirstLongId(String raw) {
		String t = nullToEmpty(raw).trim();
		if (!StringUtils.hasText(t)) {
			throw new NumberFormatException("empty rel_item_ids");
		}
		return Long.parseLong(t);
	}

	private static long parseDistributorId(String raw) {
		String t = nullToEmpty(raw).trim();
		if (!StringUtils.hasText(t) || "all".equalsIgnoreCase(t)) {
			return 0L;
		}
		return Long.parseLong(t);
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}
}
