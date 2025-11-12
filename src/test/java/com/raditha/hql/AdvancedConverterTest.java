package com.raditha.hql;

import com.raditha.hql.converter.ConversionException;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.model.QueryAnalysis;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class AdvancedConverterTest {
    
    private HQLToPostgreSQLConverter converter;
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        converter = new HQLToPostgreSQLConverter();
        parser = new HQLParser();
        
        // Register entity mappings
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        converter.registerEntityMapping("Purchase", "purchases");
        converter.registerEntityMapping("Product", "products");
        converter.registerEntityMapping("LogEntry", "log_entries");
        converter.registerEntityMapping("Session", "sessions");
        
        // Register field mappings
        converter.registerFieldMapping("User", "userName", "user_name");
        converter.registerFieldMapping("User", "firstName", "first_name");
        converter.registerFieldMapping("User", "lastName", "last_name");
        converter.registerFieldMapping("User", "emailAddress", "email");
        converter.registerFieldMapping("User", "lastLogin", "last_login_at");
        converter.registerFieldMapping("User", "isActive", "active");
        converter.registerFieldMapping("User", "lastModified", "updated_at");
        converter.registerFieldMapping("User", "joinDate", "created_at");
        
        converter.registerFieldMapping("Order", "orderDate", "order_date");
        converter.registerFieldMapping("Order", "totalAmount", "total");
        converter.registerFieldMapping("Order", "userId", "user_id");
        converter.registerFieldMapping("Order", "createdDate", "created_at");
        
        converter.registerFieldMapping("Purchase", "orderDate", "order_date");
        converter.registerFieldMapping("Purchase", "totalAmount", "total");
        converter.registerFieldMapping("Purchase", "userId", "user_id");
        converter.registerFieldMapping("Purchase", "createdDate", "created_at");
        
        converter.registerFieldMapping("Product", "productName", "name");
        converter.registerFieldMapping("Product", "unitPrice", "price");
        
        converter.registerFieldMapping("LogEntry", "createdAt", "created_at");
        converter.registerFieldMapping("Session", "lastAccessTime", "last_access");
    }
    
    // ========== UPDATE Tests ==========
    
    @Test
    void testUpdateWithMultipleFields() throws ParseException, ConversionException {
        String hql = "UPDATE User SET userName = :newName, isActive = false, lastModified = CURRENT_TIMESTAMP WHERE id = :userId";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("UPDATE users");
        assertThat(sql).contains("SET");
        assertThat(sql).contains("user_name = :newName");
        assertThat(sql).contains("active = false");
        assertThat(sql).contains("updated_at = CURRENT_TIMESTAMP");
        assertThat(sql).contains("WHERE");
        assertThat(sql).contains(":userId");
    }
    
    @Test
    void testUpdateWithCalculation() throws ParseException, ConversionException {
        String hql = "UPDATE Product SET unitPrice = unitPrice * 1.1 WHERE category = :category";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("UPDATE products");
        assertThat(sql).contains("price = price * 1.1");
        assertThat(sql).contains("category = :category");
    }
    
    @Test
    void testUpdateWithNoWhere() throws ParseException, ConversionException {
        String hql = "UPDATE User SET isActive = false";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        // Unqualified fields DO get mapped when in UPDATE/DELETE context
        assertThat(sql).isEqualTo("UPDATE users SET active = false");
    }
    
    @Test
    void testUpdateWithBetween() throws ParseException, ConversionException {
        String hql = "UPDATE Purchase SET status = 'ARCHIVED' WHERE orderDate BETWEEN :startDate AND :endDate";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("UPDATE purchases");
        assertThat(sql).contains("order_date BETWEEN :startDate AND :endDate");
    }
    
    // ========== DELETE Tests ==========
    
    @Test
    void testDeleteWithComplexWhere() throws ParseException, ConversionException {
        String hql = "DELETE FROM Purchase p WHERE p.status = 'CANCELLED' AND p.createdDate < :cutoffDate";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("DELETE FROM purchases");
        assertThat(sql).contains("status = 'CANCELLED'");
        assertThat(sql).contains("created_at < :cutoffDate");
    }
    
    @Test
    void testDeleteWithIn() throws ParseException, ConversionException {
        String hql = "DELETE FROM User u WHERE u.status IN ('INACTIVE', 'BANNED', 'DELETED')";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith("DELETE FROM users");
        assertThat(sql).contains("IN ('INACTIVE', 'BANNED', 'DELETED')");
    }
    
    @ParameterizedTest
    @CsvSource({
        "DELETE FROM Session s WHERE s.lastAccessTime IS NULL, DELETE FROM sessions, last_access IS NULL",
        "DELETE FROM Session s WHERE s.lastAccessTime IS NOT NULL, DELETE FROM sessions, last_access IS NOT NULL"
    })
    void testDeleteWithNullChecks(String hql, String expectedStart, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith(expectedStart);
        assertThat(sql).contains(expectedContains);
    }
    
    @ParameterizedTest
    @CsvSource({
        "DELETE FROM LogEntry WHERE createdAt BETWEEN :startDate AND :endDate, DELETE FROM log_entries, created_at BETWEEN :startDate AND :endDate",
        "DELETE FROM LogEntry l WHERE l.level NOT BETWEEN 1 AND 3, DELETE FROM log_entries, level NOT BETWEEN 1 AND 3"
    })
    void testDeleteWithBetween(String hql, String expectedStart, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).startsWith(expectedStart);
        assertThat(sql).contains(expectedContains);
    }
    
    // ========== SELECT Advanced Tests ==========
    
    @Test
    void testSelectWithMultipleJoins() throws ParseException, ConversionException {
        String hql = "SELECT u.userName, o.totalAmount, p.productName " +
                    "FROM User u " +
                    "INNER JOIN u.orders o " +
                    "LEFT JOIN o.products p " +
                    "WHERE u.isActive = true";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("SELECT u.user_name, o.total, p.name");
        assertThat(sql).contains("FROM users u");
        assertThat(sql).contains("INNER JOIN orders o");
        assertThat(sql).contains("LEFT JOIN products p");
        assertThat(sql).contains("WHERE u.active = true");
    }
    
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "SELECT u.userName, o.totalAmount FROM User u RIGHT JOIN u.orders o | RIGHT JOIN orders o,u.user_name,o.total",
        "SELECT u FROM User u LEFT OUTER JOIN u.orders o WHERE o.id IS NULL | LEFT OUTER JOIN"
    })
    void testSelectWithJoinVariants(String hql, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        for (String expected : expectedContains.split(",")) {
            assertThat(sql).contains(expected);
        }
    }
    
    @Test
    void testSelectWithAggregates() throws ParseException, ConversionException {
        String hql = "SELECT COUNT(u), SUM(u.salary), AVG(u.age), MAX(u.joinDate), MIN(u.joinDate) FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("COUNT(u)");
        assertThat(sql).contains("SUM(u.salary)");
        assertThat(sql).contains("AVG(u.age)");
        assertThat(sql).contains("MAX(u.created_at)");
        assertThat(sql).contains("MIN(u.created_at)");
    }
    
    @Test
    void testSelectWithDistinctAggregate() throws ParseException, ConversionException {
        String hql = "SELECT COUNT(DISTINCT u.country) FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("COUNT(DISTINCT u.country)");
    }
    
    @Test
    void testSelectWithStringFunctions() throws ParseException, ConversionException {
        String hql = "SELECT UPPER(u.firstName), LOWER(u.lastName), LENGTH(u.email) FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("UPPER(u.first_name)");
        assertThat(sql).contains("LOWER(u.last_name)");
        assertThat(sql).contains("LENGTH(u.email)");
    }
    
    @Test
    void testSelectWithMathFunctions() throws ParseException, ConversionException {
        String hql = "SELECT ABS(p.balance), SQRT(p.amount) FROM Purchase p";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("ABS(p.balance)");
        assertThat(sql).contains("SQRT(p.amount)");
    }
    
    @Test
    void testSelectWithCoalesce() throws ParseException, ConversionException {
        String hql = "SELECT COALESCE(u.nickname, u.firstName, 'Unknown') FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("COALESCE(u.nickname, u.first_name, 'Unknown')");
    }
    
    @Test
    void testSelectWithOrderByMultiple() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u ORDER BY u.country ASC, u.lastName DESC";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("ORDER BY u.country ASC, u.last_name DESC");
    }
    
    @ParameterizedTest
    @CsvSource({
        "SELECT u FROM User u ORDER BY u.lastLogin DESC NULLS FIRST, ORDER BY u.last_login_at DESC NULLS FIRST",
        "SELECT u FROM User u ORDER BY u.lastLogin ASC NULLS LAST, ORDER BY u.last_login_at ASC NULLS LAST"
    })
    void testSelectWithOrderByNulls(String hql, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains(expectedContains);
    }
    
    @ParameterizedTest
    @CsvSource({
        "SELECT u FROM User u WHERE u.emailAddress LIKE :pattern, u.email LIKE :pattern",
        "SELECT u FROM User u WHERE u.emailAddress NOT LIKE '%@test.com', u.email NOT LIKE '%@test.com'"
    })
    void testSelectWithLike(String hql, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains(expectedContains);
    }
    
    @Test
    void testSelectWithNotIn() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE u.status NOT IN ('DELETED', 'BANNED')";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("status NOT IN ('DELETED', 'BANNED')");
    }
    
    // ========== Complex Expression Tests ==========
    
    @Test
    void testComplexBooleanExpression() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE (u.age > 18 AND u.country = 'US') OR (u.verified = true AND u.premium = true)";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("(u.age > 18 AND u.country = 'US')");
        assertThat(sql).contains("(u.verified = true AND u.premium = true)");
        assertThat(sql).contains("OR");
    }
    
    @Test
    void testArithmeticExpressions() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE u.salary * 12 > 100000 AND u.bonus + u.commission > 10000";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("u.salary * 12 > 100000");
        assertThat(sql).contains("u.bonus + u.commission > 10000");
    }
    
    @Test
    void testNotExpression() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE NOT u.deleted";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("NOT u.deleted");
    }
    
    @Test
    void testParenthesizedExpression() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE (u.age + 5) * 2 > 50";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("(u.age + 5) * 2 > 50");
    }
    
    // ========== Current Date/Time Functions ==========
    
    @ParameterizedTest
    @CsvSource({
        "SELECT CURRENT_DATE FROM User u, CURRENT_DATE",
        "SELECT CURRENT_TIME FROM User u, CURRENT_TIME",
        "UPDATE User SET lastModified = CURRENT_TIMESTAMP WHERE id = :id, CURRENT_TIMESTAMP"
    })
    void testCurrentDateTimeFunctions(String hql, String expectedContains) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains(expectedContains);
    }
    
    // ========== Named and Positional Parameters ==========
    
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "SELECT u FROM User u WHERE u.userName = :name AND u.age > :minAge | :name,:minAge",
        "SELECT u FROM User u WHERE u.userName = ?1 AND u.age > ?2 | ?1,?2"
    })
    void testParameters(String hql, String expectedParams) throws ParseException, ConversionException {
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        for (String param : expectedParams.split(",")) {
            assertThat(sql).contains(param);
        }
    }
    
    // ========== Edge Cases ==========
    
    @Test
    void testSelectWithAlias() throws ParseException, ConversionException {
        String hql = "SELECT u.userName AS username, u.emailAddress AS email FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("u.user_name AS username");
        assertThat(sql).contains("u.email AS email");
    }
    
    @Test
    void testUnmappedEntity() throws ParseException, ConversionException {
        String hql = "SELECT c FROM Customer c";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        // Should use lowercase fallback
        assertThat(sql).contains("FROM customer c");
    }
    
    @Test
    void testUnmappedField() throws ParseException, ConversionException {
        String hql = "SELECT u.unknownField FROM User u";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        // Should keep field name as-is
        assertThat(sql).contains("u.unknownField");
    }
    
    @Test
    void testGroupByWithHaving() throws ParseException, ConversionException {
        String hql = "SELECT u.country, COUNT(u) FROM User u GROUP BY u.country HAVING COUNT(u) > 5";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("GROUP BY u.country");
        assertThat(sql).contains("HAVING COUNT(u) > 5");
    }
    
    @Test
    void testComparisonOperators() throws ParseException, ConversionException {
        String hql = "SELECT u FROM User u WHERE u.age >= 18 AND u.age <= 65 AND u.status != 'INACTIVE'";
        QueryAnalysis analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);
        
        assertThat(sql).contains("u.age >= 18");
        assertThat(sql).contains("u.age <= 65");
        assertThat(sql).contains("u.status != 'INACTIVE'");
    }
}
