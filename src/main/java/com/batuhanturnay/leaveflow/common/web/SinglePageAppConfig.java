package com.batuhanturnay.leaveflow.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The production image puts the React build into {@code static/}, so one container serves the
 * client and the API from the same origin and no CORS configuration is needed.
 * <p>
 * The client routes its own pages. A reload or a shared link on such a page reaches the
 * server, which answers with {@code index.html} so the client can take over.
 * {@code /} already gets {@code index.html} as Spring Boot's welcome page.
 */
@Configuration(proxyBeanMethods = false)
public class SinglePageAppConfig implements WebMvcConfigurer {

	/**
	 * Client-side routes; keep in step with {@code frontend/src/App.tsx}. The security
	 * configuration makes the same paths public (they only ever return {@code index.html}).
	 */
	public static final String[] CLIENT_ROUTES = { "/login", "/approvals", "/people", "/people/**", "/holidays",
			"/reports" };

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		for (String route : CLIENT_ROUTES) {
			registry.addViewController(route).setViewName("forward:/index.html");
		}
	}

}
