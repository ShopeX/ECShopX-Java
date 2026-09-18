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

package cn.shopex.ecshopx.salesperson.integration.members;

import cn.shopex.ecshopx.members.integration.salesperson.AdminMemberListSalesmanMobileResolvePort;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service("adminMemberListSalesmanMobileResolvePortImpl")
public class AdminMemberListSalesmanMobileResolvePortImpl implements AdminMemberListSalesmanMobileResolvePort {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final WorkWechatRelMapper workWechatRelMapper;

	public AdminMemberListSalesmanMobileResolvePortImpl(
			ShopSalespersonMapper shopSalespersonMapper, WorkWechatRelMapper workWechatRelMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.workWechatRelMapper = workWechatRelMapper;
	}

	@Override
	public Long resolveMemberUserIdForSalesmanMobile(long companyId, String salesmanMobileTrimmed) {
		ShopSalesperson sp =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getMobile, salesmanMobileTrimmed)
								.last("LIMIT 1"));
		if (sp == null) {
			return null;
		}
		WorkWechatRel rel =
				workWechatRelMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatRel>()
								.eq(WorkWechatRel::getCompanyId, companyId)
								.eq(WorkWechatRel::getSalespersonId, sp.getSalespersonId())
								.last("LIMIT 1"));
		if (rel == null || rel.getUserId() == null) {
			return null;
		}
		return rel.getUserId();
	}
}
