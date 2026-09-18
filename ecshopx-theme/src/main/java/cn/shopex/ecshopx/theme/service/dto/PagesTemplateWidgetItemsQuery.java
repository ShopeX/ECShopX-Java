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

package cn.shopex.ecshopx.theme.service.dto;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class PagesTemplateWidgetItemsQuery {

	private static final Pattern DATA_VALUE_INDEXED_KEY = Pattern.compile("^data_value\\[(\\d+)\\]$");

	private static final class RegionauthSnapshot {
		private final boolean present;
		private final long regionauthId;

		private RegionauthSnapshot(boolean present, long regionauthId) {
			this.present = present;
			this.regionauthId = regionauthId;
		}
	}

	private static final class SortGteSnapshot {
		private final boolean present;
		private final Integer sortGte;

		private SortGteSnapshot(boolean present, Integer sortGte) {
			this.present = present;
			this.sortGte = sortGte;
		}
	}

	private final boolean regionauthIdPresent;
	private final long regionauthId;
	private final Integer distributorId;
	private final String dataType;
	private final String dataValueRaw;
	private final List<Long> dataValueIds;
	private final List<String> dataValueOrderedSegments;
	private final boolean sortGtePresent;
	private final Integer sortGte;
	private final int num;
	private final int page;
	private final Integer pageSizeOverride;
	private final long eActivityId;
	private final boolean applyStoreOnsaleFilter;

	private PagesTemplateWidgetItemsQuery(
			boolean regionauthIdPresent,
			long regionauthId,
			Integer distributorId,
			String dataType,
			String dataValueRaw,
			List<Long> dataValueIds,
			List<String> dataValueOrderedSegments,
			boolean sortGtePresent,
			Integer sortGte,
			int num,
			int page,
			Integer pageSizeOverride,
			long eActivityId,
			boolean applyStoreOnsaleFilter) {
		this.regionauthIdPresent = regionauthIdPresent;
		this.regionauthId = regionauthId;
		this.distributorId = distributorId;
		this.dataType = dataType;
		this.dataValueRaw = dataValueRaw;
		this.dataValueIds = dataValueIds == null ? List.of() : List.copyOf(dataValueIds);
		this.dataValueOrderedSegments =
				dataValueOrderedSegments == null ? List.of() : List.copyOf(dataValueOrderedSegments);
		this.sortGtePresent = sortGtePresent;
		this.sortGte = sortGte;
		this.num = num;
		this.page = page;
		this.pageSizeOverride = pageSizeOverride;
		this.eActivityId = eActivityId;
		this.applyStoreOnsaleFilter = applyStoreOnsaleFilter;
	}

	public static PagesTemplateWidgetItemsQuery fromHttpServletRequest(HttpServletRequest request) {
		String dtRaw = request.getParameter("data_type");
		String dataType = dtRaw == null ? "" : dtRaw.trim();
		if (!StringUtils.hasText(dataType)) {
			throw new BadRequestException("data_type 无效");
		}
		DataValueSnapshot dv = parseDataValue(request);
		Integer distributorId = parseDistributorId(request);
		RegionauthSnapshot ra = parseRegionauth(request);
		SortGteSnapshot sg = parseSortGte(request);
		int num = parseNumWithDefault(request);
		int page = parsePage(request);
		Integer pageSizeOverride = parsePageSizeOverride(request);
		long eActivityId = parseEActivityId(request);
		return new PagesTemplateWidgetItemsQuery(
				ra.present,
				ra.regionauthId,
				distributorId,
				dataType,
				dv.raw,
				dv.ids,
				dv.orderedSegments,
				sg.present,
				sg.sortGte,
				num,
				page,
				pageSizeOverride,
				eActivityId,
				false);
	}

	public static PagesTemplateWidgetItemsQuery fromH5FrontHttpServletRequest(HttpServletRequest request) {
		String dtRaw = request.getParameter("data_type");
		String dataType;
		if (dtRaw == null || !StringUtils.hasText(dtRaw.trim())) {
			dataType = "";
		} else {
			dataType = dtRaw.trim();
		}
		DataValueSnapshot dv = parseDataValue(request);
		Integer distributorId = parseDistributorId(request);
		RegionauthSnapshot ra = parseRegionauth(request);
		SortGteSnapshot sg = parseSortGte(request);
		int num = parseNumWithDefault(request);
		int page = parsePage(request);
		Integer pageSizeOverride = parsePageSizeOverride(request);
		long eActivityId = parseEActivityId(request);
		return new PagesTemplateWidgetItemsQuery(
				ra.present,
				ra.regionauthId,
				distributorId,
				dataType,
				dv.raw,
				dv.ids,
				dv.orderedSegments,
				sg.present,
				sg.sortGte,
				num,
				page,
				pageSizeOverride,
				eActivityId,
				true);
	}

	private static final class DataValueSnapshot {
		private final String raw;
		private final List<Long> ids;
		private final List<String> orderedSegments;

		private DataValueSnapshot(String raw, List<Long> ids, List<String> orderedSegments) {
			this.raw = raw;
			this.ids = ids;
			this.orderedSegments = orderedSegments;
		}
	}

	private static DataValueSnapshot parseDataValue(HttpServletRequest request) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		ArrayList<String> orderedSegments = new ArrayList<>();
		appendOrderedSegments(request.getParameterValues("data_value[]"), orderedSegments);
		String[] dataValueParams = request.getParameterValues("data_value");
		boolean multiDataValue = dataValueParams != null && dataValueParams.length > 1;
		if (multiDataValue) {
			appendOrderedSegments(dataValueParams, orderedSegments);
		}
		appendParsedLongs(request.getParameterValues("data_value"), ids);
		appendParsedLongs(request.getParameterValues("data_value[]"), ids);
		appendIndexedDataValueParams(request, ids, orderedSegments);
		String scalar = request.getParameter("data_value");
		String raw = null;
		if (scalar != null && StringUtils.hasText(scalar.trim())) {
			raw = scalar.trim();
			if (!multiDataValue && orderedSegments.isEmpty() && ids.isEmpty()) {
				appendParsedLongs(raw.split(",", -1), ids);
			}
		}
		return new DataValueSnapshot(raw, List.copyOf(ids), List.copyOf(orderedSegments));
	}

	private static void appendIndexedDataValueParams(
			HttpServletRequest request, LinkedHashSet<Long> ids, ArrayList<String> orderedSegments) {
		TreeMap<Integer, String> byIndex = new TreeMap<>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			Matcher matcher = DATA_VALUE_INDEXED_KEY.matcher(e.getKey());
			if (!matcher.matches()) {
				continue;
			}
			int idx = Integer.parseInt(matcher.group(1));
			String[] vals = e.getValue();
			if (vals == null || vals.length == 0) {
				continue;
			}
			String seg = vals[0] == null ? "" : vals[0].trim();
			if (!StringUtils.hasText(seg)) {
				continue;
			}
			byIndex.putIfAbsent(idx, seg);
		}
		if (byIndex.isEmpty()) {
			return;
		}
		String[] ordered = byIndex.values().toArray(String[]::new);
		appendOrderedSegments(ordered, orderedSegments);
		appendParsedLongs(ordered, ids);
	}

	private static void appendOrderedSegments(String[] values, List<String> ordered) {
		if (values == null) {
			return;
		}
		for (String value : values) {
			String seg = value == null ? "" : value.trim();
			if (!StringUtils.hasText(seg)) {
				continue;
			}
			for (String part : seg.split(",", -1)) {
				String p = part == null ? "" : part.trim();
				if (!StringUtils.hasText(p)) {
					continue;
				}
				ordered.add(p);
			}
		}
	}

	private static void appendParsedLongs(String[] values, LinkedHashSet<Long> ids) {
		if (values == null) {
			return;
		}
		for (String value : values) {
			String seg = value == null ? "" : value.trim();
			if (!StringUtils.hasText(seg)) {
				continue;
			}
			for (String part : seg.split(",", -1)) {
				String p = part == null ? "" : part.trim();
				if (!StringUtils.hasText(p)) {
					continue;
				}
				try {
					long v = Long.parseLong(p);
					if (v > 0L) {
						ids.add(v);
					}
				} catch (NumberFormatException ex) {
					throw new BadRequestException("data_value 无效");
				}
			}
		}
	}

	private static Integer parseDistributorId(HttpServletRequest request) {
		Integer distributorId = null;
		if (request.getParameterMap().containsKey("distributor_id")) {
			String d = request.getParameter("distributor_id");
			if (StringUtils.hasText(d)) {
				try {
					int v = Integer.parseInt(d.trim());
					if (v > 0) {
						distributorId = v;
					}
				} catch (NumberFormatException ex) {
					throw new BadRequestException("distributor_id 无效");
				}
			}
		}
		return distributorId;
	}

	private static RegionauthSnapshot parseRegionauth(HttpServletRequest request) {
		boolean regionauthIdPresent = request.getParameterMap().containsKey("regionauth_id");
		long regionauthId = 0L;
		if (regionauthIdPresent) {
			String ridRaw = request.getParameter("regionauth_id");
			String t = ridRaw == null ? "" : ridRaw.trim();
			if (!StringUtils.hasText(t)) {
				regionauthId = 0L;
			} else {
				try {
					regionauthId = new BigDecimal(t).longValueExact();
				} catch (ArithmeticException | NumberFormatException ex) {
					throw new BadRequestException("regionauth_id 无效");
				}
			}
		}
		return new RegionauthSnapshot(regionauthIdPresent, regionauthId);
	}

	private static SortGteSnapshot parseSortGte(HttpServletRequest request) {
		boolean sortGtePresent = request.getParameterMap().containsKey("sort_gte");
		Integer sortGte = null;
		if (sortGtePresent) {
			String sg = request.getParameter("sort_gte");
			if (sg == null || !StringUtils.hasText(sg.trim())) {
				throw new BadRequestException("sort_gte 无效");
			}
			try {
				sortGte = Integer.parseInt(sg.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("sort_gte 无效");
			}
		}
		return new SortGteSnapshot(sortGtePresent, sortGte);
	}

	private static int parseNumWithDefault(HttpServletRequest request) {
		String numStr = request.getParameter("num");
		int num = 10;
		if (StringUtils.hasText(numStr)) {
			try {
				num = Integer.parseInt(numStr.trim());
				if (num <= 0) {
					num = 10;
				}
			} catch (NumberFormatException ex) {
				throw new BadRequestException("num 无效");
			}
		}
		return num;
	}

	private static int parsePage(HttpServletRequest request) {
		String pageStr = request.getParameter("page");
		String pageEffective = (StringUtils.hasText(pageStr)) ? pageStr.trim() : "1";
		if (!StringUtils.hasText(pageEffective)) {
			pageEffective = "1";
		}
		int page;
		try {
			page = Integer.parseInt(pageEffective);
			if (page < 1) {
				throw new BadRequestException("page 无效");
			}
		} catch (NumberFormatException ex) {
			throw new BadRequestException("page 无效");
		}
		return page;
	}

	private static Integer parsePageSizeOverride(HttpServletRequest request) {
		Integer pageSizeOverride = null;
		if (request.getParameterMap().containsKey("pageSize")) {
			String ps = request.getParameter("pageSize");
			if (StringUtils.hasText(ps)) {
				try {
					int v = Integer.parseInt(ps.trim());
					if (v <= 0) {
						throw new BadRequestException("pageSize 无效");
					}
					pageSizeOverride = v;
				} catch (NumberFormatException ex) {
					throw new BadRequestException("pageSize 无效");
				}
			}
		}
		return pageSizeOverride;
	}

	public boolean isRegionauthIdPresent() {
		return regionauthIdPresent;
	}

	public long getRegionauthId() {
		return regionauthId;
	}

	public Integer getDistributorId() {
		return distributorId;
	}

	public String getDataType() {
		return dataType;
	}

	public String getDataValueRaw() {
		return dataValueRaw;
	}

	public List<Long> getDataValueIds() {
		return dataValueIds;
	}

	public List<String> getDataValueOrderedSegments() {
		return dataValueOrderedSegments;
	}

	public boolean isSortGtePresent() {
		return sortGtePresent;
	}

	public Integer getSortGte() {
		return sortGte;
	}

	public int getNum() {
		return num;
	}

	public int getPage() {
		return page;
	}

	public Integer getPageSizeOverride() {
		return pageSizeOverride;
	}

	public long getEActivityId() {
		return eActivityId;
	}

	public boolean isApplyStoreOnsaleFilter() {
		return applyStoreOnsaleFilter;
	}

	private static long parseEActivityId(HttpServletRequest request) {
		String raw = request.getParameter("e_activity_id");
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
