package com.raditha.hql.converter;

import com.raditha.hql.grammar.HQLBaseVisitor;
import com.raditha.hql.grammar.HQLLexer;
import com.raditha.hql.grammar.HQLParser;
import com.raditha.hql.grammar.HQLParser.*;
import com.raditha.hql.parser.ParseException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/**
 * Converts HQL/JPQL queries to PostgreSQL SQL queries.
 * Requires entity-to-table mapping configuration.
 */
public class HQLToPostgreSQLConverter {
    
    private final Map<String, String> entityToTableMap;
    private final Map<String, Map<String, String>> entityFieldToColumnMap;
    
    public HQLToPostgreSQLConverter() {
        this.entityToTableMap = new HashMap<>();
        this.entityFieldToColumnMap = new HashMap<>();
    }
    
    /**
     * Register entity to table mapping.
     * 
     * @param entityName The entity class name
     * @param tableName The corresponding database table name
     */
    public void registerEntityMapping(String entityName, String tableName) {
        entityToTableMap.put(entityName, tableName);
    }
    
    /**
     * Register field to column mapping for an entity.
     * 
     * @param entityName The entity class name
     * @param fieldName The field name in the entity
     * @param columnName The corresponding column name in the database
     */
    public void registerFieldMapping(String entityName, String fieldName, String columnName) {
        entityFieldToColumnMap
            .computeIfAbsent(entityName, k -> new HashMap<>())
            .put(fieldName, columnName);
    }
    
