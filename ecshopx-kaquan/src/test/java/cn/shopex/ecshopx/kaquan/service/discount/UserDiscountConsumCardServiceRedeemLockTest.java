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

package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponStatisticsPayCompleteService;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgCouponNotifyService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10899：核销更新须命中可核销状态，未命中时不得记成功日志。 */
class UserDiscountConsumCardServiceRedeemLockTest {

	private UserDiscountMapper userDiscountMapper;
	private UserDiscountLogsMapper userDiscountLogsMapper;
	private WxShopsListForUserDiscountService shopsService;
	private DiscountCardKaquanDetailForConsumeService cardDetailService;
	private MemberAccountService memberAccountService;
	private UserDiscountUserRecordDetailAssembler detailAssembler;
	private UserDiscountConsumCardService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), UserDiscount.class);
	}

	@BeforeEach
	void setUp() {
		userDiscountMapper = mock(UserDiscountMapper.class);
		userDiscountLogsMapper = mock(UserDiscountLogsMapper.class);
		shopsService = mock(WxShopsListForUserDiscountService.class);
		cardDetailService = mock(DiscountCardKaquanDetailForConsumeService.class);
		memberAccountService = mock(MemberAccountService.class);
		detailAssembler = mock(UserDiscountUserRecordDetailAssembler.class);
		service = new UserDiscountConsumCardService(
				userDiscountMapper,
				userDiscountLogsMapper,
				shopsService,
				cardDetailService,
				mock(SalespersonCouponStatisticsPayCompleteService.class),
				memberAccountService,
				mock(WxaTemplateMsgCouponNotifyService.class),
				detailAssembler);
	}

	@Test
	void consumePayBillCard_skipsSuccessLogWhenUpdateMatchesNothing() {
		stubConsumable(1);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(0);

		Map<String, Object> result = service.consumePayBillCard(100L, 5L, "13800000000", "CODE-1");

		assertThat(result.get("status")).isEqualTo(false);
		verify(userDiscountLogsMapper, never()).insert(any(UserDiscountLogs.class));
	}

	@Test
	void consumePayBillCard_redeemsOnlyWhileStatusIsUnusedOrReservedAndWritesLog() {
		stubConsumable(1);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(1);

		Map<String, Object> result = service.consumePayBillCard(100L, 5L, "13800000000", "CODE-1");

		assertThat(result.get("status")).isEqualTo(true);
		assertRedeemLimitedToStatusOneOrFour();
		verify(userDiscountLogsMapper).insert(any(UserDiscountLogs.class));
	}

	@Test
	void consumePayBillCard_writesLogWhenStatusIsFour() {
		stubConsumable(4);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(1);

		Map<String, Object> result = service.consumePayBillCard(100L, 5L, "13800000000", "CODE-1");

		assertThat(result.get("status")).isEqualTo(true);
		verify(userDiscountLogsMapper).insert(any(UserDiscountLogs.class));
	}

	@Test
	void userUsedCardSelfConsume_skipsSuccessLogWhenUpdateMatchesNothing() {
		stubConsumable(1);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(0);

		Map<String, Object> result = service.userUsedCardSelfConsume(100L, 5L, "CODE-1", null, null, null, null);

		assertThat(result.get("status")).isEqualTo(false);
		verify(userDiscountLogsMapper, never()).insert(any(UserDiscountLogs.class));
	}

	@Test
	void consumeCouponForShopadminOrderCreate_throwsExistingMessageWhenUpdateMatchesNothing() {
		stubConsumable(1);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(0);

		assertThatThrownBy(() -> service.consumeCouponForShopadminOrderCreate(100L, 5L, "CODE-1", "下单", "T1", "10"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("优惠券使用失败，该优惠券已被使用或无效");
		verify(userDiscountLogsMapper, never()).insert(any(UserDiscountLogs.class));
	}

	@Test
	void consumeCouponForTradeFinishPayBill_throwsExistingMessageWhenUpdateMatchesNothing() {
		stubConsumable(1);
		when(userDiscountMapper.update(nullable(UserDiscount.class), any())).thenReturn(0);

		assertThatThrownBy(() -> service.consumeCouponForTradeFinishPayBill(100L, 5L, "CODE-1", "T1", "10", null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("优惠券使用失败，该优惠券已被使用或无效");
		verify(userDiscountLogsMapper, never()).insert(any(UserDiscountLogs.class));
	}

	private void stubConsumable(int status) {
		UserDiscount row = new UserDiscount();
		row.setId(9L);
		row.setCompanyId(100L);
		row.setUserId(5L);
		row.setCode("CODE-1");
		row.setCardId(3L);
		row.setStatus(status);
		row.setTitle("满减券");
		row.setCardType("cash");
		when(userDiscountMapper.selectOne(any())).thenReturn(row);
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("rel_shops_ids", List.of());
		when(detailAssembler.toDetailMap(any())).thenReturn(detail);
		Map<String, Object> shops = new LinkedHashMap<>();
		shops.put("list", List.of());
		when(shopsService.listShopsPoi(anyLong(), any())).thenReturn(shops);
		Map<String, Object> cardInfo = new LinkedHashMap<>();
		cardInfo.put("use_scenes", "QUICK");
		when(cardDetailService.loadDetail(100L, 3L)).thenReturn(cardInfo);
		when(memberAccountService.getMemberInfo(5L, 100L)).thenReturn(Map.of("mobile", "13800000000", "username", "u"));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void assertRedeemLimitedToStatusOneOrFour() {
		ArgumentCaptor<LambdaUpdateWrapper<UserDiscount>> captor =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(userDiscountMapper).update(isNull(), captor.capture());
		LambdaUpdateWrapper<UserDiscount> wrapper = captor.getValue();
		assertThat(wrapper.getSqlSegment()).containsIgnoringCase("status");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(1, 4);
	}
}
