package com.raditha.hql.model;

import java.util.*;

/**
 * Represents the analysis result of an HQL/JPQL query.
 */
public class QueryAnalysis {
    private final String originalQuery;
    private final QueryType queryType;
    private final Set<String> entityNames;
    private final Map<String, Set<String>> entityFields; // entity -> fields
    private final List<String> aliases;
    private final Set<String> parameters;
    
    public QueryAnalysis(String originalQuery, QueryType queryType) {
        this.originalQuery = originalQuery;
        this.queryType = queryType;
        this.entityNames = new LinkedHashSet<>();
        this.entityFields = new LinkedHashMap<>();
        this.aliases = new ArrayList<>();
        this.parameters = new LinkedHashSet<>();
    }
    
    public void addEntity(String entityName) {
        entityNames.add(entityName);
        entityFields.putIfAbsent(entityName, new LinkedHashSet<>());
    }
    
    public void addEntityField(String entityName, String fieldName) {
        entityFields.computeIfAbsent(entityName, k -> new LinkedHashSet<>()).add(fieldName);
    }
    
    public void addAlias(String alias) {
        aliases.add(alias);
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
    
    public Map<String, Set<String>> getEntityFields() {
        return Collections.unmodifiableMap(entityFields);
    }
    
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }
    
    public Set<String> getParameters() {
        return Collections.unmodifiableSet(parameters);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryAnalysis {\n");
        sb.append("  Query Type: ").append(queryType).append("\n");
        sb.append("  Entities: ").append(entityNames).append("\n");
        sb.append("  Entity Fields: ").append(entityFields).append("\n");
        sb.append("  Aliases: ").append(aliases).append("\n");
        sb.append("  Parameters: ").append(parameters).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
