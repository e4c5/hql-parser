package com.raditha.hql.model;

import java.util.*;

/**
 * Represents the analysis result of an HQL/JPQL query.
 */
public class MetaData {
    private final String originalQuery;
    private final QueryType queryType;
    private final Set<String> entityNames;
    private final Map<String, String> aliasToEntity; // alias -> entity
    private final Map<String, Set<String>> entityFields; // entity -> fields
    private final Set<String> parameters;
    private final Map<String, JoinPathInfo> joinPaths; // path expression -> join path info
    
    public MetaData(String originalQuery, QueryType queryType) {
        this.originalQuery = originalQuery;
        this.queryType = queryType;
        this.entityNames = new LinkedHashSet<>();
        this.aliasToEntity = new LinkedHashMap<>();
        this.entityFields = new LinkedHashMap<>();
        this.parameters = new LinkedHashSet<>();
        this.joinPaths = new LinkedHashMap<>();
    }
    
    public void addEntity(String entityName) {
        addEntity(entityName, null);
    }
    
    public void addEntity(String entityName, String alias) {
        entityNames.add(entityName);
        if (alias != null) {
            aliasToEntity.put(alias, entityName);
        }
        entityFields.putIfAbsent(entityName, new LinkedHashSet<>());
    }
    
    public void addEntityField(String entityName, String fieldName) {
        entityFields.computeIfAbsent(entityName, k -> new LinkedHashSet<>()).add(fieldName);
    }
    
    public void addParameter(String parameter) {
        parameters.add(parameter);
    }
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    public QueryType getQueryType() {
        return queryType;
    }
    
    public Set<String> getEntityNames() {
        return Collections.unmodifiableSet(entityNames);
    }
    
    public Map<String, String> getAliasToEntity() {
        return Collections.unmodifiableMap(aliasToEntity);
    }
    
    public List<String> getAliases() {
        return new ArrayList<>(aliasToEntity.keySet());
    }
    
    public Map<String, Set<String>> getEntityFields() {
        return Collections.unmodifiableMap(entityFields);
    }
    
    public Set<String> getParameters() {
        return Collections.unmodifiableSet(parameters);
    }
    
    /**
     * Get the entity name for a given alias.
     * @param alias The alias to look up
     * @return The entity name, or null if not found
     */
    public String getEntityForAlias(String alias) {
        return aliasToEntity.get(alias);
    }
    
    /**
     * Adds information about an implicit join path.
     * 
     * @param sourceAlias The alias of the source entity
     * @param pathExpression The path expression (e.g., "u.orders")
     * @param targetAlias The alias of the target entity
     * @param targetEntity The target entity name
     */
    public void addJoinPath(String sourceAlias, String pathExpression, String targetAlias, String targetEntity) {
        joinPaths.put(pathExpression, new JoinPathInfo(sourceAlias, pathExpression, targetAlias, targetEntity));
    }
    
    /**
     * Represents information about an implicit join path.
     */
    public static class JoinPathInfo {
        private final String sourceAlias;
        private final String pathExpression;
        private final String targetAlias;
        private final String targetEntity;

        public JoinPathInfo(String sourceAlias, String pathExpression, String targetAlias, String targetEntity) {
            this.sourceAlias = sourceAlias;
            this.pathExpression = pathExpression;
            this.targetAlias = targetAlias;
            this.targetEntity = targetEntity;
        }

        public String sourceAlias() {
            return sourceAlias;
        }

        public String pathExpression() {
            return pathExpression;
        }

        public String targetAlias() {
            return targetAlias;
        }

        public String targetEntity() {
            return targetEntity;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryAnalysis {\n");
        sb.append("  Query Type: ").append(queryType).append("\n");
        sb.append("  Entities: ").append(entityNames).append("\n");
        sb.append("  Alias to Entity: ").append(aliasToEntity).append("\n");
        sb.append("  Entity Fields: ").append(entityFields).append("\n");
        sb.append("  Parameters: ").append(parameters).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
