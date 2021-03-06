/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.http.dsl;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link MessageHandlerSpec} implementation for the {@link HttpRequestExecutingMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see HttpRequestExecutingMessageHandler
 */
public class HttpMessageHandlerSpec
		extends MessageHandlerSpec<HttpMessageHandlerSpec, HttpRequestExecutingMessageHandler>
		implements ComponentsRegistration {

	private final RestTemplate restTemplate;

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	private HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private boolean headerMapperExplicitlySet;

	HttpMessageHandlerSpec(URI uri, RestTemplate restTemplate) {
		this(new ValueExpression<>(uri), restTemplate);
	}

	HttpMessageHandlerSpec(String uri, RestTemplate restTemplate) {
		this(new LiteralExpression(uri), restTemplate);
	}

	HttpMessageHandlerSpec(Expression uriExpression, RestTemplate restTemplate) {
		this.target = new HttpRequestExecutingMessageHandler(uriExpression, restTemplate);
		this.target.setUriVariableExpressions(this.uriVariableExpressions);
		this.target.setHeaderMapper(this.headerMapper);
		this.restTemplate = restTemplate;
	}

	HttpMessageHandlerSpec expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		return this;
	}

	/**
	 * Specify whether the real URI should be encoded after <code>uriVariables</code>
	 * expanding and before send request via {@link RestTemplate}. The default value is <code>true</code>.
	 * @param encodeUri true if the URI should be encoded.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec encodeUri(boolean encodeUri) {
		this.target.setEncodeUri(encodeUri);
		return this;
	}

	/**
	 * Specify the SpEL {@link Expression} to determine {@link HttpMethod} at runtime.
	 * @param httpMethodExpression The method expression.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec httpMethodExpression(Expression httpMethodExpression) {
		this.target.setHttpMethodExpression(httpMethodExpression);
		return this;
	}

	/**
	 * Specify a {@link Function} to determine {@link HttpMethod} at runtime.
	 * @param httpMethodFunction The HTTP method {@link Function}.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> HttpMessageHandlerSpec httpMethodFunction(Function<Message<P>, ?> httpMethodFunction) {
		return httpMethodExpression(new FunctionExpression<>(httpMethodFunction));
	}

	/**
	 * Specify the {@link HttpMethod} for requests.
	 * The default method is {@code POST}.
	 * @param httpMethod the {@link HttpMethod} to use.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec httpMethod(HttpMethod httpMethod) {
		this.target.setHttpMethod(httpMethod);
		return this;
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body.
	 * Otherwise the Message instance itself is serialized.
	 * The default value is {@code true}.
	 * @param extractPayload true if the payload should be extracted.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to bytes.
	 * The default is {@code UTF-8}.
	 * @param charset The charset.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec charset(String charset) {
		this.target.setCharset(charset);
		return this;
	}

	/**
	 * Specify the expected response type for the REST request.
	 * @param expectedResponseType The expected type.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec expectedResponseType(Class<?> expectedResponseType) {
		this.target.setExpectedResponseType(expectedResponseType);
		return this;
	}

	/**
	 * Specify a {@link ParameterizedTypeReference} for the expected response type for the REST request.
	 * @param expectedResponseType The {@link ParameterizedTypeReference} for expected type.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec expectedResponseType(ParameterizedTypeReference<?> expectedResponseType) {
		return expectedResponseTypeExpression(new ValueExpression<ParameterizedTypeReference<?>>(expectedResponseType));
	}

	/**
	 * Specify a SpEL {@link Expression} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name.
	 * @param expectedResponseTypeExpression The expected response type expression.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec expectedResponseTypeExpression(Expression expectedResponseTypeExpression) {
		this.target.setExpectedResponseTypeExpression(expectedResponseTypeExpression);
		return this;
	}

	/**
	 * Specify a {@link Function} to determine the type for the expected response
	 * The returned value of the expression could be an instance of {@link Class} or
	 * {@link String} representing a fully qualified class name.
	 * @param expectedResponseTypeFunction The expected response type {@link Function}.
	 * @param <P> the payload type.
	 * @return the spec
	 */
	public <P> HttpMessageHandlerSpec expectedResponseTypeFunction(
			Function<Message<P>, ?> expectedResponseTypeFunction) {
		return expectedResponseTypeExpression(new FunctionExpression<>(expectedResponseTypeFunction));
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 * @param errorHandler The error handler.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec errorHandler(ResponseErrorHandler errorHandler) {
		Assert.isNull(this.restTemplate,
				"the 'errorHandler' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setErrorHandler(errorHandler);
		return this;
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 * @param messageConverters The message converters.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
		Assert.isNull(this.restTemplate,
				"the 'messageConverters' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setMessageConverters(Arrays.asList(messageConverters));
		return this;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @param requestFactory The request factory.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.isNull(this.restTemplate,
				"the 'requestFactory' must be specified on the provided 'restTemplate': " + this.restTemplate);
		this.target.setRequestFactory(requestFactory);
		return this;
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and {@code MessageHeaders}.
	 * @param headerMapper The header mapper.
	 * @return the spec
	 */
	public HttpMessageHandlerSpec headerMapper(HeaderMapper<HttpHeaders> headerMapper) {
		this.headerMapper = headerMapper;
		this.target.setHeaderMapper(this.headerMapper);
		this.headerMapperExplicitlySet = true;
		return this;
	}

	/**
	 * Provide the pattern array for request headers to map.
	 * @param patterns the patterns for request headers to map.
	 * @return the spec
	 * @see DefaultHttpHeaderMapper#setOutboundHeaderNames(String[])
	 */
	public HttpMessageHandlerSpec mappedRequestHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setOutboundHeaderNames(patterns);
		return this;
	}

	/**
	 * Provide the pattern array for response headers to map.
	 * @param patterns the patterns for response headers to map.
	 * @return the current Spec.
	 * @see DefaultHttpHeaderMapper#setInboundHeaderNames(String[])
	 */
	public HttpMessageHandlerSpec mappedResponseHeaders(String... patterns) {
		Assert.isTrue(!this.headerMapperExplicitlySet,
				"The 'mappedResponseHeaders' must be specified on the provided 'headerMapper': " + this.headerMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setInboundHeaderNames(patterns);
		return this;
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 * @param uriVariableExpressions The URI variable expressions.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public HttpMessageHandlerSpec uriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.uriVariableExpressions.clear();
		this.uriVariableExpressions.putAll(uriVariableExpressions);
		return this;
	}

	/**
	 * Specify a SpEL expression to evaluate a value for the uri template variable.
	 * @param variable the uri template variable.
	 * @param value the expression to evaluate value for te uri template variable.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public HttpMessageHandlerSpec uriVariable(String variable, Expression value) {
		this.uriVariableExpressions.put(variable, value);
		return this;
	}

	/**
	 * Specify a value for the uri template variable.
	 * @param variable the uri template variable.
	 * @param value the expression to evaluate value for te uri template variable.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public HttpMessageHandlerSpec uriVariable(String variable, String value) {
		return uriVariable(variable, PARSER.parseExpression(value));
	}

	/**
	 * Specify a {@link Function} to evaluate a value for the uri template variable.
	 * @param variable the uri template variable.
	 * @param valueFunction the {@link Function} to evaluate a value for the uri template variable.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariableExpressions(Map)
	 */
	public <P> HttpMessageHandlerSpec uriVariable(String variable, Function<Message<P>, ?> valueFunction) {
		return uriVariable(variable, new FunctionExpression<>(valueFunction));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesExpression to use.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public HttpMessageHandlerSpec uriVariablesExpression(String uriVariablesExpression) {
		return uriVariablesExpression(PARSER.parseExpression(uriVariablesExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesExpression to use.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public HttpMessageHandlerSpec uriVariablesExpression(Expression uriVariablesExpression) {
		this.target.setUriVariablesExpression(uriVariablesExpression);
		return this;
	}

	/**
	 * Specify a {@link Function} to evaluate a {@link Map} of URI variables at runtime against request message.
	 * @param uriVariablesFunction the {@link Function} to use.
	 * @param <P> the payload type.
	 * @return the current Spec.
	 * @see HttpRequestExecutingMessageHandler#setUriVariablesExpression(Expression)
	 */
	public <P> HttpMessageHandlerSpec uriVariablesFunction(Function<Message<P>, Map<String, ?>> uriVariablesFunction) {
		return uriVariablesExpression(new FunctionExpression<>(uriVariablesFunction));
	}

	/**
	 * Set to {@code true} if you wish {@code Set-Cookie} header in response to be
	 * transferred as {@code Cookie} header in subsequent interaction for a message.
	 * @param transferCookies the transferCookies to set.
	 * @return the current Spec.
	 */
	public HttpMessageHandlerSpec transferCookies(boolean transferCookies) {
		this.target.setTransferCookies(transferCookies);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.singletonList(this.headerMapper);
	}

}
