package com.raditha.hql;

import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for HQL to PostgreSQL conversion with constructor expressions
 */
class ConstructorExpressionConverterTest {

    private HQLToPostgreSQLConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HQLToPostgreSQLConverter();
    }

    @Test
    void testSimpleConstructorConversion() throws Exception {
        String hql = "SELECT NEW dto.AccountDTO(a.accountNumber, a.balance) FROM Account a";

        converter.registerEntityMapping("Account", "account");
        converter.registerFieldMapping("Account", "accountNumber", "account_number");
        converter.registerFieldMapping("Account", "balance", "balance");

        MetaData analysis = new MetaData(hql, QueryType.SELECT);
        analysis.addEntity("Account", "a");

        String result = converter.convert(hql, analysis);

        // Constructor expression should be converted to field list (no NEW keyword in
        // SQL)
        assertThat(result).doesNotContain("NEW");
        assertThat(result).doesNotContain("dto.AccountDTO");
        assertThat(result).contains("a.account_number");
        assertThat(result).contains("a.balance");
        assertThat(result).contains("FROM account a");
    }

    @Test
    void testConstructorWithBetweenExpression() throws Exception {
        String hql = "SELECT NEW dto.TransactionDTO(t.id, t.amount) FROM Transaction t WHERE t.date BETWEEN :start AND :end";

        converter.registerEntityMapping("Transaction", "transaction");
        converter.registerFieldMapping("Transaction", "id", "id");
        converter.registerFieldMapping("Transaction", "amount", "amount");
        converter.registerFieldMapping("Transaction", "date", "date");

        MetaData analysis = new MetaData(hql, QueryType.SELECT);
        analysis.addEntity("Transaction", "t");
        analysis.addParameter("start");
        analysis.addParameter("end");

        String result = converter.convert(hql, analysis);

        // Constructor expression should be converted to field list (no NEW keyword in
        // SQL)
        assertThat(result).doesNotContain("NEW");
        assertThat(result).doesNotContain("dto.TransactionDTO");
        assertThat(result).contains("t.id");
        assertThat(result).contains("t.amount");
        assertThat(result).contains("BETWEEN");
        assertThat(result).contains(":start");
        assertThat(result).contains(":end");
    }

}
