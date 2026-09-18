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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityItems;
import cn.shopex.ecshopx.community.mapper.CommunityItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CommunityItemsCreateService {

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final CommunityItemsMapper communityItemsMapper;
	private final TransactionTemplate transactionTemplate;

	public CommunityItemsCreateService(ItemsListQueryRepository itemsListQueryRepository,
			CommunityItemsMapper communityItemsMapper,
			PlatformTransactionManager transactionManager) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.communityItemsMapper = communityItemsMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public void updateMinDeliveryNumByFilter(long companyId, int distributorId, List<Long> goodsIds, int minDeliveryNum) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return;
		}
		LambdaUpdateWrapper<CommunityItems> uw = Wrappers.lambdaUpdate();
		uw.eq(CommunityItems::getCompanyId, companyId)
				.eq(CommunityItems::getDistributorId, distributorId);
		if (goodsIds.size() == 1) {
			uw.eq(CommunityItems::getGoodsId, goodsIds.get(0));
		} else {
			uw.in(CommunityItems::getGoodsId, goodsIds);
		}
		uw.set(CommunityItems::getMinDeliveryNum, minDeliveryNum);
		communityItemsMapper.update(null, uw);
	}

	public void deleteByGoodsFilter(long companyId, int distributorId, long goodsId) {
		LambdaQueryWrapper<CommunityItems> w = Wrappers.lambdaQuery();
		w.eq(CommunityItems::getCompanyId, companyId)
				.eq(CommunityItems::getDistributorId, distributorId)
				.eq(CommunityItems::getGoodsId, goodsId);
		communityItemsMapper.delete(w);
	}

	public void updateSortByFilter(long companyId, int distributorId, List<Long> goodsIds, int sort) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return;
		}
		LambdaUpdateWrapper<CommunityItems> uw = Wrappers.lambdaUpdate();
		uw.eq(CommunityItems::getCompanyId, companyId)
				.eq(CommunityItems::getDistributorId, distributorId);
		if (goodsIds.size() == 1) {
			uw.eq(CommunityItems::getGoodsId, goodsIds.get(0));
		} else {
			uw.in(CommunityItems::getGoodsId, goodsIds);
		}
		uw.set(CommunityItems::getSort, sort);
		communityItemsMapper.update(null, uw);
	}

	public void batchInsert(long companyId, int distributorId, List<Long> goodsIds) {
		for (Long goodsId : goodsIds) {
			validateSkusForGoods(companyId, distributorId, goodsId);
		}
		transactionTemplate.executeWithoutResult(status -> {
			int now = (int) (System.currentTimeMillis() / 1000L);
			for (Long goodsId : goodsIds) {
				insertOneIfAbsent(companyId, distributorId, goodsId, now);
			}
		});
	}

	private void validateSkusForGoods(long companyId, int distributorId, long goodsId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		params.put(ItemsListQueryRepository.KEY_GOODS_ID_IN, List.of(goodsId));
		params.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, distributorId);
		List<Items> rows = itemsListQueryRepository.selectPageByParamsForSku(params, 0, -1);

		boolean any = false;
		boolean onsale = false;
		String lastItemName = null;
		for (Items item : rows) {
			if (item == null) {
				continue;
			}
			any = true;
			lastItemName = item.getItemName();
			if (!"approved".equals(item.getAuditStatus())) {
				throw new ResourceException("商品" + item.getItemName() + "还未审核通过");
			}
			if ("onsale".equals(item.getApproveStatus())) {
				onsale = true;
			}
		}
		if (!any) {
			throw new ResourceException("ID为" + goodsId + "的商品不存在");
		}
		if (!onsale) {
			throw new ResourceException("商品" + (lastItemName != null ? lastItemName : "") + "还未上架");
		}
	}

	private void insertOneIfAbsent(long companyId, int distributorId, long goodsId, int now) {
		try {
			LambdaQueryWrapper<CommunityItems> w = Wrappers.lambdaQuery();
			w.eq(CommunityItems::getGoodsId, goodsId)
					.eq(CommunityItems::getCompanyId, companyId)
					.eq(CommunityItems::getDistributorId, distributorId);
			CommunityItems existing = communityItemsMapper.selectOne(w);
			if (existing != null) {
				return;
			}
			CommunityItems entity = new CommunityItems();
			entity.setGoodsId(goodsId);
			entity.setCompanyId(companyId);
			entity.setDistributorId(distributorId);
			entity.setCreatedAt(now);
			entity.setUpdatedAt(now);
			communityItemsMapper.insert(entity);
		} catch (DataAccessException e) {
			throw new ResourceException("validation.conflict");
		}
	}
}
