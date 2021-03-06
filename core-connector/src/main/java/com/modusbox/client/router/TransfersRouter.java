package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.EncodeAuthHeader;
import com.modusbox.client.processor.TrimMFICode;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.model.dataformat.JsonLibrary;


public class TransfersRouter extends RouteBuilder {

    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    private static final String TIMER_NAME_POST = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";

    public static final Counter reqCounterPost = Counter.build()
            .name("counter_post_transfers_requests_total")
            .help("Total requests for POST /transfers.")
            .register();

    private static final Histogram reqLatencyPost = Histogram.build()
            .name("histogram_post_transfers_request_latency")
            .help("Request latency in seconds for POST /transfers.")
            .register();

    public static final Counter reqCounterPut = Counter.build()
            .name("counter_put_transfers_requests_total")
            .help("Total requests for PUT /transfers.")
            .register();

    private static final Histogram reqLatencyPut = Histogram.build()
            .name("histogram_put_transfers_request_latency")
            .help("Request latency in seconds for PUT /transfers.")
            .register();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:postTransfers").routeId("com.modusbox.postTransfers").doTry()
                .process(exchange -> {
                    reqCounterPost.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_POST, reqLatencyPost.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Request received POST /transfers', " +
                        "'Tracking the request', " +
                        "'Call the BCX API,  Track the response', " +
                        "'fspiop-source: ${header.fspiop-source} Input Payload: ${body}')") // default logger
                /*
                 * BEGIN processing
                 */
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/postTransactionRequestPre.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))

                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Calling BCX backend API, payment', " +
                        "'Tracking the request', " +
                        "'Track the response', " +
                        "'Request sent to, POST http://172.25.29.22:19996/api/payment/process/payment Payload: ${body}')")
                .toD("http://172.25.29.22:19996/api/payment/process/payment")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Response from BCX backend API, payment, postTransaction: ${body}', " +
                        "'Tracking the response', " +
                        "'Verify the response', null)")
//                .process(exchange -> System.out.println())
                .choice()
                  .when(simple("${body['responseCode']} != '00' && ${body['responseCode']} != '09' && ${body['responseCode']} != '10'"))
                    .to("direct:catchCBSError")
                .endDoTry()

                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant("{\"homeTransactionId\": \"1234\"}"))
                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Response for POST /transfers', " +
                        "'Tracking the response', " +
                        "null, " +
                        "'Output Payload: ${body}')") // default logger
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_POST)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:putTransfersByTransferId").routeId("com.modusbox.putTransfersByTransferId").doTry()
                .process(exchange -> {
                    reqCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, reqLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Request received PUT /transfers/${header.transferId}', " +
                        "'Tracking the request', " +
                        "'Call the BCX API,  Track the response', " +
                        "'fspiop-source: ${header.fspiop-source} Input Payload: ${body}')") // default logger
                /*
                 * BEGIN processing
                 */
//                .marshal().json()
//                .transform(datasonnet("resource:classpath:mappings/postTransactionRequest.ds"))
//                .setBody(simple("${body.content}"))
//                .marshal().json()
//
//                .removeHeaders("CamelHttp*")
//                .setHeader("Content-Type", constant("application/json"))
//                .setHeader("Accept", constant("application/json"))
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//
//                .to("bean:customJsonMessage?method=logJsonMessage(" +
//                        "'info', " +
//                        "${header.X-CorrelationId}, " +
//                        "'Calling BCX backend API, payment', " +
//                        "'Tracking the request', " +
//                        "'Track the response', " +
//                        "'Request sent to, POST http://172.25.29.22:19996/api/payment/process/payment Payload: ${body}')")
//                .toD("http://172.25.29.22:19996/api/payment/process/payment")
//                .unmarshal().json(JsonLibrary.Gson)
//                .to("bean:customJsonMessage?method=logJsonMessage(" +
//                        "'info', " +
//                        "${header.X-CorrelationId}, " +
//                        "'Response from BCX backend API, collect, TRANSFER action, postTransaction: ${body}', " +
//                        "'Tracking the response', " +
//                        "'Verify the response', null)")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant(""))
                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Response for PUT /transfers/${header.transferId}', " +
                        "'Tracking the response', " +
                        "null, " +
                        "'Output Payload: ${body}')") // default logger
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

    }
}
