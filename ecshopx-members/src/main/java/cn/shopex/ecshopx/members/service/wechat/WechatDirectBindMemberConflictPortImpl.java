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

package cn.shopex.ecshopx.members.service.wechat;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WechatDirectBindMemberConflictPort;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatDirectBindMemberConflictPortImpl implements WechatDirectBindMemberConflictPort {

	private final WechatUsersMapper wechatUsersMapper;

	public WechatDirectBindMemberConflictPortImpl(WechatUsersMapper wechatUsersMapper) {
		this.wechatUsersMapper = wechatUsersMapper;
	}

	@Override
	public void assertNoBlockingUsers(long companyId, String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			return;
		}
		LambdaQueryWrapper<WechatUsers> w = new LambdaQueryWrapper<>();
		w.eq(WechatUsers::getCompanyId, companyId).eq(WechatUsers::getAuthorizerAppid, authorizerAppid.trim());
		if (wechatUsersMapper.selectCount(w) > 0) {
			throw new ResourceException("小程序已有用户授权信息，不可更换绑定");
		}
	}
}
