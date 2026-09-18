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

package cn.shopex.ecshopx.openapi.thirdapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiHandlerForwardRegistry {

	private final Map<String, String> forwardPaths = new LinkedHashMap<>();

	public OpenapiHandlerForwardRegistry() {
		register("1.0", "POST", "ecx.product.sku_list", "/api/openapi/internal/v1/ecx.product.sku_list");
		register("1.0", "POST", "ecx.product.goods_list", "/api/openapi/internal/v1/ecx.product.goods_list");
		register("1.0", "POST", "ecx.product.stock_update", "/api/openapi/internal/v1/ecx.product.stock_update");
		register("1.0", "POST", "ecx.order.deliver", "/api/openapi/internal/v1/ecx.order.deliver");
		register("1.0", "POST", "ecx.order.list", "/api/openapi/internal/v1/ecx.order.list");
		register("1.0", "POST", "ecx.coupon.create", "/api/openapi/internal/v1/ecx.coupon.create");
		register("1.0", "POST", "ecx.coupon.verify", "/api/openapi/internal/v1/ecx.coupon.verify");
		register("1.0", "POST", "ecx.coupon.update", "/api/openapi/internal/v1/ecx.coupon.update");
		register("1.0", "POST", "ecx.coupon.list", "/api/openapi/internal/v1/ecx.coupon.list");
		register(
				"1.0",
				"POST",
				"ecx.discountcard.info",
				"/api/openapi/internal/v1/ecx.discountcard.info");
		register(
				"1.0",
				"POST",
				"ecx.discountcard.list",
				"/api/openapi/internal/v1/ecx.discountcard.list");
		register(
				"2.0",
				"POST",
				"ecx.discountcard.list",
				"/api/openapi/internal/v2/ecx.discountcard.list");
		register(
				"2.0",
				"POST",
				"ecx.discountcard.send",
				"/api/openapi/internal/v2/ecx.discountcard.send");
		register(
				"2.0",
				"POST",
				"ecx.userdiscount.list",
				"/api/openapi/internal/v2/ecx.userdiscount.list");
		register("1.0", "POST", "ecx.member.query", "/api/openapi/internal/v1/ecx.member.query");
		register("1.0", "POST", "ecx.member.create", "/api/openapi/internal/v1/ecx.member.create");
		register("2.0", "POST", "ecx.member.create", "/api/openapi/internal/v2/ecx.member.create");
		register(
				"2.0",
				"POST",
				"ecx.member.batch_create",
				"/api/openapi/internal/v2/ecx.member.batch_create");
		register("2.0", "GET", "ecx.member.list", "/api/openapi/internal/v2/ecx.member.list");
		register("2.0", "GET", "ecx.member.detail", "/api/openapi/internal/v2/ecx.member.detail");
		register(
				"2.0",
				"GET",
				"ecx.member_card_grade_rel.list",
				"/api/openapi/internal/v2/ecx.member_card_grade_rel.list");
		register(
				"2.0",
				"GET",
				"ecx.member_order.list",
				"/api/openapi/internal/v2/ecx.member_order.list");
		register(
				"2.0",
				"GET",
				"ecx.member_point_order.list",
				"/api/openapi/internal/v2/ecx.member_point_order.list");
		register(
				"2.0",
				"GET",
				"ecx.member_operate_log.list",
				"/api/openapi/internal/v2/ecx.member_operate_log.list");
		register(
				"2.0",
				"GET",
				"ecx.member_point_log.list",
				"/api/openapi/internal/v2/ecx.member_point_log.list");
		register(
				"2.0",
				"PATCH",
				"ecx.member_point.update",
				"/api/openapi/internal/v2/ecx.member_point.update");
		register(
				"2.0",
				"GET",
				"ecx.member_point.detail",
				"/api/openapi/internal/v2/ecx.member_point.detail");
		register(
				"2.0",
				"PATCH",
				"ecx.member_info.update",
				"/api/openapi/internal/v2/ecx.member_info.update");
		register(
				"2.0",
				"PATCH",
				"ecx.member_mobile.update",
				"/api/openapi/internal/v2/ecx.member_mobile.update");
		register(
				"2.0",
				"PATCH",
				"ecx.member_card_code_grade.update",
				"/api/openapi/internal/v2/ecx.member_card_code_grade.update");
		register(
				"2.0",
				"GET",
				"ecx.member_card.detail",
				"/api/openapi/internal/v2/ecx.member_card.detail");
		register(
				"2.0",
				"POST",
				"ecx.member_card.update",
				"/api/openapi/internal/v2/ecx.member_card.update");
		register(
				"2.0",
				"GET",
				"ecx.member_card_grade.list",
				"/api/openapi/internal/v2/ecx.member_card_grade.list");
		register(
				"2.0",
				"GET",
				"ecx.member_card_vip_grade.list",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade.list");
		register(
				"2.0",
				"GET",
				"ecx.member_card_vip_grade.detail",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade.detail");
		register(
				"2.0",
				"POST",
				"ecx.member_card_vip_grade.create",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade.create");
		register(
				"2.0",
				"PATCH",
				"ecx.member_card_vip_grade.update",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade.update");
		register(
				"2.0",
				"DELETE",
				"ecx.member_card_vip_grade.delete",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade.delete");
		register(
				"2.0",
				"GET",
				"ecx.member_card_vip_grade_order.list",
				"/api/openapi/internal/v2/ecx.member_card_vip_grade_order.list");
		register(
				"2.0",
				"GET",
				"ecx.member_card_grade.detail",
				"/api/openapi/internal/v2/ecx.member_card_grade.detail");
		register(
				"2.0",
				"POST",
				"ecx.member_card_grade.create",
				"/api/openapi/internal/v2/ecx.member_card_grade.create");
		register(
				"2.0",
				"PATCH",
				"ecx.member_card_grade.update",
				"/api/openapi/internal/v2/ecx.member_card_grade.update");
		register(
				"2.0",
				"DELETE",
				"ecx.member_card_grade.delete",
				"/api/openapi/internal/v2/ecx.member_card_grade.delete");
		register(
				"2.0",
				"POST",
				"ecx.card_grade.batch_save",
				"/api/openapi/internal/v2/ecx.card_grade.batch_save");
		register(
				"2.0",
				"POST",
				"ecx.member.card_grade.update",
				"/api/openapi/internal/v2/ecx.member.card_grade.update");
		register("1.0", "POST", "ecx.member.basicInfo", "/api/openapi/internal/v1/ecx.member.basicInfo");
		register("1.0", "POST", "ecx.member.orderList", "/api/openapi/internal/v1/ecx.member.orderList");
		register(
				"1.0",
				"POST",
				"ecx.member.browserHistory",
				"/api/openapi/internal/v1/ecx.member.browserHistory");
		register(
				"1.0",
				"POST",
				"ecx.member.browserHistoryList",
				"/api/openapi/internal/v1/ecx.member.browserHistoryList");
		register("1.0", "POST", "ecx.member.list", "/api/openapi/internal/v1/ecx.member.list");
		register("1.0", "POST", "ecx.member.listFp", "/api/openapi/internal/v1/ecx.member.listFp");
		register(
				"1.0",
				"POST",
				"ecx.member.assignMemberToSalesperson",
				"/api/openapi/internal/v1/ecx.member.assignMemberToSalesperson");
		register(
				"1.0",
				"POST",
				"ecx.member.notifyBecomeFriend",
				"/api/openapi/internal/v1/ecx.member.notifyBecomeFriend");
		register(
				"1.0",
				"POST",
				"ecx.member.frequentItems",
				"/api/openapi/internal/v1/ecx.member.frequentItems");
		register(
				"1.0",
				"POST",
				"ecx.member.cardGrades",
				"/api/openapi/internal/v1/ecx.member.cardGrades");
		register(
				"1.0",
				"POST",
				"ecx.member.tagLibrary",
				"/api/openapi/internal/v1/ecx.member.tagLibrary");
		register(
				"1.0",
				"POST",
				"ecx.member.tagLibrary.push",
				"/api/openapi/internal/v1/ecx.member.tagLibrary.push");
		register(
				"1.0",
				"POST",
				"ecx.member.tag.relation.push",
				"/api/openapi/internal/v1/ecx.member.tag.relation.push");
		register("2.0", "POST", "ecx.order.deliver", "/api/openapi/internal/v2/ecx.order.deliver");
		register("2.0", "GET", "ecx.order.deliver.get", "/api/openapi/internal/v2/ecx.order.deliver.get");
		register("2.0", "POST", "ecx.order.writeoff", "/api/openapi/internal/v2/ecx.order.writeoff");
		register("2.0", "POST", "ecx.order.list", "/api/openapi/internal/v2/ecx.order.list");
		register("2.0", "GET", "ecx.orders.incr.get", "/api/openapi/internal/v2/ecx.orders.incr.get");
		register("2.0", "GET", "ecx.orders.sold.get", "/api/openapi/internal/v2/ecx.orders.sold.get");
		register("2.0", "GET", "ecx.order.get", "/api/openapi/internal/v2/ecx.order.get");
		register(
				"2.0",
				"GET",
				"ecx.order.cancel.reasons.get",
				"/api/openapi/internal/v2/ecx.order.cancel.reasons.get");
		register("2.0", "POST", "ecx.order.cancel", "/api/openapi/internal/v2/ecx.order.cancel");
		register(
				"2.0",
				"POST",
				"ecx.order.cancel.confirm",
				"/api/openapi/internal/v2/ecx.order.cancel.confirm");
		register(
				"2.0",
				"GET",
				"ecx.logistics.enabled.get",
				"/api/openapi/internal/v2/ecx.logistics.enabled.get");
		register(
				"2.0",
				"GET",
				"ecx.shipping.templates.get",
				"/api/openapi/internal/v2/ecx.shipping.templates.get");
		register(
				"2.0",
				"GET",
				"ecx.trades.get",
				"/api/openapi/internal/v2/ecx.trades.get");
		register(
				"2.0",
				"GET",
				"ecx.aftersales.get",
				"/api/openapi/internal/v2/ecx.aftersales.get");
		register(
				"2.0",
				"GET",
				"ecx.aftersales.incr.get",
				"/api/openapi/internal/v2/ecx.aftersales.incr.get");
		register(
				"2.0",
				"POST",
				"ecx.aftersales.detail.get",
				"/api/openapi/internal/v2/ecx.aftersales.detail.get");
		register(
				"2.0",
				"GET",
				"ecx.refund.get",
				"/api/openapi/internal/v2/ecx.refund.get");
		register(
				"2.0",
				"GET",
				"ecx.refund.incr.get",
				"/api/openapi/internal/v2/ecx.refund.incr.get");
		register(
				"2.0",
				"POST",
				"ecx.refund.detail.get",
				"/api/openapi/internal/v2/ecx.refund.detail.get");
		register(
				"2.0",
				"POST",
				"ecx.member.tagcategory.add",
				"/api/openapi/internal/v2/ecx.member.tagcategory.add");
		register(
				"2.0",
				"DELETE",
				"ecx.member.tagcategory.delete",
				"/api/openapi/internal/v2/ecx.member.tagcategory.delete");
		register(
				"2.0",
				"POST",
				"ecx.member.tagcategory.update",
				"/api/openapi/internal/v2/ecx.member.tagcategory.update");
		register(
				"2.0",
				"GET",
				"ecx.member.tagcategorys.get",
				"/api/openapi/internal/v2/ecx.member.tagcategorys.get");
		register(
				"2.0",
				"POST",
				"ecx.member.tag.add",
				"/api/openapi/internal/v2/ecx.member.tag.add");
		register(
				"2.0",
				"DELETE",
				"ecx.member.tag.delete",
				"/api/openapi/internal/v2/ecx.member.tag.delete");
		register(
				"2.0",
				"POST",
				"ecx.member.tag.update",
				"/api/openapi/internal/v2/ecx.member.tag.update");
		register(
				"2.0",
				"GET",
				"ecx.member.tags.get",
				"/api/openapi/internal/v2/ecx.member.tags.get");
		register(
				"2.0",
				"POST",
				"ecx.member.tagging.batch.cover",
				"/api/openapi/internal/v2/ecx.member.tagging.batch.cover");
		register(
				"2.0",
				"POST",
				"ecx.member.tagging.batch.update",
				"/api/openapi/internal/v2/ecx.member.tagging.batch.update");
		register(
				"2.0",
				"DELETE",
				"ecx.member.tagged.delete",
				"/api/openapi/internal/v2/ecx.member.tagged.delete");
		register(
				"2.0",
				"GET",
				"ecx.member.tagged.get",
				"/api/openapi/internal/v2/ecx.member.tagged.get");
		register(
				"2.0",
				"GET",
				"ecx.tag.members.get",
				"/api/openapi/internal/v2/ecx.tag.members.get");
		register(
				"2.0",
				"POST",
				"ecx.member.rechargerule.add",
				"/api/openapi/internal/v2/ecx.member.rechargerule.add");
		register(
				"2.0",
				"DELETE",
				"ecx.member.rechargerule.delete",
				"/api/openapi/internal/v2/ecx.member.rechargerule.delete");
		register(
				"2.0",
				"POST",
				"ecx.member.rechargerule.update",
				"/api/openapi/internal/v2/ecx.member.rechargerule.update");
		register(
				"2.0",
				"GET",
				"ecx.member.rechargerule.get",
				"/api/openapi/internal/v2/ecx.member.rechargerule.get");
		register(
				"2.0",
				"GET",
				"ecx.member.recharge.trade.get",
				"/api/openapi/internal/v2/ecx.member.recharge.trade.get");
		register(
				"2.0",
				"POST",
				"ecx.product.sku_list",
				"/api/openapi/internal/v2/ecx.product.sku_list");
		register(
				"2.0",
				"POST",
				"ecx.product.goods_list",
				"/api/openapi/internal/v2/ecx.product.goods_list");
		register(
				"2.0",
				"POST",
				"ecx.product.stock_update",
				"/api/openapi/internal/v2/ecx.product.stock_update");
		register(
				"2.0",
				"GET",
				"ecx.items.entity.get",
				"/api/openapi/internal/v2/ecx.items.entity.get");
		register(
				"2.0",
				"GET",
				"ecx.item.entity.get",
				"/api/openapi/internal/v2/ecx.item.entity.get");
		register(
				"2.0",
				"POST",
				"ecx.item.entity.status.update",
				"/api/openapi/internal/v2/ecx.item.entity.status.update");
		register(
				"2.0",
				"DELETE",
				"ecx.item.entity.delete",
				"/api/openapi/internal/v2/ecx.item.entity.delete");
		register(
				"2.0",
				"POST",
				"ecx.item.entity.add",
				"/api/openapi/internal/v2/ecx.item.entity.add");
		register(
				"2.0",
				"POST",
				"ecx.item.entity.update",
				"/api/openapi/internal/v2/ecx.item.entity.update");
		register(
				"2.0",
				"POST",
				"ecx.item.brand.add",
				"/api/openapi/internal/v2/ecx.item.brand.add");
		register(
				"2.0",
				"DELETE",
				"ecx.item.brand.delete",
				"/api/openapi/internal/v2/ecx.item.brand.delete");
		register(
				"2.0",
				"POST",
				"ecx.item.brand.update",
				"/api/openapi/internal/v2/ecx.item.brand.update");
		register(
				"2.0",
				"GET",
				"ecx.item.brand.get",
				"/api/openapi/internal/v2/ecx.item.brand.get");
		register(
				"2.0",
				"POST",
				"ecx.item.category.add",
				"/api/openapi/internal/v2/ecx.item.category.add");
		register(
				"2.0",
				"DELETE",
				"ecx.item.category.delete",
				"/api/openapi/internal/v2/ecx.item.category.delete");
		register(
				"2.0",
				"POST",
				"ecx.item.category.update",
				"/api/openapi/internal/v2/ecx.item.category.update");
		register(
				"2.0",
				"GET",
				"ecx.item.category.get",
				"/api/openapi/internal/v2/ecx.item.category.get");
		register(
				"2.0",
				"GET",
				"ecx.item.maincategory.get",
				"/api/openapi/internal/v2/ecx.item.maincategory.get");
		register(
				"2.0",
				"GET",
				"ecx.item.maincategory.detail.get",
				"/api/openapi/internal/v2/ecx.item.maincategory.detail.get");
		register(
				"2.0",
				"PUT",
				"ecx.item.store.sync",
				"/api/openapi/internal/v2/ecx.item.store.sync");
		register(
				"2.0",
				"PUT",
				"ecx.item.price.sync",
				"/api/openapi/internal/v2/ecx.item.price.sync");
		register(
				"2.0",
				"GET",
				"ecx.item.price.get",
				"/api/openapi/internal/v2/ecx.item.price.get");
		register(
				"2.0",
				"PUT",
				"ecx.item.store.update",
				"/api/openapi/internal/v2/ecx.item.store.update");
		register(
				"2.0",
				"GET",
				"ecx.item.store.get",
				"/api/openapi/internal/v2/ecx.item.store.get");
		register("1.0", "POST", "ecx.salesperson.push", "/api/openapi/internal/v1/ecx.salesperson.push");
		register(
				"1.0",
				"POST",
				"ecx.salesperson.bathStatusUpdate",
				"/api/openapi/internal/v1/ecx.salesperson.bathStatusUpdate");
		register(
				"1.0",
				"POST",
				"ecx.salesperson.destroy",
				"/api/openapi/internal/v1/ecx.salesperson.destroy");
		register(
				"1.0",
				"POST",
				"ecx.salesperson.updateStores",
				"/api/openapi/internal/v1/ecx.salesperson.updateStores");
		register(
				"1.0",
				"POST",
				"ecx.image.upload_token",
				"/api/openapi/internal/v1/ecx.image.upload_token");
		register(
				"1.0",
				"POST",
				"ecx.image.upload_localimage",
				"/api/openapi/internal/v1/ecx.image.upload_localimage");
		register(
				"1.0",
				"POST",
				"ecx.image.list",
				"/api/openapi/internal/v1/ecx.image.list");
		register(
				"1.0",
				"POST",
				"ecx.image.save",
				"/api/openapi/internal/v1/ecx.image.save");
		register(
				"1.0",
				"POST",
				"ecx.image.del",
				"/api/openapi/internal/v1/ecx.image.del");
		register(
				"1.0",
				"POST",
				"ecx.wxapp.qrcode",
				"/api/openapi/internal/v1/ecx.wxapp.qrcode");
		register(
				"1.0",
				"POST",
				"ecx.wxapp.shoplist",
				"/api/openapi/internal/v1/ecx.wxapp.shoplist");
		register(
				"1.0",
				"POST",
				"ecx.distributor.detail",
				"/api/openapi/internal/v1/ecx.distributor.detail");
		register(
				"2.0",
				"GET",
				"ecx.distributor.list",
				"/api/openapi/internal/v2/ecx.distributor.list");
		register(
				"2.0",
				"POST",
				"ecx.distributor.create",
				"/api/openapi/internal/v2/ecx.distributor.create");
		register(
				"2.0",
				"PATCH",
				"ecx.distributor.update",
				"/api/openapi/internal/v2/ecx.distributor.update");
		register(
				"2.0",
				"GET",
				"ecx.distributor.detail",
				"/api/openapi/internal/v2/ecx.distributor.detail");
		register(
				"2.0",
				"GET",
				"ecx.distributor.download",
				"/api/openapi/internal/v2/ecx.distributor.download");
		register(
				"2.0",
				"GET",
				"ecx.distributor_item.list",
				"/api/openapi/internal/v2/ecx.distributor_item.list");
		register(
				"2.0",
				"PATCH",
				"ecx.distributor_item.update",
				"/api/openapi/internal/v2/ecx.distributor_item.update");
		register(
				"2.0",
				"GET",
				"ecx.distributor_item.download",
				"/api/openapi/internal/v2/ecx.distributor_item.download");
		register(
				"1.0",
				"POST",
				"ecx.weappid.get",
				"/api/openapi/internal/v1/ecx.weappid.get");
		register(
				"1.0",
				"POST",
				"ecx.jurisdiction.role",
				"/api/openapi/internal/v1/ecx.jurisdiction.role");
		register(
				"1.0",
				"POST",
				"ecx.jurisdiction.sysuser",
				"/api/openapi/internal/v1/ecx.jurisdiction.sysuser");
		register(
				"1.0",
				"POST",
				"ecx.jurisdiction.getuser",
				"/api/openapi/internal/v1/ecx.jurisdiction.getuser");
		register(
				"1.0",
				"POST",
				"exc.operator.resetpwd",
				"/api/openapi/internal/v1/exc.operator.resetpwd");
		register(
				"1.0",
				"POST",
				"ecx.company.info",
				"/api/openapi/internal/v1/ecx.company.info");
	}

	public void register(String version, String httpVerb, String method, String internalPath) {
		forwardPaths.put(routeKey(version, httpVerb, method), internalPath);
	}

	public Optional<String> resolveForwardPath(String version, String httpVerb, String method) {
		return Optional.ofNullable(forwardPaths.get(routeKey(version, httpVerb, method)));
	}

	public boolean isImplemented(String version, String httpVerb, String method) {
		return forwardPaths.containsKey(routeKey(version, httpVerb, method));
	}

	private static String routeKey(String version, String httpVerb, String method) {
		return OpenapiRequestParamCollector.normalizeVersion(version)
				+ " "
				+ httpVerb.toUpperCase()
				+ " "
				+ method.trim();
	}
}
