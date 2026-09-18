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

import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OpenapiSalespersonMemberNumIncreaseService {

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public OpenapiSalespersonMemberNumIncreaseService(
			ShopSalespersonMapper shopSalespersonMapper, StringRedisTemplate stringRedisTemplate) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void increaseSalespersonMemberNum(long companyId, long inviterId, long userId) {
		if (inviterId < 0L) {
			return;
		}
		String date = LocalDate.now().format(YMD);
		String memberKey = "Member:" + companyId + ":" + date;
		stringRedisTemplate.opsForSet().add(memberKey, String.valueOf(userId));
		ShopSalesperson salesperson =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getUserId, (int) inviterId)
								.last("LIMIT 1"));
		if (salesperson != null && salesperson.getSalespersonId() != null) {
			String salespersonKey =
					"Member:Salesperson:"
							+ salesperson.getSalespersonId()
							+ ":Company:"
							+ companyId
							+ ":"
							+ date;
			stringRedisTemplate.opsForSet().add(salespersonKey, String.valueOf(userId));
		}
	}
}
