package com.raditha.hql.model;

import java.util.*;

/**
 * Represents the analysis result of an HQL/JPQL query.
 */
public class QueryAnalysis {
    private final String originalQuery;
    private final QueryType queryType;
    private final Set<String> entityNames;
    private final Map<String, String> aliasToEntity; // alias -> entity
    private final Map<String, Set<String>> entityFields; // entity -> fields
    private final Set<String> parameters;
    
    public QueryAnalysis(String originalQuery, QueryType queryType) {
        this.originalQuery = originalQuery;
        this.queryType = queryType;
        this.entityNames = new LinkedHashSet<>();
        this.aliasToEntity = new LinkedHashMap<>();
        this.entityFields = new LinkedHashMap<>();
        this.parameters = new LinkedHashSet<>();
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
