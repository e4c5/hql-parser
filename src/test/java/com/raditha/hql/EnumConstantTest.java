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
}
