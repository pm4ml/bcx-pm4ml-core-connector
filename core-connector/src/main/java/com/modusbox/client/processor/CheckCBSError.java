package com.modusbox.client.processor;

import com.google.gson.Gson;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

public class CheckCBSError implements Processor {

    public void process(Exchange exchange) throws Exception {
        Gson gson = new Gson();
        String s = gson.toJson(exchange.getIn().getBody(), LinkedHashMap.class);
        JSONObject respObject = new JSONObject(s);
        int errorCode = 0;
        int responseCode = 0;
        String responseMessage = "";

        try {
            errorCode = respObject.getInt("code");
            responseCode = respObject.getInt("responseCode");
            responseMessage = respObject.getString("responseMessage");
//          respObject.getString("message");
            if (errorCode == 200) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, responseCode + " - " + responseMessage));
            }
            else if (errorCode == 301) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Payee CBS failed due to Token Failure"));
            }
        } catch (JSONException e) {
            System.out.println("Problem extracting error code from CBS response occurred.");
        }

    }

}