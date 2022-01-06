package com.modusbox.client.router;

import com.modusbox.client.processor.CheckCBSError;
import com.modusbox.client.processor.CustomErrorProcessor;
import org.apache.camel.builder.RouteBuilder;

public final class CustomErrorRouter extends RouteBuilder {
    private CustomErrorProcessor customErrorProcessor = new CustomErrorProcessor();
    private CheckCBSError checkCBSError = new CheckCBSError();

    public void configure() {

        from("direct:extractCustomErrors")
            .process(customErrorProcessor)
        ;

        from("direct:catchCBSError")
//                .process(exchange -> System.out.println())
                .process(checkCBSError)
        ;
    }
}
