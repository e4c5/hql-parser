package com.raditha.hql;

import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for HQL constructor expressions (SELECT NEW ClassName(...))
 */
class ConstructorExpressionTest {
    
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new HQLParser();
    }
    
    @Test
    void testSimpleConstructorExpression() throws ParseException {
        String query = "SELECT NEW com.example.UserDTO(u.name, u.email) FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getEntityNames()).doesNotContain("com", "example", "UserDTO");
        assertThat(analysis.getEntityFields().get("User")).contains("name", "email");
    }
    
    @Test
    void testConstructorWithWhereClause() throws ParseException {
        String query = "SELECT NEW dto.PersonDTO(p.firstName, p.lastName) " +
                      "FROM Person p WHERE p.age > :minAge";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Person");
        assertThat(analysis.getEntityNames()).doesNotContain("dto", "PersonDTO");
        assertThat(analysis.getEntityFields().get("Person"))
            .contains("firstName", "lastName", "age");
        assertThat(analysis.getParameters()).contains("minAge");
    }
    
    @Test
    void testConstructorWithNestedFields() throws ParseException {
        String query = "SELECT NEW com.dto.OrderDTO(o.id, o.customer.name, o.total) " +
                      "FROM Order o";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Order");
        assertThat(analysis.getEntityFields().get("Order"))
            .contains("id", "total", "customer", "customer.name");
    }
    
    @Test
    void testConstructorWithMultipleParameters() throws ParseException {
        String query = "SELECT NEW dto.UserDTO(u.id, u.username, u.email, u.active) " +
                      "FROM User u WHERE u.createdDate > :startDate AND u.role = :role";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("User"))
            .contains("id", "username", "email", "active", "createdDate", "role");
        assertThat(analysis.getParameters()).contains("startDate", "role");
    }
    
    @Test
    void testConstructorWithBetweenClause() throws ParseException {
        String query = "SELECT NEW dto.ReportDTO(r.id, r.date, r.amount) " +
                      "FROM Report r WHERE r.date BETWEEN :start AND :end";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Report");
        assertThat(analysis.getEntityFields().get("Report"))
            .contains("id", "date", "amount");
        assertThat(analysis.getParameters()).contains("start", "end");
    }
    
    @Test
    void testProblemStatementQuery() throws ParseException {
        String query = "SELECT NEW com.csi.bm.approval.model.to.UcafFormDTO(" +
                      "u.visitNo, u.visitType, u.doctorName, u.visitDate, u.patientPomrId, " +
                      "u.id, u.approval.id) " +
                      "FROM UCAF u WHERE u.patientId = :patientId AND u.tenantId = :tenantId " +
                      "AND u.hospital = :hospitalId AND u.visitDate BETWEEN :start AND :end";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("UCAF");
        assertThat(analysis.getEntityNames()).doesNotContain("com", "csi", "bm");
        assertThat(analysis.getEntityFields().get("UCAF"))
            .contains("visitNo", "visitType", "doctorName", "visitDate", "patientPomrId", 
                     "id", "approval", "approval.id", "patientId", "tenantId", "hospital");
        assertThat(analysis.getParameters())
            .contains("patientId", "tenantId", "hospitalId", "start", "end");
    }
    
    @Test
    void testConstructorQueryValidation() {
        String validQuery = "SELECT NEW dto.UserDTO(u.name) FROM User u";
        assertThat(parser.isValid(validQuery)).isTrue();
        
        String invalidQuery = "SELECT NEW dto.UserDTO(u.name FROM User u";
        assertThat(parser.isValid(invalidQuery)).isFalse();
    }
    
    @Test
    void testConstructorWithJoin() throws ParseException {
        String query = "SELECT NEW dto.OrderDTO(o.id, c.name, o.total) " +
                      "FROM Order o JOIN o.customer c WHERE o.status = :status";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityNames()).contains("Order");
        assertThat(analysis.getAliases()).contains("o", "c");
        assertThat(analysis.getParameters()).contains("status");
    }
    
    @Test
    void testConstructorWithAggregateFunction() throws ParseException {
        String query = "SELECT NEW dto.SummaryDTO(u.department, COUNT(u), SUM(u.salary)) " +
                      "FROM User u GROUP BY u.department";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getEntityFields().get("User"))
            .contains("department", "salary");
    }
    
    @Test
    void testConstructorWithOrderBy() throws ParseException {
        String query = "SELECT NEW dto.ProductDTO(p.name, p.price) " +
                      "FROM Product p ORDER BY p.price DESC";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityFields().get("Product"))
            .contains("name", "price");
    }
    
    @Test
    void testConstructorWithComplexPath() throws ParseException {
        String query = "SELECT NEW dto.DetailDTO(e.id, e.department.name, e.manager.email) " +
                      "FROM Employee e WHERE e.active = true";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getEntityNames()).contains("Employee");
        assertThat(analysis.getEntityFields().get("Employee"))
            .contains("id", "department", "department.name", "manager", "manager.email", "active");
    }
    
    @Test
    void testConstructorWithPositionalParameter() throws ParseException {
        String query = "SELECT NEW dto.UserDTO(u.name, u.email) " +
                      "FROM User u WHERE u.id = ?1";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getParameters()).contains("?1");
        assertThat(analysis.getEntityFields().get("User"))
            .contains("name", "email", "id");
    }
    
    @Test
    void testConstructorWithDistinct() throws ParseException {
        String query = "SELECT DISTINCT NEW dto.CategoryDTO(p.category) FROM Product p";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Product");
        assertThat(analysis.getEntityFields().get("Product")).contains("category");
    }
}
