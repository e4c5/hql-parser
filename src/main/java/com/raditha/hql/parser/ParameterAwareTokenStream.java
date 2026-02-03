package com.raditha.hql.parser;

import com.raditha.hql.grammar.HQLLexer;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.WritableToken;

/**
 * A token stream that rewrites keyword tokens following a COLON to IDENTIFIER tokens.
 * This allows keywords like 'from', 'to', 'in', etc. to be used as parameter names
 * (e.g., :from, :to, :in) without modifying the grammar.
 *
 * <p>The rewriting happens after lexing but before parsing, keeping the grammar clean
 * while still supporting keyword parameter names.</p>
 */
public class ParameterAwareTokenStream extends CommonTokenStream {

    public ParameterAwareTokenStream(Lexer lexer) {
        super(lexer);
    }

    private boolean rewritten = false;

    /**
     * Ensures token rewriting happens before parsing begins.
     * Call this after creating the token stream and before creating the parser.
     */
    public void rewriteParameterNames() {
        if (!rewritten) {
            fill();
            rewriteKeywordsAfterColon();
            rewritten = true;
        }
    }

    @Override
    public void fill() {
        super.fill();
        // Also rewrite when fill is called directly
        if (!rewritten) {
            rewriteKeywordsAfterColon();
            rewritten = true;
        }
    }

    /**
     * Rewrites any keyword token that immediately follows a COLON token to an IDENTIFIER token.
     * This allows :from, :select, :in, etc. to be parsed as valid parameters.
     */
    private void rewriteKeywordsAfterColon() {
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);

            if (current.getType() == HQLLexer.COLON && isKeyword(next.getType())) {
                // Rewrite the keyword token to IDENTIFIER
                if (next instanceof WritableToken) {
                    ((WritableToken) next).setType(HQLLexer.IDENTIFIER);
                }
            }
        }
    }

    /**
     * Checks if a token type is a keyword that should be rewritten when used as a parameter name.
     * This includes all SQL/HQL keywords that might reasonably be used as parameter names.
     */
    private boolean isKeyword(int tokenType) {
        return tokenType == HQLLexer.SELECT
            || tokenType == HQLLexer.FROM
            || tokenType == HQLLexer.WHERE
            || tokenType == HQLLexer.UPDATE
            || tokenType == HQLLexer.DELETE
            || tokenType == HQLLexer.INSERT
            || tokenType == HQLLexer.INTO
            || tokenType == HQLLexer.SET
            || tokenType == HQLLexer.AS
            || tokenType == HQLLexer.NEW
            || tokenType == HQLLexer.JOIN
            || tokenType == HQLLexer.LEFT
            || tokenType == HQLLexer.RIGHT
            || tokenType == HQLLexer.INNER
            || tokenType == HQLLexer.OUTER
            || tokenType == HQLLexer.ON
            || tokenType == HQLLexer.FETCH
            || tokenType == HQLLexer.DISTINCT
            || tokenType == HQLLexer.GROUP
            || tokenType == HQLLexer.BY
            || tokenType == HQLLexer.HAVING
            || tokenType == HQLLexer.ORDER
            || tokenType == HQLLexer.ASC
            || tokenType == HQLLexer.DESC
            || tokenType == HQLLexer.AND
            || tokenType == HQLLexer.OR
            || tokenType == HQLLexer.NOT
            || tokenType == HQLLexer.IN
            || tokenType == HQLLexer.LIKE
            || tokenType == HQLLexer.BETWEEN
            || tokenType == HQLLexer.IS
            || tokenType == HQLLexer.NULL
            || tokenType == HQLLexer.TRUE
            || tokenType == HQLLexer.FALSE
            || tokenType == HQLLexer.EXISTS
            || tokenType == HQLLexer.MEMBER
            || tokenType == HQLLexer.OF
            || tokenType == HQLLexer.ESCAPE
            || tokenType == HQLLexer.CASE
            || tokenType == HQLLexer.WHEN
            || tokenType == HQLLexer.THEN
            || tokenType == HQLLexer.ELSE
            || tokenType == HQLLexer.END
            || tokenType == HQLLexer.AVG
            || tokenType == HQLLexer.COUNT
            || tokenType == HQLLexer.MAX
            || tokenType == HQLLexer.MIN
            || tokenType == HQLLexer.SUM
            || tokenType == HQLLexer.UPPER
            || tokenType == HQLLexer.LOWER
            || tokenType == HQLLexer.TRIM
            || tokenType == HQLLexer.LEADING
            || tokenType == HQLLexer.TRAILING
            || tokenType == HQLLexer.BOTH
            || tokenType == HQLLexer.LENGTH
            || tokenType == HQLLexer.CONCAT
            || tokenType == HQLLexer.SUBSTRING
            || tokenType == HQLLexer.CURRENT_DATE
            || tokenType == HQLLexer.CURRENT_TIME
            || tokenType == HQLLexer.CURRENT_TIMESTAMP
            || tokenType == HQLLexer.ABS
            || tokenType == HQLLexer.SQRT
            || tokenType == HQLLexer.MOD
            || tokenType == HQLLexer.SIZE
            || tokenType == HQLLexer.COALESCE
            || tokenType == HQLLexer.NULLIF
            || tokenType == HQLLexer.CAST
            || tokenType == HQLLexer.NULLS
            || tokenType == HQLLexer.FIRST
            || tokenType == HQLLexer.LAST;
    }
}
