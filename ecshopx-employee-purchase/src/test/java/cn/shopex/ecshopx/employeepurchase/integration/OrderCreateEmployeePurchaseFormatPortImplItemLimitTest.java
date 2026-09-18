package cn.shopex.ecshopx.employeepurchase.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePurchasePassphraseOrderService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreateEmployeePurchaseFormatPortImplItemLimitTest {

	@Mock EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	@Mock ActivityItemsMapper activityItemsMapper;
	@Mock MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;
	@Mock EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService;

	@InjectMocks OrderCreateEmployeePurchaseFormatPortImpl service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ActivityItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MemberActivityItemsAggregate.class);
	}

	@BeforeEach
	void stubActivity() {
		lenient().doNothing().when(employeePurchasePassphraseOrderService).checkBeforeOrder(any(), anyBoolean());
		Activities activity = new Activities();
		activity.setPurchaseMode("cash");
		activity.setIsPassphraseEnabled(false);
		activity.setMinimumAmount(0);
		when(employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(anyLong(), anyLong(), anyLong()))
				.thenReturn(activity);
		lenient()
				.when(employeePurchaseActivityDataService.getAggregateFeeForInvitee(anyLong(), anyLong(), anyLong(), anyLong()))
				.thenReturn(Map.of("left_fee", 1_000_000L));
		lenient().when(memberActivityItemsAggregateMapper.selectList(any())).thenReturn(List.of());
	}

	@Test
	void checkOrder_quantityExceed_usesPhpNumMessageWithoutItemName() {
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(activityItem(1L, 1, 100000)));
		NormalOrderCreateParams p = orderParams("某某商品", 2, 200);

		ResourceException ex =
				assertThrows(ResourceException.class, () -> service.applyEmployeePurchaseFormat(p, true));
		assertEquals(EmployeePurchaseItemLimitValidator.MSG_NUM, ex.getMessage());
	}

	@Test
	void previewOrder_feeExceed_setsExtraTipsPhpFeeMessage() {
		when(activityItemsMapper.selectList(any())).thenReturn(List.of(activityItem(1L, 100, 100)));
		NormalOrderCreateParams p = orderParams("某某商品", 1, 200);

		service.applyEmployeePurchaseFormat(p, false);

		assertEquals(EmployeePurchaseItemLimitValidator.MSG_FEE, p.getOrderData().get("extraTips"));
	}

	private static ActivityItems activityItem(long itemId, int limitNum, int limitFee) {
		ActivityItems ai = new ActivityItems();
		ai.setItemId(itemId);
		ai.setActivityPrice(100);
		ai.setLimitNum(limitNum);
		ai.setLimitFee(limitFee);
		ai.setShelfStatus(1);
		return ai;
	}

	private static NormalOrderCreateParams orderParams(String itemName, int num, int itemFee) {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		p.getParams().put("company_id", 1L);
		p.getParams().put("enterprise_id", 10L);
		p.getParams().put("activity_id", 20L);
		p.getParams().put("user_id", 30L);
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", 1L);
		item.put("item_name", itemName);
		item.put("num", num);
		item.put("item_fee", itemFee);
		p.getOrderData().put("items", List.of(item));
		p.getOrderData().put("item_fee", itemFee);
		return p;
	}
}
