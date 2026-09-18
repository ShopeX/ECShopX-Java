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

package cn.shopex.ecshopx.aftersales.repository;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AftersalesRefundQueryRepository {

	private static final List<String> UNFINISHED_STATUSES =
			List.of("READY", "AUDIT_SUCCESS", "PROCESSING", "CHANGE");

	private final AftersalesRefundMapper aftersalesRefundMapper;

	public AftersalesRefundQueryRepository(AftersalesRefundMapper aftersalesRefundMapper) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
	}

	public long countUnfinishedRefunds(long companyId, long distributorId) {
		LambdaQueryWrapper<AftersalesRefund> w = new LambdaQueryWrapper<>();
		w.eq(AftersalesRefund::getCompanyId, companyId)
				.eq(AftersalesRefund::getDistributorId, distributorId)
				.in(AftersalesRefund::getRefundStatus, UNFINISHED_STATUSES);
		return aftersalesRefundMapper.selectCount(w);
	}
}
