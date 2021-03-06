package com.soybean.uaa.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soybean.framework.commons.entity.Result;
import com.soybean.framework.commons.exception.CheckedException;
import com.soybean.framework.commons.util.StringUtils;
import com.soybean.framework.db.TenantEnvironment;
import com.soybean.framework.security.client.utils.SecurityUtils;
import com.soybean.uaa.domain.dto.ChangePasswordDTO;
import com.soybean.uaa.service.UserService;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

/**
 * oauth认证授权
 * 重写响应端点。主要是为了封装响应结果
 *
 * @author wenxina
 * @date 2022/07/12
 */
@Slf4j
@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class RewriteTokenEndpoint {

    private final TenantEnvironment tenantEnvironment;
    private final TokenEndpoint tokenEndpoint;
    private final UserService userService;
    private final TokenStore tokenStore;

    /**
     * 获取访问令牌
     *
     * @param principal  主要
     * @param parameters 参数
     * @param request    请求
     * @return {@link SwaggerEnhancer}<{@link OAuth2AccessToken}>
     * @throws HttpRequestMethodNotSupportedException http请求方法不支持异常
     */
    @ResponseBody
    @RequestMapping(value = "/token", method = {RequestMethod.GET, RequestMethod.POST})
    public SwaggerEnhancer<OAuth2AccessToken> postAccessToken(Principal principal, @RequestParam Map<String, String> parameters, HttpServletRequest request) throws HttpRequestMethodNotSupportedException {
        final Result<OAuth2AccessToken> success = Result.success(tokenEndpoint.postAccessToken(principal, parameters).getBody());
        SwaggerEnhancer<OAuth2AccessToken> r = new SwaggerEnhancer<>(success.getCode(), success.getData(), success.getMessage());
        r.setTimestamp(System.currentTimeMillis());
        // 说明是swagger 来的请求，那么增强实现
        r.setAccessToken(success.getData().getValue());
        return r;
    }

    /**
     * 退出登录
     */
    @DeleteMapping("/logout")
    @ResponseBody
    public void removeToken() {
        if (SecurityUtils.anonymous()) {
            return;
        }
        final OAuth2AccessToken accessToken = tokenStore.getAccessToken(SecurityUtils.getAuthentication());
        if (accessToken == null) {
            return;
        }
        tokenStore.removeAccessToken(accessToken);
        final OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(accessToken.getValue());
        if (refreshToken != null) {
            tokenStore.removeRefreshToken(refreshToken);
        }
    }

    /**
     * 登录
     * 支持 POST 流传输方式
     *
     * @param request request
     * @return 重定向到 OAUTH 端点
     * @throws IOException ioexception
     */
    @PostMapping("/access_token")
    public RedirectView login(HttpServletRequest request) throws IOException {
        final ServletInputStream inputStream = request.getInputStream();
        final String string = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        JSONObject object = JSON.parseObject(string);
        RedirectView view = new RedirectView("/oauth/token");
        view.addStaticAttribute("grant_type", object.getString("grantType"));
        view.addStaticAttribute("scope", object.getString("scope"));
        view.addStaticAttribute("username", object.getString("username"));
        view.addStaticAttribute("password", object.getString("password"));
        view.addStaticAttribute("client_id", object.getString("clientId"));
        view.addStaticAttribute("client_secret", object.getString("clientSecret"));
        return view;
    }

    /**
     * 用户信息
     *
     * @param principal 主要
     * @return {@link Result}<{@link Object}>
     */
    @ResponseBody
    @GetMapping("/info")
    public Result<Object> userInfo(Principal principal) {
        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) principal;
        return Result.success(oAuth2Authentication.getPrincipal());
    }

    /**
     * 用户
     *
     * @param principal 主要
     * @return {@link Principal}
     */
    @ResponseBody
    @GetMapping("/users")
    public Principal users(Principal principal) {
        return principal;
    }

    /**
     * 更改密码
     *
     * @param dto dto
     */
    @ResponseBody
    @PutMapping("/change_password")
    public void changePassword(@Validated @RequestBody ChangePasswordDTO dto) {
        if (!StringUtils.equals(dto.getPassword(), dto.getConfirmPassword())) {
            throw CheckedException.badRequest("新密码与确认密码不一致");
        }
        final Long userId = tenantEnvironment.userId();
        this.userService.changePassword(userId, dto.getOriginalPassword(), dto.getPassword());
    }

    /**
     * @author wenxina
     */
    @EqualsAndHashCode(callSuper = true)
    public static class SwaggerEnhancer<T> extends Result<T> {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty(value = "access_token")
        private String accessToken;

        public SwaggerEnhancer(int code, T data, String message) {
            super(code, data, message);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
