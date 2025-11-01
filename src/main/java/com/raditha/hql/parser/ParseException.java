package com.raditha.hql.parser;

/**
 * Exception thrown when parsing HQL/JPQL queries fails.
 */
public class ParseException extends Exception {
    
    public ParseException(String message) {
        super(message);
    }
    
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
