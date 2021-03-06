package com.soybean.framework.security.feign;

import com.soybean.framework.commons.util.StringUtils;
import com.soybean.framework.security.feign.properties.InnerServiceProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Objects;

/**
 * 扩展 OAuth2FeignRequestInterceptor
 *
 * @author wenxina
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty("security.oauth2.client.client-id")
public class HeaderFeignClientInterceptor implements RequestInterceptor {

    private static final String HEADER_CLIENT_ID = "client_id";
    private static final String AUTHORIZATION = "authorization";
    private final InnerServiceProperties innerServiceProperties;
    @Value("${security.oauth2.client.client-id:'none'}")
    private String clientId;

    /**
     * 1. 如果使用 非web 请求，header 区别
     * 2. 根据authentication 还原请求token
     *
     * @param template template
     */
    @Override
    public void apply(RequestTemplate template) {
        template.header(HEADER_CLIENT_ID, clientId);
        log.info("[透传的 clientId ] - [{}]", clientId);
        if (StringUtils.isNotBlank(innerServiceProperties.getHeader())) {
            Collection<String> fromHeader = template.headers().get(innerServiceProperties.getHeader());
            if (!CollectionUtils.isEmpty(fromHeader) && fromHeader.contains(innerServiceProperties.getHeaderValue())) {
                return;
            }
        }
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(attributes)).getRequest();
            final String authorization = request.getHeader(AUTHORIZATION);
            log.info("[透传的 authorization ] - [{}]", authorization);
            if (StringUtils.isNotBlank(authorization)) {
                template.header(AUTHORIZATION, authorization);
            }
        }
    }
}
