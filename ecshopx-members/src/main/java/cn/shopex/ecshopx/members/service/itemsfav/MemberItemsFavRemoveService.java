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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberItemsFavRemoveService {

	private final MemberItemsFavMapper memberItemsFavMapper;

	public MemberItemsFavRemoveService(MemberItemsFavMapper memberItemsFavMapper) {
		this.memberItemsFavMapper = memberItemsFavMapper;
	}

	public int removeItemsFav(
			long companyId, long userId, boolean isEmpty, List<Object> itemIdConditionsWhenNotEmpty) {
		LambdaQueryWrapper<MemberItemsFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberItemsFav::getCompanyId, companyId).eq(MemberItemsFav::getUserId, userId);
		if (isEmpty) {
			return memberItemsFavMapper.delete(w);
		}
		if (itemIdConditionsWhenNotEmpty == null || itemIdConditionsWhenNotEmpty.isEmpty()) {
			throw new BadRequestException(
					"删除收藏商品出错.", Map.of("item_ids", List.of("validation.required_if")), 422);
		}
		if (itemIdConditionsWhenNotEmpty.size() == 1) {
			w.apply("item_id = {0}", itemIdConditionsWhenNotEmpty.get(0));
		} else {
			List<Object> elems = itemIdConditionsWhenNotEmpty;
			StringBuilder sql = new StringBuilder("item_id IN (");
			for (int i = 0; i < elems.size(); i++) {
				if (i > 0) {
					sql.append(',');
				}
				sql.append('{').append(i).append('}');
			}
			sql.append(')');
			w.apply(sql.toString(), elems.toArray());
		}
		return memberItemsFavMapper.delete(w);
	}
}
