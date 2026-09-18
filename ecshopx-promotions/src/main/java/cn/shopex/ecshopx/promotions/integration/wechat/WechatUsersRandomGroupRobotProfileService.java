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

package cn.shopex.ecshopx.promotions.integration.wechat;

import cn.shopex.ecshopx.common.cron.GroupRobotWechatInfo;
import cn.shopex.ecshopx.common.cron.PromotionGroupRobotWechatProfilePort;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 从全库微信用户中随机取资料，为拼团机器人行填充展示信息（生产环境）；测试 profile 由 Noop 覆盖。
 */
@Service
@Profile("!test-cron")
@RequiredArgsConstructor
public class WechatUsersRandomGroupRobotProfileService implements PromotionGroupRobotWechatProfilePort {

	private final WechatUsersMapper wechatUsersMapper;

	@Override
	public List<GroupRobotWechatInfo> getRandUserInfo(int limit) {
		if (limit <= 0) {
			return List.of();
		}
		List<WechatUsers> rows =
				wechatUsersMapper.selectList(
						new LambdaQueryWrapper<WechatUsers>()
								.last("ORDER BY RAND() LIMIT " + limit));
		return rows.stream()
				.map(
						u ->
								new GroupRobotWechatInfo(
										u.getHeadimgurl() == null ? "" : u.getHeadimgurl(),
										u.getNickname() == null || u.getNickname().isEmpty() ? "用户" : u.getNickname()))
				.collect(Collectors.toList());
	}
}
