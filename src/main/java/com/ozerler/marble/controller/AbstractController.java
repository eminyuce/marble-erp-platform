package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base abstract controller providing standardized BackEndResponse builders
 * aligned with enterprise controller conventions.
 */
public abstract class AbstractController {

    protected BackEndResponse buildFatalResponse(BackEndResponse ber, ServiceStatus serviceStatus, Status status,
                                                String action, String code) {
        if (ber == null) {
            ber = new BackEndResponse();
        }
        if (serviceStatus == null) {
            serviceStatus = new ServiceStatus();
        }
        if (status == null) {
            status = new Status();
        }
        serviceStatus.setHttpStatus(HttpStatus.BAD_REQUEST);
        status.setMessage("A serious error occurred in " + action);
        status.setErrorCode(code != null ? code : Constants.ERR_FATAL);
        status.addError("Request failed");
        serviceStatus.setStatus(status);
        ber.setServiceStatus(serviceStatus);
        return ber;
    }

    protected BackEndResponse buildFailureResponse(BackEndResponse ber, ServiceStatus serviceStatus, Status status,
                                                  String action, String code, HttpStatus httpStatus) {
        if (ber == null) {
            ber = new BackEndResponse();
        }
        if (serviceStatus == null) {
            serviceStatus = new ServiceStatus();
        }
        if (status == null) {
            status = new Status();
        }
        serviceStatus.setHttpStatus(httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST);
        status.setMessage("A serious error occurred in " + action);
        status.setErrorCode(code != null ? code : Constants.ERR_BAD_REQUEST);
        status.addError("Request failed");
        serviceStatus.setStatus(status);
        ber.setServiceStatus(serviceStatus);
        return ber;
    }

    protected BackEndResponse buildSuccessResponse(BackEndResponse ber, ServiceStatus serviceStatus, Status status,
                                                  Object data, String message) {
        if (ber == null) {
            ber = new BackEndResponse();
        }
        if (serviceStatus == null) {
            serviceStatus = new ServiceStatus();
        }
        if (status == null) {
            status = new Status();
        }
        status.setErrorCode(Constants.NO_ERR);
        status.setMessage(message != null ? message : "Operation successful");
        serviceStatus.setHttpStatus(HttpStatus.OK);
        serviceStatus.setStatus(status);

        HttpHeaders responseHeaders = new HttpHeaders();
        ResponseEntity<?> resp = new ResponseEntity<>(data, responseHeaders, HttpStatus.OK);

        ber.setResponse(resp);
        ber.setServiceStatus(serviceStatus);
        return ber;
    }

    protected BackEndResponse buildSuccessResponse(Object data, String message) {
        return buildSuccessResponse(new BackEndResponse(), new ServiceStatus(), new Status(), data, message);
    }
}
