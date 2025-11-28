grammar HQL;

// Parser Rules
statement
    : selectStatement
    | updateStatement
    | deleteStatement
    | insertStatement
    ;

selectStatement
    : selectClause fromClause whereClause? groupByClause? havingClause? orderByClause?
    ;

updateStatement
    : UPDATE entityName (AS? identifier)? setClause whereClause?
    ;

deleteStatement
    : DELETE FROM? entityName (AS? identifier)? whereClause?
    ;

insertStatement
    : INSERT INTO entityName LP identifierList RP selectStatement
    ;

selectClause
    : SELECT DISTINCT? selectItemList
    ;

selectItemList
    : selectItem (COMMA selectItem)*
    ;

selectItem
    : NEW path LP expressionList? RP (AS? identifier)?  // Constructor expression
    | expression (AS? identifier)?
    ;

fromClause
    : FROM fromItem (COMMA fromItem)*
    ;

fromItem
    : entityName (AS? identifier)? joinClause*
    ;

joinClause
    : joinType? JOIN FETCH? path (AS? identifier)? (ON expression)?
    ;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    ;

whereClause
    : WHERE expression
    ;

groupByClause
    : GROUP BY expressionList
    ;

havingClause
    : HAVING expression
    ;

orderByClause
    : ORDER BY orderByItem (COMMA orderByItem)*
    ;

orderByItem
    : expression (ASC | DESC)? (NULLS (FIRST | LAST))?
    ;

setClause
    : SET assignment (COMMA assignment)*
    ;

assignment
    : path EQ expression
    ;

expression
    : primary                                                       # PrimaryExpression
    | expression DOT identifier                                    # MemberAccessExpression
    | functionCall                                                 # FunctionCallExpression
    | LP expression RP                                             # ParenthesizedExpression
    | NOT expression                                               # NotExpression
    | expression op=(STAR | SLASH | PERCENT) expression           # MultiplicativeExpression
    | expression op=(PLUS | MINUS) expression                     # AdditiveExpression
    | expression op=(LT | LE | GT | GE) expression                # ComparisonExpression
    | expression IS NOT? NULL                                      # IsNullExpression
    | expression NOT? BETWEEN expression AND expression           # BetweenExpression
    | expression NOT? IN LP (expressionList | selectStatement) RP # InExpression
    | expression NOT? LIKE expression (ESCAPE STRING)?            # LikeExpression
    | expression MEMBER OF? path                                   # MemberOfExpression
    | EXISTS LP selectStatement RP                                 # ExistsExpression
    | expression op=(EQ | NE) expression                          # EqualityExpression
    | expression AND expression                                    # AndExpression
    | expression OR expression                                     # OrExpression
    | caseExpression                                               # CaseExpr
    ;

primary
    : literal
    | path
    | parameter
    | LP selectStatement RP
    ;

path
    : identifier (DOT identifier)*
    ;

parameter
    : COLON identifier
    | QUESTION_MARK INTEGER?
    ;

functionCall
    : AVG LP DISTINCT? expression RP
    | COUNT LP (DISTINCT? expression | STAR) RP
    | MAX LP expression RP
    | MIN LP expression RP
    | SUM LP DISTINCT? expression RP
    | UPPER LP expression RP
    | LOWER LP expression RP
    | TRIM LP (LEADING | TRAILING | BOTH)? (STRING FROM)? expression RP
    | LENGTH LP expression RP
    | CONCAT LP expressionList RP
    | SUBSTRING LP expression COMMA expression (COMMA expression)? RP
    | CURRENT_DATE
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | SIZE LP path RP
    | ABS LP expression RP
    | SQRT LP expression RP
    | MOD LP expression COMMA expression RP
    | COALESCE LP expressionList RP
    | NULLIF LP expression COMMA expression RP
    | CAST LP expression AS identifier RP
    | identifier LP expressionList? RP  // Custom functions
    ;

caseExpression
    : CASE whenClause+ (ELSE expression)? END
    | CASE expression whenClause+ (ELSE expression)? END
    ;

whenClause
    : WHEN expression THEN expression
    ;

expressionList
    : expression (COMMA expression)*
    ;

identifierList
    : identifier (COMMA identifier)*
    ;

literal
    : STRING
    | INTEGER
    | DECIMAL
    | TRUE
    | FALSE
    | NULL
    ;

entityName
    : identifier
    ;

identifier
    : IDENTIFIER
    | nonReservedWord
    ;

nonReservedWord
    : SIZE
    | CAST
    | NULLS
    | FIRST
    | LAST
    | END
    ;

