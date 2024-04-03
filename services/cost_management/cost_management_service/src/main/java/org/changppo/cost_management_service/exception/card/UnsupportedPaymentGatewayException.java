package org.changppo.cost_management_service.exception.card;

import org.changppo.cost_management_service.exception.common.ExceptionType;

public class UnsupportedPaymentGatewayException extends CardBusinessException {
    public UnsupportedPaymentGatewayException() {
        super(ExceptionType.UNSUPPORTED_PAYMENT_GATEWAY_EXCEPTION);
    }
}
