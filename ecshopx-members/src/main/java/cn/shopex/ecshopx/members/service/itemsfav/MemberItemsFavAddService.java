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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.MemberItemsFavSkuSnapshotPort;
import cn.shopex.ecshopx.common.members.port.MemberItemsFavSkuSnapshotPort.SkuSnapshot;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MemberItemsFavAddService {

	private final MemberItemsFavMapper memberItemsFavMapper;

	private final MemberItemsFavSkuSnapshotPort skuSnapshotPort;

	public MemberItemsFavAddService(
			MemberItemsFavMapper memberItemsFavMapper, MemberItemsFavSkuSnapshotPort skuSnapshotPort) {
		this.memberItemsFavMapper = memberItemsFavMapper;
		this.skuSnapshotPort = skuSnapshotPort;
	}

	public Map<String, Object> addItemsFav(long companyId, long userId, long itemId, String itemTypeParam) {
		LambdaQueryWrapper<MemberItemsFav> cw = new LambdaQueryWrapper<>();
		cw.eq(MemberItemsFav::getCompanyId, companyId).eq(MemberItemsFav::getUserId, userId);
		long c = memberItemsFavMapper.selectCount(cw);
		if (c >= 100) {
			throw new ResourceException("最多可以收藏100个商品");
		}

		LambdaQueryWrapper<MemberItemsFav> qw = new LambdaQueryWrapper<>();
		qw.eq(MemberItemsFav::getCompanyId, companyId)
				.eq(MemberItemsFav::getUserId, userId)
				.eq(MemberItemsFav::getItemId, itemId)
				.last("LIMIT 1");
		MemberItemsFav existing = memberItemsFavMapper.selectOne(qw);
		if (existing != null) {
			return toRowMap(existing);
		}

		boolean pointsmall = "pointsmall".equals(itemTypeParam);
		Optional<SkuSnapshot> snap =
				pointsmall ? skuSnapshotPort.loadPointsmallSnapshot(itemId) : skuSnapshotPort.loadGoodsSnapshot(itemId);
		if (snap.isEmpty()) {
			throw new ResourceException("商品不存在");
		}

		SkuSnapshot s = snap.get();
		long now = System.currentTimeMillis() / 1000L;
		MemberItemsFav row = new MemberItemsFav();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setItemId(itemId);
		row.setItemName(s.itemName());
		row.setItemImage(s.firstImageUrl());
		row.setItemPrice(s.priceFen());
		row.setItemType(itemTypeParam);
		row.setPoint(pointsmall ? s.point() : 0);
		row.setCreated(now);
		row.setUpdated(now);
		memberItemsFavMapper.insert(row);
		return toRowMap(row);
	}

	private static Map<String, Object> toRowMap(MemberItemsFav e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("fav_id", e.getFavId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("item_id", e.getItemId());
		m.put("item_name", e.getItemName());
		m.put("item_image", e.getItemImage());
		m.put("item_price", e.getItemPrice());
		m.put("item_type", e.getItemType());
		m.put("point", e.getPoint());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
