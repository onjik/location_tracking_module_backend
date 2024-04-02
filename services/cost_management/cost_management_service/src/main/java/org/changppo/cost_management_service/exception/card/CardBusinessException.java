package org.changppo.cost_management_service.exception.card;

import org.changppo.cost_management_service.exception.common.BusinessException;
import org.changppo.cost_management_service.exception.common.ExceptionType;

public class CardBusinessException extends BusinessException {

    public CardBusinessException(ExceptionType exceptionType, Throwable cause) {
        super(exceptionType, cause);
    }

    public CardBusinessException(ExceptionType exceptionType) {
        super(exceptionType);
    }
}
