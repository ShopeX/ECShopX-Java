package cn.shopex.ecshopx.mcp.auth;

/**
 * Business JWT audience for MCP OAuth (authorization flow uses these in later tasks).
 */
public enum TokenKind {
	OPERATOR,
	POS,
	MEMBER
}