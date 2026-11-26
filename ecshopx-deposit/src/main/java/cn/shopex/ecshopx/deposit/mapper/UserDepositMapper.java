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

package cn.shopex.ecshopx.deposit.mapper;

import cn.shopex.ecshopx.deposit.domain.UserDeposit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserDepositMapper extends BaseMapper<UserDeposit> {

	/**
	 * Atomically apply {@code delta} to {@code money}. When {@code minBalance} &gt; 0, requires
	 * {@code money >= minBalance} (use {@code -delta} for debits; use {@code 0} for credits).
	 */
	@Update("""
			UPDATE user_deposit
			SET money = money + #{delta}, updated = #{updated}
			WHERE company_id = #{companyId}
			  AND user_id = #{userId}
			  AND money >= #{minBalance}
			""")
	int addMoneyDelta(
			@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("delta") long delta,
			@Param("minBalance") long minBalance,
			@Param("updated") int updated);
}
