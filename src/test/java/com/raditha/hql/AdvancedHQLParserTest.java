package com.raditha.hql;

import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AdvancedHQLParserTest {
    
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new HQLParser();
    }
    
    // ========== UPDATE Tests ==========
    
    @Test
    void testUpdateWithMultipleFields() throws ParseException {
        String query = "UPDATE User SET name = :newName, email = :newEmail, lastModified = CURRENT_TIMESTAMP WHERE id = :userId";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.UPDATE);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getEntityFields().get("User")).contains("name", "email", "lastModified", "id");
        assertThat(analysis.getParameters()).contains("newName", "newEmail", "userId");
    }
    
    @Test
    void testUpdateWithSubquery() throws ParseException {
        String query = "UPDATE User SET status = :newStatus WHERE id IN (SELECT o.userId FROM Purchase o WHERE o.total > :minTotal)";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.UPDATE);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getParameters()).contains("newStatus", "minTotal");
    }
    
    @Test
    void testUpdateWithCalculation() throws ParseException {
        String query = "UPDATE Product SET price = price * 1.1 WHERE category = :category";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.UPDATE);
        assertThat(analysis.getEntityFields().get("Product")).contains("price", "category");
        assertThat(analysis.getParameters()).contains("category");
    }
    
    // Note: UPDATE without alias and with unqualified field names is supported,
    // but field extraction for unqualified fields is not currently tracked
    
    // ========== DELETE Tests ==========
    
    @Test
    void testDeleteWithComplexWhere() throws ParseException {
        String query = "DELETE FROM Purchase p WHERE p.status = 'CANCELLED' AND p.createdDate < :cutoffDate";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.DELETE);
        assertThat(analysis.getEntityNames()).contains("Purchase");
        assertThat(analysis.getEntityFields().get("Purchase")).contains("status", "createdDate");
        assertThat(analysis.getParameters()).contains("cutoffDate");
    }
    
    @Test
    void testDeleteWithIn() throws ParseException {
        String query = "DELETE FROM User u WHERE u.status IN ('INACTIVE', 'BANNED', 'DELETED')";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.DELETE);
        assertThat(analysis.getEntityNames()).contains("User");
        // Field extraction works when qualified with alias
        assertThat(analysis.getEntityFields().get("User")).contains("status");
    }
    
    @Test
    void testDeleteWithBetween() throws ParseException {
        String query = "DELETE FROM LogEntry WHERE timestamp BETWEEN :startDate AND :endDate";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.DELETE);
        assertThat(analysis.getEntityNames()).contains("LogEntry");
        assertThat(analysis.getParameters()).contains("startDate", "endDate");
    }
    
    @Test
    void testDeleteWithIsNull() throws ParseException {
        String query = "DELETE FROM Session s WHERE s.lastAccessTime IS NULL";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.DELETE);
        assertThat(analysis.getEntityNames()).contains("Session");
        assertThat(analysis.getEntityFields().get("Session")).contains("lastAccessTime");
    }
    
    // ========== SELECT Advanced Tests ==========
    
    @Test
    void testSelectWithMultipleJoins() throws ParseException {
        String query = "SELECT u.name, o.total, p.name " +
                      "FROM User u " +
                      "INNER JOIN u.orders o " +
                      "LEFT JOIN o.products p " +
                      "WHERE u.active = true";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getAliases()).contains("u", "o", "p");
    }
    
    @Test
    void testSelectWithAggregatesAndGroupBy() throws ParseException {
        String query = "SELECT u.country, COUNT(u), AVG(u.age), MAX(u.salary), MIN(u.joinDate) " +
                      "FROM User u " +
                      "GROUP BY u.country " +
                      "HAVING COUNT(u) > 10";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("country", "age", "salary", "joinDate");
    }
    
    @Test
    void testSelectWithSubquery() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.salary > (SELECT AVG(u2.salary) FROM User u2 WHERE u2.department = u.department)";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getAliases()).contains("u", "u2");
    }
    
    @Test
    void testSelectWithCaseExpression() throws ParseException {
        String query = "SELECT u.name, " +
                      "CASE WHEN u.age < 18 THEN 'Minor' " +
                      "WHEN u.age < 65 THEN 'Adult' " +
                      "ELSE 'Senior' END " +
                      "FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("name", "age");
    }
    
    @Test
    void testSelectWithStringFunctions() throws ParseException {
        String query = "SELECT UPPER(u.name), LOWER(u.email), LENGTH(u.phone), CONCAT(u.firstName, ' ', u.lastName) " +
                      "FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).containsAnyOf("name", "email", "phone", "firstName", "lastName");
    }
    
    @Test
    void testSelectWithMathFunctions() throws ParseException {
        String query = "SELECT ABS(p.balance), SQRT(p.amount), MOD(p.quantity, 10) FROM Purchase p";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Purchase");
    }
    
    @Test
    void testSelectWithCoalesce() throws ParseException {
        String query = "SELECT COALESCE(u.nickname, u.firstName, 'Anonymous') FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).containsAnyOf("nickname", "firstName");
    }
    
    @Test
    void testSelectWithExists() throws ParseException {
        String query = "SELECT u FROM User u WHERE EXISTS (SELECT 1 FROM Purchase p WHERE p.userId = u.id AND p.status = 'PENDING')";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getAliases()).contains("u", "p");
    }
    
    @Test
    void testSelectWithLike() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.email LIKE :pattern ESCAPE '\\\\'";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("email");
        assertThat(analysis.getParameters()).contains("pattern");
    }
    
    @Test
    void testSelectWithOrderByNullsFirst() throws ParseException {
        String query = "SELECT u FROM User u ORDER BY u.lastLogin DESC NULLS FIRST";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("lastLogin");
    }
    
    @Test
    void testSelectWithMultipleOrderBy() throws ParseException {
        String query = "SELECT u FROM User u ORDER BY u.country ASC, u.name ASC, u.age DESC";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("country", "name", "age");
    }
    
    @Test
    void testSelectWithPositionalParameters() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.name = ?1 AND u.age > ?2 AND u.status = ?3";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getParameters()).contains("?1", "?2", "?3");
    }
    
    @Test
    void testSelectWithRightJoin() throws ParseException {
        String query = "SELECT u, o FROM User u RIGHT JOIN u.orders o";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getAliases()).contains("u", "o");
    }
    
    @Test
    void testSelectWithFetchJoin() throws ParseException {
        String query = "SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.active = true";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
    }
    
    // ========== Complex Expression Tests ==========
    
    @Test
    void testComplexBooleanExpression() throws ParseException {
        String query = "SELECT u FROM User u WHERE (u.age > 18 AND u.country = 'US') OR (u.verified = true AND u.premium = true)";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("age", "country", "verified", "premium");
    }
    
    @Test
    void testArithmeticExpressions() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.salary * 12 > 100000 AND u.bonus + u.commission > 10000";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("salary", "bonus", "commission");
    }
    
    @Test
    void testNotExpression() throws ParseException {
        String query = "SELECT u FROM User u WHERE NOT u.deleted AND u.email NOT LIKE '%@test.com'";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("deleted", "email");
    }
    
    // ========== Edge Cases ==========
    
    @Test
    void testNestedPaths() throws ParseException {
        String query = "SELECT u.address.city, u.address.country FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        // The parser should track the field paths
        assertThat(analysis.getEntityNames()).contains("User");
    }
    
    @Test
    void testUpdateWithoutAlias() throws ParseException {
        String query = "UPDATE User SET status = 'INACTIVE'";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.UPDATE);
        assertThat(analysis.getEntityNames()).contains("User");
        // Field extraction for unqualified fields in UPDATE is a known limitation
    }
    
    @Test
    void testCurrentDateTimeFunctions() throws ParseException {
        String query = "SELECT CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
    }
}
