package com.raditha.hql;

import com.raditha.hql.converter.ConversionException;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.model.QueryAnalysis;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PostgreSQLConverterTest {
    
    private HQLToPostgreSQLConverter converter;
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        converter = new HQLToPostgreSQLConverter();
        parser = new HQLParser();
        
        // Register common mappings
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        converter.registerEntityMapping("Product", "products");
        
        converter.registerFieldMapping("User", "userName", "user_name");
        converter.registerFieldMapping("User", "firstName", "first_name");
        converter.registerFieldMapping("User", "lastName", "last_name");
        converter.registerFieldMapping("User", "emailAddress", "email");
        
        converter.registerFieldMapping("Order", "orderDate", "order_date");
        converter.registerFieldMapping("Order", "totalAmount", "total");
        
        converter.registerFieldMapping("Product", "productName", "name");
    }
    
    @Test
    void testSimpleSelect() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("FROM users u");
    }
    
    @Test
    void testSelectWithWhere() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE u.id = :userId";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("FROM users u");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains(":userId");
    }
    
    @Test
    void testSelectWithFieldMapping() throws ParseException, ConversionException {
        String hql = "SELECT u.userName FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("u.user_name");
    }
    
    @Test
    void testSelectWithOrderBy() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u ORDER BY u.firstName ASC";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("ORDER BY");
        assertThat(sql).contains("u.first_name");
        assertThat(sql).contains("ASC");
    }
    
    @Test
    void testUpdateQuery() throws ParseException, ConversionException {
        String hql = "UPDATE User SET userName = :newName WHERE id = :userId";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("UPDATE users");
        assertThat(sql).contains("SET");
        assertThat(sql).contains("WHERE");
    }
    
    @Test
    void testDeleteQuery() throws ParseException, ConversionException {
        String hql = "DELETE FROM User WHERE id = :userId";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("DELETE FROM users");
        assertThat(sql).contains("WHERE");
    }
    
    @Test
    void testSelectWithJoin() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u INNER JOIN u.orders o";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("INNER JOIN");
    }
    
    @Test
    void testSelectWithGroupBy() throws ParseException, ConversionException {
        String hql = "SELECT u.country, COUNT(u) FROM User u GROUP BY u.country";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("GROUP BY");
    }
    
    @Test
    void testSelectWithHaving() throws ParseException, ConversionException {
        String hql = "SELECT u.country, COUNT(u) FROM User u GROUP BY u.country HAVING COUNT(u) > 5";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("HAVING");
    }
    
    @Test
    void testSelectDistinct() throws ParseException, ConversionException {
        String hql = "SELECT DISTINCT u.country FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT DISTINCT");
    }
    
    @Test
    void testComplexQuery() throws ParseException, ConversionException {
        String hql = "SELECT u.userName, o.orderDate FROM User u " +
                    "LEFT JOIN u.orders o " +
                    "WHERE u.active = true AND o.totalAmount > 100 " +
                    "ORDER BY o.orderDate DESC";
        
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("u.user_name");
        assertThat(sql).contains("o.order_date");
        assertThat(sql).contains("LEFT JOIN");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("ORDER BY");
        assertThat(sql).contains("DESC");
    }
}
