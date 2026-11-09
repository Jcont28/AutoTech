package hn.autotech.exception;

public class AccesoDatosException extends RuntimeException {
    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
