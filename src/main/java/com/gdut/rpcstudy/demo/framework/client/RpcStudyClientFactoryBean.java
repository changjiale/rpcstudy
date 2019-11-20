package com.gdut.rpcstudy.demo.framework.client;

import com.gdut.rpcstudy.demo.framework.ProxyFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author: lele
 * @date: 2019/11/20 下午3:08
 * bean工厂类，这里为接口代理其方法
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class RpcStudyClientFactoryBean implements FactoryBean<Object> {
    private Class<?> type;
    @Override
    public Object getObject() throws Exception {
        return ProxyFactory.getProxy(this.type);
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }
}
