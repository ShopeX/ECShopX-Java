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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsDetailFacadeService;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerGoods;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerGoodsRel;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksPic;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerGoodsMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerGoodsRelMapper;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksPicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KujialeH5ProductListByPicIdService {

	private static final String KUJIALE_WWW = "www.kujiale.com";
	private static final String KUJIALE_PANO53 = "pano53.p.kujiale.com";

	private final KujialeDesignerGoodsRelMapper goodsRelMapper;
	private final KujialeDesignerGoodsMapper designerGoodsMapper;
	private final KujialeDesignerWorksPicMapper worksPicMapper;
	private final ItemsRepository itemsRepository;
	private final GoodsItemsDetailFacadeService goodsItemsDetailFacadeService;

	public KujialeH5ProductListByPicIdService(
			KujialeDesignerGoodsRelMapper goodsRelMapper,
			KujialeDesignerGoodsMapper designerGoodsMapper,
			KujialeDesignerWorksPicMapper worksPicMapper,
			ItemsRepository itemsRepository,
			GoodsItemsDetailFacadeService goodsItemsDetailFacadeService) {
		this.goodsRelMapper = goodsRelMapper;
		this.designerGoodsMapper = designerGoodsMapper;
		this.worksPicMapper = worksPicMapper;
		this.itemsRepository = itemsRepository;
		this.goodsItemsDetailFacadeService = goodsItemsDetailFacadeService;
	}

	public Map<String, Object> build(String picId, Long companyId, HttpServletRequest request) {
		List<KujialeDesignerGoodsRel> goodsRel =
				goodsRelMapper.selectList(
						new LambdaQueryWrapper<KujialeDesignerGoodsRel>()
								.eq(KujialeDesignerGoodsRel::getPicId, picId));

		List<Object> goods = new ArrayList<>();
		for (KujialeDesignerGoodsRel rel : goodsRel) {
			String obsBrandGoodId = rel.getObsBrandGoodId();
			List<KujialeDesignerGoods> tempGoods =
					designerGoodsMapper.selectList(
							new LambdaQueryWrapper<KujialeDesignerGoods>()
									.eq(KujialeDesignerGoods::getGoodId, obsBrandGoodId));
			for (KujialeDesignerGoods item : tempGoods) {
				String brandGoodCode = item.getBrandGoodCode();
				if (!StringUtils.hasText(brandGoodCode)) {
					continue;
				}
				Items tempItemInfo =
						itemsRepository.findByItemBnAndCompanyIdAndAuditStatus(brandGoodCode, companyId, "approved");
				if (tempItemInfo == null) {
					continue;
				}
				Long itemIdObj = tempItemInfo.getItemId();
				if (itemIdObj == null) {
					continue;
				}
				long itemId = itemIdObj.longValue();
				Map<String, Object> detail =
						goodsItemsDetailFacadeService.getDetailForH5Kujiale(request, companyId, itemId, "");
				if (detail == null || detail.containsKey("status_code")) {
					goods.add(Collections.emptyList());
				} else {
					goods.add(detail);
				}
			}
		}

		KujialeDesignerWorksPic picRow =
				worksPicMapper.selectOne(
						new LambdaQueryWrapper<KujialeDesignerWorksPic>()
								.eq(KujialeDesignerWorksPic::getPicId, picId)
								.last("LIMIT 1"));

		Object picInfo;
		if (picRow == null) {
			picInfo = Collections.emptyList();
		} else {
			Map<String, Object> m = picRowToSnakeMap(picRow);
			Object pano = m.get("pano_link");
			if (pano != null && StringUtils.hasText(pano.toString())) {
				m.put("pano_link", pano.toString().replace(KUJIALE_WWW, KUJIALE_PANO53));
			}
			picInfo = m;
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("goods_list", goods);
		out.put("pic_info", picInfo);
		return out;
	}

	private static Map<String, Object> picRowToSnakeMap(KujialeDesignerWorksPic r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.getId());
		m.put("pic_id", r.getPicId());
		m.put("pic_type", r.getPicType());
		m.put("pic_detail_type", r.getPicDetailType());
		m.put("room_name", r.getRoomName());
		m.put("img", r.getImg());
		m.put("pano_link", r.getPanoLink());
		m.put("design_id", r.getDesignId());
		m.put("plan_id", r.getPlanId());
		m.put("level", r.getLevel());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}
}
