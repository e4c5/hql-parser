package com.raditha.hql.parser;

import com.raditha.hql.grammar.HQLLexer;
import com.raditha.hql.model.MetaData;
import com.raditha.hql.model.QueryType;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Main parser class for HQL/JPQL queries.
 * Provides methods to parse and analyze HQL/JPQL queries.
 */
public class HQLParser {
    
    /**
     * Parses an HQL/JPQL query and returns the parse tree.
     * 
     * @param query The HQL/JPQL query string
     * @return The parse tree root node
     * @throws ParseException if the query is syntactically invalid
     */
    public ParseTree parse(String query) throws ParseException {
        try {
            CharStream input = CharStreams.fromString(query);
            HQLLexer lexer = new HQLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            com.raditha.hql.grammar.HQLParser parser = new com.raditha.hql.grammar.HQLParser(tokens);
            
            // Add error listener
            parser.removeErrorListeners();
            ErrorListener errorListener = new ErrorListener();
            parser.addErrorListener(errorListener);
            
            ParseTree tree = parser.statement();
            
            if (errorListener.hasErrors()) {
                throw new ParseException("Parse errors: " + errorListener.getErrors());
            }
            
            return tree;
        } catch (Exception e) {
            throw new ParseException("Failed to parse query: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyzes an HQL/JPQL query and extracts information about entities, fields, and parameters.
     * 
     * @param query The HQL/JPQL query string
     * @return QueryAnalysis object containing the analysis results
     * @throws ParseException if the query is syntactically invalid
     */
    public MetaData analyze(String query) throws ParseException {
        ParseTree tree = parse(query);
        
        // Determine query type
        QueryType queryType = determineQueryType(tree);
        MetaData analysis = new MetaData(query, queryType);
        
        // Walk the parse tree to extract information
        QueryAnalysisVisitor visitor = new QueryAnalysisVisitor(analysis);
        visitor.visit(tree);
        
        return analysis;
    }
    
    /**
     * Validates if a query is syntactically correct.
     * 
     * @param query The HQL/JPQL query string
     * @return true if valid, false otherwise
     */
    public boolean isValid(String query) {
        try {
            parse(query);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
    
    private QueryType determineQueryType(ParseTree tree) {
        String rootRule = tree.getClass().getSimpleName();
        if (rootRule.contains("Select")) {
            return QueryType.SELECT;
        } else if (rootRule.contains("Update")) {
            return QueryType.UPDATE;
        } else if (rootRule.contains("Delete")) {
            return QueryType.DELETE;
        } else if (rootRule.contains("Insert")) {
            return QueryType.INSERT;
        }
        
        // Fallback: check first child
        if (tree.getChildCount() > 0) {
            ParseTree firstChild = tree.getChild(0);
            String childRule = firstChild.getClass().getSimpleName();
            if (childRule.contains("Select")) {
                return QueryType.SELECT;
            } else if (childRule.contains("Update")) {
                return QueryType.UPDATE;
            } else if (childRule.contains("Delete")) {
                return QueryType.DELETE;
            } else if (childRule.contains("Insert")) {
                return QueryType.INSERT;
            }
        }
        
        return QueryType.SELECT; // Default
    }
    
    /**
     * Custom error listener for collecting parse errors.
     */
    private static class ErrorListener extends BaseErrorListener {
        private final StringBuilder errors = new StringBuilder();
        private boolean hasErrors = false;
        
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                               int line, int charPositionInLine, String msg, RecognitionException e) {
            hasErrors = true;
            errors.append("Line ").append(line).append(":").append(charPositionInLine)
                  .append(" ").append(msg).append("\n");
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
        
        public String getErrors() {
            return errors.toString();
        }
    }
}
