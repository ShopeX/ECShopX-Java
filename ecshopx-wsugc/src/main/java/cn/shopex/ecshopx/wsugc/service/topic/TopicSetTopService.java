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

package cn.shopex.ecshopx.wsugc.service.topic;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TopicSetTopService {

	private final TopicMapper topicMapper;

	public TopicSetTopService(TopicMapper topicMapper) {
		this.topicMapper = topicMapper;
	}

	public Map<String, Object> setTop(List<Long> topicIds) {
		LambdaUpdateWrapper<Topic> clear = new LambdaUpdateWrapper<>();
		clear.eq(Topic::getIsTop, 1).set(Topic::getIsTop, 0).set(Topic::getPOrder, 0);
		topicMapper.update(null, clear);

		List<Map<String, Object>> allUpdate = new ArrayList<>();
		if (!topicIds.isEmpty()) {
			int n = topicIds.size();
			int now = (int) (System.currentTimeMillis() / 1000);
			for (int k = 0; k < n; k++) {
				long id = topicIds.get(k);
				int pOrder = (n - k) * -1;
				Topic row = topicMapper.selectById(id);
				if (row == null) {
					throw new ResourceException("未查询到更新数据");
				}
				row.setIsTop(1);
				row.setPOrder(pOrder);
				row.setUpdated(now);
				topicMapper.updateById(row);
				Map<String, Object> one = new LinkedHashMap<>();
				one.put("is_top", 1);
				one.put("p_order", pOrder);
				allUpdate.add(one);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("allUpdate", allUpdate);
		data.put("message", "置顶成功");
		return data;
	}
}
