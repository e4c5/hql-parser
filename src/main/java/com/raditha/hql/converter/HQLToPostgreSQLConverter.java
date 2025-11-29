package com.raditha.hql.converter;

import com.raditha.hql.grammar.HQLBaseVisitor;
import com.raditha.hql.grammar.HQLLexer;
import com.raditha.hql.grammar.HQLParser;
import com.raditha.hql.grammar.HQLParser.*;
import com.raditha.hql.model.MetaData;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

/**
 * Converts HQL/JPQL queries to PostgreSQL SQL queries.
 * Uses QueryAnalysis for entity-to-table mapping information.
 */
public class HQLToPostgreSQLConverter {

    private final Map<String, String> entityToTableMap;
    private final Map<String, Map<String, String>> entityFieldToColumnMap;
    private Map<String, Map<String, JoinMapping>> relationshipMetadata; // entity name -> property -> JoinMapping

    public HQLToPostgreSQLConverter() {
        this.entityToTableMap = new HashMap<>();
        this.entityFieldToColumnMap = new HashMap<>();
        this.relationshipMetadata = new HashMap<>();
    }

    /**
     * Sets the relationship metadata for generating implicit join ON clauses.
     * 
     * @param relationshipMetadata Map of entity name (as used in HQL) to property
     *                             name to JoinMapping
     */
    public void setRelationshipMetadata(Map<String, Map<String, JoinMapping>> relationshipMetadata) {
        this.relationshipMetadata = relationshipMetadata != null ? relationshipMetadata : new HashMap<>();
    }

    /**
     * Register entity to table mapping.
     *
     * @param entityName The entity class name
     * @param tableName  The corresponding database table name
     */
    public void registerEntityMapping(String entityName, String tableName) {
        entityToTableMap.put(entityName, tableName);
    }

    /**
     * Register field to column mapping for an entity.
     * 
     * @param entityName The entity class name
     * @param fieldName  The field name in the entity
     * @param columnName The corresponding column name in the database
     */
    public void registerFieldMapping(String entityName, String fieldName, String columnName) {
        entityFieldToColumnMap
                .computeIfAbsent(entityName, k -> new HashMap<>())
                .put(fieldName, columnName);
    }

    /**
     * Converts an HQL/JPQL query to PostgreSQL SQL using QueryAnalysis.
     * 
     * @param hqlQuery The HQL/JPQL query string
     * @param analysis The QueryAnalysis containing entity and alias information
     * @return The converted PostgreSQL SQL query
     * @throws ConversionException if the query cannot be converted
     */
    public String convert(String hqlQuery, MetaData analysis) throws ConversionException {
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
                entityToTableMap, entityFieldToColumnMap, analysis, relationshipMetadata);