// Lexer Rules
SELECT      : [Ss][Ee][Ll][Ee][Cc][Tt];
FROM        : [Ff][Rr][Oo][Mm];
WHERE       : [Ww][Hh][Ee][Rr][Ee];
UPDATE      : [Uu][Pp][Dd][Aa][Tt][Ee];
DELETE      : [Dd][Ee][Ll][Ee][Tt][Ee];
INSERT      : [Ii][Nn][Ss][Ee][Rr][Tt];
INTO        : [Ii][Nn][Tt][Oo];
SET         : [Ss][Ee][Tt];
AS          : [Aa][Ss];
NEW         : [Nn][Ee][Ww];
JOIN        : [Jj][Oo][Ii][Nn];
LEFT        : [Ll][Ee][Ff][Tt];
RIGHT       : [Rr][Ii][Gg][Hh][Tt];
INNER       : [Ii][Nn][Nn][Ee][Rr];
OUTER       : [Oo][Uu][Tt][Ee][Rr];
ON          : [Oo][Nn];
FETCH       : [Ff][Ee][Tt][Cc][Hh];
DISTINCT    : [Dd][Ii][Ss][Tt][Ii][Nn][Cc][Tt];
GROUP       : [Gg][Rr][Oo][Uu][Pp];
BY          : [Bb][Yy];
HAVING      : [Hh][Aa][Vv][Ii][Nn][Gg];
ORDER       : [Oo][Rr][Dd][Ee][Rr];
ASC         : [Aa][Ss][Cc];
DESC        : [Dd][Ee][Ss][Cc];
AND         : [Aa][Nn][Dd];
OR          : [Oo][Rr];
NOT         : [Nn][Oo][Tt];
IN          : [Ii][Nn];
LIKE        : [Ll][Ii][Kk][Ee];
BETWEEN     : [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
IS          : [Ii][Ss];
NULL        : [Nn][Uu][Ll][Ll];
TRUE        : [Tt][Rr][Uu][Ee];
FALSE       : [Ff][Aa][Ll][Ss][Ee];
EXISTS      : [Ee][Xx][Ii][Ss][Tt][Ss];
MEMBER      : [Mm][Ee][Mm][Bb][Ee][Rr];
OF          : [Oo][Ff];
ESCAPE      : [Ee][Ss][Cc][Aa][Pp][Ee];
CASE        : [Cc][Aa][Ss][Ee];
WHEN        : [Ww][Hh][Ee][Nn];
THEN        : [Tt][Hh][Ee][Nn];
ELSE        : [Ee][Ll][Ss][Ee];
END         : [Ee][Nn][Dd];
NULLS       : [Nn][Uu][Ll][Ll][Ss];
FIRST       : [Ff][Ii][Rr][Ss][Tt];
LAST        : [Ll][Aa][Ss][Tt];

// Aggregate functions
AVG         : [Aa][Vv][Gg];
COUNT       : [Cc][Oo][Uu][Nn][Tt];
MAX         : [Mm][Aa][Xx];
MIN         : [Mm][Ii][Nn];
SUM         : [Ss][Uu][Mm];

// String functions
UPPER       : [Uu][Pp][Pp][Ee][Rr];
LOWER       : [Ll][Oo][Ww][Ee][Rr];
TRIM        : [Tt][Rr][Ii][Mm];
LEADING     : [Ll][Ee][Aa][Dd][Ii][Nn][Gg];
TRAILING    : [Tt][Rr][Aa][Ii][Ll][Ii][Nn][Gg];
BOTH        : [Bb][Oo][Tt][Hh];
LENGTH      : [Ll][Ee][Nn][Gg][Tt][Hh];
CONCAT      : [Cc][Oo][Nn][Cc][Aa][Tt];
SUBSTRING   : [Ss][Uu][Bb][Ss][Tt][Rr][Ii][Nn][Gg];

// Date/Time functions
CURRENT_DATE      : [Cc][Uu][Rr][Rr][Ee][Nn][Tt]'_'[Dd][Aa][Tt][Ee];
CURRENT_TIME      : [Cc][Uu][Rr][Rr][Ee][Nn][Tt]'_'[Tt][Ii][Mm][Ee];
CURRENT_TIMESTAMP : [Cc][Uu][Rr][Rr][Ee][Nn][Tt]'_'[Tt][Ii][Mm][Ee][Ss][Tt][Aa][Mm][Pp];

// Math functions
ABS         : [Aa][Bb][Ss];
SQRT        : [Ss][Qq][Rr][Tt];
MOD         : [Mm][Oo][Dd];

// Other functions
SIZE        : [Ss][Ii][Zz][Ee];
COALESCE    : [Cc][Oo][Aa][Ll][Ee][Ss][Cc][Ee];
NULLIF      : [Nn][Uu][Ll][Ll][Ii][Ff];
CAST        : [Cc][Aa][Ss][Tt];

// Operators
EQ          : '=';
NE          : '!=' | '<>';
LT          : '<';
LE          : '<=';
GT          : '>';
GE          : '>=';
PLUS        : '+';
MINUS       : '-';
STAR        : '*';
SLASH       : '/';
PERCENT     : '%';
DOT         : '.';
COMMA       : ',';
LP          : '(';
RP          : ')';
COLON       : ':';
QUESTION_MARK : '?';

// Literals
STRING      : '\'' (~['\r\n] | '\'\'')* '\'';
INTEGER     : [0-9]+;
DECIMAL     : [0-9]+ '.' [0-9]* | '.' [0-9]+;
IDENTIFIER  : [a-zA-Z_] [a-zA-Z0-9_]*;

// Whitespace
WS          : [ \t\r\n]+ -> skip;

// Comments
LINE_COMMENT    : '--' ~[\r\n]* -> skip;
BLOCK_COMMENT   : '/*' .*? '*/' -> skip;
