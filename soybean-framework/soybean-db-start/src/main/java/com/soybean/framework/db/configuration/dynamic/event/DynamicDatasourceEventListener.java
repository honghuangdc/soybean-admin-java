package com.soybean.framework.db.configuration.dynamic.event;

import com.soybean.framework.db.configuration.dynamic.TenantDynamicDataSourceProcess;
import com.soybean.framework.db.configuration.dynamic.event.body.EventAction;
import com.soybean.framework.db.configuration.dynamic.event.body.TenantDynamicDatasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

import java.util.Objects;

/**
 * @author wenxina
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicDatasourceEventListener implements ApplicationListener<DynamicDatasourceEvent> {

    private final TenantDynamicDataSourceProcess tenantDynamicDataSourceProcess;

    @Override
    public void onApplicationEvent(DynamicDatasourceEvent event) {
        final TenantDynamicDatasource dynamicDatasource = event.getDynamicDatasource();
        if (Objects.isNull(event.getDynamicDatasource())) {
            log.warn("event dynamicDatasource is null....");
            return;
        }
        log.info("接收租户事件消息: - {} - {}", event.getAction(), dynamicDatasource);
        tenantDynamicDataSourceProcess.handler(EventAction.of(event.getAction()), dynamicDatasource);
    }
}