package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthorizePageRenderer {

	public String render(AuthorizeViewModel model) {
		String errorBlock = StringUtils.hasText(model.errorMessage())
				? "<p class=\"error\">" + escape(model.errorMessage()) + "</p>"
				: "";
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head>
				  <meta charset="UTF-8"/>
				  <title>ECShopX MCP 授权</title>
				  <style>
				    body { font-family: system-ui, sans-serif; max-width: 28rem; margin: 2rem auto; }
				    label { display: block; margin-top: 0.75rem; }
				    input, select { width: 100%%; padding: 0.4rem; box-sizing: border-box; }
				    button { margin-top: 1rem; padding: 0.5rem 1rem; }
				    .error { color: #b00020; }
				    .hint { color: #555; font-size: 0.9rem; }
				    fieldset { margin-top: 1rem; border: 1px solid #ccc; padding: 0.75rem; }
				  </style>
				</head>
				<body>
				  <h1>MCP OAuth 登录</h1>
				  <p class="hint">选择业务身份并登录，以授权 MCP 客户端访问。</p>
				  %s
				  <form method="post" action="/api/mcp/oauth/authorize/login">
				    <input type="hidden" name="client_id" value="%s"/>
				    <input type="hidden" name="redirect_uri" value="%s"/>
				    <input type="hidden" name="state" value="%s"/>
				    <input type="hidden" name="code_challenge" value="%s"/>
				    <input type="hidden" name="code_challenge_method" value="%s"/>
				    <input type="hidden" name="response_type" value="code"/>
				    <label>身份类型
				      <select name="token_kind" required>
				        <option value="OPERATOR"%s>后台 OPERATOR</option>
				        <option value="POS"%s>门店 POS</option>
				        <option value="MEMBER"%s>会员 MEMBER</option>
				      </select>
				    </label>
				    <fieldset>
				      <legend>OPERATOR</legend>
				      <label>用户名 <input name="username" autocomplete="username"/></label>
				      <label>密码 <input name="password" type="password" autocomplete="current-password"/></label>
				      <label>logintype <input name="logintype" placeholder="localadmin"/></label>
				    </fieldset>
				    <fieldset>
				      <legend>POS</legend>
				      <label>工号 employee_no <input name="employee_no"/></label>
				      <label>密码 <input name="pos_password" type="password"/></label>
				    </fieldset>
				    <fieldset>
				      <legend>MEMBER (auth_type=local)</legend>
				      <label>company_id <input name="company_id"/></label>
				      <label>手机号 username <input name="member_username"/></label>
				      <label>密码 <input name="member_password" type="password"/></label>
				    </fieldset>
				    <button type="submit">登录并授权</button>
				  </form>
				</body>
				</html>
				"""
				.formatted(
						errorBlock,
						escape(model.clientId()),
						escape(model.redirectUri()),
						escape(nullToEmpty(model.state())),
						escape(model.codeChallenge()),
						escape(nullToEmpty(model.codeChallengeMethod())),
						selected(model.tokenKind(), "OPERATOR"),
						selected(model.tokenKind(), "POS"),
						selected(model.tokenKind(), "MEMBER"));
	}

	private static String selected(String tokenKind, String option) {
		if (tokenKind != null && tokenKind.equalsIgnoreCase(option)) {
			return " selected";
		}
		return "";
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	static String escape(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&#39;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	public record AuthorizeViewModel(
			String clientId,
			String redirectUri,
			String state,
			String codeChallenge,
			String codeChallengeMethod,
			String tokenKind,
			String errorMessage) {
	}
}