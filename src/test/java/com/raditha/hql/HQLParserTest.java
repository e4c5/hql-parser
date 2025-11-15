package com.raditha.hql;

import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HQLParserTest {
    
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new HQLParser();
    }
    
    @Test
    void testSimpleSelectQuery() throws ParseException {
        String query = "SELECT u FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getAliases()).contains("u");
    }
    
    @Test
    void testSelectWithWhere() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.age > 18";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getEntityFields().get("User")).contains("age");
    }
    
    @Test
    void testSelectWithNamedParameter() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.name = :userName";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getParameters()).contains("userName");
        assertThat(analysis.getEntityFields().get("User")).contains("name");
    }
    
    @Test
    void testSelectWithPositionalParameter() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.id = ?1";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getParameters()).contains("?1");
    }
    
    @Test
    void testSelectWithMultipleFields() throws ParseException {
        String query = "SELECT u.name, u.email, u.age FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User"))
            .contains("name", "email", "age");
    }
    
    @Test
    void testSelectWithJoin() throws ParseException {
        String query = "SELECT u FROM User u INNER JOIN u.orders o WHERE o.total > 100";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getAliases()).contains("u", "o");
    }
    
    @Test
    void testSelectWithOrderBy() throws ParseException {
        String query = "SELECT u FROM User u ORDER BY u.name ASC";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("name");
    }
    
    @Test
    void testSelectWithGroupBy() throws ParseException {
        String query = "SELECT u.status, COUNT(u) FROM User u GROUP BY u.status";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User")).contains("status");
    }
    
    @Test
    void testUpdateQuery() throws ParseException {
        String query = "UPDATE User SET active = false WHERE id = :userId";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.UPDATE);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getParameters()).contains("userId");
    }
    
    @Test
    void testDeleteQuery() throws ParseException {
        String query = "DELETE FROM User WHERE age < 18";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.DELETE);
        assertThat(analysis.getEntityNames()).contains("User");
    }
    
    @Test
    void testComplexQuery() throws ParseException {
        String query = "SELECT u.name, o.orderDate, o.total " +
                      "FROM User u " +
                      "LEFT JOIN u.orders o " +
                      "WHERE u.active = true AND o.total > :minTotal " +
                      "ORDER BY o.orderDate DESC";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getAliases()).contains("u", "o");
        assertThat(analysis.getParameters()).contains("minTotal");
    }
    
    @Test
    void testQueryValidation_Valid() {
        String query = "SELECT u FROM User u";
        
        assertThat(parser.isValid(query)).isTrue();
    }
    
    @Test
    void testQueryValidation_Invalid() {
        String query = "INVALID QUERY SYNTAX";
        
        assertThat(parser.isValid(query)).isFalse();
    }
    
    @Test
    void testDistinctQuery() throws ParseException {
        String query = "SELECT DISTINCT u.country FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityFields().get("User")).contains("country");
    }
    
    @Test
    void testQueryWithBetween() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.age BETWEEN 18 AND 65";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User")).contains("age");
    }
    
    @Test
    void testQueryWithIn() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.status IN ('ACTIVE', 'PENDING')";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User")).contains("status");
    }
    
    @Test
    void testQueryWithLike() throws ParseException {
        String query = "SELECT u FROM User u WHERE u.name LIKE :pattern";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User")).contains("name");
        assertThat(analysis.getParameters()).contains("pattern");
    }
}
