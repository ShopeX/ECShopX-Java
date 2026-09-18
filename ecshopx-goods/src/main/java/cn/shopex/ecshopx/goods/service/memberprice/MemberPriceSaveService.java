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

package cn.shopex.ecshopx.goods.service.memberprice;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.repository.ItemsMemberPriceCheckRepository;
import cn.shopex.ecshopx.goods.repository.ItemsStartNumUpdateRepository;
import cn.shopex.ecshopx.promotions.domain.MemberPrice;
import cn.shopex.ecshopx.promotions.repository.MemberPriceBulkDeleteRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceInsertRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberPriceSaveService {

	private final ObjectMapper objectMapper;
	private final MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository;
	private final MemberPriceInsertRepository memberPriceInsertRepository;
	private final ItemsMemberPriceCheckRepository itemsMemberPriceCheckRepository;
	private final ItemsStartNumUpdateRepository itemsStartNumUpdateRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorItemsRepository distributorItemsRepository;

	public MemberPriceSaveService(
			ObjectMapper objectMapper,
			MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository,
			MemberPriceInsertRepository memberPriceInsertRepository,
			ItemsMemberPriceCheckRepository itemsMemberPriceCheckRepository,
			ItemsStartNumUpdateRepository itemsStartNumUpdateRepository,
			DistributorListQueryService distributorListQueryService,
			DistributorItemsRepository distributorItemsRepository) {
		this.objectMapper = objectMapper;
		this.memberPriceBulkDeleteRepository = memberPriceBulkDeleteRepository;
		this.memberPriceInsertRepository = memberPriceInsertRepository;
		this.itemsMemberPriceCheckRepository = itemsMemberPriceCheckRepository;
		this.itemsStartNumUpdateRepository = itemsStartNumUpdateRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorItemsRepository = distributorItemsRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveMemberPrice(long companyId, String itemIdIgnored, String mpriceJson) {
		JsonNode root;
		try {
			root = objectMapper.readTree(mpriceJson);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("会员价数据有误");
		}
		if (root == null || !root.isObject() || root.size() == 0) {
			throw new BadRequestException("会员价数据有误");
		}

		List<Long> itemIds = new ArrayList<>();
		Iterator<Map.Entry<String, JsonNode>> it0 = root.fields();
		while (it0.hasNext()) {
			Map.Entry<String, JsonNode> en = it0.next();
			long parsed = parsePositiveItemIdKey(en.getKey());
			if (parsed > 0) {
				itemIds.add(parsed);
			}
		}

		memberPriceBulkDeleteRepository.deleteByCompanyIdAndItemIds(companyId, itemIds);

		Iterator<Map.Entry<String, JsonNode>> it = root.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> en = it.next();
			long itemId = parsePositiveItemIdKey(en.getKey());
			if (itemId <= 0) {
				continue;
			}
			JsonNode valNode = en.getValue();
			if (!valNode.isObject()) {
				throw new BadRequestException("会员价数据有误");
			}
			ObjectNode valObj = valNode.deepCopy();
			JsonNode gradeNode = valObj.get("grade");
			JsonNode vipNode = valObj.get("vipGrade");
			if (gradeNode == null || !gradeNode.isObject() || vipNode == null || !vipNode.isObject()) {
				throw new BadRequestException("会员价数据有误");
			}

			convertGradeOrVipToFenAndCheck(companyId, itemId, valObj, "grade");
			convertGradeOrVipToFenAndCheck(companyId, itemId, valObj, "vipGrade");

			MemberPrice entity = new MemberPrice();
			entity.setCompanyId(companyId);
			entity.setItemId(itemId);
			try {
				entity.setMprice(MemberPriceColumnCodec.encodeForStorage(objectMapper, valObj));
			} catch (JsonProcessingException e) {
				throw new BadRequestException("会员价数据有误");
			}

			int rows = memberPriceInsertRepository.insert(entity);
			if (rows != 1) {
				throw new ResourceException("保存会员价失败");
			}

			JsonNode startNumNode = valObj.get("start_num");
			if (startNumNode != null && startNumNode.isNumber() && startNumNode.doubleValue() > 0) {
				long sl = startNumNode.longValue();
				if (sl > 0 && sl <= Integer.MAX_VALUE) {
					itemsStartNumUpdateRepository.updateStartNumIfItemExists(itemId, (int) sl);
				}
			}
		}
	}

	private static long parsePositiveItemIdKey(String key) {
		if (key == null || key.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(key.trim());
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void convertGradeOrVipToFenAndCheck(long companyId, long itemId, ObjectNode valObj, String fieldName) {
		ObjectNode bucket = (ObjectNode) valObj.get(fieldName);
		Iterator<String> names = bucket.fieldNames();
		while (names.hasNext()) {
			String tierKey = names.next();
			JsonNode leaf = bucket.get(tierKey);
			if (leaf == null || leaf.isNull()) {
				putZeroFenAfterCheck(companyId, itemId, bucket, tierKey);
				continue;
			}
			if (leaf.isTextual() && leaf.asText().trim().isEmpty()) {
				putZeroFenAfterCheck(companyId, itemId, bucket, tierKey);
				continue;
			}
			BigDecimal yuan;
			if (leaf.isNumber()) {
				yuan = leaf.decimalValue();
			} else if (leaf.isTextual()) {
				try {
					yuan = new BigDecimal(leaf.asText().trim());
				} catch (NumberFormatException e) {
					throw new BadRequestException("会员价数据有误");
				}
			} else {
				throw new BadRequestException("会员价数据有误");
			}
			if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
				throw new ResourceException("会员价必须大于0");
			}
			long fen;
			try {
				fen = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact();
			} catch (ArithmeticException e) {
				throw new BadRequestException("会员价数据有误");
			}
			checkMemberPrice(companyId, itemId, fen);
			bucket.put(tierKey, String.valueOf(fen));
		}
	}

	private void putZeroFenAfterCheck(long companyId, long itemId, ObjectNode bucket, String tierKey) {
		checkMemberPrice(companyId, itemId, 0L);
		bucket.put(tierKey, "0");
	}

	private void checkMemberPrice(long companyId, long itemId, long mpriceFen) {
		if (itemsMemberPriceCheckRepository.existsHeadquartersPriceLessThanMember(companyId, itemId, mpriceFen)) {
			throw new ResourceException("会员价格不能比销售价高");
		}
		List<Long> distributorIds = distributorListQueryService.listNonDeletedDistributorIdsForCompany(companyId);
		if (distributorIds == null || distributorIds.isEmpty()) {
			return;
		}
		Optional<DistributorItems> hit =
				distributorItemsRepository.findFirstNonTotalStorePriceBelowMember(companyId, distributorIds, itemId, mpriceFen);
		if (hit.isEmpty()) {
			return;
		}
		long distributorId = hit.get().getDistributorId();
		List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
		String shopCode = "";
		if (!dists.isEmpty() && dists.get(0).getShopCode() != null) {
			shopCode = dists.get(0).getShopCode();
		}
		throw new ResourceException("会员价格不能比门店销售价高（店铺号：" + shopCode + "）");
	}
}
