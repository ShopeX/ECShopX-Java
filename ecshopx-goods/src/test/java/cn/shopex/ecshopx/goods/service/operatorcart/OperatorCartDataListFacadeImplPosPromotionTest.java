package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCleanupService;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonDistributorCartFormatAndTotalService;
import cn.shopex.ecshopx.goods.service.items.ItemSpecImageLookupService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * POS 购物车列表接入促销（ECX-10719）：格式化完成后调用
 * {@link OperatorCartPosPromotionService#applyPosPromotions}，
 * 对同一 cartData 自动应用满减、满折、限时特惠。
 */
@ExtendWith(MockitoExtension.class)
class OperatorCartDataListFacadeImplPosPromotionTest {

	private static final long COMPANY_ID = 38L;
	private static final long OPERATOR_ID = 21L;
	private static final long DISTRIBUTOR_ID = 129L;
	private static final long TARGET_USER_ID = 0L;
	private static final long ITEM_ID = 7666L;

	@Mock private OperatorCartMapper operatorCartMapper;
	@Mock private OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;
	@Mock private OperatorCartCompanyProductModelReader productModelReader;
	@Mock private OperatorCartHandleValidCartService operatorCartHandleValidCartService;
	@Mock private SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService;
	@Mock private ItemSpecImageLookupService itemSpecImageLookupService;
	@Mock private OperatorCartCleanupService operatorCartCleanupService;
	@Mock private LangueProperties langueProperties;
	@Mock private OperatorCartPosPromotionService operatorCartPosPromotionService;

	private OperatorCartDataListFacadeImpl facade;

	@BeforeEach
	void setUp() {
		facade =
				new OperatorCartDataListFacadeImpl(
						operatorCartMapper,
						operatorCartSkuLoadFacade,
						productModelReader,
						operatorCartHandleValidCartService,
						salespersonDistributorCartFormatAndTotalService,
						itemSpecImageLookupService,
						operatorCartCleanupService,
						langueProperties,
						operatorCartPosPromotionService);
	}

	@Test
	void getCartDataList_appliesPosPromotionsToFormattedCartData() {
		OperatorCart cartRow = new OperatorCart();
		cartRow.setCartId(631L);
		cartRow.setCompanyId(COMPANY_ID);
		cartRow.setOperatorId(OPERATOR_ID);
		cartRow.setDistributorId(DISTRIBUTOR_ID);
		cartRow.setItemId(ITEM_ID);
		cartRow.setNum(1L);
		cartRow.setIsChecked(true);
		when(operatorCartMapper.selectList(any(Wrapper.class))).thenReturn(List.of(cartRow));

		OperatorCartSkuRowDto sku = new OperatorCartSkuRowDto();
		sku.setItemId(ITEM_ID);
		sku.setUnitPriceFen(19900);
		sku.setStore(100);
		sku.setItemName("java 一个规格 仅一个sku");
		sku.setItemType("normal");
		sku.setItemApproveStatus("onsale");
		when(operatorCartSkuLoadFacade.loadSkus(eq(COMPANY_ID), eq(DISTRIBUTOR_ID), eq(TARGET_USER_ID), anyList()))
				.thenReturn(List.of(sku));
		when(productModelReader.getProductModel(COMPANY_ID)).thenReturn("bbc");

		Map<String, Object> handled = new LinkedHashMap<>();
		Map<String, Object> cartLine = new LinkedHashMap<>();
		cartLine.put("cart_id", 631L);
		cartLine.put("item_id", ITEM_ID);
		cartLine.put("num", 1L);
		handled.put("valid_cart", new ArrayList<>(List.of(cartLine)));
		handled.put("invalid_cart", new ArrayList<>());
		when(operatorCartHandleValidCartService.handle(
						eq(COMPANY_ID), eq(TARGET_USER_ID), anyList(), anyMap()))
				.thenReturn(handled);

		Map<String, Object> cartData = new LinkedHashMap<>();
		Map<String, Object> shopBlock = new LinkedHashMap<>();
		shopBlock.put("shop_id", DISTRIBUTOR_ID);
		shopBlock.put("list", new ArrayList<>(List.of(cartLine)));
		cartData.put("valid_cart", new ArrayList<>(List.of(shopBlock)));
		cartData.put("invalid_cart", new ArrayList<>());
		when(salespersonDistributorCartFormatAndTotalService.apply(
						eq(COMPANY_ID), eq(TARGET_USER_ID), anyMap(), anyBoolean(),
						eq(DISTRIBUTOR_ID), eq("distributor"), any()))
				.thenReturn(cartData);

		Map<String, Object> out =
				facade.getCartDataList(COMPANY_ID, OPERATOR_ID, DISTRIBUTOR_ID, TARGET_USER_ID, null, false);

		assertSame(cartData, out);
		verify(operatorCartPosPromotionService).applyPosPromotions(COMPANY_ID, TARGET_USER_ID, cartData);
	}

	@Test
	void getCartDataList_emptyCartSkipsPromotionApply() {
		when(operatorCartMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

		Map<String, Object> out =
				facade.getCartDataList(COMPANY_ID, OPERATOR_ID, DISTRIBUTOR_ID, TARGET_USER_ID, null, false);

		assertEquals(0, ((List<?>) out.get("valid_cart")).size());
		verify(operatorCartPosPromotionService, never()).applyPosPromotions(anyLong(), anyLong(), anyMap());
	}
}
