package com.lcyl.framework.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.SentinelWebInterceptor;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.config.SentinelWebMvcConfig;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流配置
 */
@Configuration
public class SentinelConfig implements WebMvcConfigurer {

    /**
     * 初始化限流规则（应用启动时自动加载）
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 1. AI 智能助手接口 — QPS 3
        FlowRule aiRule = new FlowRule();
        aiRule.setResource("/wxLogin/ai/**");
        aiRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        aiRule.setCount(3);
        rules.add(aiRule);

        // 2. 小程序通用接口 — QPS 50
        FlowRule wxRule = new FlowRule();
        wxRule.setResource("/wxLogin/**");
        wxRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        wxRule.setCount(50);
        rules.add(wxRule);

        // 3. 登录接口 — QPS 5
        FlowRule loginRule = new FlowRule();
        loginRule.setResource("/login");
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(5);
        rules.add(loginRule);

        FlowRuleManager.loadRules(rules);
    }

    /**
     * 注册 Sentinel 拦截器（对 /wxLogin/** 和 /login 生效）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SentinelWebMvcConfig config = new SentinelWebMvcConfig();
        config.setBlockExceptionHandler(blockExceptionHandler());
        config.setOriginParser(requestOriginParser());

        registry.addInterceptor(new SentinelWebInterceptor(config))
                .addPathPatterns("/wxLogin/**", "/login");
    }

    /**
     * 自定义限流后的返回信息
     */
    @Bean
    public BlockExceptionHandler blockExceptionHandler() {
        return new BlockExceptionHandler() {
            @Override
            public void handle(HttpServletRequest request, javax.servlet.http.HttpServletResponse response,
                               com.alibaba.csp.sentinel.slots.block.BlockException e) throws Exception {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(429);
                response.getWriter().write("{\"code\":429,\"msg\":\"请求太频繁，请稍后再试\"}");
            }
        };
    }

    /**
     * 按用户标识区分限流
     */
    @Bean
    public RequestOriginParser requestOriginParser() {
        return new RequestOriginParser() {
            @Override
            public String parseOrigin(HttpServletRequest request) {
                String token = request.getHeader("authorization");
                if (token != null && token.length() > 10) {
                    return token.substring(token.length() - 10);
                }
                return request.getRemoteAddr();
            }
        };
    }
}
