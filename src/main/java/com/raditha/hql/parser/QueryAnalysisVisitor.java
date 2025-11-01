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
    
    // Make sure top-level statements traverse their children
    @Override
    public Void visitStatement(StatementContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitSelectStatement(SelectStatementContext ctx) {
        // Visit FROM clause first to build alias map, then visit other clauses
        if (ctx.fromClause() != null) {
            visit(ctx.fromClause());
        }
        
        // Now visit other clauses
        if (ctx.selectClause() != null) {
            visit(ctx.selectClause());
        }
        
        if (ctx.whereClause() != null) {
            visit(ctx.whereClause());
        }
        
        if (ctx.groupByClause() != null) {
            visit(ctx.groupByClause());
        }
        
        if (ctx.havingClause() != null) {
            visit(ctx.havingClause());
        }
        
        if (ctx.orderByClause() != null) {
            visit(ctx.orderByClause());
        }
        
        return null;
    }
    
    @Override
    public Void visitUpdateStatement(UpdateStatementContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitDeleteStatement(DeleteStatementContext ctx) {
        return visitChildren(ctx);
    }
    
    // Override all the clause-level methods to ensure traversal
    @Override
    public Void visitSelectClause(SelectClauseContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitFromClause(FromClauseContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitWhereClause(WhereClauseContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitSelectItemList(SelectItemListContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitSelectItem(SelectItemContext ctx) {
        return visitChildren(ctx);
    }
    
    // For all expression types, traverse children
    @Override
    public Void visitPrimaryExpression(PrimaryExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitPrimary(PrimaryContext ctx) {
        return visitChildren(ctx);
    }
    
    // The key insight: BaseVisitor's implementation of each visit method returns defaultResult()
    // WITHOUT calling visitChildren(). So we need to override the base behavior.
    // The solution: for any context not explicitly overridden, call visitChildren!
    @Override
    public Void visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
        Void result = null;
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            org.antlr.v4.runtime.tree.ParseTree c = node.getChild(i);
            Void childResult = c.accept(this);
            // No need to aggregate, we're just traversing
        }
        return result;
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
            // Could be an entity alias or field name alone
            String name = identifiers.get(0).getText();
            // Check if it's a known alias - if so, don't treat as a field
            if (!aliasToEntity.containsKey(name) && currentEntity != null) {
                // Single identifier without dot - might be a field on implicit entity
                // But in HQL, fields must be qualified, so skip this case
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
        
        // Paths are leaf nodes (identifiers are terminals), so no children to visit
        return null;
    }
    
    // Override visit() to ensure all nodes are visited
    @Override
    public Void visit(org.antlr.v4.runtime.tree.ParseTree tree) {
        if (tree == null) {
            return null;
        }
        return tree.accept(this);
    }
    
    // For terminal nodes, just return null
    @Override
    public Void visitTerminal(org.antlr.v4.runtime.tree.TerminalNode node) {
        return null;
    }
    
    // For error nodes, just return null
    @Override
    public Void visitErrorNode(org.antlr.v4.runtime.tree.ErrorNode node) {
        return null;
    }
    
    @Override
    public Void visitParameter(ParameterContext ctx) {
        String paramText = ctx.getText();
        if (paramText.startsWith(":")) {
            analysis.addParameter(paramText.substring(1));
        } else if (paramText.startsWith("?")) {
            analysis.addParameter(paramText);
        }
        // Parameters are leaf nodes
        return null;
    }
    
    @Override
    public Void visitEntityName(EntityNameContext ctx) {
        String entityName = ctx.getText();
        if (!analysis.getEntityNames().contains(entityName)) {
            analysis.addEntity(entityName);
        }
        // EntityName is a leaf node (just an identifier)
        return null;
    }
}
