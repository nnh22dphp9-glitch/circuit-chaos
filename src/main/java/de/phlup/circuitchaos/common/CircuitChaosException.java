package de.phlup.circuitchaos.common;

public class CircuitChaosException extends RuntimeException {

    public CircuitChaosException(String msg) {
        super(msg);
    }

    public CircuitChaosException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
