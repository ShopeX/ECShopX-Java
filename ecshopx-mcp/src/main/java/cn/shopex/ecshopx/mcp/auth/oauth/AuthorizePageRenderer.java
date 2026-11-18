package cn.shopex.ecshopx.mcp.auth.oauth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Authorize HTML aligned with Figma「MCP设置」授权帧 + ecshopx-admin login shell.
 */
@Component
public class AuthorizePageRenderer {

	private static final String ASSET_BASE = "/api/mcp/oauth/assets";

	public String render(AuthorizeViewModel model) {
		String errorBlock = StringUtils.hasText(model.errorMessage())
				? "<div class=\"field-error\" role=\"alert\">" + escape(model.errorMessage()) + "</div>"
				: "";
		String selectedLogintype = StringUtils.hasText(model.logintype())
				? model.logintype()
				: OperatorLoginTypes.DEFAULT;
		StringBuilder options = new StringBuilder();
		for (Map.Entry<String, String> entry : OperatorLoginTypes.labels().entrySet()) {
			options.append("<option value=\"")
					.append(escape(entry.getKey()))
					.append('"');
			if (entry.getKey().equalsIgnoreCase(selectedLogintype)) {
				options.append(" selected");
			}
			options.append('>')
					.append(escape(entry.getValue()))
					.append("</option>");
		}
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head>
				  <meta charset="UTF-8"/>
				  <meta name="viewport" content="width=device-width,initial-scale=1"/>
				  <meta name="referrer" content="never"/>
				  <title>ECShopX - MCP 授权</title>
				  %s
				</head>
				<body>
				  <div class="auth-shell">
				    <div class="auth-center">
				      <div class="auth-card">
				        <img class="brand-logo" src="%s/logo.png" alt="ECShopX"/>
				        <h1 class="title">请登录并完成MCP授权</h1>
				        <form method="post" action="/api/mcp/oauth/authorize/login" data-testid="admin-mcp-login-form" novalidate>
				          <input type="hidden" name="client_id" value="%s"/>
				          <input type="hidden" name="redirect_uri" value="%s"/>
				          <input type="hidden" name="state" value="%s"/>
				          <input type="hidden" name="code_challenge" value="%s"/>
				          <input type="hidden" name="code_challenge_method" value="%s"/>
				          <input type="hidden" name="response_type" value="code"/>
				          <div class="field">
				            <label class="field-label" for="username">用户名</label>
				            <div class="field-control-wrap">
				              <input class="field-control" id="username" name="username" autocomplete="username"
				                     required placeholder="请输入用户名"/>
				            </div>
				            %s
				          </div>
				          <div class="field">
				            <label class="field-label" for="password">密码</label>
				            <div class="field-control-wrap">
				              <input class="field-control" id="password" name="password" type="password"
				                     autocomplete="current-password" required placeholder="密码"/>
				            </div>
				          </div>
				          <div class="field">
				            <label class="field-label" for="logintype">角色</label>
				            <div class="field-control-wrap">
				              <select class="field-control" id="logintype" name="logintype" required>
				                %s
				              </select>
				            </div>
				          </div>
				          <div class="actions">
				            <button class="btn-login" type="submit">登录并授权</button>
				          </div>
				        </form>
				      </div>
				    </div>
				    <div class="powered">Powered by shopex <img src="%s/logo-powerby.png" alt=""/></div>
				  </div>
				</body>
				</html>
				"""
				.formatted(
						sharedStyles(),
						ASSET_BASE,
						escape(model.clientId()),
						escape(model.redirectUri()),
						escape(nullToEmpty(model.state())),
						escape(model.codeChallenge()),
						escape(nullToEmpty(model.codeChallengeMethod())),
						errorBlock,
						options.toString(),
						ASSET_BASE);
	}

	public String renderShopSelect(ShopSelectViewModel model) {
		String errorBlock = StringUtils.hasText(model.errorMessage())
				? "<div class=\"field-error\" role=\"alert\">" + escape(model.errorMessage()) + "</div>"
				: "";
		StringBuilder options = new StringBuilder();
		for (PendingDistributorAuthStore.ShopOption shop : model.shops()) {
			options.append("<option value=\"")
					.append(shop.distributorId())
					.append("\">")
					.append(escape(shop.name()))
					.append(" (#")
					.append(shop.distributorId())
					.append(")</option>");
		}
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head>
				  <meta charset="UTF-8"/>
				  <meta name="viewport" content="width=device-width,initial-scale=1"/>
				  <meta name="referrer" content="never"/>
				  <title>ECShopX - 选择店铺</title>
				  %s
				</head>
				<body>
				  <div class="auth-shell">
				    <div class="auth-center">
				      <div class="auth-card">
				        <img class="brand-logo" src="%s/logo.png" alt="ECShopX"/>
				        <h1 class="title">选择店铺</h1>
				        <p class="hint">该账号可管理多家店铺，请先选择当前店铺后再继续授权。</p>
				        <form method="post" action="/api/mcp/oauth/authorize/select-shop" data-testid="admin-mcp-shop-select-form" novalidate>
				          <input type="hidden" name="pending_id" value="%s"/>
				          <div class="field">
				            <label class="field-label" for="set_distributor_id">店铺</label>
				            <div class="field-control-wrap">
				              <select class="field-control" id="set_distributor_id" name="set_distributor_id" required>
				                %s
				              </select>
				            </div>
				            %s
				          </div>
				          <div class="actions">
				            <button class="btn-login" type="submit">确认并继续</button>
				          </div>
				        </form>
				      </div>
				    </div>
				    <div class="powered">Powered by shopex <img src="%s/logo-powerby.png" alt=""/></div>
				  </div>
				</body>
				</html>
				"""
				.formatted(
						sharedStyles(),
						ASSET_BASE,
						escape(model.pendingId()),
						options.toString(),
						errorBlock,
						ASSET_BASE);
	}

