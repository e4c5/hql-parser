package com.raditha.hql.converter;

/**
 * Exception thrown when HQL to SQL conversion fails.
 */
public class ConversionException extends Exception {
    
    public ConversionException(String message) {
        super(message);
    }
    
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
