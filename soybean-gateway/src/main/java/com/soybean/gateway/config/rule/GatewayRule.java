package com.soybean.gateway.config.rule;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.soybean.gateway.controller.domain.BlacklistRule;
import com.soybean.gateway.controller.domain.CommonRule;
import com.soybean.gateway.controller.domain.LimitRule;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 网关规则
 *
 * @author wenxina
 * @date 2022/03/22
 */
public interface GatewayRule<T> {

    AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /**
     * //（1）? 匹配一个字符（除过操作系统默认的文件分隔符）
     * //（2）* 匹配0个或多个字符
     * //（3）**匹配0个或多个目录
     * //（4）{spring:[a-z]+} 将正则表达式[a-z]+匹配到的值,赋值给名为 spring 的路径变量.
     * //    (PS:必须是完全匹配才行,在SpringMVC中只有完全匹配才会进入controller层的方法)
     *
     * @param stringRedisTemplate redis
     * @param request             request
     * @param gatewayRule         枚举
     * @return CommonRule
     */
    default T getByPath(StringRedisTemplate stringRedisTemplate, ServerHttpRequest request, GatewayRuleEnum gatewayRule) {
        final Set<Object> keys = stringRedisTemplate.opsForHash().keys(gatewayRule.hashKey);
        if (CollUtil.isEmpty(keys)) {
            return null;
        }
        final String path = request.getURI().getPath();
        final HttpMethod httpMethod = request.getMethod();
        final List<Object> objects = stringRedisTemplate.opsForHash().multiGet(gatewayRule.hashKey, keys);
        if (CollUtil.isEmpty(objects)) {
            return null;
        }
        for (Object object : objects) {
            CommonRule rule = JSON.parseObject(object.toString(), CommonRule.class);
            if (!rule.getStatus()) {
                continue;
            }
            if (ObjectUtils.allNotNull(rule.getStartTime(), rule.getEndTime())) {
                final LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(rule.getStartTime()) || now.isAfter(rule.getEndTime())) {
                    continue;
                }
            }
            final boolean match = ANT_PATH_MATCHER.match(rule.getPath(), path);
            final boolean methodFilter = StringUtils.equals(rule.getMethod(), "ALL") || StringUtils.equalsIgnoreCase(rule.getMethod(), Objects.requireNonNull(httpMethod).name());
            if (match && methodFilter) {
                return JSON.parseObject(object.toString(), (Type) gatewayRule.clazz);
            }
        }
        return null;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    enum GatewayRuleEnum {
        /**
         * 限流
         */
        RULE_LIMIT("gateway:rule:limit", "gateway:rule:limit:visits", LimitRule.class),
        RULE_BLACKLIST("gateway:rule:blacklist", "gateway:blacklist:visits", BlacklistRule.class),

        ;
        private String hashKey;
        private String visitsKey;
        private Class<?> clazz;

        public String hashKey() {
            return hashKey;
        }

        public String visitsKey() {
            return visitsKey;
        }

        public Class<?> clazz() {
            return clazz;
        }
    }

    interface Constants {
        String GATEWAY_RULE_ROUTE = "gateway:rule:route";
        String DEFAULT_RULE_LIMIT_TOTAL = "gateway:rule:limit:total";
        int GLOBAL_RANGE = 0;
        int IP_RANGE = 1;
    }

}
