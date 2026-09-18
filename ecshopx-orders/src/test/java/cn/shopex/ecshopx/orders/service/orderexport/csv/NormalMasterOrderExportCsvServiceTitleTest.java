package cn.shopex.ecshopx.orders.service.orderexport.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalMasterOrderExportCsvServiceTitleTest {

	private static final List<String> PHP_GET_TITLE_HEADERS =
			List.of(
					"订单号",
					"用户名",
					"下单时间",
					"订单分类",
					"订单总金额(¥)",
					"现金实付(¥)",
					"积分抵扣",
					"成本价(¥)",
					"运费(总)",
					"佣金(总)",
					"优惠金额",
					"优惠详情",
					"订单类型",
					"订单状态",
					"订单完成时间",
					"收货方式",
					"自提状态",
					"收货人姓名",
					"收货人手机",
					"收货人邮编",
					"收货人所在省份",
					"收货人所在城市",
					"收货人所在地区、县",
					"收货地址",
					"发货状态",
					"发货时间",
					"快递单号",
					"快递公司",
					"支付方式",
					"支付时间",
					"发票内容",
					"订单备注",
					"自提地址",
					"提货时间",
					"订单来源",
					"角色",
					"员工姓名",
					"所属企业");

	@Test
	@SuppressWarnings("unchecked")
	void buildTitle_matchesPhpNormalMasterOrderGetTitle() throws Exception {
		Method m = NormalMasterOrderExportCsvService.class.getDeclaredMethod("buildTitle");
		m.setAccessible(true);
		LinkedHashMap<String, String> title = (LinkedHashMap<String, String>) m.invoke(null);
		assertEquals(PHP_GET_TITLE_HEADERS.size(), title.size());
		assertEquals(PHP_GET_TITLE_HEADERS, List.copyOf(title.values()));
	}
}