	public String renderSuccess(SuccessViewModel model) {
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head>
				  <meta charset="UTF-8"/>
				  <meta name="viewport" content="width=device-width,initial-scale=1"/>
				  <meta name="referrer" content="never"/>
				  <title>ECShopX - 授权成功</title>
				  %s
				</head>
				<body>
				  <div class="auth-shell">
				    <div class="auth-center">
				      <div class="auth-card auth-card-success">
				        <img class="brand-logo" src="%s/logo.png" alt="ECShopX"/>
				        <h1 class="title">授权成功</h1>
				        <div class="success-copy">
				          <p>MCP 授权已完成。</p>
				          <p>你可以关闭此页面，返回应用继续使用。</p>
				        </div>
				        <div class="actions actions-center">
				          <a class="btn-login btn-link" href="%s" data-testid="admin-mcp-auth-done">完成</a>
				        </div>
				      </div>
				    </div>
				    <div class="powered">Powered by shopex <img src="%s/logo-powerby.png" alt=""/></div>
				  </div>
				</body>
				</html>
				"""
				.formatted(sharedStyles(), ASSET_BASE, escape(model.continueUrl()), ASSET_BASE);
	}

	private static String sharedStyles() {
		return """
				<style>
				  * { box-sizing: border-box; }
				  html, body {
				    margin: 0;
				    padding: 0;
				    height: 100%%;
				    font-family: Helvetica, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
				    color: #333;
				    background: #f5f5f5;
				  }
				  .auth-shell {
				    position: relative;
				    width: 100%%;
				    min-height: 100vh;
				    background-image: url('%s/ecxbg.jpg');
				    background-size: cover;
				    background-position: center;
				    background-repeat: no-repeat;
				  }
				  .auth-center {
				    width: 530px;
				    max-width: calc(100vw - 2rem);
				    position: absolute;
				    top: 50%%;
				    left: 50%%;
				    transform: translate(-50%%, -50%%);
				  }
				  .auth-card {
				    background: #fff;
				    padding: 64px 80px;
				    border-radius: 16px;
				    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
				  }
				  .auth-card-success { text-align: left; }
				  .brand-logo {
				    display: block;
				    width: 300px;
				    max-width: 100%%;
				    height: auto;
				  }
				  .title {
				    margin: 32px 0 0 8px;
				    font-size: 28px;
				    font-weight: 400;
				    line-height: 1.2;
				    color: #333;
				  }
				  .hint {
				    margin: 12px 0 0 8px;
				    color: #909399;
				    font-size: 13px;
				    line-height: 1.5;
				  }
				  .success-copy {
				    margin: 12px 0 0 8px;
				    color: #5d6165;
				    font-size: 16px;
				    line-height: 28px;
				  }
				  .success-copy p { margin: 0; }
				  form {
				    margin-top: 32px;
				    margin-left: 8px;
				  }
				  .field { margin-top: 18px; }
				  .field:first-of-type { margin-top: 0; }
				  .field-label {
				    display: block;
				    margin-bottom: 8px;
				    font-size: 14px;
				    font-weight: 500;
				    color: #606266;
				    line-height: 1.4;
				  }
				  .field-control-wrap {
				    border: 1px solid #333;
				    border-radius: 0;
				    background: #fff;
				  }
				  .field-control-wrap:focus-within { border-color: #000; }
				  .field-control {
				    display: block;
				    width: 100%%;
				    height: 40px;
				    padding: 0 15px;
				    border: none;
				    border-radius: 0;
				    background: transparent;
				    color: #333;
				    font-size: 14px;
				    outline: none;
				    appearance: none;
				    -webkit-appearance: none;
				  }
				  select.field-control {
				    background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'><path fill='%%23333333' d='M1.2 1.2L6 6l4.8-4.8'/></svg>");
				    background-repeat: no-repeat;
				    background-position: right 12px center;
				    padding-right: 32px;
				  }
				  .field-control::placeholder { color: #c0c4cc; }
				  .field-error {
				    margin-top: 4px;
				    color: #f56c6c;
				    font-size: 12px;
				    line-height: 1.4;
				  }
				  .actions {
				    margin-top: 32px;
				    text-align: right;
				  }
				  .actions-center { text-align: right; }
				  .btn-login {
				    display: inline-flex;
				    align-items: center;
				    justify-content: center;
				    min-width: 120px;
				    height: 50px;
				    padding: 0 30px;
				    border: none;
				    border-radius: 50px;
				    background: #000;
				    color: #fff;
				    font-size: 18px;
				    cursor: pointer;
				    line-height: 1;
				    text-decoration: none;
				  }
				  .btn-login:hover { opacity: 0.92; }
				  .btn-link { box-sizing: border-box; }
				  .powered {
				    position: absolute;
				    left: 0;
				    right: 0;
				    bottom: 24px;
				    text-align: center;
				    color: #b3b3b3;
				    font-size: 12px;
				    letter-spacing: 0.02em;
				  }
				  .powered img {
				    height: 14px;
				    vertical-align: -2px;
				    margin-left: 4px;
				    opacity: 0.85;
				  }
				  @media (max-width: 640px) {
				    .auth-card { padding: 40px 24px; }
				    .brand-logo { width: 220px; }
				    .title, form, .hint { margin-left: 0; }
				    .title { font-size: 24px; }
				  }
				</style>
				"""
				.formatted(ASSET_BASE);
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
			String logintype,
			String errorMessage) {
	}

	public record ShopSelectViewModel(
			String pendingId,
			List<PendingDistributorAuthStore.ShopOption> shops,
			String errorMessage) {
	}

	public record SuccessViewModel(String continueUrl) {
	}
}