    /**
     * Converts an HQL/JPQL query to PostgreSQL SQL.
     * 
     * @param hqlQuery The HQL/JPQL query string
     * @return The converted PostgreSQL SQL query
     * @throws ParseException if the query cannot be parsed
     * @throws ConversionException if the query cannot be converted
     */
    public String convert(String hqlQuery) throws ParseException, ConversionException {
        try {
            CharStream input = CharStreams.fromString(hqlQuery);
            HQLLexer lexer = new HQLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            HQLParser parser = new HQLParser(tokens);
            
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                       int line, int charPositionInLine, String msg, RecognitionException e) {
                    throw new RuntimeException("Parse error at " + line + ":" + charPositionInLine + " " + msg);
                }
            });
            
            ParseTree tree = parser.statement();
            
            PostgreSQLConversionVisitor visitor = new PostgreSQLConversionVisitor(
                entityToTableMap, entityFieldToColumnMap
            );
            
            return visitor.visit(tree);
        } catch (Exception e) {
            throw new ConversionException("Failed to convert HQL to PostgreSQL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Visitor that converts HQL parse tree to PostgreSQL SQL.
     */
    private static class PostgreSQLConversionVisitor extends HQLBaseVisitor<String> {
        
        private final Map<String, String> entityToTableMap;
        private final Map<String, Map<String, String>> entityFieldToColumnMap;
        private final Map<String, String> aliasToEntity = new HashMap<>();
        
        public PostgreSQLConversionVisitor(Map<String, String> entityToTableMap,
                                          Map<String, Map<String, String>> entityFieldToColumnMap) {
            this.entityToTableMap = entityToTableMap;
            this.entityFieldToColumnMap = entityFieldToColumnMap;
        }
        
        @Override
        public String visitStatement(StatementContext ctx) {
            return visitChildren(ctx);
        }
        
        @Override
        public String visitSelectStatement(SelectStatementContext ctx) {
            StringBuilder sql = new StringBuilder();
            
            sql.append(visit(ctx.selectClause()));
            sql.append(" ");
            sql.append(visit(ctx.fromClause()));
            
            if (ctx.whereClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.whereClause()));
            }
            
            if (ctx.groupByClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.groupByClause()));
            }
            
            if (ctx.havingClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.havingClause()));
            }
            
            if (ctx.orderByClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.orderByClause()));
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitSelectClause(SelectClauseContext ctx) {
            StringBuilder sql = new StringBuilder("SELECT");
            
            if (ctx.DISTINCT() != null) {
                sql.append(" DISTINCT");
            }
            
            sql.append(" ");
            sql.append(visit(ctx.selectItemList()));
            
            return sql.toString();
        }
        
        @Override
        public String visitSelectItemList(SelectItemListContext ctx) {
            List<String> items = new ArrayList<>();
            for (SelectItemContext item : ctx.selectItem()) {
                items.add(visit(item));
            }
            return String.join(", ", items);
        }
        
        @Override
        public String visitSelectItem(SelectItemContext ctx) {
            String expr = visit(ctx.expression());
            
            if (ctx.identifier() != null) {
                expr += " AS " + ctx.identifier().getText();
            }
            
            return expr;
        }
        
        @Override
        public String visitFromClause(FromClauseContext ctx) {
            List<String> items = new ArrayList<>();
            for (FromItemContext item : ctx.fromItem()) {
                items.add(visit(item));
            }
            return "FROM " + String.join(", ", items);
        }
        
        @Override
        public String visitFromItem(FromItemContext ctx) {
            String entityName = ctx.entityName().getText();
            String tableName = entityToTableMap.getOrDefault(entityName, entityName.toLowerCase());
            
            StringBuilder sql = new StringBuilder(tableName);
            
            if (ctx.identifier() != null) {
                String alias = ctx.identifier().getText();
                sql.append(" ").append(alias);
                aliasToEntity.put(alias, entityName);
            }
            
            // Handle joins
            if (ctx.joinClause() != null && !ctx.joinClause().isEmpty()) {
                for (JoinClauseContext join : ctx.joinClause()) {
                    sql.append(" ");
                    sql.append(visit(join));
                }
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitJoinClause(JoinClauseContext ctx) {
            StringBuilder sql = new StringBuilder();
            
            if (ctx.joinType() != null) {
                sql.append(visit(ctx.joinType())).append(" ");
            }
            
            sql.append("JOIN");
            
            // Note: FETCH is ignored in SQL conversion
            
            String joinPath = visit(ctx.path());
            sql.append(" ").append(joinPath);
            
            if (ctx.identifier() != null) {
                sql.append(" ").append(ctx.identifier().getText());
            }
            
            if (ctx.expression() != null) {
                sql.append(" ON ").append(visit(ctx.expression()));
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitJoinType(JoinTypeContext ctx) {
            if (ctx.INNER() != null) {
                return "INNER";
            } else if (ctx.LEFT() != null) {
                return ctx.OUTER() != null ? "LEFT OUTER" : "LEFT";
            } else if (ctx.RIGHT() != null) {
                return ctx.OUTER() != null ? "RIGHT OUTER" : "RIGHT";
            }
            return "";
        }
        
        @Override
        public String visitWhereClause(WhereClauseContext ctx) {
            return "WHERE " + visit(ctx.expression());
        }
        
        @Override
        public String visitGroupByClause(GroupByClauseContext ctx) {
            return "GROUP BY " + visit(ctx.expressionList());
        }
        
        @Override
        public String visitHavingClause(HavingClauseContext ctx) {
            return "HAVING " + visit(ctx.expression());
        }
        
        @Override
        public String visitOrderByClause(OrderByClauseContext ctx) {
            List<String> items = new ArrayList<>();
            for (OrderByItemContext item : ctx.orderByItem()) {
                items.add(visit(item));
            }
            return "ORDER BY " + String.join(", ", items);
        }
        
        @Override
        public String visitOrderByItem(OrderByItemContext ctx) {
            StringBuilder sql = new StringBuilder(visit(ctx.expression()));
            
            if (ctx.ASC() != null) {
                sql.append(" ASC");
            } else if (ctx.DESC() != null) {
                sql.append(" DESC");
            }
            
            if (ctx.NULLS() != null) {
                sql.append(" NULLS ");
                sql.append(ctx.FIRST() != null ? "FIRST" : "LAST");
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitUpdateStatement(UpdateStatementContext ctx) {
            String entityName = ctx.entityName().getText();
            String tableName = entityToTableMap.getOrDefault(entityName, entityName.toLowerCase());
            
            StringBuilder sql = new StringBuilder("UPDATE ");
            sql.append(tableName);
            sql.append(" ");
            sql.append(visit(ctx.setClause()));
            
            if (ctx.whereClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.whereClause()));
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitDeleteStatement(DeleteStatementContext ctx) {
            String entityName = ctx.entityName().getText();
            String tableName = entityToTableMap.getOrDefault(entityName, entityName.toLowerCase());
            
            StringBuilder sql = new StringBuilder("DELETE FROM ");
            sql.append(tableName);
            
            if (ctx.whereClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.whereClause()));
            }
            
            return sql.toString();
        }
        
        @Override
        public String visitSetClause(SetClauseContext ctx) {
            List<String> assignments = new ArrayList<>();
            for (AssignmentContext assignment : ctx.assignment()) {
                assignments.add(visit(assignment));
            }
            return "SET " + String.join(", ", assignments);
        }
        
        @Override
        public String visitAssignment(AssignmentContext ctx) {
            return visit(ctx.path()) + " = " + visit(ctx.expression());
        }
        
        @Override
        public String visitPath(PathContext ctx) {
            List<IdentifierContext> identifiers = ctx.identifier();
            
            if (identifiers.size() == 1) {
                return identifiers.get(0).getText();
            } else if (identifiers.size() >= 2) {
                String first = identifiers.get(0).getText();
                String second = identifiers.get(1).getText();
                
                // Check if first is an alias
                String entityName = aliasToEntity.getOrDefault(first, first);
                
                // Get column name mapping
                String columnName = second;
                if (entityFieldToColumnMap.containsKey(entityName) &&
                    entityFieldToColumnMap.get(entityName).containsKey(second)) {
                    columnName = entityFieldToColumnMap.get(entityName).get(second);
                }
                
                return first + "." + columnName;
            }
            
            return ctx.getText();
        }
        
        @Override
        public String visitParameter(ParameterContext ctx) {
            // PostgreSQL uses $1, $2, etc., but we'll keep the original format for simplicity
            return ctx.getText();
        }
        
        @Override
        public String visitExpressionList(ExpressionListContext ctx) {
            List<String> expressions = new ArrayList<>();
            for (ExpressionContext expr : ctx.expression()) {
                expressions.add(visit(expr));
            }
            return String.join(", ", expressions);
        }
        
        @Override
        protected String aggregateResult(String aggregate, String nextResult) {
            if (aggregate == null) {
                return nextResult;
            }
            if (nextResult == null) {
                return aggregate;
            }
            return aggregate + " " + nextResult;
        }
        
        @Override
        protected String defaultResult() {
            return "";
        }
    }
}