        return visitor.visit(tree);
    }

    /**
     * Visitor that converts HQL parse tree to PostgreSQL SQL.
     */
    private static class PostgreSQLConversionVisitor extends HQLBaseVisitor<String> {

        private final Map<String, String> entityToTableMap;
        private final Map<String, Map<String, String>> entityFieldToColumnMap;
        private final Map<String, Map<String, JoinMapping>> relationshipMetadata;
        private String currentEntity = null; // For UPDATE/DELETE without alias
        private String updateAlias = null; // For UPDATE with alias
        private final MetaData analysis;
        private final ImplicitJoinOnClauseGenerator onClauseGenerator;

        public PostgreSQLConversionVisitor(Map<String, String> entityToTableMap,
                Map<String, Map<String, String>> entityFieldToColumnMap,
                MetaData analysis,
                Map<String, Map<String, JoinMapping>> relationshipMetadata) {
            this.entityToTableMap = entityToTableMap;
            this.entityFieldToColumnMap = entityFieldToColumnMap;
            this.analysis = analysis;
            this.relationshipMetadata = relationshipMetadata != null ? relationshipMetadata : new HashMap<>();
            this.onClauseGenerator = new ImplicitJoinOnClauseGenerator();
        }

        @Override
        public String visitSelectStatement(SelectStatementContext ctx) {
            StringBuilder sql = new StringBuilder();

            // IMPORTANT: Visit FROM first to establish table context,
            // but don't append to SQL yet
            String fromSql = visit(ctx.fromClause());

            // Now visit SELECT with aliases resolved
            sql.append(visit(ctx.selectClause()));
            sql.append(" ");
            sql.append(fromSql);

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
            String expr;

            // Handle constructor expression: NEW path LP expressionList? RP
            // In SQL, we should just return the field list, not the constructor
            if (ctx.NEW() != null && ctx.path() != null) {
                // Convert "SELECT NEW DTO(field1, field2)" to "SELECT field1, field2"
                if (ctx.expressionList() != null) {
                    expr = visit(ctx.expressionList());
                } else {
                    // Constructor with no arguments - return empty string
                    expr = "";
                }
            } else if (ctx.expression() != null) {
                // Handle regular expression
                expr = visit(ctx.expression());
            } else {
                // Fallback: this should not happen with valid grammar
                expr = ctx.getText();
            }

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
            }

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
            // Handle JOIN FETCH where alias might be missing
            if (ctx.identifier() == null) {
                // If there is no alias, it's likely a FETCH join (e.g. LEFT JOIN FETCH path)
                // For SQL generation, we can typically ignore LEFT JOIN FETCH as it's for data
                // loading
                // and doesn't affect the main result set rows (unlike INNER JOIN).
                // Even for INNER JOIN FETCH, without an alias we can't easily generate the SQL
                // JOIN
                // so skipping it is the safest option to avoid a crash.
                return "";
            }

            StringBuilder sql = new StringBuilder();

            if (ctx.joinType() != null) {
                sql.append(visit(ctx.joinType())).append(" ");
            }

            sql.append("JOIN");

            String joinAlias = ctx.identifier().getText();
            String joinEntityName = analysis.getEntityForAlias(joinAlias);
            String tableName = entityToTableMap.getOrDefault(joinEntityName, joinEntityName.toLowerCase());

            sql.append(" ").append(tableName);
            sql.append(" ").append(joinAlias);

            if (ctx.expression() != null) {
                // Explicit ON clause - use it as-is
                sql.append(" ON ").append(visit(ctx.expression()));
            } else if (ctx.path() != null) {
                // Implicit join - generate ON clause from relationship metadata
                String pathText = ctx.path().getText();
                String[] parts = pathText.split("\\.");
                if (parts.length == 2) {
                    String sourceAlias = parts[0];
                    String propertyName = parts[1];
                    String sourceEntity = analysis.getEntityForAlias(sourceAlias);

                    if (sourceEntity != null) {
                        Map<String, JoinMapping> entityMappings = relationshipMetadata.get(sourceEntity);
                        if (entityMappings != null) {
                            JoinMapping mapping = entityMappings.get(propertyName);
                            if (mapping != null) {
                                String onClause = onClauseGenerator.generateOnClause(
                                        sourceAlias, pathText, joinAlias, mapping, analysis);
                                if (onClause != null) {
                                    sql.append(" ON ").append(onClause);
                                }
                            }
                        }
                    }
                }
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

            currentEntity = entityName;
            updateAlias = ctx.identifier() != null ? ctx.identifier().getText() : null;

            StringBuilder sql = new StringBuilder("UPDATE ");
            sql.append(tableName);

            // Include alias if present (required for PostgreSQL when using qualified column
            // references)
            if (updateAlias != null) {
                sql.append(" ");
                sql.append(updateAlias);
            }

            sql.append(" ");
            sql.append(visit(ctx.setClause()));

            if (ctx.whereClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.whereClause()));
            }

            currentEntity = null;
            updateAlias = null;

            return sql.toString();
        }

        @Override
        public String visitDeleteStatement(DeleteStatementContext ctx) {
            String entityName = ctx.entityName().getText();
            String tableName = entityToTableMap.getOrDefault(entityName, entityName.toLowerCase());

            currentEntity = entityName;

            StringBuilder sql = new StringBuilder("DELETE FROM ");
            sql.append(tableName);

            if (ctx.whereClause() != null) {
                sql.append(" ");
                sql.append(visit(ctx.whereClause()));
            }

            currentEntity = null;

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
            // In UPDATE statements, PostgreSQL requires unqualified column names on the LHS
            // e.g., "UPDATE table t SET col = value" not "UPDATE table t SET t.col = value"
            PathContext pathCtx = ctx.path();
            List<IdentifierContext> ids = pathCtx.identifier();
            String lhs;

            if (ids.size() == 1) {
                // Unqualified field: resolve to column name
                lhs = resolveColumnForField(currentEntity, ids.get(0).getText());
            } else if (ids.size() >= 2) {
                // Qualified field (alias.field): drop alias if it's the update alias
                String first = ids.get(0).getText();
                String second = ids.get(1).getText();
                String entityName = analysis.getEntityForAlias(first);
                if (entityName == null)
                    entityName = first;

                // If this matches the update alias, drop it for the assignment LHS
                if (updateAlias != null && first.equals(updateAlias) && entityName.equals(currentEntity)) {
                    lhs = resolveColumnForField(entityName, second);
                } else {
                    // Keep qualification for other aliases (shouldn't happen in standard UPDATE)
                    lhs = first + "." + resolveColumnForField(entityName, second);
                }
            } else {
                lhs = pathCtx.getText();
            }

            return lhs + " = " + visit(ctx.expression());
        }

        private String resolveColumnForField(String entityName, String fieldName) {
            if (entityName != null && entityFieldToColumnMap.containsKey(entityName)) {
                Map<String, String> mapping = entityFieldToColumnMap.get(entityName);
                if (mapping.containsKey(fieldName)) {
                    return mapping.get(fieldName);
                }
            }
            // Fallback: apply snake_case conversion for unmapped fields
            return toSnakeCase(fieldName);
        }

        private String toSnakeCase(String input) {
            if (input == null || input.isEmpty())
                return input;
            if (input.indexOf('_') >= 0 || input.equals(input.toLowerCase()))
                return input;
            StringBuilder sb = new StringBuilder();
            char[] chars = input.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (Character.isUpperCase(c)) {
                    if (i > 0 && (Character.isLowerCase(chars[i - 1]) || Character.isDigit(chars[i - 1]) ||
                            (i + 1 < chars.length && Character.isLowerCase(chars[i + 1])))) {
                        sb.append('_');
                    }
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        @Override
        public String visitPath(PathContext ctx) {
            List<IdentifierContext> identifiers = ctx.identifier();

            if (identifiers.size() == 1) {
                // Unqualified field - check if we can map it
                String fieldName = identifiers.get(0).getText();

                // If we're in UPDATE/DELETE and have a current entity, try to map the field
                if (currentEntity != null && entityFieldToColumnMap.containsKey(currentEntity)) {
                    Map<String, String> fieldMappings = entityFieldToColumnMap.get(currentEntity);
                    if (fieldMappings.containsKey(fieldName)) {
                        return fieldMappings.get(fieldName);
                    }
                }

                // Apply snake_case fallback for unmapped fields in UPDATE/DELETE context
                if (currentEntity != null) {
                    return toSnakeCase(fieldName);
                }

                // No mapping found, return as-is
                return fieldName;
            } else if (identifiers.size() >= 2) {
                String first = identifiers.get(0).getText();
                String second = identifiers.get(1).getText();

                // Check if first is an alias - use QueryAnalysis instead of local map
                String entityName = analysis.getEntityForAlias(first);
                boolean isAlias = (entityName != null);

                if (!isAlias) {
                    // Check if it is a known entity
                    if (entityToTableMap.containsKey(first)) {
                        entityName = first;
                    } else {
                        // Not an alias and not a known entity - likely a fully qualified name (enum,
                        // constant)
                        return ctx.getText();
                    }
                }

                // Get column name mapping
                String columnName = second;
                if (entityFieldToColumnMap.containsKey(entityName) &&
                        entityFieldToColumnMap.get(entityName).containsKey(second)) {
                    columnName = entityFieldToColumnMap.get(entityName).get(second);
                } else {
                    // Apply snake_case fallback for unmapped fields
                    columnName = toSnakeCase(columnName);
                }

                StringBuilder result = new StringBuilder(first).append(".").append(columnName);

                // Append any remaining parts of the path
                for (int i = 2; i < identifiers.size(); i++) {
                    result.append(".").append(identifiers.get(i).getText());
                }

                return result.toString();
            }

            return ctx.getText();
        }

        @Override
        public String visitParameter(ParameterContext ctx) {
            // PostgreSQL uses $1, $2, etc., but we'll keep the original format for
            // simplicity
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
        public String visitStatement(StatementContext ctx) {
            // Don't use default aggregation for statement - handle each child explicitly
            if (ctx.selectStatement() != null) {
                return visit(ctx.selectStatement());
            } else if (ctx.updateStatement() != null) {
                return visit(ctx.updateStatement());
            } else if (ctx.deleteStatement() != null) {
                return visit(ctx.deleteStatement());
            } else if (ctx.insertStatement() != null) {
                return visit(ctx.insertStatement());
            }
            return "";
        }

        // Override all expression visitor methods to handle them explicitly
        @Override
        public String visitPrimaryExpression(PrimaryExpressionContext ctx) {
            return visit(ctx.primary());
        }

        @Override
        public String visitEqualityExpression(EqualityExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " " + ctx.op.getText() + " " + visit(expressions.get(1));
        }

        @Override
        public String visitComparisonExpression(ComparisonExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " " + ctx.op.getText() + " " + visit(expressions.get(1));
        }

        @Override
        public String visitAdditiveExpression(AdditiveExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " " + ctx.op.getText() + " " + visit(expressions.get(1));
        }

        @Override
        public String visitMultiplicativeExpression(MultiplicativeExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " " + ctx.op.getText() + " " + visit(expressions.get(1));
        }

        @Override
        public String visitAndExpression(AndExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " AND " + visit(expressions.get(1));
        }

        @Override
        public String visitOrExpression(OrExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return visit(expressions.get(0)) + " OR " + visit(expressions.get(1));
        }

        @Override
        public String visitNotExpression(NotExpressionContext ctx) {
            return "NOT " + visit(ctx.expression());
        }

        @Override
        public String visitIsNullExpression(IsNullExpressionContext ctx) {
            String result = visit(ctx.expression()) + " IS";
            if (ctx.NOT() != null) {
                result += " NOT";
            }
            result += " NULL";
            return result;
        }

        @Override
        public String visitBetweenExpression(BetweenExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            String result = visit(expressions.get(0));
            if (ctx.NOT() != null) {
                result += " NOT";
            }
            result += " BETWEEN " + visit(expressions.get(1)) + " AND " + visit(expressions.get(2));
            return result;
        }

        @Override
        public String visitInExpression(InExpressionContext ctx) {
            String result = visit(ctx.expression());
            if (ctx.NOT() != null) {
                result += " NOT";
            }
            result += " IN (";
            if (ctx.expressionList() != null) {
                result += visit(ctx.expressionList());
            } else if (ctx.selectStatement() != null) {
                result += visit(ctx.selectStatement());
            }
            result += ")";
            return result;
        }

        @Override
        public String visitInParameterExpression(InParameterExpressionContext ctx) {
            String result = visit(ctx.expression());
            if (ctx.NOT() != null) {
                result += " NOT";
            }
            result += " IN (";
            result += visit(ctx.parameter());
            result += ")";
            return result;
        }

        @Override
        public String visitLikeExpression(LikeExpressionContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            String result = visit(expressions.get(0));
            if (ctx.NOT() != null) {
                result += " NOT";
            }
            result += " LIKE " + visit(expressions.get(1));
            if (ctx.STRING() != null) {
                result += " ESCAPE " + ctx.STRING().getText();
            }
            return result;
        }

        @Override
        public String visitMemberOfExpression(MemberOfExpressionContext ctx) {
            String result = visit(ctx.expression()) + " MEMBER";
            if (ctx.OF() != null) {
                result += " OF";
            }
            result += " " + visit(ctx.path());
            return result;
        }

        @Override
        public String visitExistsExpression(ExistsExpressionContext ctx) {
            return "EXISTS (" + visit(ctx.selectStatement()) + ")";
        }

        @Override
        public String visitParenthesizedExpression(ParenthesizedExpressionContext ctx) {
            return "(" + visit(ctx.expression()) + ")";
        }

        @Override
        public String visitMemberAccessExpression(MemberAccessExpressionContext ctx) {
            return visit(ctx.expression()) + "." + ctx.identifier().getText();
        }

        @Override
        public String visitFunctionCallExpression(FunctionCallExpressionContext ctx) {
            return visit(ctx.functionCall());
        }

        @Override
        public String visitCaseExpr(CaseExprContext ctx) {
            return visit(ctx.caseExpression());
        }

        @Override
        public String visitPrimary(PrimaryContext ctx) {
            if (ctx.literal() != null) {
                return ctx.literal().getText();
            } else if (ctx.path() != null) {
                return visit(ctx.path());
            } else if (ctx.parameter() != null) {
                return visit(ctx.parameter());
            } else if (ctx.selectStatement() != null) {
                return "(" + visit(ctx.selectStatement()) + ")";
            }
            return "";
        }

        @Override
        public String visitCaseExpression(CaseExpressionContext ctx) {
            StringBuilder sb = new StringBuilder("CASE");
            List<ExpressionContext> expressions = ctx.expression();
            int whenClauseCount = ctx.whenClause().size();

            // Determine if this is a simple CASE (CASE expr WHEN...) or searched CASE (CASE
            // WHEN...)
            // Simple CASE has: 1 initial expr + (2 * whenClauseCount) + (ELSE ? 1 : 0)
            // Searched CASE has: (2 * whenClauseCount) + (ELSE ? 1 : 0)
            int expectedSearchedCaseExprs = whenClauseCount * 2 + (ctx.ELSE() != null ? 1 : 0);
            int expectedSimpleCaseExprs = 1 + whenClauseCount * 2 + (ctx.ELSE() != null ? 1 : 0);

            boolean isSimpleCase = (expressions != null && expressions.size() == expectedSimpleCaseExprs);
            int exprIndex = 0;

            // For simple CASE, add the initial expression
            if (isSimpleCase && !expressions.isEmpty()) {
                sb.append(" ").append(visit(expressions.get(exprIndex++)));
            }

            // Add WHEN clauses - but since whenClause visitor handles its own expressions,
            // we just visit each whenClause
            for (WhenClauseContext when : ctx.whenClause()) {
                sb.append(" ").append(visit(when));
            }

            // Add ELSE clause if present
            if (ctx.ELSE() != null && expressions != null) {
                // The ELSE expression is the last one in the list
                sb.append(" ELSE ").append(visit(expressions.get(expressions.size() - 1)));
            }

            sb.append(" END");
            return sb.toString();
        }

        @Override
        public String visitWhenClause(WhenClauseContext ctx) {
            List<ExpressionContext> expressions = ctx.expression();
            return "WHEN " + visit(expressions.get(0)) + " THEN " + visit(expressions.get(1));
        }

        @Override
        public String visitFunctionCall(FunctionCallContext ctx) {
            // Handle specific function patterns
            List<ExpressionContext> expressions = ctx.expression();
            if (ctx.AVG() != null) {
                String distinct = ctx.DISTINCT() != null ? "DISTINCT " : "";
                return "AVG(" + distinct + visit(expressions.get(0)) + ")";
            } else if (ctx.COUNT() != null) {
                if (ctx.STAR() != null) {
                    return "COUNT(*)";
                }
                String distinct = ctx.DISTINCT() != null ? "DISTINCT " : "";
                return "COUNT(" + distinct + visit(expressions.get(0)) + ")";
            } else if (ctx.SUM() != null) {
                String distinct = ctx.DISTINCT() != null ? "DISTINCT " : "";
                return "SUM(" + distinct + visit(expressions.get(0)) + ")";
            } else if (ctx.MAX() != null) {
                return "MAX(" + visit(expressions.get(0)) + ")";
            } else if (ctx.MIN() != null) {
                return "MIN(" + visit(expressions.get(0)) + ")";
            } else if (ctx.UPPER() != null) {
                return "UPPER(" + visit(expressions.get(0)) + ")";
            } else if (ctx.LOWER() != null) {
                return "LOWER(" + visit(expressions.get(0)) + ")";
            } else if (ctx.LENGTH() != null) {
                return "LENGTH(" + visit(expressions.get(0)) + ")";
            } else if (ctx.ABS() != null) {
                return "ABS(" + visit(expressions.get(0)) + ")";
            } else if (ctx.SQRT() != null) {
                return "SQRT(" + visit(expressions.get(0)) + ")";
            } else if (ctx.CURRENT_DATE() != null) {
                return "CURRENT_DATE";
            } else if (ctx.CURRENT_TIME() != null) {
                return "CURRENT_TIME";
            } else if (ctx.CURRENT_TIMESTAMP() != null) {
                return "CURRENT_TIMESTAMP";
            } else if (ctx.CONCAT() != null) {
                return "CONCAT(" + visit(ctx.expressionList()) + ")";
            } else if (ctx.COALESCE() != null) {
                return "COALESCE(" + visit(ctx.expressionList()) + ")";
            } else if (ctx.SIZE() != null) {
                return "SIZE(" + visit(ctx.path()) + ")";
            }
            // Default for custom functions
            if (ctx.identifier() != null) {
                String args = ctx.expressionList() != null ? visit(ctx.expressionList()) : "";
                return ctx.identifier().getText() + "(" + args + ")";
            }
            return "";
        }

        @Override
        protected String aggregateResult(String aggregate, String nextResult) {
            // Should not be called anymore since we handle everything explicitly
            if (aggregate == null) {
                return nextResult;
            }
            if (nextResult == null) {
                return aggregate;
            }
            return aggregate + nextResult;
        }

        @Override
        protected String defaultResult() {
            return "";
        }
    }
}
