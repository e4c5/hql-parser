package com.raditha.hql.parser;

import com.raditha.hql.grammar.HQLBaseVisitor;
import com.raditha.hql.grammar.HQLParser.*;
import com.raditha.hql.model.QueryAnalysis;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Visitor that extracts entities, fields, and parameters from the parse tree.
 */
public class QueryAnalysisVisitor extends HQLBaseVisitor<Void> {
    
    private final QueryAnalysis analysis;
    private final Map<String, String> aliasToEntity = new HashMap<>();
    private String currentEntity = null;
    
    public QueryAnalysisVisitor(QueryAnalysis analysis) {
        this.analysis = analysis;
    }
    
    @Override
    public Void visitFromItem(FromItemContext ctx) {
        if (ctx.entityName() != null) {
            String entityName = ctx.entityName().getText();
            analysis.addEntity(entityName);
            currentEntity = entityName;
            
            // Track alias
            if (ctx.identifier() != null) {
                String alias = ctx.identifier().getText();
                analysis.addAlias(alias);
                aliasToEntity.put(alias, entityName);
            }
        }
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitJoinClause(JoinClauseContext ctx) {
        if (ctx.path() != null) {
            String joinPath = ctx.path().getText();
            
            // Track alias for join
            if (ctx.identifier() != null) {
                String alias = ctx.identifier().getText();
                analysis.addAlias(alias);
            }
        }
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitPath(PathContext ctx) {
        List<IdentifierContext> identifiers = ctx.identifier();
        
        if (identifiers.size() == 1) {
            // Could be an entity alias or field
            String name = identifiers.get(0).getText();
            if (aliasToEntity.containsKey(name)) {
                // It's an alias reference
                String entity = aliasToEntity.get(name);
                // Don't add as entity again
            }
        } else if (identifiers.size() >= 2) {
            // Format: alias.field or entity.field
            String first = identifiers.get(0).getText();
            String second = identifiers.get(1).getText();
            
            String entity = aliasToEntity.getOrDefault(first, first);
            
            // If entity is not yet tracked, add it
            if (!analysis.getEntityNames().contains(entity)) {
                analysis.addEntity(entity);
            }
            
            // Add field to entity
            analysis.addEntityField(entity, second);
            
            // Handle nested paths (e.g., alias.field.nestedField)
            for (int i = 2; i < identifiers.size(); i++) {
                String nestedField = identifiers.get(i).getText();
                analysis.addEntityField(entity, second + "." + nestedField);
            }
        }
        
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitParameter(ParameterContext ctx) {
        String paramText = ctx.getText();
        if (paramText.startsWith(":")) {
            analysis.addParameter(paramText.substring(1));
        } else if (paramText.startsWith("?")) {
            analysis.addParameter(paramText);
        }
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitEntityName(EntityNameContext ctx) {
        String entityName = ctx.getText();
        if (!analysis.getEntityNames().contains(entityName)) {
            analysis.addEntity(entityName);
        }
        return visitChildren(ctx);
    }
}
