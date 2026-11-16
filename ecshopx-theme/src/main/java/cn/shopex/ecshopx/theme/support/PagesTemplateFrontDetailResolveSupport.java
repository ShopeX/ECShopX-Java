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

package cn.shopex.ecshopx.theme.support;

import java.util.Map;
import java.util.Objects;

/** Front {@code pagestemplate/detail} template row lookup rules (shop vs platform pool). */
public final class PagesTemplateFrontDetailResolveSupport {

	private PagesTemplateFrontDetailResolveSupport() {}

	/**
	 * Platform template pool (distributor_id=0) when headquarters index mode, or shop index mode
	 * on a virtual store ({@code distributor_self=1}).
	 */
	public static boolean usePlatformTemplatePool(int indexType, boolean virtualDistributor) {
		if (indexType == 1) {
			return true;
		}
		return indexType == 2 && virtualDistributor;
	}

	public static boolean isVirtualDistributorRow(Map<String, Object> distributorRow) {
		if (distributorRow == null || distributorRow.isEmpty()) {
			return false;
		}
		return isDistributorSelfTruthy(distributorRow.get("distributor_self"));
	}

	/** Align with {@link cn.shopex.ecshopx.theme.service.PagesTemplateFrontShopDetailService}. */
	public static String weappPagesForShopTemplateDb(String weappPagesRaw) {
		String resolved = nullToIndex(weappPagesRaw);
		return Objects.equals(resolved, "index") ? "distributor_index" : resolved;
	}

	public static String weappPagesForPlatformPoolDb(String weappPagesRaw) {
		return nullToIndex(weappPagesRaw);
	}

	private static String nullToIndex(String weappPages) {
		return weappPages == null || weappPages.isBlank() ? "index" : weappPages.trim();
	}

	private static boolean isDistributorSelfTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String t = String.valueOf(raw).trim();
		return "1".equals(t) || "true".equalsIgnoreCase(t);
	}
}