package com.raditha.hql;

import com.raditha.hql.model.QueryAnalysis;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JoinAnalysisTest {
    
    private final HQLParser parser = new HQLParser();
    
    @Test
    public void testJoinWithAliases() throws ParseException {
        String query = "SELECT SUM(CASE WHEN oc.balanceAmount > 0 THEN COALESCE(oc.balanceAmount, 0) ELSE 0 END), " +
                "SUM(CASE WHEN oc.approvedAmount > 0 THEN COALESCE(oc.approvedAmount, 0) ELSE 0 END) " +
                "FROM Approval a left join OpenCoverage oc on a.id = oc.approvalId " +
                "where a.admissionId = :admissionId " +
                "AND oc.isActive = true AND oc.isDeleted = false " +
                "AND oc.payerGroupId = :payerGroupId AND oc.payerContractId = :payerContractId";
        
        QueryAnalysis analysis = parser.analyze(query);
        
        System.out.println("=== Query Analysis ===");
        System.out.println(analysis);
        System.out.println("\n=== Detailed Analysis ===");
        System.out.println("Entities: " + analysis.getEntityNames());
        System.out.println("Aliases: " + analysis.getAliases());
        System.out.println("Alias to Entity: " + analysis.getAliasToEntity());
        System.out.println("Entity Fields: " + analysis.getEntityFields());
        System.out.println("Parameters: " + analysis.getParameters());
        
        // Expected: 2 entities (Approval, OpenCoverage)
        assertEquals(2, analysis.getEntityNames().size(), "Should have 2 entities");
        assertTrue(analysis.getEntityNames().contains("Approval"), "Should contain Approval entity");
        assertTrue(analysis.getEntityNames().contains("OpenCoverage"), "Should contain OpenCoverage entity");
        
        // Expected: 2 aliases (a, oc)
        assertEquals(2, analysis.getAliases().size(), "Should have 2 aliases");
        assertTrue(analysis.getAliases().contains("a"), "Should contain alias 'a'");
        assertTrue(analysis.getAliases().contains("oc"), "Should contain alias 'oc'");
        
        // Verify alias-to-entity mapping
        assertEquals("Approval", analysis.getEntityForAlias("a"), "Alias 'a' should map to Approval");
        assertEquals("OpenCoverage", analysis.getEntityForAlias("oc"), "Alias 'oc' should map to OpenCoverage");
        
        // Expected: 3 parameters (admissionId, payerGroupId, payerContractId)
        assertEquals(3, analysis.getParameters().size(), "Should have 3 parameters");
        assertTrue(analysis.getParameters().contains("admissionId"));
        assertTrue(analysis.getParameters().contains("payerGroupId"));
        assertTrue(analysis.getParameters().contains("payerContractId"));
        
        // Verify fields are correctly mapped to entities
        assertTrue(analysis.getEntityFields().get("Approval").contains("id"));
        assertTrue(analysis.getEntityFields().get("Approval").contains("admissionId"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("approvalId"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("balanceAmount"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("approvedAmount"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("isActive"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("isDeleted"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("payerGroupId"));
        assertTrue(analysis.getEntityFields().get("OpenCoverage").contains("payerContractId"));
    }
    
    @Test
    public void testMultipleJoins() throws ParseException {
        String query = "SELECT u.name, d.name, p.title " +
                "FROM User u " +
                "INNER JOIN u.department d " +
                "LEFT JOIN u.projects p " +
                "WHERE u.active = true";
        
        QueryAnalysis analysis = parser.analyze(query);
        
        // Should have User entity and aliases for all three
        assertTrue(analysis.getEntityNames().contains("User"));
        assertEquals(3, analysis.getAliases().size());
        assertTrue(analysis.getAliases().contains("u"));
        assertTrue(analysis.getAliases().contains("d"));
        assertTrue(analysis.getAliases().contains("p"));
        
        // Verify alias mapping
        assertEquals("User", analysis.getEntityForAlias("u"));
    }
    
    @Test
    public void testJoinWithoutAlias() throws ParseException {
        String query = "SELECT e FROM Employee e JOIN e.department WHERE e.salary > 50000";
        
        QueryAnalysis analysis = parser.analyze(query);
        
        assertTrue(analysis.getEntityNames().contains("Employee"));
        assertEquals(1, analysis.getAliases().size());
        assertTrue(analysis.getAliases().contains("e"));
        assertEquals("Employee", analysis.getEntityForAlias("e"));
    }
}
