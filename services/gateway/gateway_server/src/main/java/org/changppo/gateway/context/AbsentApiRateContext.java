package org.changppo.gateway.context;

/**
 * API Key가 존재하지 않는 경우의 Rate Context
 */
public final record AbsentApiRateContext(

) implements ApiRateContext {
}
