/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.transform.Source;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.MappedInterceptors;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Provides default Spring MVC configuration and registers Spring MVC infrastructure components expected by the 
 * {@link DispatcherServlet}. Also provides support for configuration options equivalent to those of the 
 * Spring MVC XML namespace. See @{@link EnableMvcConfiguration} for details on how to enable and customize the 
 * configuration provided by this class.
 * 
 * <p>Many of the methods in this class delegate to a list of one or more {@link MvcConfigurer}s detected by 
 * type in the Spring context giving each an opportunity to customize the configuration - e.g. register 
 * additional converters, add message converters, configure resource handling, and so on.  
 *
 * <p>Registers the following Spring beans:
 * <ul>
 * 	<li>A {@link RequestMappingHandlerMethodMapping} ordered at 0 for mapping requests to annotated controller methods.
 * 	<li>A {@link RequestMappingHandlerMethodAdapter} for processing requests using annotated controller methods. 
 * 	<li>A {@link HttpRequestHandlerAdapter} for processing requests with {@link HttpRequestHandler}s.
 * 	<li>A {@link SimpleControllerHandlerAdapter} for processing requests with interface-based {@link Controller}s.
 * 	<li>A {@link FormattingConversionService} for use with annotated controller methods and the spring:eval JSP tag.
 * 	<li>A {@link Validator} for validating model attributes on annotated controller methods.
 * 	<li>An {@link HandlerExceptionResolverComposite} with a list of the following exception resolvers: 
 * 		<ul>
 * 			<li>{@link RequestMappingHandlerMethodExceptionResolver},
 * 			<li>{@link ResponseStatusExceptionResolver}
 * 			<li>{@link DefaultHandlerExceptionResolver}.
 * 		</ul>
 * 	<li>A {@link MappedInterceptors} with a list of Spring MVC lifecycle interceptors 
 * 	<li>A {@link SimpleUrlHandlerMapping} with view controller mappings ordered at 1.
 * 	<li>A {@link SimpleUrlHandlerMapping} with static resource request mappings ordered at the lowest precedence -1.
 * 	<li>A {@link SimpleUrlHandlerMapping} with a {@link DefaultServletHttpRequestHandler} ordered at the lowest precedence.
 * </ul>
 * 
 * @see EnableMvcConfiguration
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@Configuration
class MvcConfiguration implements ApplicationContextAware, ServletContextAware {

	private final MvcConfigurerComposite configurers = new MvcConfigurerComposite();

	private ServletContext servletContext;

	private ApplicationContext applicationContext;

	@Autowired(required = false)
	public void setConfigurers(List<MvcConfigurer> configurers) {
		this.configurers.addConfigurers(configurers);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	RequestMappingHandlerMethodMapping annotationHandlerMapping() {
		RequestMappingHandlerMethodMapping mapping = new RequestMappingHandlerMethodMapping();
		mapping.setOrder(0);
		return mapping;
	}

	@Bean
	RequestMappingHandlerMethodAdapter annotationHandlerAdapter() {
		RequestMappingHandlerMethodAdapter adapter = new RequestMappingHandlerMethodAdapter();

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(conversionService());
		bindingInitializer.setValidator(validator());
		adapter.setWebBindingInitializer(bindingInitializer);

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
		configurers.addCustomArgumentResolvers(argumentResolvers);
		adapter.setCustomArgumentResolvers(argumentResolvers);

		List<HttpMessageConverter<?>> converters = getDefaultHttpMessageConverters();
		configurers.configureMessageConverters(converters);
		adapter.setMessageConverters(converters);

		return adapter;
	}

	@Bean(name="mvcConversionService")
	FormattingConversionService conversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		configurers.registerFormatters(conversionService);
		return conversionService;
	}

	@Bean(name="mvcValidator")
	Validator validator() {
		Validator validator = configurers.getCustomValidator();
		if (validator != null) {
			return validator;
		}
		else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
			LocalValidatorFactoryBean jsr303Validator = new LocalValidatorFactoryBean();
			configurers.configureValidator(jsr303Validator);
			return jsr303Validator;
		} 
		else {
			return new Validator() {
				public void validate(Object target, Errors errors) {
				}

				public boolean supports(Class<?> clazz) {
					return false;
				}
			};
		}
	}

	private List<HttpMessageConverter<?>> getDefaultHttpMessageConverters() {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(stringConverter);
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new SourceHttpMessageConverter<Source>());
		converters.add(new XmlAwareFormHttpMessageConverter());

		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent("javax.xml.bind.Binder", classLoader)) {
			converters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader)) {
			converters.add(new MappingJacksonHttpMessageConverter());
		}
		if (ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", classLoader)) {
			converters.add(new AtomFeedHttpMessageConverter());
			converters.add(new RssChannelHttpMessageConverter());
		}

		return converters;
	}

	@Bean
	HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	@Bean
	SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	@Bean
	HandlerExceptionResolver handlerExceptionResolver() throws Exception {
		List<HandlerExceptionResolver> resolvers = new ArrayList<HandlerExceptionResolver>();
		resolvers.add(annotationHandlerExceptionResolver());
		resolvers.add(new ResponseStatusExceptionResolver());
		resolvers.add(new DefaultHandlerExceptionResolver());
		configurers.configureHandlerExceptionResolvers(resolvers);

		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(resolvers);
		return composite;
	}

	private HandlerExceptionResolver annotationHandlerExceptionResolver() throws Exception {
		RequestMappingHandlerMethodExceptionResolver resolver = new RequestMappingHandlerMethodExceptionResolver();

		List<HttpMessageConverter<?>> converters = getDefaultHttpMessageConverters();
		configurers.configureMessageConverters(converters);
		resolver.setMessageConverters(converters);
		resolver.setOrder(0);

		resolver.afterPropertiesSet();
		return resolver;
	}

	@Bean
	MappedInterceptors mappedInterceptors() {
		InterceptorConfigurer configurer = new InterceptorConfigurer();
		configurer.addInterceptor(new ConversionServiceExposingInterceptor(conversionService()));
		configurers.addInterceptors(configurer);
		return configurer.getMappedInterceptors();
	}

	@Bean
	HandlerMapping viewControllerHandlerMapping() {
		ViewControllerConfigurer configurer = new ViewControllerConfigurer();
		configurer.setOrder(annotationHandlerMapping().getOrder() + 1);
		configurers.addViewControllers(configurer);
		return configurer.getHandlerMapping();
	}

	@Bean
	HandlerMapping resourceHandlerMapping() {
		ResourceConfigurer configurer = new ResourceConfigurer(applicationContext, servletContext);
		configurers.configureResourceHandling(configurer);
		return configurer.getHandlerMapping();
	}

	@Bean
	HandlerMapping defaultServletHandlerMapping() {
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(servletContext);
		configurers.configureDefaultServletHandling(configurer);
		return configurer.getHandlerMapping();
	}

}