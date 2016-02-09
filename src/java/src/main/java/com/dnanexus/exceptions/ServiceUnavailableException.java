package com.dnanexus.exceptions;

/**
 * Exception used to indicate that the request yielded 503 Service Unavailable and suggested that we
 * retry at some point in the future.
 */
@SuppressWarnings("serial")
public class ServiceUnavailableException extends RuntimeException {
    public int secondsToWaitForRetry;

    public ServiceUnavailableException(int secondsToWaitForRetry) {
        this.secondsToWaitForRetry = secondsToWaitForRetry;
    }
}
