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

package cn.shopex.ecshopx.kujiale.integration;

import cn.shopex.ecshopx.goods.integration.kujiale.KujialeDesignerWorksByItemIdPort;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksItemRel;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KujialeDesignerWorksByItemIdPortImpl implements KujialeDesignerWorksByItemIdPort {

	private final KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper;

	public KujialeDesignerWorksByItemIdPortImpl(KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper) {
		this.kujialeDesignerWorksItemRelMapper = kujialeDesignerWorksItemRelMapper;
	}

	@Override
	public List<Map<String, Object>> listByItemId(long itemId) {
		try {
			LambdaQueryWrapper<KujialeDesignerWorksItemRel> w = new LambdaQueryWrapper<>();
			w.eq(KujialeDesignerWorksItemRel::getItemId, itemId);
			List<KujialeDesignerWorksItemRel> rows = kujialeDesignerWorksItemRelMapper.selectList(w);
			List<Map<String, Object>> out = new ArrayList<>();
			for (KujialeDesignerWorksItemRel r : rows) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", r.getId());
				m.put("item_id", r.getItemId());
				m.put("design_id", r.getDesignId());
				m.put("goods_bn", r.getGoodsBn());
				m.put("created", r.getCreated());
				m.put("updated", r.getUpdated());
				out.add(m);
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}
}
