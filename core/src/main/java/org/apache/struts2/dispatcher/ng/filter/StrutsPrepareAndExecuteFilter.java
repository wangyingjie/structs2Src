/*
 * $Id: DefaultActionSupport.java 651946 2008-04-27 13:41:38Z apetrelli $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.dispatcher.ng.filter;

import org.apache.struts2.StrutsStatics;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.dispatcher.ng.ExecuteOperations;
import org.apache.struts2.dispatcher.ng.InitOperations;
import org.apache.struts2.dispatcher.ng.PrepareOperations;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles both the preparation and execution phases of the Struts dispatching process.  This filter is better to use
 * when you don't have another filter that needs access to action context information, such as Sitemesh.
 *
 * 该类是配置在 web.xml 中的 structs2 的核心过滤器
 *
 * struts2 请求预处理 and 执行业务逻辑的执行入口类
 *
 */
public class StrutsPrepareAndExecuteFilter implements StrutsStatics, Filter {

    // struts2 进行 Http 请求预处理的操作集合
    protected PrepareOperations prepare;

    // struts2 进行 Http 请求逻辑处理的操作集合
    protected ExecuteOperations execute;

    //排除在 Struts2 处理之外的 url 模式
    protected List<Pattern> excludedPatterns = null;

    public void init(FilterConfig filterConfig) throws ServletException {

        //初始化操作类
        InitOperations init = new InitOperations();

        //struts2 的核心分发器
        Dispatcher dispatcher = null;
        try {
            FilterHostConfig config = new FilterHostConfig(filterConfig);
            init.initLogging(config);

            //核心分发器的初始化
            dispatcher = init.initDispatcher(config);

            // 初始化静态资源加载器
            init.initStaticContentLoader(config, dispatcher);

            //Http 预处理操作类
            prepare = new PrepareOperations(dispatcher);

            //Http 请求处理的逻辑操作类
            execute = new ExecuteOperations(dispatcher);

            //
            this.excludedPatterns = init.buildExcludedPatternsList(dispatcher);

            // 扩展方法
            postInit(dispatcher, filterConfig);
        } finally {
            if (dispatcher != null) {
                dispatcher.cleanUpAfterInit();
            }
            init.cleanup();
        }
    }

    /**
     * Callback for post initialization
     */
    protected void postInit(Dispatcher dispatcher, FilterConfig filterConfig) {
    }

    /**
     *  structs2 的 执行入口，以Filter为切入点
     *
     * @param req
     * @param res
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        try {
            if (excludedPatterns != null && prepare.isUrlExcluded(request, excludedPatterns)) {

                //对过滤掉的url进行处理
                chain.doFilter(request, response);
            } else {
                // 设置 encoding、locale
                prepare.setEncodingAndLocale(request, response);

                // 创建ActionContext
                prepare.createActionContext(request, response);

                // todo 把核心分发器 Dispatcher 分配给当前线程
                prepare.assignDispatcherToThread();

                // 利用装饰模式对request进行包装，将其转化为了：StrutsRequestWrapper or MultiPartRequestWrapper
                request = prepare.wrapRequest(request);

                // 根据request请求查找 ActionMapping
                ActionMapping mapping = prepare.findActionMapping(request, response, true);

                // 通过request ----> ActionMapping ----> Dispatcher
                if (mapping == null) {

                    // 没找到ActionMapping 判断是否需要将请求处理为静态资源
                    boolean handled = execute.executeStaticResourceRequest(request, response);
                    if (!handled) {
                        chain.doFilter(request, response);
                    }
                } else {
                    //真正执行Action的地方
                    execute.executeAction(request, response, mapping);
                }
            }
        } finally {
            // 这里面包含了 ActionContext 的销毁过程， ActionContext 横跨了整个XWork控制流执行周期
            prepare.cleanupRequest(request);
        }
    }

    //清理分发器
    public void destroy() {
        prepare.cleanupDispatcher();
    }

}
