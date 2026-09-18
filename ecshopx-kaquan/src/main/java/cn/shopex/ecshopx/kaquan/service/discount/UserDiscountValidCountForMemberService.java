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

import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountValidCountForMemberService {

	private final UserDiscountMapper userDiscountMapper;

	public UserDiscountValidCountForMemberService(UserDiscountMapper userDiscountMapper) {
		this.userDiscountMapper = userDiscountMapper;
	}

	public long countValidForMember(long companyId, long userId) {
		int nowEpoch = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getUserId, userId)
				.eq(UserDiscount::getCompanyId, companyId)
				.in(UserDiscount::getStatus, Arrays.asList(1, 10))
				.gt(UserDiscount::getEndDate, nowEpoch);
		Long c = userDiscountMapper.selectCount(w);
		return c == null ? 0L : c.longValue();
	}

	public long countStatusOneOnly(long companyId, long userId) {
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getUserId, userId)
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getStatus, 1);
		Long c = userDiscountMapper.selectCount(w);
		return c == null ? 0L : c.longValue();
	}

	public long countAllStatusesForMember(long companyId, long userId) {
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getUserId, userId)
				.eq(UserDiscount::getCompanyId, companyId);
		Long c = userDiscountMapper.selectCount(w);
		return c == null ? 0L : c.longValue();
	}

	/**
	 * Wxapp order count API: rows in kaquan_user_discount with status 1 and end_date &gt;= current epoch seconds (inclusive).
	 */
	public long countForWxappOrdersCouponTotal(long companyId, long userId) {
		int nowEpoch = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId)
				.eq(UserDiscount::getStatus, 1)
				.ge(UserDiscount::getEndDate, nowEpoch);
		Long c = userDiscountMapper.selectCount(w);
		return c == null ? 0L : c.longValue();
	}
}
