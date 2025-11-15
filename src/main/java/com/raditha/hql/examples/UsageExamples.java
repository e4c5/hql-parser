package com.raditha.hql.examples;

import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import com.raditha.hql.model.MetaData;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.converter.ConversionException;

/**
 * Example usage of the HQL Parser library.
 */
public class UsageExamples {
    
    public static void main(String[] args) throws ConversionException, ParseException {
        example1_BasicParsing();
//        example2_QueryAnalysis();
//        example3_PostgreSQLConversion();
        example4_ComplexQuery();
    }
    
    /**
     * Example 1: Basic query parsing and validation
     */
    public static void example1_BasicParsing() {
        System.out.println("=== Example 1: Basic Parsing ===");
        
        HQLParser parser = new HQLParser();
        
        String query = "SELECT u FROM User u WHERE u.age > 18";
        
        if (parser.isValid(query)) {
            System.out.println("Query is valid: " + query);
        } else {
            System.out.println("Query is invalid");
        }
        
        System.out.println();
    }
    
    /**
     * Example 2: Analyzing a query to extract entities and fields
     */
    public static void example2_QueryAnalysis() {
        System.out.println("=== Example 2: Query Analysis ===");
        
        HQLParser parser = new HQLParser();
        
        String query = "SELECT u.name, u.email FROM User u WHERE u.active = true AND u.age > :minAge";
        
        try {
            MetaData analysis = parser.analyze(query);
            
            System.out.println("Query Type: " + analysis.getQueryType());
            System.out.println("Entities: " + analysis.getEntityNames());
            System.out.println("Entity Fields: " + analysis.getEntityFields());
            System.out.println("Parameters: " + analysis.getParameters());
            System.out.println("Aliases: " + analysis.getAliases());
            
        } catch (ParseException e) {
            System.err.println("Failed to parse query: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Example 3: Converting HQL to PostgreSQL
     */
    public static void example3_PostgreSQLConversion() {
        System.out.println("=== Example 3: PostgreSQL Conversion ===");
        
        HQLParser parser = new HQLParser();
        HQLToPostgreSQLConverter converter = new HQLToPostgreSQLConverter();
        
        // Register entity-to-table mappings
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        
        // Register field-to-column mappings
        converter.registerFieldMapping("User", "userName", "user_name");
        converter.registerFieldMapping("User", "email", "email_address");
        converter.registerFieldMapping("Order", "orderDate", "order_date");
        
        String hqlQuery = "SELECT u FROM User u WHERE u.userName = :name";
        
        try {
            MetaData analysis = parser.analyze(hqlQuery);
            String sqlQuery = converter.convert(hqlQuery, analysis);
            System.out.println("HQL: " + hqlQuery);
            System.out.println("SQL: " + sqlQuery);
        } catch (ParseException | ConversionException e) {
            System.err.println("Failed to convert query: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Example 4: Complex query with joins
     */
    public static void example4_ComplexQuery() throws ConversionException, ParseException {
        System.out.println("=== Example 4: Complex Query Analysis ===");
        
        HQLParser parser = new HQLParser();
        
        String query = "SELECT u.name, o.total " +
                      "FROM User u " +
                      "INNER JOIN u.orders o " +
                      "WHERE u.active = true AND o.total > 100 " +
                      "ORDER BY o.total DESC";
        
        MetaData analysis = parser.analyze(query);

        System.out.println("Query: " + query);
        System.out.println("\nAnalysis:");
        System.out.println(analysis);

        // Convert to PostgreSQL
        HQLToPostgreSQLConverter converter = new HQLToPostgreSQLConverter();
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        converter.registerFieldMapping("User", "name", "full_name");
        converter.registerFieldMapping("User", "active", "is_active");
        converter.registerFieldMapping("Order", "total", "total_amount");

        String sqlQuery = converter.convert(query, analysis);
        System.out.println("\nPostgreSQL: " + sqlQuery);

        System.out.println();
    }
    
    /**
     * Example 5: Update query
     */
    public static void example5_UpdateQuery() {
        System.out.println("=== Example 5: Update Query ===");
        
        HQLParser parser = new HQLParser();
        HQLToPostgreSQLConverter converter = new HQLToPostgreSQLConverter();
        
        converter.registerEntityMapping("User", "users");
        converter.registerFieldMapping("User", "lastLogin", "last_login_time");
        
        String hqlQuery = "UPDATE User SET lastLogin = CURRENT_TIMESTAMP WHERE userName = :username";
        
        try {
            MetaData analysis = parser.analyze(hqlQuery);
            System.out.println("Entities involved: " + analysis.getEntityNames());
            
            String sqlQuery = converter.convert(hqlQuery, analysis);
            System.out.println("SQL: " + sqlQuery);
            
        } catch (ParseException | ConversionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
