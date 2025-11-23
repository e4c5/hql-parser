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
        
        // Determine join direction based on where the foreign key is located.
        // 
        // In JPA relationships:
        // - @ManyToOne/@OneToOne: FK is on the source entity (the entity with the annotation)
        //   e.g., Order.user -> FK (user_id) is on orders table
        // - @OneToMany/@ManyToMany: FK is on the target entity (the "many" side)
        //   e.g., User.orders -> FK (user_id) is on orders table
        //
        // We determine this by checking which table the joinColumn logically belongs to.
        // The joinColumn name typically references the other table (e.g., "user_id" references "users").
        boolean isCollectionJoin = isCollectionRelationship(mapping, sourceAlias, targetAlias, analysis);
        
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
     * Determines if a relationship is a collection (OneToMany/ManyToMany) based on
     * where the foreign key is located.
     * 
     * In JPA:
     * - @ManyToOne/@OneToOne: FK is on source table (the entity with the annotation)
     * - @OneToMany/@ManyToMany: FK is on target table (the "many" side)
     * 
     * We determine this by checking if the joinColumn name suggests it references
     * the source table. If so, the FK is on the target (OneToMany). Otherwise,
     * the FK is on the source (ManyToOne).
     * 
     * @param mapping The JoinMapping containing relationship information
     * @param sourceAlias The source entity alias
     * @param targetAlias The target entity alias
     * @param analysis The MetaData for entity resolution
     * @return true if this is a collection relationship (FK on target table)
     */
    private boolean isCollectionRelationship(JoinMapping mapping, String sourceAlias, 
                                            String targetAlias, MetaData analysis) {
        // Get the source and target entity names
        String sourceEntity = analysis.getEntityForAlias(sourceAlias);
        String targetEntity = analysis.getEntityForAlias(targetAlias);
        
        if (sourceEntity == null || targetEntity == null) {
            // Fallback: if we can't determine entities, assume ManyToOne (FK on source)
            // This is the more common case and safer default
            return false;
        }
        
        // Get table names from the mapping
        String sourceTable = mapping.sourceTable();
        String targetTable = mapping.targetTable();
        String joinColumn = mapping.joinColumn();
        
        // The joinColumn name typically follows patterns like:
        // - "user_id" references "users" table
        // - "order_id" references "orders" table
        // 
        // For OneToMany: joinColumn references source table, FK is on target
        //   e.g., User.orders -> joinColumn="user_id" (references users), FK on orders
        // For ManyToOne: joinColumn references target table, FK is on source
        //   e.g., Order.user -> joinColumn="user_id" (references users), FK on orders
        
        // Normalize table names and join column for comparison
        String sourceTableNormalized = sourceTable.toLowerCase().replace("_", "");
        String targetTableNormalized = targetTable.toLowerCase().replace("_", "");
        String joinColumnNormalized = joinColumn.toLowerCase().replace("_", "");
        
        // Check if joinColumn name contains reference to source table
        // If joinColumn is like "user_id" and sourceTable is "users", it's OneToMany
        if (joinColumnNormalized.contains(sourceTableNormalized) || 
            sourceTableNormalized.contains(extractEntityNameFromColumn(joinColumnNormalized))) {
            return true; // FK is on target (OneToMany)
        }
        
        // Check if joinColumn name contains reference to target table
        // If joinColumn is like "user_id" and targetTable is "users", it's ManyToOne
        if (joinColumnNormalized.contains(targetTableNormalized) ||
            targetTableNormalized.contains(extractEntityNameFromColumn(joinColumnNormalized))) {
            return false; // FK is on source (ManyToOne)
        }
        
        // Fallback: Default to ManyToOne (FK on source) as it's more common
        return false;
    }
    
    /**
     * Extracts entity name from a column name.
     * e.g., "user_id" -> "user", "order_id" -> "order"
     */
    private String extractEntityNameFromColumn(String columnName) {
        // Remove common suffixes like "_id", "_fk", etc.
        if (columnName.endsWith("_id")) {
            return columnName.substring(0, columnName.length() - 3);
        }
        if (columnName.endsWith("_fk")) {
            return columnName.substring(0, columnName.length() - 3);
        }
        if (columnName.endsWith("id")) {
            return columnName.substring(0, columnName.length() - 2);
        }
        return columnName;
    }
}

