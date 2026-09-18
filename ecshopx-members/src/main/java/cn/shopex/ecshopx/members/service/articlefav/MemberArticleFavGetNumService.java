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

package cn.shopex.ecshopx.members.service.articlefav;

import cn.shopex.ecshopx.members.domain.MemberArticleFav;
import cn.shopex.ecshopx.members.mapper.MemberArticleFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MemberArticleFavGetNumService {

	private final MemberArticleFavMapper memberArticleFavMapper;

	public MemberArticleFavGetNumService(MemberArticleFavMapper memberArticleFavMapper) {
		this.memberArticleFavMapper = memberArticleFavMapper;
	}

	public long getArticleFavNum(long companyId, long userId) {
		Long count =
				memberArticleFavMapper.selectCount(
						new LambdaQueryWrapper<MemberArticleFav>()
								.eq(MemberArticleFav::getCompanyId, companyId)
								.eq(MemberArticleFav::getUserId, userId));
		if (count == null) {
			return 0L;
		}
		return count.longValue();
	}
}
