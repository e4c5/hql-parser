package com.raditha.hql;

import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for HQL constructor expressions (SELECT NEW ClassName(...))
 */
class ConstructorExpressionTest {
    
    private HQLParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new HQLParser();
    }
    
    @Test
    void testSimpleConstructorExpression() throws ParseException {
        String query = "SELECT NEW dto.UserDTO(u.name, u.email) FROM User u";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("User");
        assertThat(analysis.getEntityNames()).doesNotContain("dto");
        assertThat(analysis.getEntityFields().get("User")).contains("name", "email");
    }
    
    @Test
    void testConstructorWithBetweenAndParameters() throws ParseException {
        String query = "SELECT NEW dto.ReportDTO(r.id, r.date) " +
                      "FROM Report r WHERE r.date BETWEEN :start AND :end";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Report");
        assertThat(analysis.getEntityFields().get("Report")).contains("id", "date");
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
        assertThat(analysis.getEntityNames()).doesNotContain("com", "csi");
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
}
