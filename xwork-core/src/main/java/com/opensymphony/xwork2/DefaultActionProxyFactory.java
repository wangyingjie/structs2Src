/*
 * Copyright 2002-2007,2009 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensymphony.xwork2;

import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.Inject;

import java.util.Map;


/**
 * Default factory for {@link com.opensymphony.xwork2.ActionProxyFactory}.
 *
 * @author Jason Carreira
 */
public class DefaultActionProxyFactory implements ActionProxyFactory {

    protected Container container;
    
    public DefaultActionProxyFactory() {
        super();
    }

    //注入容器
    @Inject
    public void setContainer(Container container) {
        this.container = container;
    }
    
    public ActionProxy createActionProxy(String namespace, String actionName, Map<String, Object> extraContext) {
        return createActionProxy(namespace, actionName, null, extraContext, true, true);
    }

    public ActionProxy createActionProxy(String namespace, String actionName, String methodName, Map<String, Object> extraContext) {
        return createActionProxy(namespace, actionName, methodName, extraContext, true, true);
    }

    public ActionProxy createActionProxy(String namespace, String actionName, Map<String, Object> extraContext, boolean executeResult, boolean cleanupContext) {
        return createActionProxy(namespace, actionName, null, extraContext, executeResult, cleanupContext);
    }

    // 构建 ActionProxy 对象
    public ActionProxy createActionProxy(String namespace, String actionName, String methodName, Map<String, Object> extraContext, boolean executeResult, boolean cleanupContext) {
        
        ActionInvocation inv = createActionInvocation(extraContext, true);

        // 注入
        container.inject(inv);

        //
        return createActionProxy(inv, namespace, actionName, methodName, executeResult, cleanupContext);
    }
    
    protected ActionInvocation createActionInvocation(Map<String, Object> extraContext, boolean pushAction) {
        return new DefaultActionInvocation(extraContext, pushAction);
    }

    public ActionProxy createActionProxy(ActionInvocation inv, String namespace, String actionName, boolean executeResult, boolean cleanupContext) {
        
        return createActionProxy(inv, namespace, actionName, null, executeResult, cleanupContext);
    }

    public ActionProxy createActionProxy(ActionInvocation inv, String namespace, String actionName, String methodName, boolean executeResult, boolean cleanupContext) {

        //默认实现  DefaultActionProxy
        DefaultActionProxy proxy = new DefaultActionProxy(inv, namespace, actionName, methodName, executeResult, cleanupContext);

        //注入
        container.inject(proxy);

        // 预处理、初始化
        proxy.prepare();

        return proxy;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
