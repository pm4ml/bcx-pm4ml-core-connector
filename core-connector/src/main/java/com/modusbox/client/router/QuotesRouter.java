package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.model.dataformat.JsonLibrary;

public class QuotesRouter extends RouteBuilder {

	private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

	private static final String TIMER_NAME = "histogram_post_quoterequests_timer";

	public static final Counter reqCounter = Counter.build()
			.name("counter_post_quoterequests_requests_total")
			.help("Total requests for POST /quoterequests.")
			.register();

	private static final Histogram reqLatency = Histogram.build()
			.name("histogram_post_quoterequests_request_latency")
			.help("Request latency in seconds for POST /quoterequests.")
			.register();

    public void configure() {

		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:postQuoteRequests").routeId("com.modusbox.postQuoterequests").doTry()
				.process(exchange -> {
					reqCounter.inc(1); // increment Prometheus Counter metric
					exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
				})
				.to("bean:customJsonMessage?method=logJsonMessage(" +
						"'info', " +
						"${header.X-CorrelationId}, " +
						"'Request received POST /quoterequests', " +
						"'Tracking the request', " +
						"'Call the BCX API,  Track the response', " +
						"'fspiop-source: ${header.fspiop-source} Input Payload: ${body}')") // default logger
				/*
				 * BEGIN processing
				 */
				.setProperty("origPayload", simple("${body}"))
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))

				.marshal().json()
				.transform(datasonnet("resource:classpath:mappings/postQuoterequestsResponse.ds"))
				.setBody(simple("${body.content}"))
				/*
				 * END processing
				 */
				.to("bean:customJsonMessage?method=logJsonMessage(" +
						"'info', " +
						"${header.X-CorrelationId}, " +
						"'Response for POST /quoterequests', " +
						"'Tracking the response', " +
						"null, " +
						"'Output Payload: ${body}')") // default logger
				.doFinally().process(exchange -> {
					((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
				}).end()
		;

    }
}
