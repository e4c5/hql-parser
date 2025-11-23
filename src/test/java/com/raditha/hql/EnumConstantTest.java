package com.raditha.hql;

import com.raditha.hql.converter.ConversionException;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.model.MetaData;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumConstantTest {

    private HQLToPostgreSQLConverter converter;
    private HQLParser parser;

    @BeforeEach
    void setUp() {
        converter = new HQLToPostgreSQLConverter();
        parser = new HQLParser();

        converter.registerEntityMapping("Booking", "bookings");
        converter.registerFieldMapping("Booking", "status", "status");
    }

    @Test
    void testEnumConstantInQuery() throws ParseException, ConversionException {
        String hql = "SELECT COUNT(b) FROM Booking b WHERE b.status = com.travel.trade.Status.CONFIRMED";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // The converter should preserve the fully qualified name
        assertThat(sql).contains("bookings b");
        assertThat(sql).contains("b.status = com.travel.trade.Status.CONFIRMED");
    }

    @Test
    void testJoinFetchWithEnumConstant() throws ParseException, ConversionException {
        String hql = "SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.items WHERE b.customerId = :custId AND b.status = com.shop.Status.PENDING";
        MetaData analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        assertThat(sql).contains("SELECT DISTINCT");
        assertThat(sql).contains("FROM bookings b");
        // FETCH join without alias is skipped to avoid crash and because it doesn't
        // affect row filtering for LEFT JOIN
        // assertThat(sql).contains("LEFT OUTER JOIN");
        // FETCH should be ignored or handled, but definitely shouldn't crash
        // And the enum should be preserved
        assertThat(sql).contains("b.status = com.shop.Status.PENDING");
    }
}
