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

import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogKind;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 与 PHP Job 中 debug 文案及顺序对齐。
 */
@Slf4j
@Profile("!test-cron")
@Service
public class Slf4jSalespersonStatisticsCronLogPort implements SalespersonStatisticsCronLogPort {

	@Override
	public void debugStart(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId) {
		if (kind == SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD) {
			log.debug("活动转发数统计开始=>: companyId={}, salespersonId={}", companyId, salespersonId);
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购数据分润统计开始");
			} else {
				log.debug("导购数据分润统计开始=>: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购数据推广统计开始");
			} else {
				log.debug("导购数据推广统计开始=>: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购赠券统计开始");
			} else {
				log.debug("导购赠券统计开始=>: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else {
			log.debug("导购数据统计开始=>: companyId={}, salespersonId={}", companyId, salespersonId);
		}
	}

	@Override
	public void debugError(
			SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId, Throwable error) {
		if (kind == SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD) {
			log.debug("活动转发数统计error: companyId={}, salespersonId={}, err={}", companyId, salespersonId, error);
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION) {
			log.debug("导购数据分润统计error: companyId={}, salespersonId={}, err={}", companyId, salespersonId, error);
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE) {
			log.debug("导购数据推广统计error: companyId={}, salespersonId={}, err={}", companyId, salespersonId, error);
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS) {
			log.debug("导购赠券统计error: companyId={}, salespersonId={}, err={}", companyId, salespersonId, error);
		} else {
			log.debug("导购数据统计error: companyId={}, salespersonId={}, err={}", companyId, salespersonId, error);
		}
	}

	@Override
	public void debugEnd(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId) {
		if (kind == SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD) {
			log.debug("活动转发数统计结束: companyId={}, salespersonId={}", companyId, salespersonId);
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_COMMISSION) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购数据分润统计结束");
			} else {
				log.debug("导购数据分润统计结束: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购数据推广统计结束");
			} else {
				log.debug("导购数据推广统计结束: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else if (kind == SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS) {
			if (companyId == 0L && salespersonId == 0L) {
				log.debug("导购赠券统计结束");
			} else {
				log.debug("导购赠券统计结束: companyId={}, salespersonId={}", companyId, salespersonId);
			}
		} else {
			log.debug("导购数据统计结束: companyId={}, salespersonId={}", companyId, salespersonId);
		}
	}
}
