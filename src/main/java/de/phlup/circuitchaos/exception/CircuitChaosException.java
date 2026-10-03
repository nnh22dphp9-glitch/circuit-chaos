package de.phlup.circuitchaos.exception;

public class CircuitChaosException extends RuntimeException {

    public CircuitChaosException(String msg) {
        super(msg);
    }

    public CircuitChaosException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
