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
        String query = "SELECT NEW dto.AccountDTO(a.accountNumber, a.balance) FROM Account a";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Account");
        assertThat(analysis.getEntityNames()).doesNotContain("dto");
        assertThat(analysis.getEntityFields().get("Account")).contains("accountNumber", "balance");
    }
    
    @Test
    void testConstructorWithBetweenAndParameters() throws ParseException {
        String query = "SELECT NEW dto.TransactionDTO(t.id, t.amount) " +
                      "FROM Transaction t WHERE t.transactionDate BETWEEN :start AND :end";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Transaction");
        assertThat(analysis.getEntityFields().get("Transaction")).contains("id", "amount", "transactionDate");
        assertThat(analysis.getParameters()).contains("start", "end");
    }
    
    @Test
    void testProblemStatementQuery() throws ParseException {
        String query = "SELECT NEW com.finance.reporting.model.dto.TransactionSummaryDTO(" +
                      "t.transactionId, t.accountNumber, t.transactionType, t.amount, " +
                      "t.currency, t.id, t.account.id) " +
                      "FROM Transaction t WHERE t.customerId = :customerId AND t.branchId = :branchId " +
                      "AND t.status = :status AND t.transactionDate BETWEEN :start AND :end";
        
        MetaData analysis = parser.analyze(query);
        
        assertThat(analysis.getQueryType()).isEqualTo(QueryType.SELECT);
        assertThat(analysis.getEntityNames()).contains("Transaction");
        assertThat(analysis.getEntityNames()).doesNotContain("com", "finance");
        assertThat(analysis.getEntityFields().get("Transaction"))
            .contains("transactionId", "accountNumber", "transactionType", "amount", "currency", 
                     "id", "account", "account.id", "customerId", "branchId", "status", "transactionDate");
        assertThat(analysis.getParameters())
            .contains("customerId", "branchId", "status", "start", "end");
    }
    
    @Test
    void testConstructorQueryValidation() {
        String validQuery = "SELECT NEW dto.AccountDTO(a.accountNumber) FROM Account a";
        assertThat(parser.isValid(validQuery)).isTrue();
        
        String invalidQuery = "SELECT NEW dto.AccountDTO(a.accountNumber FROM Account a";
        assertThat(parser.isValid(invalidQuery)).isFalse();
    }
}
