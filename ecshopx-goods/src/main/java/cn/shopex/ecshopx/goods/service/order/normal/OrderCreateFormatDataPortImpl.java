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

package cn.shopex.ecshopx.goods.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.OrderTypeSlugMapper;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.deposit.service.UserDepositBalanceReadService;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendCheckoutAddService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.kaquan.service.order.normal.NormalOrderCheckoutCouponFacade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateFormatDataPortImpl implements OrderCreateFormatDataPort {

	private static final String MSG_INSUFFICIENT_BALANCE = "goods.order.insufficient_balance";
	private static final String DEFAULT_INSUFFICIENT_BALANCE = "余额不足";
	private static final String MSG_CART_ITEMS_CHANGED = "goods.order.cart_items_changed";
	private static final String DEFAULT_CART_ITEMS_CHANGED = "购物车商品已变更";

	private final OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort;
	private final NormalOrderCheckoutCouponFacade normalOrderCheckoutCouponFacade;
	private final UserDepositBalanceReadService userDepositBalanceReadService;
	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final OrderCheckoutFullGiftService orderCheckoutFullGiftService;

	private final OrderCheckoutPlusBuyService orderCheckoutPlusBuyService;
	private final MessageSource messageSource;

	public OrderCreateFormatDataPortImpl(
			OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort,
			NormalOrderCheckoutCouponFacade normalOrderCheckoutCouponFacade,
			UserDepositBalanceReadService userDepositBalanceReadService,
			WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			OrderCheckoutFullGiftService orderCheckoutFullGiftService,
			OrderCheckoutPlusBuyService orderCheckoutPlusBuyService,
			MessageSource messageSource) {
		this.orderDirectedCrowdDiscountPort = orderDirectedCrowdDiscountPort;
		this.normalOrderCheckoutCouponFacade = normalOrderCheckoutCouponFacade;
		this.userDepositBalanceReadService = userDepositBalanceReadService;
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.orderCheckoutFullGiftService = orderCheckoutFullGiftService;
		this.orderCheckoutPlusBuyService = orderCheckoutPlusBuyService;
		this.messageSource = messageSource;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void format(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		Map<String, Object> od = p.getOrderData();
		od.clear();
		long companyId = longVal(pr.get("company_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		long distributorId = longVal(pr.get("distributor_id"), 0L);
		od.put("company_id", companyId);
		od.put("user_id", userId);
		od.put("mobile", stringVal(pr.get("mobile")));
		od.put("distributor_id", distributorId);
		// PHP OrderService::_formatOrderData: shop_id from shopInfo (0 when isCheckShopValid=false)
		od.put("shop_id", 0L);
		String slug = stringVal(pr.get("order_type"));
		OrderTypeSlugMapper.Resolved typed = OrderTypeSlugMapper.resolve(slug);
		// wxapp 在线普通单的"购物车驱动"分支：跳过收件方式回填、不覆盖 promotion / operator_id
		boolean wxappOnlineNormal =
				Boolean.TRUE.equals(pr.get("is_online_order"))
						&& "normal".equals(typed.orderType())
						&& "normal".equals(typed.orderClass());
		boolean wxappOnlineGroups =
				Boolean.TRUE.equals(pr.get("is_online_order"))
						&& "normal".equals(typed.orderType())
						&& "groups".equals(typed.orderClass());
		boolean wxappOnlineEmployeePurchase =
				Boolean.TRUE.equals(pr.get("is_online_order"))
						&& "normal".equals(typed.orderType())
						&& "employee_purchase".equals(typed.orderClass());
		boolean pointsmallOrder = "pointsmall".equals(typed.orderClass());
		String receiptTypeParam = stringVal(pr.get("receipt_type"));
		if (!(wxappOnlineNormal && "logistics".equalsIgnoreCase(receiptTypeParam.trim()))) {
			od.put("receipt_type", receiptTypeParam);
		}
		if ("ziti".equals(receiptTypeParam.trim())) {
			od.put("ziti_code", 100000L + ThreadLocalRandom.current().nextLong(900000L));
			od.put("ziti_status", "PENDING");
		}
		od.put("order_type", typed.orderType());
		od.put("order_class", typed.orderClass());
		od.put("order_type_slug", slug);
		od.put("order_source", stringVal(pr.get("order_source")));
		od.put("pay_type", stringVal(pr.get("pay_type")));
		od.put("pay_channel", stringVal(pr.get("pay_channel")));
		if (!wxappOnlineNormal) {
			od.put("promotion", stringVal(pr.get("promotion")));
		}
		if (!wxappOnlineNormal) {
			od.put("operator_id", longVal(pr.get("operator_id"), 0L));
		}
		od.put("salesman_id", 0L);
		od.put("freight_fee", 0);
		od.put("point_fee", 0);
		Object pointUseRaw = pr.get("point_use");
		if (pointUseRaw == null) {
			od.put("point_use", 0);
		} else if (!StringUtils.hasText(stringVal(pointUseRaw))) {
			od.put("point_use", "");
		} else {
			od.put("point_use", intFromPointUse(pointUseRaw));
		}
		od.put("remark", stringVal(pr.get("remark")));
		od.put("authorizer_appid", stringVal(pr.get("authorizer_appid")));
		od.put("wxa_appid", stringVal(pr.get("wxa_appid")));
		od.put("discount_info", new ArrayList<>());
		od.put("items_promotion", List.of());
		od.put("discount_fee", 0);
		od.put("member_discount", 0);
		od.put("goods_discount", 0);
		if (wxappOnlineNormal || wxappOnlineEmployeePurchase || (Boolean.TRUE.equals(pr.get("is_online_order")) && pointsmallOrder)) {
			od.put("order_holder", "self");
		}
		Map<Long, Map<String, Object>> cartByItem = new LinkedHashMap<>();
		Object meta = pr.get("_checkout_cart_meta");
		if (meta instanceof Map<?, ?> metaMap) {
			Object listObj = metaMap.get("list");
			if (listObj instanceof List<?> lst) {
				for (Object row : lst) {
					if (row instanceof Map<?, ?> rm) {
						long iid = longVal(rm.get("item_id"), 0L);
						if (iid > 0L) {
							@SuppressWarnings("unchecked")
							Map<String, Object> m = (Map<String, Object>) rm;
							cartByItem.put(iid, m);
						}
					}
				}
			}
			int metaDiscount = intVal(metaMap.get("discount_fee"), 0);
			if (metaDiscount > 0) {
				od.put("discount_fee", metaDiscount);
				od.put("goods_discount", metaDiscount);
			}
			int metaMemberDiscount = intVal(metaMap.get("member_discount"), 0);
			if (metaMemberDiscount > 0) {
				od.put("member_discount", metaMemberDiscount);
			}
		}
		Object storeQtyAdj = pr.get("store_quantity_adjustments");
		if (storeQtyAdj instanceof List<?> adjList && !adjList.isEmpty()) {
			od.put("store_quantity_adjustments", storeQtyAdj);
		}
		Object storeQtyTip = pr.get("store_quantity_adjust_tip");
		if (storeQtyTip != null && !storeQtyTip.toString().isBlank()) {
			od.put("store_quantity_adjust_tip", storeQtyTip.toString());
		}
		Object itemsRaw = pr.get("items");
		List<Map<String, Object>> orderItems = new ArrayList<>();
		BigDecimal sumItemFeeBd = BigDecimal.ZERO;
		long sumItemPoint = 0L;
		if (itemsRaw instanceof List<?> rawList) {
			for (Object o : rawList) {
				if (!(o instanceof Map<?, ?> pm)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> spec = (Map<String, Object>) pm;
				long itemId = longVal(spec.get("item_id"), 0L);
				int num = (int) Math.min(longVal(spec.get("num"), 1L), Integer.MAX_VALUE);
				Map<String, Object> cartRow = cartByItem.getOrDefault(itemId, Map.of());
				long cartActivityId = longVal(cartRow.get("activity_id"), 0L);
				Object activityId =
						cartActivityId > 0L
								? cartActivityId
								: (spec.get("activity_id") != null ? spec.get("activity_id") : 0L);
				String cartActivityType = stringVal(cartRow.get("activity_type"));
				String activityType =
						StringUtils.hasText(cartActivityType) && !"normal".equals(cartActivityType)
								? cartActivityType
								: (spec.get("activity_type") != null
										? stringVal(spec.get("activity_type"))
										: "normal");
				boolean isPackage = "package".equals(activityType);
				List<Long> childItemIds = parseChildItemIds(spec.get("items_id"));

				Map<String, Object> mainLine =
						buildOrderLineSkeleton(
								itemId, num, companyId, userId, distributorId, cartRow, activityId, activityType);
				if (isPackage && !childItemIds.isEmpty()) {
					int mainPrice = intVal(cartRow.get("price"), 0);
					long mainLineTotal = (long) mainPrice * num;
					applyLineAmounts(mainLine, mainPrice, mainLineTotal, 0);
					mainLine.put("order_item_type", "package");
					mainLine.put("act_id", activityId);
					orderItems.add(mainLine);
					sumItemFeeBd = sumItemFeeBd.add(BigDecimal.valueOf(mainLineTotal));

					for (long childId : childItemIds) {
						Map<String, Object> pkgChild = findPackageChildRow(cartRow, childId);
						int childPrice = intVal(pkgChild.get("price"), 0);
						long childCartPay = longVal(pkgChild.get("total_fee"), -1L);
						long childLineTotal =
								childCartPay >= 0L ? childCartPay : (long) childPrice * num;
						Map<String, Object> childLine =
								buildOrderLineSkeleton(
										childId,
										num,
										companyId,
										userId,
										distributorId,
										pkgChild.isEmpty() ? cartRow : pkgChild,
										activityId,
										activityType);
						if (!pkgChild.isEmpty()) {
							childLine.put("item_name", stringVal(pkgChild.get("item_name")));
							childLine.put("pic", stringVal(pkgChild.get("pics")));
							if (pkgChild.get("item_spec_desc") != null) {
								childLine.put("item_spec_desc", pkgChild.get("item_spec_desc"));
							}
						}
						applyLineAmounts(childLine, childPrice, childLineTotal, 0);
						childLine.put("order_item_type", "package");
						childLine.put("act_id", activityId);
						orderItems.add(childLine);
						sumItemFeeBd = sumItemFeeBd.add(BigDecimal.valueOf(childLineTotal));
					}
				} else {
					BigDecimal priceBd = BigDecimal.valueOf(longVal(cartRow.get("price"), 0L));
					BigDecimal lineTotalBd =
							priceBd.multiply(BigDecimal.valueOf(num)).setScale(0, RoundingMode.DOWN);
					sumItemFeeBd = sumItemFeeBd.add(lineTotalBd);
					long lineTotal = lineTotalBd.longValue();
					int lineDiscount = intVal(cartRow.get("discount_fee"), 0);
					long cartLinePay = longVal(cartRow.get("total_fee"), -1L);
					int linePayFen =
							cartLinePay >= 0L
									? (int) Math.min(cartLinePay, Integer.MAX_VALUE)
									: (int) Math.min(Math.max(0L, lineTotal - lineDiscount), Integer.MAX_VALUE);
					applyLineAmounts(
							mainLine,
							(int) Math.min(priceBd.longValue(), Integer.MAX_VALUE),
							linePayFen,
							lineDiscount);
					mainLine.put("order_item_type", isPackage ? "package" : "normal");
					if (isPackage) {
						mainLine.put("act_id", activityId);
					}
					if (pointsmallOrder) {
						int itemPoint = intVal(cartRow.get("point"), 0);
						mainLine.put("item_point", itemPoint);
						mainLine.put("point", itemPoint * num);
						sumItemPoint += (long) itemPoint * num;
						copyPointsmallCartFieldsOntoLine(mainLine, cartRow);
					}
					orderItems.add(mainLine);
				}
			}
		}
		od.put("items", orderItems);
		long sumItemFee = sumItemFeeBd.setScale(0, RoundingMode.DOWN).longValue();
		if (pointsmallOrder) {
			od.put("item_fee", String.valueOf(sumItemFee));
			od.put("total_fee", sumItemFee);
			od.put("market_fee", String.valueOf(sumItemFee));
			od.put("item_point", sumItemPoint);
			od.put("point", sumItemPoint);
		} else {
			od.put("item_fee", String.valueOf(sumItemFee));
			od.put("total_fee", sumItemFee);
			od.put("market_fee", String.valueOf(sumItemFee));
		}
		String title = orderItems.isEmpty() ? "" : stringVal(orderItems.get(0).get("item_name"));
		if (!wxappOnlineNormal && !wxappOnlineEmployeePurchase && orderItems.size() > 1) {
			title = title + "等" + orderItems.size() + "件商品";
		}
		od.put("title", title);
		if (wxappOnlineNormal || wxappOnlineGroups) {
			mergeSkuFromQueryForOnlineWxappNormal(p, companyId, userId, distributorId);
			applyPackageChildSalePriceFromMainLine(p.getOrderData());
			if (wxappOnlineGroups) {
				Object mergedItems = od.get("items");
				if (mergedItems instanceof List<?> mergedList && !mergedList.isEmpty()) {
					Object first = mergedList.get(0);
					if (first instanceof Map<?, ?> firstLine) {
						String mergedTitle = stringVal(firstLine.get("item_name"));
						if (StringUtils.hasText(mergedTitle)) {
							od.put("title", mergedTitle);
						}
					}
				}
			}
		} else if (wxappOnlineEmployeePurchase) {
			mergeSkuFromQueryForOnlineWxappNormal(p, companyId, userId, distributorId);
			applyPackageChildSalePriceFromMainLine(p.getOrderData());
			Object mergedItems = od.get("items");
			if (mergedItems instanceof List<?> mergedList && !mergedList.isEmpty()) {
				Object first = mergedList.get(0);
				if (first instanceof Map<?, ?> firstLine) {
					String mergedTitle = stringVal(firstLine.get("item_name"));
					if (StringUtils.hasText(mergedTitle)) {
						od.put("title", mergedTitle);
					}
				}
			}
		}
		if (wxappOnlineNormal && meta instanceof Map<?, ?> metaMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> checkoutMeta = (Map<String, Object>) metaMap;
			applyCheckoutCartPromotionMerge(p, checkoutMeta, cartByItem);
			orderCheckoutFullGiftService.applyGiftActivitiesFromCheckoutMeta(p, checkoutMeta);
			if (!"fastbuy".equals(stringVal(pr.get("cart_type")))) {
				orderCheckoutPlusBuyService.applyPlusBuyActivitiesFromCheckoutMeta(p, checkoutMeta);
			}
		}
		if (!wxappOnlineNormal) {
			normalOrderCheckoutCouponFacade.applyOptimalCouponAndSilentDeduction(p);
		}
		orderDirectedCrowdDiscountPort.applySetUserTotalDiscountIfNeeded(p);
		if (wxappOnlineNormal) {
			orderCheckoutFullGiftService.allocateFullGiftFees(p);
			applyOrderHolderFromItems(od);
		}
		if ("deposit".equals(stringVal(pr.get("pay_type")))) {
			long uid = longVal(pr.get("user_id"), 0L);
			if (uid > 0L) {
				long balance = userDepositBalanceReadService.getUserDepositTotal(companyId, uid);
				long needPay = longVal(od.get("total_fee"), 0L);
				if (balance < needPay) {
					throw new ResourceException(
							messageSource.getMessage(
									MSG_INSUFFICIENT_BALANCE,
									null,
									DEFAULT_INSUFFICIENT_BALANCE,
									resolveLocale()));
				}
			}
		}
	}

	@Override
	public void applyCheckoutCouponAfterFreight(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!Boolean.TRUE.equals(pr.get("is_online_order"))) {
			return;
		}
		if (!"normal".equals(stringVal(pr.get("order_type")).trim())) {
			return;
		}
		normalOrderCheckoutCouponFacade.applyOptimalCouponAndSilentDeduction(p);
	}

	@SuppressWarnings("unchecked")
	private void mergeSkuFromQueryForOnlineWxappNormal(
			NormalOrderCreateParams p, long companyId, long userId, long distributorId) {
		Map<String, Object> od = p.getOrderData();
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || itemList.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (Object o : itemList) {
			if (o instanceof Map<?, ?> row) {
				long iid = longVal(row.get("item_id"), 0L);
				if (iid > 0L) {
					ids.add(iid);
				}
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		q.put("user_id", userId);
		q.put("item_id", new ArrayList<>(ids));
		Map<String, Object> pack = wxappGoodsItemsListQueryOrchestrator.querySkuItemsList(companyId, q, List.of());
		Object listObj = pack.get("list");
		Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		if (listObj instanceof List<?> skuLines) {
			for (Object o : skuLines) {
				if (o instanceof Map<?, ?> sku) {
					long iid = longVal(sku.get("item_id"), 0L);
					if (iid > 0L) {
						byId.put(iid, (Map<String, Object>) sku);
					}
				}
			}
		}
		List<Map<String, Object>> merged = new ArrayList<>();
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> lineRaw)) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>((Map<String, Object>) lineRaw);
			long itemId = longVal(line.get("item_id"), 0L);
			Map<String, Object> sku = byId.get(itemId);
			if (sku == null) {
				merged.add(line);
				continue;
			}
			if (!StringUtils.hasText(stringVal(line.get("item_name")))) {
				line.put("item_name", stringVal(sku.get("item_name")));
			}
			if (!StringUtils.hasText(stringVal(line.get("item_bn")))) {
				line.put("item_bn", stringVal(sku.get("item_bn")));
			}
			if (!StringUtils.hasText(stringVal(line.get("goods_bn")))) {
				line.put("goods_bn", stringVal(sku.get("goods_bn")));
			}
			if (!StringUtils.hasText(stringVal(line.get("item_unit")))) {
				line.put("item_unit", stringVal(sku.get("item_unit")));
			}
			if (!StringUtils.hasText(stringVal(line.get("pic")))) {
				line.put("pic", firstPicFromSku(sku));
			}
			if (sku.get("templates_id") != null) {
				line.put("templates_id", sku.get("templates_id"));
			} else if (sku.get("template_id") != null) {
				line.put("templates_id", sku.get("template_id"));
			}
			if (line.get("item_spec_desc") == null || !StringUtils.hasText(stringVal(line.get("item_spec_desc")))) {
				Object spec = sku.get("item_spec_desc");
				if (spec != null && StringUtils.hasText(String.valueOf(spec))) {
					line.put("item_spec_desc", spec);
				}
			}
			if (sku.get("weight") != null) {
				line.put("weight", sku.get("weight"));
			}
			if (sku.get("volume") != null) {
				line.put("volume", sku.get("volume"));
			}
			if (!line.containsKey("volume") || line.get("volume") == null) {
				line.put("volume", 0);
			}
			if (!line.containsKey("weight") || line.get("weight") == null) {
				line.put("weight", 0);
			}
			if (sku.containsKey("market_price")) {
				line.put("market_price", sku.get("market_price"));
			} else {
				line.put("market_price", "0");
			}
			if (sku.containsKey("cost_price")) {
				line.put("cost_price", intVal(sku.get("cost_price"), intVal(line.get("cost_price"), 0)));
			} else if (!line.containsKey("cost_price")) {
				line.put("cost_price", 0);
			}
			if (sku.get("item_category") != null) {
				line.put("item_category", String.valueOf(sku.get("item_category")));
			}
			long goodsIdFromSku = longVal(sku.get("goods_id"), 0L);
			if (goodsIdFromSku > 0L) {
				line.put("goods_id", goodsIdFromSku);
			}
			long defaultItemFromSku = longVal(sku.get("default_item_id"), 0L);
			if (defaultItemFromSku > 0L) {
				line.put("default_item_id", defaultItemFromSku);
			} else {
				line.put("default_item_id", itemId);
			}
			long skuListPrice = longVal(sku.get("price"), 0L);
			if (skuListPrice > 0L) {
				line.put("sale_price", String.valueOf(skuListPrice));
			}
			long supplierId = longVal(sku.get("supplier_id"), 0L);
			line.put("supplier_id", (int) Math.min(supplierId, Integer.MAX_VALUE));
			merged.add(line);
		}
		od.put("items", merged);
	}

	@SuppressWarnings("unchecked")
	private void applyCheckoutCartPromotionMerge(
			NormalOrderCreateParams p,
			Map<String, Object> checkoutMeta,
			Map<Long, Map<String, Object>> cartByItem) {
		if (cartByItem.isEmpty()) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || itemList.isEmpty()) {
			return;
		}
		Set<Long> usedActivityIds = parseUsedActivityIds(checkoutMeta.get("used_activity_ids"));
		List<Map<String, Object>> itemsPromotion = new ArrayList<>();
		List<Map<String, Object>> joinActivityInfo = new ArrayList<>();
		for (Map<String, Object> cart : cartByItem.values()) {
			joinActivityInfo.addAll(copyActivityInfoList(cart.get("activity_info")));
		}
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> lineRaw)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) lineRaw;
			if ("package".equals(stringVal(line.get("activity_type")))) {
				continue;
			}
			long itemId = longVal(line.get("item_id"), 0L);
			Map<String, Object> cart = cartByItem.get(itemId);
			if (cart == null || cart.isEmpty()) {
				if (Boolean.TRUE.equals(
						p.getParams().get(GoodsRecommendCheckoutAddService.CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS))) {
					continue;
				}
				throw new ResourceException(
						messageSource.getMessage(
								MSG_CART_ITEMS_CHANGED, null, DEFAULT_CART_ITEMS_CHANGED, resolveLocale()));
			}
			List<Map<String, Object>> itemDiscountInfo = copyActivityInfoList(line.get("discount_info"));
			itemDiscountInfo.addAll(copyActivityInfoList(cart.get("activity_info")));
			line.put("discount_info", itemDiscountInfo);
			int cartDiscount = intVal(cart.get("discount_fee"), 0);
			line.put("discount_fee", cartDiscount);
			long cartPay = longVal(cart.get("total_fee"), -1L);
			if (cartPay >= 0L) {
				line.put("total_fee", (int) Math.min(cartPay, Integer.MAX_VALUE));
			} else {
				int currentPay = intVal(line.get("total_fee"), 0);
				line.put("total_fee", Math.max(0, currentPay - cartDiscount));
			}
			if (cart.get("activity_price") != null) {
				line.put("activity_price", intVal(cart.get("activity_price"), 0));
			}
			if (cart.get("member_price") != null) {
				line.put("member_price", intVal(cart.get("member_price"), 0));
			}
			if (cart.get("member_discount") != null) {
				line.put("member_discount", intVal(cart.get("member_discount"), 0));
			}
			long activityId = longVal(cart.get("activity_id"), 0L);
			if (activityId > 0L) {
				line.put("activity_id", activityId);
				String at = stringVal(cart.get("activity_type"));
				if (StringUtils.hasText(at)) {
					line.put("activity_type", at);
				}
			}
			if (activityId > 0L
					&& (usedActivityIds.isEmpty() || usedActivityIds.contains(activityId))) {
				Map<String, Object> activity = findPromotionOnCart(cart, activityId);
				if (activity == null) {
					activity = fallbackPromotionFromCartLine(cart, activityId);
				}
				if (activity != null) {
					itemsPromotion.add(buildItemsPromotionRow(line, activity, cart, itemId));
				}
			}
			Object limited = cart.get("limitedTimeSaleAct");
			if (limited instanceof Map<?, ?> actRaw) {
				itemsPromotion.add(buildItemsPromotionRow(line, (Map<String, Object>) actRaw, cart, itemId));
			}
			Object limitedBuy = cart.get("limitedBuy");
			if (limitedBuy instanceof Map<?, ?> actRaw) {
				itemsPromotion.add(buildItemsPromotionRow(line, (Map<String, Object>) actRaw, cart, itemId));
			}
		}
		int metaDiscount = intVal(checkoutMeta.get("discount_fee"), intVal(od.get("discount_fee"), 0));
		if (metaDiscount > 0) {
			od.put("discount_fee", metaDiscount);
			od.put("goods_discount", metaDiscount);
		}
		int metaMemberDiscount = intVal(checkoutMeta.get("member_discount"), intVal(od.get("member_discount"), 0));
		od.put("member_discount", metaMemberDiscount);
		long metaTotalFee = longVal(checkoutMeta.get("total_fee"), -1L);
		if (metaTotalFee >= 0L) {
			od.put("total_fee", metaTotalFee);
		}
		Map<String, Object> orderDiscountInfo = aggregateOrderDiscountInfo(joinActivityInfo);
		if (!orderDiscountInfo.isEmpty()) {
			od.put("discount_info", orderDiscountInfo);
		}
		if (!itemsPromotion.isEmpty()) {
			od.put("items_promotion", itemsPromotion);
		}
	}

	private static Map<String, Object> buildItemsPromotionRow(
			Map<String, Object> orderitem,
			Map<String, Object> activity,
			Map<String, Object> cart,
			long itemId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", orderitem.get("company_id"));
		row.put("user_id", orderitem.get("user_id"));
		row.put("shop_id", longVal(orderitem.get("distributor_id"), 0L));
		row.put("item_id", itemId);
		row.put("item_name", orderitem.get("item_name"));
		row.put("item_type", "normal");
		row.put("order_type", "normal");
		long activityId = longVal(activity.get("activity_id"), longVal(activity.get("marketing_id"), 0L));
		row.put("activity_id", activityId);
		String activityType = stringVal(activity.get("activity_type"));
		if (!StringUtils.hasText(activityType)) {
			activityType = stringVal(activity.get("marketing_type"));
		}
		row.put("activity_type", activityType);
		row.put("activity_name", stringVal(activity.get("activity_name"), stringVal(activity.get("marketing_name"))));
		row.put("activity_tag", stringVal(activity.get("activity_tag"), stringVal(activity.get("promotion_tag"))));
		row.put("activity_desc", copyActivityInfoList(cart.get("activity_info")));
		row.put("activity_rule", activity.get("rule") != null ? activity.get("rule") : List.of());
		return row;
	}

	private static Map<String, Object> fallbackPromotionFromCartLine(Map<String, Object> cart, long activityId) {
		String at = stringVal(cart.get("activity_type"));
		if (!StringUtils.hasText(at) || "normal".equals(at)) {
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("activity_id", activityId);
		out.put("marketing_id", activityId);
		out.put("marketing_type", at);
		out.put("activity_type", at);
		List<Map<String, Object>> activityInfo = copyActivityInfoList(cart.get("activity_info"));
		String marketingName = "";
		String promotionTag = "";
		if (!activityInfo.isEmpty()) {
			marketingName = stringVal(activityInfo.get(0).get("info"));
			promotionTag = stringVal(activityInfo.get(0).get("rule"));
		}
		out.put("marketing_name", marketingName);
		out.put("promotion_tag", promotionTag);
		return out;
	}

	private static Map<String, Object> findPromotionOnCart(Map<String, Object> cart, long activityId) {
		Object raw = cart.get("promotions");
		if (!(raw instanceof List<?> list)) {
			return null;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> promo = (Map<String, Object>) m;
			if (longVal(promo.get("marketing_id"), 0L) == activityId) {
				Map<String, Object> out = new LinkedHashMap<>(promo);
				out.put("activity_id", activityId);
				return out;
			}
		}
		return null;
	}

	private static Map<String, Object> aggregateOrderDiscountInfo(List<Map<String, Object>> joinActivityInfo) {
		Map<String, Map<String, Object>> disInfo = new LinkedHashMap<>();
		Map<String, Long> nds = new LinkedHashMap<>();
		for (Map<String, Object> desc : joinActivityInfo) {
			String type = stringVal(desc.get("type"));
			String id = stringVal(desc.get("id"));
			String key = type + id;
			disInfo.putIfAbsent(key, new LinkedHashMap<>(desc));
			if (Set.of("member_price", "limited_time_sale", "full_minus", "full_discount").contains(type)) {
				nds.merge(key, longVal(desc.get("discount_fee"), 0L), Long::sum);
			}
		}
		for (Map.Entry<String, Long> e : nds.entrySet()) {
			Map<String, Object> row = disInfo.get(e.getKey());
			if (row != null) {
				row.put("discount_fee", e.getValue());
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, Object>> e : disInfo.entrySet()) {
			Map<String, Object> value = e.getValue();
			Object fee = value.get("discount_fee");
			if (fee instanceof Number n) {
				value.put("discount_fee", String.valueOf(n.longValue()));
			}
			out.put(e.getKey(), value);
		}
		return out;
	}

	private static Set<Long> parseUsedActivityIds(Object raw) {
		Set<Long> ids = new LinkedHashSet<>();
		if (raw instanceof Collection<?> coll) {
			for (Object el : coll) {
				long id = longVal(el, 0L);
				if (id > 0L) {
					ids.add(id);
				}
			}
		}
		return ids;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> copyActivityInfoList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof Map<?, ?> m) {
				out.add(new LinkedHashMap<>((Map<String, Object>) m));
			}
		}
		return out;
	}

	private static String stringVal(Object primary, String fallback) {
		String s = stringVal(primary);
		return StringUtils.hasText(s) ? s : fallback;
	}

	private static String firstPicFromSku(Map<String, Object> sku) {
		Object pics = sku.get("pics");
		if (pics == null) {
			return "";
		}
		if (pics instanceof String s) {
			return s.trim();
		}
		if (pics instanceof List<?> lst && !lst.isEmpty()) {
			Object first = lst.get(0);
			return first == null ? "" : String.valueOf(first).trim();
		}
		return String.valueOf(pics).trim();
	}

	private static int intFromPointUse(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static void applyOrderHolderFromItems(Map<String, Object> od) {
		String current = stringVal(od.get("order_holder"));
		if (!StringUtils.hasText(current)) {
			current = "self";
		}
		if (!"self".equals(current)) {
			return;
		}
		boolean hasSupplier = false;
		boolean hasSelf = false;
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			od.put("order_holder", "self");
			return;
		}
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			if (longVal(row.get("supplier_id"), 0L) > 0L) {
				hasSupplier = true;
			} else {
				hasSelf = true;
			}
		}
		if (!hasSupplier) {
			od.put("order_holder", "self");
			return;
		}
		od.put("order_holder", hasSelf ? "self_supplier" : "supplier");
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString();
	}

	private static void copyPointsmallCartFieldsOntoLine(Map<String, Object> line, Map<String, Object> cartRow) {
		if (cartRow.get("templates_id") != null) {
			line.put("templates_id", cartRow.get("templates_id"));
		}
		if (cartRow.get("default_item_id") != null) {
			line.put("default_item_id", cartRow.get("default_item_id"));
		}
		if (cartRow.get("item_bn") != null) {
			line.put("item_bn", cartRow.get("item_bn"));
		}
		if (cartRow.get("item_category") != null) {
			line.put("item_category", cartRow.get("item_category"));
		}
		if (cartRow.get("is_total_store") != null) {
			line.put("is_total_store", cartRow.get("is_total_store"));
		}
		if (cartRow.get("weight") != null) {
			line.put("weight", cartRow.get("weight"));
		}
	}

	private static Map<String, Object> buildOrderLineSkeleton(
			long itemId,
			int num,
			long companyId,
			long userId,
			long distributorId,
			Map<String, Object> cartRow,
			Object activityId,
			String activityType) {
		Map<String, Object> line = new LinkedHashMap<>();
		int price = intVal(cartRow.get("price"), 0);
		line.put("item_id", itemId);
		line.put("num", num);
		line.put("company_id", companyId);
		line.put("user_id", userId);
		line.put("distributor_id", distributorId);
		// PHP item shop_id from shopInfo (same as order header; 0 for normal/shopadmin)
		line.put("shop_id", 0L);
		line.put("price", price);
		line.put("item_name", stringVal(cartRow.get("item_name")));
		line.put("goods_id", longVal(cartRow.get("goods_id"), itemId));
		line.put("item_bn", stringVal(cartRow.get("item_bn")));
		line.put("goods_bn", stringVal(cartRow.get("goods_bn")));
		line.put("item_unit", stringVal(cartRow.get("item_unit")));
		if (cartRow.get("templates_id") != null) {
			line.put("templates_id", cartRow.get("templates_id"));
		} else if (cartRow.get("template_id") != null) {
			line.put("templates_id", cartRow.get("template_id"));
		}
		if (cartRow.get("weight") != null) {
			line.put("weight", cartRow.get("weight"));
		}
		line.put("pic", stringVal(cartRow.get("pic")));
		if (!StringUtils.hasText(stringVal(line.get("pic")))) {
			line.put("pic", stringVal(cartRow.get("pics")));
		}
		line.put("cost_price", intVal(cartRow.get("cost_price"), 0));
		line.put("market_price", intVal(cartRow.get("market_price"), price));
		line.put("is_total_store", !Boolean.FALSE.equals(cartRow.get("is_total_store")));
		line.put("activity_id", activityId);
		line.put("activity_type", activityType);
		line.put("discount_info", new ArrayList<Map<String, Object>>());
		return line;
	}

	private static void applyLineAmounts(
			Map<String, Object> line, int unitPriceFen, long lineTotalFen, int discountFen) {
		int payFen = (int) Math.min(lineTotalFen, Integer.MAX_VALUE);
		int itemFeeFen = (int) Math.min((long) unitPriceFen * intVal(line.get("num"), 1), Integer.MAX_VALUE);
		line.put("total_fee", payFen);
		line.put("item_fee", itemFeeFen > 0 ? itemFeeFen : payFen);
		line.put("discount_fee", discountFen);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> findPackageChildRow(Map<String, Object> cartRow, long childItemId) {
		Object raw = cartRow.get("packages");
		if (!(raw instanceof List<?> list)) {
			return Map.of();
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			if (longVal(row.get("item_id"), 0L) == childItemId) {
				return (Map<String, Object>) row;
			}
		}
		return Map.of();
	}

	@SuppressWarnings("unchecked")
	private static void applyPackageChildSalePriceFromMainLine(Map<String, Object> od) {
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			return;
		}
		Map<String, String> mainSaleByActivity = new LinkedHashMap<>();
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) row;
			if (!"package".equals(stringVal(line.get("activity_type")))) {
				continue;
			}
			String actKey = stringVal(line.get("activity_id"));
			if (!StringUtils.hasText(actKey)) {
				continue;
			}
			if (!mainSaleByActivity.containsKey(actKey)) {
				Object sp = line.get("sale_price");
				if (sp != null) {
					mainSaleByActivity.put(actKey, String.valueOf(sp));
				}
			} else {
				String mainSp = mainSaleByActivity.get(actKey);
				if (StringUtils.hasText(mainSp)) {
					line.put("sale_price", mainSp);
				}
			}
		}
	}

	private static List<Long> parseChildItemIds(Object itemsIdRaw) {
		List<Long> out = new ArrayList<>();
		if (!(itemsIdRaw instanceof List<?> list)) {
			return out;
		}
		for (Object el : list) {
			long childId = longVal(el, 0L);
			if (childId > 0L) {
				out.add(childId);
			}
		}
		return out;
	}

	private static Locale resolveLocale() {
		Locale locale = LocaleContextHolder.getLocale();
		return locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
	}
}
