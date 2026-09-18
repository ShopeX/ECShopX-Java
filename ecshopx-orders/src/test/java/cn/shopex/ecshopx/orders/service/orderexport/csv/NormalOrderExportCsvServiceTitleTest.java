package cn.shopex.ecshopx.orders.service.orderexport.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalOrderExportCsvServiceTitleTest {

	private static final List<String> PHP_GET_TITLE_HEADERS =
			List.of(
					"订单号",
					"用户名",
					"子订单号",
					"所属供应商",
					"商品名称",
					"管理分类",
					"商品销售单价",
					"成本单价",
					"购买数量",
					"销售总金额",
					"商品佣金",
					"结算总价（¥）",
					"运费(总)",
					"实付金额(总)",
					"积分抵扣（¥）",
					"现金实付（¥）",
					"优惠总金额",
					"优惠详情",
					"退货数量",
					"退货成本",
					"退款积分",
					"退款金额",
					"所属店铺",
					"店铺号",
					"会员手机号",
					"会员昵称",
					"下单时间",
					"支付时间",
					"订单类型",
					"订单状态",
					"收货方式",
					"自提状态",
					"收货人姓名",
					"收货人手机",
					"收货人邮编",
					"收货人所在省份",
					"收货人所在城市",
					"收货人所在地区、县",
					"收货地址",
					"收货状态",
					"发货时间",
					"快递单号",
					"快递公司",
					"订单完成时间",
					"支付方式",
					"商品货号",
					"售后状态",
					"退款时间",
					"规格描述",
					"订单备注",
					"自提地址",
					"提货时间",
					"角色",
					"员工姓名",
					"所属企业");

	@Test
	void buildTitle_matchesPhpNormalOrderGetTitle() {
		LinkedHashMap<String, String> title = NormalOrderExportCsvService.buildTitle();
		assertEquals(PHP_GET_TITLE_HEADERS.size(), title.size());
		assertEquals(PHP_GET_TITLE_HEADERS, List.copyOf(title.values()));
	}
}
