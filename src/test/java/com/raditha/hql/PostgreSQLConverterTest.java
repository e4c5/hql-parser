package com.raditha.hql;

import com.raditha.hql.converter.ConversionException;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.model.MetaData;
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
        converter.registerEntityMapping("Postage", "postage");
        converter.registerEntityMapping("PropertyListing", "property_listing");
        converter.registerEntityMapping("Commission", "commission");

        converter.registerFieldMapping("User", "userName", "user_name");
        converter.registerFieldMapping("User", "firstName", "first_name");
        converter.registerFieldMapping("User", "lastName", "last_name");
        converter.registerFieldMapping("User", "emailAddress", "email");
        
        converter.registerFieldMapping("Order", "orderDate", "order_date");
        converter.registerFieldMapping("Order", "totalAmount", "total");
        
        converter.registerFieldMapping("Product", "productName", "name");

        converter.registerFieldMapping("Postage", "isDeleted", "is_deleted");
        converter.registerFieldMapping("Postage", "isActive", "is_active");
        converter.registerFieldMapping("Postage", "postalCode", "postal_code");

        converter.registerFieldMapping("PropertyListing", "agentId", "agent_id");

        converter.registerFieldMapping("Commission", "propertyListingId", "property_listing_id");
        converter.registerFieldMapping("Commission", "brokerageId", "brokerage_id");
        converter.registerFieldMapping("Commission", "contractId", "contract_id");
        converter.registerFieldMapping("Commission", "isActive", "is_active");
        converter.registerFieldMapping("Commission", "isDeleted", "is_deleted");
        converter.registerFieldMapping("Commission", "remainingCommission", "remaining_commission");
        converter.registerFieldMapping("Commission", "totalCommission", "total_commission");
    }
    
    @Test
    void testSimpleSelect() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("FROM users u");
    }
    
    @Test
    void testSelectWithWhere() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE u.id = :userId";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("FROM users u");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains(":userId");
    }
    
    @Test
    void testSelectWithFieldMapping() throws ParseException, ConversionException {
        String hql = "SELECT u.userName FROM User u";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("u.user_name");
    }
    
    @Test
    void testSelectWithOrderBy() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u ORDER BY u.firstName ASC";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("ORDER BY");
        assertThat(sql).contains("u.first_name");
        assertThat(sql).contains("ASC");
    }
    
    @Test
    void testUpdateQuery() throws ParseException, ConversionException {
        String hql = "UPDATE User SET userName = :newName WHERE id = :userId";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("UPDATE users");
        assertThat(sql).contains("SET");
        assertThat(sql).contains("WHERE");
    }
    
    @Test
    void testDeleteQuery() throws ParseException, ConversionException {
        String hql = "DELETE FROM User WHERE id = :userId";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("DELETE FROM users");
        assertThat(sql).contains("WHERE");
    }
    
    @Test
    void testSelectWithJoin() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u INNER JOIN u.orders o";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("INNER JOIN");
    }
    
    @Test
    void testSelectWithGroupBy() throws ParseException, ConversionException {
        String hql = "SELECT u.country, COUNT(u) FROM User u GROUP BY u.country";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("GROUP BY");
    }
    
    @Test
    void testSelectWithHaving() throws ParseException, ConversionException {
        String hql = "SELECT u.country, COUNT(u) FROM User u GROUP BY u.country HAVING COUNT(u) > 5";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("HAVING");
    }
    
    @Test
    void testSelectDistinct() throws ParseException, ConversionException {
        String hql = "SELECT DISTINCT u.country FROM User u";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT DISTINCT");
    }
    
    @Test
    void testComplexQuery() throws ParseException, ConversionException {
        String hql = "SELECT u.userName, o.orderDate FROM User u " +
                    "LEFT JOIN u.orders o " +
                    "WHERE u.active = true AND o.totalAmount > 100 " +
                    "ORDER BY o.orderDate DESC";
        
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("u.user_name");
        assertThat(sql).contains("o.order_date");
        assertThat(sql).contains("LEFT JOIN");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("ORDER BY");
        assertThat(sql).contains("DESC");
    }

    @Test
    void testUpdateQueryWithAliasAndMultipleSetClauses() throws ParseException, ConversionException {
        String hql = "update Postage p set p.isDeleted = true, p.isActive = false where p.postalCode = ?1";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        assertThat(sql).startsWith("UPDATE postage p");
        assertThat(sql).contains("SET");
        assertThat(sql).contains("is_deleted");
        assertThat(sql).contains("is_active");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("postal_code");
        assertThat(sql).contains("?1");
    }

    @Test
    void testComplexCommissionQueryWithCaseExpressions() throws ParseException, ConversionException {
        String hql = "SELECT SUM(CASE WHEN c.remainingCommission > 0 THEN COALESCE(c.remainingCommission, 0) ELSE 0 END), " +
                "SUM(CASE WHEN c.totalCommission > 0 THEN COALESCE(c.totalCommission, 0) ELSE 0 END) " +
                "FROM PropertyListing pl LEFT JOIN Commission c ON pl.id = c.propertyListingId " +
                "WHERE pl.agentId = :agentId AND c.brokerageId = :brokerageId AND c.contractId = :contractId " +
                "AND c.isActive = true AND c.isDeleted = false";

        MetaData analysis = parser.analyze(hql);
        System.out.println("Query Analysis: " + analysis);
        String sql = converter.convert(hql, analysis);
        System.out.println("Converted SQL: " + sql);

        assertThat(sql).contains("SELECT");
        assertThat(sql).contains("SUM(CASE WHEN");
        assertThat(sql).contains("ELSE 0 END");
        assertThat(sql).contains("COALESCE");
        assertThat(sql).contains("remaining_commission");
        assertThat(sql).contains("total_commission");
        assertThat(sql).contains("FROM property_listing pl");
        assertThat(sql).contains("LEFT JOIN commission c");
        assertThat(sql).contains("WHERE");
    }

    @Test
    void testUpdateOpenCoverageWithAliasColumnQualification() throws ParseException, ConversionException {
        converter.registerEntityMapping("Insurance", "insurance");
        converter.registerFieldMapping("Insurance", "isDeleted", "is_deleted");
        converter.registerFieldMapping("Insurance", "isActive", "is_active");
        converter.registerFieldMapping("Insurance", "insurancePolicyId", "insurance_policy_id");
        String hql = "update Insurance o set o.isDeleted = true, o.isActive = false where o.insurancePolicyId = ?1";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        assertThat(sql).startsWith("UPDATE insurance o SET");
        // Ensure assignment columns are unqualified
        assertThat(sql).contains("SET is_deleted = true");
        assertThat(sql).contains("is_active = false");
        // Ensure WHERE clause retains alias qualification
        assertThat(sql).contains("WHERE o.insurance_policy_id = ?1");
        // Negative checks to avoid regressions
        assertThat(sql).doesNotContain("o.is_deleted = true");
        assertThat(sql).doesNotContain("o.is_active = false");
    }
}
