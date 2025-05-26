package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;

/**
 * 合并后的WebMvc配置类
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code", // 排除登录验证码接口
                        "/user/login", // 排除登录接口
                        "/blog/hot", // 排除热门文章接口
                        "/shop/**", // 排除店铺相关接口
                        "/shop-type/**",  // 排除店铺类型相关接口
                        "/upload/**", // 排除上传相关接口
                        "/voucher/**", // 排除优惠券相关接口
                        "/voucher-order/**", // 排除优惠券订单相关接口
                        "/doc.html", // 排除 Swagger 文档页面
                        "/webjars/**", // 排除 Swagger 静态资源
                        "/swagger-resources/**", // 排除 Swagger 资源
                        "/v2/api-docs", // 排除 Swagger API 文档
                        "/v3/api-docs", // 排除 OpenAPI 3 文档
                        "/swagger-ui/**", // 排除 Swagger UI 页面
                        "/csrf" // 排除 CSRF 相关路径（如果使用）
                ).order(1);

        // 刷新token拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0); // 拦截所有请求
    }

    /**
     * 配置Swagger
     * @return
     */
    @Bean
    public Docket docket() {
        log.info("准备生成接口文档....");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("黑马点评项目接口文档")
                .version("2.0")
                .description("黑马点评项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.hmdp.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}