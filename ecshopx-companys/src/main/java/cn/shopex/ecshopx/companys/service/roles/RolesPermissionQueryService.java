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

package cn.shopex.ecshopx.companys.service.roles;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.dto.PermissionTreeFilter;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.service.permission.PermissionShopMenuTreeService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RolesPermissionQueryService {

	private final CompanysMapper companysMapper;
	private final PermissionShopMenuTreeService permissionShopMenuTreeService;
	private final SupplierMapper supplierMapper;

	public RolesPermissionQueryService(
			CompanysMapper companysMapper,
			PermissionShopMenuTreeService permissionShopMenuTreeService,
			SupplierMapper supplierMapper) {
		this.companysMapper = companysMapper;
		this.permissionShopMenuTreeService = permissionShopMenuTreeService;
		this.supplierMapper = supplierMapper;
	}

	public List<Map<String, Object>> getPermission(
			String versionQuery, Map<String, Object> jwt, String requestLang) {
		long companyId = parseRequiredCompanyId(jwt);
		String operatorType = jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString().trim();

		long operatorId = 0L;
		if (isRoleBranchOperatorType(operatorType)) {
			operatorId = parseRequiredOperatorId(jwt);
		} else {
			Object oid = jwt.get("operator_id");
			if (oid != null) {
				operatorId = parseLongLoose(oid);
			}
		}

		int version = resolveMenuVersion(operatorType, versionQuery);

		Companys c = companysMapper.selectById(companyId);
		if (c == null
				|| (c.getExpiredAt() != null && c.getExpiredAt() < (System.currentTimeMillis() / 1000L))) {
			return List.of(Map.of("url", "/login"));
		}

		boolean serializeEmptyChildrenArrays = versionQueryLooksPresent(versionQuery);
		PermissionTreeFilter filter =
				new PermissionTreeFilter(companyId, version, operatorType, operatorId, serializeEmptyChildrenArrays);
		List<Map<String, Object>> tree =
				new ArrayList<>(permissionShopMenuTreeService.getPermissionTree(filter, requestLang));

		int pcTpl = c.getIsOpenPcTemplate() != null ? c.getIsOpenPcTemplate() : 2;
		int domainSetting = c.getIsOpenDomainSetting() != null ? c.getIsOpenDomainSetting() : 2;

		List<String> superPermission = List.of("/setting/operatorlogs");
		for (Iterator<Map<String, Object>> it = tree.iterator(); it.hasNext(); ) {
			Map<String, Object> v = it.next();
			Object ch = v.get("children");
			boolean noChildren =
					ch == null || (ch instanceof List<?> list && list.isEmpty());
			if (noChildren) {
				if ("/shopadmin/applications".equals(urlOf(v))) {
					it.remove();
				} else if (serializeEmptyChildrenArrays) {
					v.put("children", new ArrayList<>());
				}
				continue;
			}

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = new ArrayList<>((List<Map<String, Object>>) ch);

			for (Iterator<Map<String, Object>> cit = children.iterator(); cit.hasNext(); ) {
				Map<String, Object> vv = cit.next();
				String url = urlOf(vv);
				if (url != null && superPermission.contains(url)) {
					vv.put("is_super", "Y");
					if (!"admin".equals(operatorType)) {
						cit.remove();
						continue;
					}
				}
				if (pcTpl == 2 && "/wxapp/pcmall".equals(url)) {
					cit.remove();
					continue;
				}
				if (domainSetting == 2 && "/setting/domain_setting".equals(url)) {
					cit.remove();
				}
			}

			if (children.isEmpty()) {
				if (serializeEmptyChildrenArrays) {
					v.put("children", new ArrayList<>());
				} else {
					v.remove("children");
				}
			} else {
				v.put("children", children);
			}
		}

		if ("supplier".equals(operatorType)) {
			Supplier s =
					supplierMapper.selectOne(
							new LambdaQueryWrapper<Supplier>()
									.eq(Supplier::getOperatorId, operatorId)
									.last("LIMIT 1"));
			long check = (s != null && s.getIsCheck() != null) ? s.getIsCheck() : 0L;
			if (check != 1L) {
				List<Map<String, Object>> kept = new ArrayList<>();
				for (Map<String, Object> top : tree) {
					if (!"/supplier/setting".equals(urlOf(top))) {
						continue;
					}
					Object cobj = top.get("children");
					if (cobj instanceof List<?> raw) {
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> sub = new ArrayList<>((List<Map<String, Object>>) cobj);
						sub.removeIf(
								child -> {
									String u = urlOf(child);
									return u == null || !u.contains("supplier_register");
								});
						top.put("children", new ArrayList<>(sub));
						if (sub.isEmpty()) {
							if (serializeEmptyChildrenArrays) {
								top.put("children", new ArrayList<>());
							} else {
								top.remove("children");
							}
						}
					}
					kept.add(top);
				}
				tree.clear();
				tree.addAll(kept);
			}
		}

		for (Map<String, Object> top : tree) {
			Object cobj = top.get("children");
			if (cobj instanceof List<?> raw) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> sub = (List<Map<String, Object>>) cobj;
				top.put("children", new ArrayList<>(sub));
			}
		}

		return tree;
	}

	private static boolean isRoleBranchOperatorType(String operatorType) {
		if (operatorType == null || operatorType.isEmpty()) {
			return false;
		}
		String lc = operatorType.toLowerCase(Locale.ROOT);
		return "staff".equals(lc) || "distributor".equals(lc) || "dealer".equals(lc);
	}

	private static boolean versionQueryLooksPresent(String s) {
		return s != null && !s.isBlank() && !"0".equals(s.trim());
	}

	private static int resolveMenuVersion(String operatorType, String versionQuery) {
		int v;
		if ("distributor".equals(operatorType)) {
			v = 3;
		} else if ("dealer".equals(operatorType)) {
			v = 5;
		} else if ("merchant".equals(operatorType)) {
			v = 6;
		} else if ("supplier".equals(operatorType)) {
			v = 7;
		} else {
			v = 1;
		}
		if (versionQueryLooksPresent(versionQuery)) {
			v = 3;
		}
		return v;
	}

	private static long parseRequiredCompanyId(Map<String, Object> jwt) {
		Object v = jwt.get("company_id");
		if (v == null) {
			throw new ResourceException("登录验证错误");
		}
		long id = parseLongLoose(v);
		if (id <= 0) {
			throw new ResourceException("登录验证错误");
		}
		return id;
	}

	private static long parseRequiredOperatorId(Map<String, Object> jwt) {
		Object v = jwt.get("operator_id");
		if (v == null) {
			throw new ResourceException("登录验证错误");
		}
		long id = parseLongLoose(v);
		if (id <= 0) {
			throw new ResourceException("登录验证错误");
		}
		return id;
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String urlOf(Map<String, Object> node) {
		Object u = node.get("url");
		return u == null ? null : u.toString();
	}
}
