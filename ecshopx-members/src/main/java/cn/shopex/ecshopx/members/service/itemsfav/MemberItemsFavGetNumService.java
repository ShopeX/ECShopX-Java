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

package cn.shopex.ecshopx.members.service.itemsfav;

import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MemberItemsFavGetNumService {

	private final MemberItemsFavMapper memberItemsFavMapper;

	public MemberItemsFavGetNumService(MemberItemsFavMapper memberItemsFavMapper) {
		this.memberItemsFavMapper = memberItemsFavMapper;
	}

	public long getItemsFavNum(long companyId, long userId) {
		Long count =
				memberItemsFavMapper.selectCount(
						new LambdaQueryWrapper<MemberItemsFav>()
								.eq(MemberItemsFav::getCompanyId, companyId)
								.eq(MemberItemsFav::getUserId, userId));
		if (count == null) {
			return 0L;
		}
		return count.longValue();
	}
}
