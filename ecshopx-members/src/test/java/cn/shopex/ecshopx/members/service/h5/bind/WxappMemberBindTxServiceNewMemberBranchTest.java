package cn.shopex.ecshopx.members.service.h5.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappMemberBindTxServiceNewMemberBranchTest {

	@Mock
	MemberAccountService memberAccountService;

	@Mock
	MemberRegSettingService memberRegSettingService;

	@Test
	void bindInTransaction_whenNoExistingMember_invokesCreateMemberForWxappBind() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 1L);
		params.put("username", "13800138000");
		params.put("union_id", "union-test");
		params.put("check_type", "login");
		params.put("vcode", "123456");

		when(memberRegSettingService.checkSmsVcode(
						eq("13800138000"), eq(1L), eq("123456"), eq("login")))
				.thenReturn(true);

		Map<String, Object> wx = new HashMap<>();
		wx.put("unionid", "union-test");
		wx.put("open_id", "openid-test");
		when(memberAccountService.getWechatUserInfo(anyMap())).thenReturn(wx);

		when(memberAccountService.findMemberByCompanyAndMobile(1L, "13800138000"))
				.thenReturn(null);

		Map<String, Object> created = new HashMap<>();
		created.put("user_id", 99L);
		when(memberAccountService.createMemberForWxappBind(
						eq(1L),
						eq("13800138000"),
						eq(""),
						anyMap(),
						same(params)))
				.thenReturn(created);

		WxappMemberBindTxService svc =
				new WxappMemberBindTxService(memberAccountService, memberRegSettingService);
		Map<String, Object> out = svc.bindInTransaction(params);

		verify(memberAccountService, times(1))
				.createMemberForWxappBind(
						eq(1L),
						eq("13800138000"),
						eq(""),
						anyMap(),
						same(params));

		assertNotNull(out);
		assertEquals(99L, ((Number) out.get("user_id")).longValue());
		assertEquals(1, out.get("is_new"));
	}

	@Test
	void bindInTransaction_whenNoExistingMember_passwordBranch_invokesCreateMemberForWxappBindWithPassword() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 1L);
		params.put("username", "13700137000");
		params.put("union_id", "union-pwd");
		// Alphanumeric and 6–16 digits: SMS fields omitted when password is present (validateParams).
		params.put("password", "12345678");

		Map<String, Object> wx = new HashMap<>();
		wx.put("unionid", "union-pwd");
		wx.put("open_id", "openid-pwd");
		when(memberAccountService.getWechatUserInfo(anyMap())).thenReturn(wx);

		when(memberAccountService.findMemberByCompanyAndMobile(1L, "13700137000"))
				.thenReturn(null);

		Map<String, Object> created = new HashMap<>();
		created.put("user_id", 101L);
		when(memberAccountService.createMemberForWxappBind(
						eq(1L),
						eq("13700137000"),
						eq("12345678"),
						anyMap(),
						same(params)))
				.thenReturn(created);

		WxappMemberBindTxService svc =
				new WxappMemberBindTxService(memberAccountService, memberRegSettingService);
		Map<String, Object> out = svc.bindInTransaction(params);

		verify(memberAccountService, times(1))
				.createMemberForWxappBind(
						eq(1L),
						eq("13700137000"),
						eq("12345678"),
						anyMap(),
						same(params));

		assertNotNull(out);
		assertEquals(101L, ((Number) out.get("user_id")).longValue());
		assertEquals(1, out.get("is_new"));
	}

	@Test
	void bindInTransaction_whenExistingMember_doesNotInvokeCreateMemberForWxappBind() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 1L);
		params.put("username", "13900139000");
		params.put("union_id", "union-existing");
		params.put("check_type", "login");
		params.put("vcode", "654321");

		when(memberRegSettingService.checkSmsVcode(
						eq("13900139000"), eq(1L), eq("654321"), eq("login")))
				.thenReturn(true);

		Map<String, Object> wx = new HashMap<>();
		wx.put("unionid", "union-existing");
		wx.put("open_id", "openid-existing");
		when(memberAccountService.getWechatUserInfo(anyMap())).thenReturn(wx);

		Members existing = new Members();
		existing.setUserId(42L);
		existing.setPassword("stored-hash");
		when(memberAccountService.findMemberByCompanyAndMobile(1L, "13900139000"))
				.thenReturn(existing);

		Map<String, Object> memberRow = new HashMap<>();
		memberRow.put("user_id", 42L);
		when(memberAccountService.getMemberInfo(42L, 1L)).thenReturn(memberRow);

		WxappMemberBindTxService svc =
				new WxappMemberBindTxService(memberAccountService, memberRegSettingService);
		Map<String, Object> out = svc.bindInTransaction(params);

		verify(memberAccountService, never())
				.createMemberForWxappBind(anyLong(), anyString(), anyString(), anyMap(), any());

		assertNotNull(out);
		assertEquals(42L, ((Number) out.get("user_id")).longValue());
		assertEquals(0, out.get("is_new"));

		verify(memberAccountService, times(1))
				.createMemberWechatAssociation(1L, 42L, "union-existing");
	}
}
