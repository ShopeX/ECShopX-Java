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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class WxappItemsFavStatusService {

	private final MemberItemsFavMapper memberItemsFavMapper;

	public WxappItemsFavStatusService(MemberItemsFavMapper memberItemsFavMapper) {
		this.memberItemsFavMapper = memberItemsFavMapper;
	}

	/**
	 * 判断会员是否已收藏指定 SKU：存在收藏行返回 1，否则 0。
	 */
	public int isFavorited(long userId, long itemId) {
		LambdaQueryWrapper<MemberItemsFav> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(MemberItemsFav::getUserId, userId).eq(MemberItemsFav::getItemId, itemId);
		MemberItemsFav row = memberItemsFavMapper.selectOne(wrapper);
		return row == null ? 0 : 1;
	}
}
