package com.raditha.hql.parser;

import com.raditha.hql.grammar.HQLBaseVisitor;
import com.raditha.hql.grammar.HQLParser.*;
import com.raditha.hql.model.MetaData;

import java.util.*;

/**
 * Visitor that extracts entities, fields, and parameters from the parse tree.
 */
public class QueryAnalysisVisitor extends HQLBaseVisitor<Void> {
    
    private final MetaData analysis;
    private final Map<String, String> aliasToEntity = new HashMap<>();
    private String currentEntity = null;
    private boolean inUpdateStatement = false;
    private boolean inDeleteStatement = false;
    
    public QueryAnalysisVisitor(MetaData analysis) {
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
        inUpdateStatement = true;
        
        // Get the entity being updated
        if (ctx.entityName() != null) {
            currentEntity = ctx.entityName().getText();
            String alias = null;

            // Track alias if present
            if (ctx.identifier() != null) {
                alias = ctx.identifier().getText();
                aliasToEntity.put(alias, currentEntity);
            }

            analysis.addEntity(currentEntity, alias);
        }

        // Visit SET clause and WHERE clause
        if (ctx.setClause() != null) {
            visit(ctx.setClause());
        }
        
        if (ctx.whereClause() != null) {
            visit(ctx.whereClause());
        }

        inUpdateStatement = false;
        currentEntity = null;
        
        return null;
    }
    
    @Override
    public Void visitDeleteStatement(DeleteStatementContext ctx) {
        inDeleteStatement = true;
        
        // Get the entity being deleted
        if (ctx.entityName() != null) {
            currentEntity = ctx.entityName().getText();
            String alias = null;
            
            // Track alias if present
            if (ctx.identifier() != null) {
                alias = ctx.identifier().getText();
                aliasToEntity.put(alias, currentEntity);
            }
            
            analysis.addEntity(currentEntity, alias);
        }
        
        // Visit WHERE clause to extract fields
        if (ctx.whereClause() != null) {
            visit(ctx.whereClause());
        }
        
        inDeleteStatement = false;
        currentEntity = null;
        
        return null;
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
    
    @Override
    public Void visitConstructorExpression(ConstructorExpressionContext ctx) {
        // For constructor expressions, we want to:
        // 1. Skip the class name path (e.g., com.csi.bm.approval.model.to.UcafFormDTO)
        // 2. Visit the constructor arguments to extract field references
        
        // Visit constructor arguments only
        if (ctx.constructorArguments() != null) {
            visit(ctx.constructorArguments());
        }
        
        return null;
    }
    
    @Override
    public Void visitConstructorArguments(ConstructorArgumentsContext ctx) {
        // Visit all expression arguments
        return visitChildren(ctx);
    }
    
    // Explicitly handle expression types to ensure all children are visited
    @Override
    public Void visitBetweenExpression(BetweenExpressionContext ctx) {
        // Visit all three expressions: main, lower bound, upper bound
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitInExpression(InExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitComparisonExpression(ComparisonExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitEqualityExpression(EqualityExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitAndExpression(AndExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitOrExpression(OrExpressionContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitLikeExpression(LikeExpressionContext ctx) {
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
            c.accept(this);
            // No need to aggregate, we're just traversing
        }
        return result;
    }
    
    @Override
    public Void visitFromItem(FromItemContext ctx) {
        if (ctx.entityName() != null) {
            String entityName = ctx.entityName().getText();
            String alias = null;
            
            // Track alias
            if (ctx.identifier() != null) {
                alias = ctx.identifier().getText();
                aliasToEntity.put(alias, entityName);
            }
            
            analysis.addEntity(entityName, alias);
            currentEntity = entityName;
        }
        return visitChildren(ctx);
    }
    
    @Override
    public Void visitJoinClause(JoinClauseContext ctx) {
        if (ctx.path() != null) {
            // The path in a join clause is like "u.orders" where u is alias, orders is collection field
            // We need to infer the entity name from the collection field name
            String pathText = ctx.path().getText();
            String entityName = pathText;
            
            // Try to infer entity name from collection field
            if (pathText.contains(".")) {
                String[] parts = pathText.split("\\.");
                if (parts.length >= 2) {
                    String fieldName = parts[parts.length - 1];
                    
                    // Use heuristic to determine entity name from collection field
                    // e.g., "orders" → "Order", "users" → "User"
                    entityName = fieldName;
                    if (entityName.endsWith("s") && entityName.length() > 1) {
                        entityName = entityName.substring(0, entityName.length() - 1);
                    }
                    entityName = Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1);
                }
            }
            
            String alias = null;
            
            // Track alias if present
            if (ctx.identifier() != null) {
                alias = ctx.identifier().getText();
                aliasToEntity.put(alias, entityName);
            }
            
            analysis.addEntity(entityName, alias);
        }
        
        // Visit the ON condition if present
        if (ctx.expression() != null) {
            visit(ctx.expression());
        }
        
        return null;
    }
    
    @Override
    public Void visitPath(PathContext ctx) {
        List<IdentifierContext> identifiers = ctx.identifier();
        
        if (identifiers.size() == 1) {
            // Single identifier - could be:
            // 1. An entity/alias reference
            // 2. An unqualified field in UPDATE/DELETE
            String name = identifiers.get(0).getText();
            
            // Check if it's a known alias - if so, don't treat as a field
            if (!aliasToEntity.containsKey(name)) {
                // Not an alias, so it might be an unqualified field
                // In UPDATE/DELETE without alias, fields can be unqualified
                if ((inUpdateStatement || inDeleteStatement) && currentEntity != null) {
                    // Add this as a field of the current entity
                    analysis.addEntityField(currentEntity, name);
                }
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
