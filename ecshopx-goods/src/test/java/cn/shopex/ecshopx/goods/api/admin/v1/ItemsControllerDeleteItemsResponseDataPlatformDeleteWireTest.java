package cn.shopex.ecshopx.goods.api.admin.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ItemsAttributesCreateService;
import cn.shopex.ecshopx.goods.service.ItemsCommissionSaveService;
import cn.shopex.ecshopx.goods.service.items.DistributionGoodsWxaCodeStreamService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsBatchApproveStatusService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsByCouponService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsCreateRequestGate;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsDetailFacadeService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import cn.shopex.ecshopx.goods.service.items.ItemWarningStoreWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsAuditUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsBatchStoreRequestParser;
import cn.shopex.ecshopx.goods.service.items.ItemsBatchUpdateStoreOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemsCreateOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemsDeleteOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemsIsGiftBatchUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsPriceStoreStatusWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsRebateConfUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsRelCatsWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsSortUpdateService;
import cn.shopex.ecshopx.goods.service.items.ItemsTemplateWriteService;
import cn.shopex.ecshopx.goods.service.items.ItemsUpdateOrchestrator;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDeleteService;
import cn.shopex.ecshopx.goods.service.keywords.GoodsKeywordsQueryService;
import cn.shopex.ecshopx.goods.service.keywords.GoodsKeywordsWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsControllerDeleteItemsResponseDataPlatformDeleteWireTest {

	private ItemsCreateOrchestrator itemsCreateOrchestrator;
	private ItemsUpdateOrchestrator itemsUpdateOrchestrator;
	private GoodsItemsCreateRequestGate goodsItemsCreateRequestGate;
	private ItemsAuditUpdateService itemsAuditUpdateService;
	private OperatorLogsWriteService operatorLogsWriteService;
	private ObjectMapper objectMapper;
	private GoodsItemsListFacadeService goodsItemsListFacadeService;
	private GoodsItemsDetailFacadeService goodsItemsDetailFacadeService;
	private CompanysActivationService companysActivationService;
	private ItemsDeleteOrchestrator itemsDeleteOrchestrator;
	private PlatformItemsDeleteService platformItemsDeleteService;
	private ItemsAttributesCreateService itemsAttributesCreateService;
	private GoodsKeywordsWriteService goodsKeywordsWriteService;
	private GoodsKeywordsQueryService goodsKeywordsQueryService;
	private ItemsMedicineService itemsMedicineService;
	private ItemsRebateConfUpdateService itemsRebateConfUpdateService;
	private ItemsRelCatsWriteService itemsRelCatsWriteService;
	private ItemsSortUpdateService itemsSortUpdateService;
	private ItemsTemplateWriteService itemsTemplateWriteService;
	private ItemsCommissionSaveService itemsCommissionSaveService;
	private ItemWarningStoreWriteService itemWarningStoreWriteService;
	private ItemsIsGiftBatchUpdateService itemsIsGiftBatchUpdateService;
	private GoodsItemsBatchApproveStatusService goodsItemsBatchApproveStatusService;
	private ItemsBatchStoreRequestParser itemsBatchStoreRequestParser;
	private ItemsBatchUpdateStoreOrchestrator itemsBatchUpdateStoreOrchestrator;
	private ItemsPriceStoreStatusWriteService itemsPriceStoreStatusWriteService;
	private DistributionGoodsWxaCodeStreamService distributionGoodsWxaCodeStreamService;
	private GoodsItemsByCouponService goodsItemsByCouponService;

	private ItemsController itemsController;

	@BeforeEach
	void setUp() {
		itemsCreateOrchestrator = mock(ItemsCreateOrchestrator.class);
		itemsUpdateOrchestrator = mock(ItemsUpdateOrchestrator.class);
		goodsItemsCreateRequestGate = mock(GoodsItemsCreateRequestGate.class);
		itemsAuditUpdateService = mock(ItemsAuditUpdateService.class);
		operatorLogsWriteService = mock(OperatorLogsWriteService.class);
		objectMapper = new ObjectMapper();
		goodsItemsListFacadeService = mock(GoodsItemsListFacadeService.class);
		goodsItemsDetailFacadeService = mock(GoodsItemsDetailFacadeService.class);
		companysActivationService = mock(CompanysActivationService.class);
		itemsDeleteOrchestrator = mock(ItemsDeleteOrchestrator.class);
		platformItemsDeleteService = mock(PlatformItemsDeleteService.class);
		itemsAttributesCreateService = mock(ItemsAttributesCreateService.class);
		goodsKeywordsWriteService = mock(GoodsKeywordsWriteService.class);
		goodsKeywordsQueryService = mock(GoodsKeywordsQueryService.class);
		itemsMedicineService = mock(ItemsMedicineService.class);
		itemsRebateConfUpdateService = mock(ItemsRebateConfUpdateService.class);
		itemsRelCatsWriteService = mock(ItemsRelCatsWriteService.class);
		itemsSortUpdateService = mock(ItemsSortUpdateService.class);
		itemsTemplateWriteService = mock(ItemsTemplateWriteService.class);
		itemsCommissionSaveService = mock(ItemsCommissionSaveService.class);
		itemWarningStoreWriteService = mock(ItemWarningStoreWriteService.class);
		itemsIsGiftBatchUpdateService = mock(ItemsIsGiftBatchUpdateService.class);
		goodsItemsBatchApproveStatusService = mock(GoodsItemsBatchApproveStatusService.class);
		itemsBatchStoreRequestParser = mock(ItemsBatchStoreRequestParser.class);
		itemsBatchUpdateStoreOrchestrator = mock(ItemsBatchUpdateStoreOrchestrator.class);
		itemsPriceStoreStatusWriteService = mock(ItemsPriceStoreStatusWriteService.class);
		distributionGoodsWxaCodeStreamService = mock(DistributionGoodsWxaCodeStreamService.class);
		goodsItemsByCouponService = mock(GoodsItemsByCouponService.class);

		itemsController = new ItemsController(
				itemsCreateOrchestrator,
				itemsUpdateOrchestrator,
				goodsItemsCreateRequestGate,
				itemsAuditUpdateService,
				operatorLogsWriteService,
				objectMapper,
				goodsItemsListFacadeService,
				goodsItemsDetailFacadeService,
				companysActivationService,
				itemsDeleteOrchestrator,
				platformItemsDeleteService,
				itemsAttributesCreateService,
				goodsKeywordsWriteService,
				goodsKeywordsQueryService,
				itemsMedicineService,
				itemsRebateConfUpdateService,
				itemsRelCatsWriteService,
				itemsSortUpdateService,
				itemsTemplateWriteService,
				itemsCommissionSaveService,
				itemWarningStoreWriteService,
				itemsIsGiftBatchUpdateService,
				goodsItemsBatchApproveStatusService,
				itemsBatchStoreRequestParser,
				itemsBatchUpdateStoreOrchestrator,
				itemsPriceStoreStatusWriteService,
				distributionGoodsWxaCodeStreamService,
				goodsItemsByCouponService,
				mock(LangueProperties.class));
	}

	@Test
	void deleteItemsResponseData_callsPlatformDelete_only_neverOrchestrator() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwtUd = new LinkedHashMap<>();
		jwtUd.put("company_id", 100L);
		jwtUd.put("operator_id", 5L);
		when(request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA)).thenReturn(jwtUd);

		itemsController.deleteItemsResponseData(request, "42", "0");

		verify(platformItemsDeleteService, times(1)).deletePlatformItems(
				argThat(m -> m != null && m.containsKey("company_id") && ((Number) m.get("company_id")).longValue() == 100L),
				eq(42L),
				eq(0L));
		verify(itemsDeleteOrchestrator, never()).delete(any(), anyLong(), anyLong());
		verify(operatorLogsWriteService).addLogs(argThat(ctx -> {
			Object uri = ctx.get("request_uri");
			return uri != null && uri.toString().contains("/response");
		}));
	}

	@Test
	void deleteItemsResponseData_callsPlatformDelete_withLeadingDigitsDistributorQuery_neverOrchestrator()
			throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwtUd = new LinkedHashMap<>();
		jwtUd.put("company_id", 200L);
		jwtUd.put("operator_id", 9L);
		when(request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA)).thenReturn(jwtUd);

		itemsController.deleteItemsResponseData(request, "55", "77suffix");

		verify(platformItemsDeleteService, times(1)).deletePlatformItems(
				argThat(m -> m != null && m.containsKey("company_id") && ((Number) m.get("company_id")).longValue() == 200L),
				eq(55L),
				eq(77L));
		verify(itemsDeleteOrchestrator, never()).delete(any(), anyLong(), anyLong());
		verify(operatorLogsWriteService).addLogs(argThat(ctx -> {
			Object uri = ctx.get("request_uri");
			return uri != null && uri.toString().equals("/api/v1/goods/items/55/response");
		}));
	}
}
