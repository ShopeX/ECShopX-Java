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

package cn.shopex.ecshopx.goods.service.members;

import cn.shopex.ecshopx.common.members.port.MemberItemsFavSkuSnapshotPort;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberItemsFavSkuSnapshotPortImpl implements MemberItemsFavSkuSnapshotPort {

	private final ItemsMapper itemsMapper;

	private final ItemRelAttributesRepository itemRelAttributesRepository;

	private final PointsmallItemsMapper pointsmallItemsMapper;

	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;

	private final ObjectMapper objectMapper;

	public MemberItemsFavSkuSnapshotPortImpl(
			ItemsMapper itemsMapper,
			ItemRelAttributesRepository itemRelAttributesRepository,
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			ObjectMapper objectMapper) {
		this.itemsMapper = itemsMapper;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<SkuSnapshot> loadGoodsSnapshot(long itemId) {
		Items row = itemsMapper.selectById(itemId);
		if (row == null) {
			return Optional.empty();
		}
		String firstImage = firstPicFromPicsJson(row.getPics());
		if (isMultiSpec(row.getNospec())) {
			long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
			List<ItemRelAttributes> rels =
					itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, List.of(itemId), "item_spec");
			for (ItemRelAttributes ra : rels) {
				if (ra.getItemId() != null
						&& ra.getItemId().equals(itemId)
						&& StringUtils.hasText(ra.getImageUrl())) {
					firstImage = ra.getImageUrl();
					break;
				}
			}
		}
		String itemName = row.getItemName() != null ? row.getItemName() : "";
		int priceFen = row.getPrice() != null ? row.getPrice() : 0;
		return Optional.of(new SkuSnapshot(itemName, priceFen, firstImage, 0));
	}

	@Override
	public Optional<SkuSnapshot> loadPointsmallSnapshot(long itemId) {
		PointsmallItems p = pointsmallItemsMapper.selectById(itemId);
		if (p == null) {
			return Optional.empty();
		}
		String firstImage = firstPicFromPicsJson(p.getPics());
		if (isMultiSpec(p.getNospec())) {
			Long companyId = p.getCompanyId();
			if (companyId != null) {
				LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
				w.eq(PointsmallItemRelAttributes::getCompanyId, companyId)
						.eq(PointsmallItemRelAttributes::getItemId, itemId)
						.eq(PointsmallItemRelAttributes::getAttributeType, "item_spec");
				List<PointsmallItemRelAttributes> rels = pointsmallItemRelAttributesMapper.selectList(w);
				for (PointsmallItemRelAttributes ra : rels) {
					if (ra.getItemId() != null
							&& ra.getItemId().equals(itemId)
							&& StringUtils.hasText(ra.getImageUrl())) {
						firstImage = ra.getImageUrl();
						break;
					}
				}
			}
		}
		String itemName = p.getItemName() != null ? p.getItemName() : "";
		int priceFen = p.getPrice() != null ? p.getPrice() : 0;
		int point = p.getPoint() != null ? p.getPoint() : 0;
		return Optional.of(new SkuSnapshot(itemName, priceFen, firstImage, point));
	}

	private static boolean isMultiSpec(String nospec) {
		return "false".equalsIgnoreCase(nospec) || "0".equals(nospec);
	}

	private String firstPicFromPicsJson(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(picsJson.trim());
			if (root.isArray() && root.size() > 0) {
				JsonNode first = root.get(0);
				if (first.isTextual()) {
					return first.asText("");
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}
}
