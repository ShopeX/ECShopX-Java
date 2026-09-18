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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartDataListFacade;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonDistributorCartFormatAndTotalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartDataListFacadeImpl implements OperatorCartDataListFacade {

	private final OperatorCartMapper operatorCartMapper;
	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;
	private final OperatorCartCompanyProductModelReader productModelReader;
	private final OperatorCartHandleValidCartService operatorCartHandleValidCartService;
	private final SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService;
	private final LangueProperties langueProperties;

	public OperatorCartDataListFacadeImpl(OperatorCartMapper operatorCartMapper,
			OperatorCartSkuLoadFacade operatorCartSkuLoadFacade,
			OperatorCartCompanyProductModelReader productModelReader,
			OperatorCartHandleValidCartService operatorCartHandleValidCartService,
			SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService,
			LangueProperties langueProperties) {
		this.operatorCartMapper = operatorCartMapper;
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
		this.productModelReader = productModelReader;
		this.operatorCartHandleValidCartService = operatorCartHandleValidCartService;
		this.salespersonDistributorCartFormatAndTotalService = salespersonDistributorCartFormatAndTotalService;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> getCartDataList(long companyId, long operatorId, long distributorId, long targetUserId,
			HttpServletRequest request, boolean isSubmit) {
		LambdaQueryWrapper<OperatorCart> w = new LambdaQueryWrapper<>();
		w.eq(OperatorCart::getCompanyId, companyId).eq(OperatorCart::getOperatorId, operatorId)
				.eq(OperatorCart::getDistributorId, distributorId);
		if (isSubmit) {
			w.eq(OperatorCart::getIsChecked, true);
		}
		List<OperatorCart> cartRows = operatorCartMapper.selectList(w);
		if (cartRows.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("购物车为空");
			}
			return emptyBody();
		}

		List<Long> itemIds = new ArrayList<>();
		for (OperatorCart row : cartRows) {
			Long iid = row.getItemId();
			if (iid != null && iid > 0L && !itemIds.contains(iid)) {
				itemIds.add(iid);
			}
		}

		List<OperatorCartSkuRowDto> skus = operatorCartSkuLoadFacade.loadSkus(companyId, distributorId, targetUserId, itemIds);
		if (skus == null || skus.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("商品已失效");
			}
			return emptyBody();
		}

		Map<Long, OperatorCartSkuRowDto> skuByItemId =
				skus.stream().collect(Collectors.toMap(OperatorCartSkuRowDto::getItemId, s -> s, (a, b) -> a));

		String productModel = productModelReader.getProductModel(companyId);
		if (distributorId > 0L && "standard".equals(productModel)) {
			operatorCartSkuLoadFacade.applyDistributorSkuReplace(companyId, distributorId, productModel, skus);
		}

		Map<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		for (Long itemId : itemIds) {
			OperatorCartSkuRowDto dto = skuByItemId.get(itemId);
			if (dto == null) {
				continue;
			}
			itemByItemId.put(itemId, skuDtoToItemRow(dto));
		}
		if (itemByItemId.isEmpty()) {
			if (isSubmit) {
				throw new ResourceException("商品已失效");
			}
			return emptyBody();
		}

		List<Map<String, Object>> cartRowMaps = new ArrayList<>();
		for (OperatorCart row : cartRows) {
			cartRowMaps.add(operatorCartEntityToRowMap(row));
		}

		Map<String, Object> handled =
				operatorCartHandleValidCartService.handle(companyId, targetUserId, cartRowMaps, itemByItemId);
		handled.put("is_check_store", Boolean.FALSE);

		String acceptLang = resolveAcceptLanguage(request);
		Map<String, Object> cartData = salespersonDistributorCartFormatAndTotalService.apply(
				companyId, targetUserId, handled, isSubmit, distributorId, "distributor", acceptLang);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invalidAfter = (List<Map<String, Object>>) cartData.get("invalid_cart");
		if (invalidAfter != null && !invalidAfter.isEmpty()) {
			List<Long> cartIds = new ArrayList<>();
			for (Map<String, Object> row : invalidAfter) {
				long cid = longFrom(row.get("cart_id"));
				if (cid > 0L) {
					cartIds.add(cid);
				}
			}
			if (!cartIds.isEmpty()) {
				LambdaUpdateWrapper<OperatorCart> uw = new LambdaUpdateWrapper<>();
				uw.in(OperatorCart::getCartId, cartIds).set(OperatorCart::getIsChecked, false);
				operatorCartMapper.update(null, uw);
			}
		}

		return cartData;
	}

	private static Map<String, Object> operatorCartEntityToRowMap(OperatorCart row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", row.getCartId());
		m.put("company_id", row.getCompanyId());
		m.put("operator_id", row.getOperatorId());
		m.put("distributor_id", row.getDistributorId());
		m.put("item_id", row.getItemId());
		m.put("num", row.getNum() != null ? row.getNum() : 1L);
		m.put("is_checked", row.getIsChecked());
		m.put("special_type", row.getSpecialType() != null ? row.getSpecialType() : "normal");
		m.put("shop_type", "shop_offline");
		Long did = row.getDistributorId();
		m.put("shop_id", did != null ? did : 0L);
		return m;
	}

	private static Map<String, Object> skuDtoToItemRow(OperatorCartSkuRowDto dto) {
		Map<String, Object> row = new LinkedHashMap<>();
		long itemId = dto.getItemId();
		long unitFen = dto.getUnitPriceFen();
		long defItem = dto.getDefaultItemId() > 0L ? dto.getDefaultItemId() : itemId;

		row.put("item_id", itemId);
		row.put("price", unitFen);
		row.put("sale_price", unitFen);
		row.put("store", (long) dto.getStore());
		row.put("item_name", dto.getItemName() != null ? dto.getItemName() : "");
		row.put("item_category", dto.getItemCategory());
		row.put("brand_id", dto.getBrandId());
		row.put("type", dto.getGoodsType() != null ? dto.getGoodsType() : 0);
		row.put("item_type", dto.getItemType() != null ? dto.getItemType() : "services");
		String ap0 = dto.getItemApproveStatus();
		row.put("approve_status", ap0 != null ? ap0 : "onsale");
		String st = dto.getSpecialType();
		row.put("special_type", st != null && !st.isBlank() ? st : "normal");
		row.put("is_gift", dto.getIsGift() != null ? dto.getIsGift() : Boolean.FALSE);
		Integer mp = dto.getMarketPriceFen();
		row.put("market_price", mp != null ? mp.longValue() : unitFen);
		row.put("brief", dto.getBrief() != null ? dto.getBrief() : "");
		row.put("pics", dto.getPics() != null ? dto.getPics() : "");
		row.put("item_spec_desc", "");
		row.put("spec_image_url", "");
		row.put("crossborder_tax_rate", dto.getCrossborderTaxRate() != null ? dto.getCrossborderTaxRate() : "0");
		row.put("taxstrategy_id", dto.getTaxstrategyId() != null ? dto.getTaxstrategyId() : 0L);
		row.put("taxation_num", dto.getTaxationNum() != null ? dto.getTaxationNum() : 0);
		Long oc = dto.getOrigincountryId();
		row.put("origincountry_id", oc != null ? oc : "");
		row.put("is_medicine", dto.getIsMedicine() != null ? dto.getIsMedicine() : 0);
		row.put("is_prescription", dto.getIsPrescription() != null ? dto.getIsPrescription() : 0);
		row.put("start_num", dto.getStartNum() != null ? dto.getStartNum() : 0);
		row.put("medicine_data", null);
		Long gidObj = dto.getGoodsId();
		long gid = gidObj != null && gidObj > 0L ? gidObj : defItem;
		row.put("goods_id", gid);

		row.put("price", unitFen);
		row.put("sale_price", unitFen);
		row.put("store", (long) dto.getStore());

		if (dto.isSaleDisabled()) {
			row.put("approve_status", "instock");
		}
		return row;
	}

	private static Map<String, Object> emptyBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("invalid_cart", List.of());
		m.put("valid_cart", List.of());
		return m;
	}

	private String resolveAcceptLanguage(HttpServletRequest request) {
		if (request == null) {
			return "zh-CN";
		}
		return RequestLangTag.current(langueProperties);
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
