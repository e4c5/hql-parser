package com.raditha.hql.converter;

import com.raditha.hql.model.MetaData;

/**
 * Generates SQL ON clauses for implicit HQL joins.
 * 
 * HQL allows implicit joins without explicit ON clauses (e.g., "FROM User u JOIN u.orders o").
 * This class generates the ON clause using relationship metadata provided from JPA annotations.
 * 
 * The relationship metadata is built by parsing Java sources (in antikythera) and passed to
 * this converter, which uses it to generate SQL-compatible ON clauses.
 */
public class ImplicitJoinOnClauseGenerator {
    
    /**
     * Generates an ON clause for an implicit join.
     * 
     * @param sourceAlias The alias of the source entity (e.g., "u" in "u.orders")
     * @param pathExpression The full path expression (e.g., "u.orders")
     * @param targetAlias The alias of the target entity (e.g., "o")
     * @param mapping The JoinMapping containing relationship information
     * @param analysis The MetaData containing entity and alias mappings
     * @return The generated ON clause (e.g., "o.user_id = u.id")
     */
    public String generateOnClause(String sourceAlias, String pathExpression, 
                                   String targetAlias, JoinMapping mapping, 
                                   MetaData analysis) {
        if (mapping == null) {
            return null;
        }
        
        // Extract property name from path (e.g., "u.orders" -> "orders")
        String propertyName = extractPropertyName(pathExpression);
        if (propertyName == null) {
            return null;
        }
        
        // Determine join direction based on relationship type
        // For OneToMany (collection): FK is on target table
        //   e.g., User u JOIN u.orders o -> o.user_id = u.id
        // For ManyToOne (direct): FK is on source table  
        //   e.g., Order o JOIN o.user u -> o.user_id = u.id
        //
        // Heuristic: If property name is plural (ends with 's'), treat as OneToMany collection
        boolean isCollectionJoin = isCollectionProperty(propertyName);
        
        String leftTableAlias;
        String leftColumn;
        String rightTableAlias;
        String rightColumn;
        
        if (isCollectionJoin) {
            // Collection join (OneToMany): target table has the foreign key
            // e.g., User u JOIN u.orders o -> o.user_id = u.id
            leftTableAlias = targetAlias;
            leftColumn = mapping.joinColumn();
            rightTableAlias = sourceAlias;
            rightColumn = mapping.referencedColumn();
        } else {
            // Direct relationship (ManyToOne/OneToOne): source table has the foreign key
            // e.g., Order o JOIN o.user u -> o.user_id = u.id
            leftTableAlias = sourceAlias;
            leftColumn = mapping.joinColumn();
            rightTableAlias = targetAlias;
            rightColumn = mapping.referencedColumn();
        }
        
        return buildOnExpression(leftTableAlias, leftColumn, rightTableAlias, rightColumn);
    }
    
    /**
     * Builds an ON expression from table aliases and columns.
     * 
     * @param leftAlias The left table alias
     * @param leftColumn The left column name
     * @param rightAlias The right table alias
     * @param rightColumn The right column name
     * @return The ON expression (e.g., "o.user_id = u.id")
     */
    public String buildOnExpression(String leftAlias, String leftColumn, 
                                   String rightAlias, String rightColumn) {
        return String.format("%s.%s = %s.%s", leftAlias, leftColumn, rightAlias, rightColumn);
    }
    
    /**
     * Extracts the property name from a path expression.
     * 
     * @param pathExpression The path expression (e.g., "u.orders")
     * @return The property name (e.g., "orders") or null if invalid
     */
    private String extractPropertyName(String pathExpression) {
        if (pathExpression == null || !pathExpression.contains(".")) {
            return null;
        }
        
        String[] parts = pathExpression.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        
        return parts[parts.length - 1];
    }
    
    /**
     * Determines if a property represents a collection relationship (OneToMany/ManyToMany).
     * This is a heuristic based on the property name.
     * 
     * @param propertyName The property name (e.g., "orders", "user")
     * @return true if this appears to be a collection relationship
     */
    private boolean isCollectionProperty(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return false;
        }
        
        // Heuristic: if the property name is plural (ends with 's' and is longer than 1 char),
        // it's likely a OneToMany or ManyToMany relationship
        // This works for common cases like "orders", "users", "items", etc.
        return propertyName.length() > 1 && propertyName.endsWith("s");
    }
}

