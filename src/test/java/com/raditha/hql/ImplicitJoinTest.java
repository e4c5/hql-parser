package com.raditha.hql;

import com.raditha.hql.converter.ConversionException;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;
import com.raditha.hql.converter.JoinMapping;
import com.raditha.hql.converter.JoinType;
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for implicit join ON clause generation.
 */
class ImplicitJoinTest {

    private HQLToPostgreSQLConverter converter;
    private HQLParser parser;

    @BeforeEach
    void setUp() {
        converter = new HQLToPostgreSQLConverter();
        parser = new HQLParser();
    }

    @Test
    void testImplicitJoinGeneratesOnClause() throws ParseException, ConversionException {
        // Setup: Register entity and relationship mappings
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        
        // Create relationship metadata: User.orders -> Order
        Map<String, JoinMapping> userRelationships = new HashMap<>();
        userRelationships.put("orders", new JoinMapping(
            "orders", "Order", "user_id", "id", JoinType.LEFT, "users", "orders"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("User", userRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        // Test query with implicit join
        String hql = "SELECT u FROM User u JOIN u.orders o";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should generate ON clause
        assertThat(sql).contains("ON");
        assertThat(sql).contains("o.user_id = u.id");
    }

    @Test
    void testExplicitOnClauseNotOverridden() throws ParseException, ConversionException {
        // Setup
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        
        Map<String, JoinMapping> userRelationships = new HashMap<>();
        userRelationships.put("orders", new JoinMapping(
            "orders", "Order", "user_id", "id", JoinType.LEFT, "users", "orders"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("User", userRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        // Test query with explicit ON clause
        String hql = "SELECT u FROM User u JOIN u.orders o ON o.user_id = u.id AND o.status = 'ACTIVE'";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should use explicit ON clause, not generate one
        assertThat(sql).contains("ON o.user_id = u.id AND o.status = 'ACTIVE'");
        // Should not have duplicate ON clauses
        long onCount = sql.split("ON").length - 1;
        assertThat(onCount).isEqualTo(1);
    }

    @Test
    void testMissingRelationshipMetadataHandledGracefully() throws ParseException, ConversionException {
        // Setup: Register entities but no relationship metadata
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");

        // Test query with implicit join but no metadata
        String hql = "SELECT u FROM User u JOIN u.orders o";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should still generate SQL, just without ON clause
        assertThat(sql).contains("JOIN");
        assertThat(sql).contains("orders");
        // No ON clause should be generated if metadata is missing
        // (This is expected behavior - we can't generate ON without metadata)
    }

    @Test
    void testManyToOneRelationship() throws ParseException, ConversionException {
        // Setup: OrderEntity -> User (ManyToOne)
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("OrderEntity", "orders");
        
        Map<String, JoinMapping> orderRelationships = new HashMap<>();
        orderRelationships.put("user", new JoinMapping(
            "user", "User", "user_id", "id", JoinType.INNER, "orders", "users"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("OrderEntity", orderRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        // Test query: OrderEntity o JOIN o.user u (using OrderEntity to avoid keyword conflict)
        String hql = "SELECT o FROM OrderEntity o JOIN o.user u";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should generate ON clause for ManyToOne
        assertThat(sql).contains("ON");
        assertThat(sql).contains("o.user_id = u.id");
    }

    @Test
    void testDifferentJoinTypes() throws ParseException, ConversionException {
        // Setup
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Order", "orders");
        
        Map<String, JoinMapping> userRelationships = new HashMap<>();
        userRelationships.put("orders", new JoinMapping(
            "orders", "Order", "user_id", "id", JoinType.RIGHT, "users", "orders"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("User", userRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        String hql = "SELECT u FROM User u RIGHT JOIN u.orders o";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should preserve join type and generate ON clause
        assertThat(sql).contains("RIGHT JOIN");
        assertThat(sql).contains("ON");
    }

    @Test
    void testNonCollectionPropertyEndingWithS() throws ParseException, ConversionException {
        // Test that properties ending with 's' that are NOT collections are handled correctly
        // e.g., "status", "address", "process" should be treated as ManyToOne, not OneToMany
        
        // Setup: OrderEntity -> Status (ManyToOne relationship)
        converter.registerEntityMapping("OrderEntity", "orders");
        converter.registerEntityMapping("Status", "statuses");
        
        // "status" ends with 's' but is ManyToOne, so FK is on orders table
        Map<String, JoinMapping> orderRelationships = new HashMap<>();
        orderRelationships.put("status", new JoinMapping(
            "status", "Status", "status_id", "id", JoinType.INNER, "orders", "statuses"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("OrderEntity", orderRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        // Test query: OrderEntity o JOIN o.status s
        String hql = "SELECT o FROM OrderEntity o JOIN o.status s";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should generate correct ON clause: FK is on source (orders), not target
        // o.status_id = s.id (not s.status_id = o.id)
        assertThat(sql).contains("ON");
        assertThat(sql).contains("o.status_id = s.id");
        assertThat(sql).doesNotContain("s.status_id = o.id");
    }

    @Test
    void testAddressPropertyManyToOne() throws ParseException, ConversionException {
        // Test "address" property (ends with 's' but is ManyToOne)
        converter.registerEntityMapping("User", "users");
        converter.registerEntityMapping("Address", "addresses");
        
        Map<String, JoinMapping> userRelationships = new HashMap<>();
        userRelationships.put("address", new JoinMapping(
            "address", "Address", "address_id", "id", JoinType.LEFT, "users", "addresses"
        ));
        
        Map<String, Map<String, JoinMapping>> relationshipMetadata = new HashMap<>();
        relationshipMetadata.put("User", userRelationships);
        converter.setRelationshipMetadata(relationshipMetadata);

        String hql = "SELECT u FROM User u LEFT JOIN u.address a";
        var analysis = parser.analyze(hql);
        String sql = converter.convert(hql, analysis);

        // Should generate: u.address_id = a.id (FK on users table)
        assertThat(sql).contains("ON");
        assertThat(sql).contains("u.address_id = a.id");
    }
}

