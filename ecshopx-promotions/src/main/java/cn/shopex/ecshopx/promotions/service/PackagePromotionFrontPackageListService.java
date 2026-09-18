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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionFrontPackageListService {

	private final JdbcTemplate jdbcTemplate;
	private final PackagePromotionsMapper packagePromotionsMapper;

	public PackagePromotionFrontPackageListService(
			JdbcTemplate jdbcTemplate, PackagePromotionsMapper packagePromotionsMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.packagePromotionsMapper = packagePromotionsMapper;
	}

	public Map<String, Object> lists(
			long companyId, long itemId, int page, int pageSize, long distributorId) {
		if (itemId <= 0L) {
			return emptyPage();
		}
		Long goodsIdObj = resolveGoodsIdByItemId(itemId);
		if (goodsIdObj == null || goodsIdObj <= 0L) {
			return emptyPage();
		}
		long goodsId = goodsIdObj;
		Map<String, Object> r;
		if (distributorId > 0L) {
			r = loadPackagePage(companyId, goodsId, page, pageSize, distributorId);
			@SuppressWarnings("unchecked")
			List<?> lst = (List<?>) r.get("list");
			Number tc = (Number) r.get("total_count");
			if (lst.isEmpty() || tc.longValue() == 0L) {
				r = loadPackagePage(companyId, goodsId, page, pageSize, 0L);
			}
		} else {
			r = loadPackagePage(companyId, goodsId, page, pageSize, distributorId);
		}
		return r;
	}

	private Long resolveGoodsIdByItemId(long itemId) {
		List<Long> rows =
				jdbcTemplate.query(
						"SELECT goods_id FROM items WHERE item_id = ? LIMIT 1",
						(rs, rowNum) -> {
							long g = rs.getLong("goods_id");
							return rs.wasNull() ? null : g;
						},
						itemId);
		if (rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private static Map<String, Object> emptyPage() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", 0L);
		m.put("list", List.of());
		return m;
	}

	private Map<String, Object> loadPackagePage(
			long companyId, long goodsId, int page, int pageSize, long distributorFilter) {
		long nowEpoch = System.currentTimeMillis() / 1000L;
		int nowInt = (int) Math.min(nowEpoch, (long) Integer.MAX_VALUE);

		LambdaQueryWrapper<PackagePromotions> w = new LambdaQueryWrapper<>();
		w.eq(PackagePromotions::getCompanyId, companyId)
				.eq(PackagePromotions::getGoodsId, goodsId)
				.eq(PackagePromotions::getPackageStatus, "AGREE")
				.lt(PackagePromotions::getStartTime, nowInt)
				.gt(PackagePromotions::getEndTime, nowInt);
		if (distributorFilter > 0L) {
			w.eq(PackagePromotions::getSourceType, "distributor")
					.eq(PackagePromotions::getSourceId, distributorFilter);
		}
		w.orderByDesc(PackagePromotions::getStartTime);

		Page<PackagePromotions> pageReq = new Page<>(page, pageSize);
		packagePromotionsMapper.selectPage(pageReq, w);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", pageReq.getTotal());
		List<Map<String, Object>> rows = new ArrayList<>();
		for (PackagePromotions e : pageReq.getRecords()) {
			rows.add(toListRow(e, nowEpoch));
		}
		out.put("list", rows);
		return out;
	}

	private static Map<String, Object> toListRow(PackagePromotions e, long nowEpoch) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("package_id", e.getPackageId());
		row.put("company_id", e.getCompanyId());
		row.put("goods_id", e.getGoodsId());
		row.put("main_item_id", e.getMainItemId());
		row.put("main_item_price", e.getMainItemPrice());
		row.put("package_name", e.getPackageName());

		String vg = e.getValidGrade();
		if (StringUtils.hasText(vg)) {
			row.put(
					"valid_grade",
					Arrays.stream(vg.split(","))
							.map(String::trim)
							.filter(s -> !s.isEmpty())
							.toList());
		} else {
			row.put("valid_grade", List.of());
		}

		row.put("used_platform", e.getUsedPlatform());
		row.put("free_postage", e.getFreePostage());
		row.put("package_total_price", e.getPackageTotalPrice());
		row.put("start_time", e.getStartTime());
		row.put("end_time", e.getEndTime());
		row.put("package_status", e.getPackageStatus());
		row.put("reason", e.getReason());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		row.put("source_type", e.getSourceType());
		row.put("source_id", e.getSourceId() != null ? e.getSourceId() : 0L);

		long st = e.getStartTime() == null ? 0L : e.getStartTime().longValue();
		long en = e.getEndTime() == null ? 0L : e.getEndTime().longValue();
		String displayStatus;
		if (st > nowEpoch) {
			displayStatus = "waiting";
		} else if (en < nowEpoch) {
			displayStatus = "end";
		} else {
			displayStatus = "ongoing";
		}
		row.put("status", displayStatus);

		return row;
	}
}
