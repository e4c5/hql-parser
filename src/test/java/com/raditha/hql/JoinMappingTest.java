package com.raditha.hql;

import com.raditha.hql.converter.JoinMapping;
import com.raditha.hql.converter.JoinType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinMappingTest {

    @Test
    void testLegacyConstructorDefaultsConstraintNameToNull() {
        JoinMapping mapping = new JoinMapping(
                "orders",
                "Order",
                "user_id",
                "id",
                JoinType.LEFT,
                "users",
                "orders"
        );

        assertThat(mapping.constraintName()).isNull();
    }

    @Test
    void testExtendedConstructorStoresConstraintName() {
        JoinMapping mapping = new JoinMapping(
                "orders",
                "Order",
                "user_id",
                "id",
                JoinType.LEFT,
                "users",
                "orders",
                "fk_orders_user"
        );

        assertThat(mapping.constraintName()).isEqualTo("fk_orders_user");
    }
}
