# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Java-based HQL/JPQL parser built with ANTLR4 that parses Hibernate Query Language and Java Persistence Query Language, analyzes queries, and converts them to PostgreSQL SQL. The project uses Maven for build management and targets Java 11.

## Essential Commands

### Build and Compile
```bash
# Clean and compile (includes ANTLR grammar generation)
mvn clean compile

# Generate ANTLR sources only (useful for IDE setup)
mvn generate-sources

# Create JAR package
mvn package

# Full build with install
mvn clean install
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=HQLParserTest

# Run a specific test method
mvn test -Dtest=HQLParserTest#testSimpleSelectQuery
```

### Running Examples
```bash
mvn compile exec:java -Dexec.mainClass="com.raditha.hql.examples.UsageExamples"
```

### ANTLR Grammar Development
The ANTLR grammar is at `src/main/antlr4/com/raditha/hql/grammar/HQL.g4`. After modifying it:
- Run `mvn clean compile` to regenerate parser/lexer classes
- Generated code appears in `target/generated-sources/antlr4/`

### IDE Setup (IntelliJ IDEA)
If IntelliJ shows "Cannot find symbol: HQLLexer" errors:
1. Run `mvn generate-sources` to create ANTLR-generated files
2. In IntelliJ Maven tool window, click "Reload All Maven Projects"
3. The `target/generated-sources/antlr4/` directory should be marked as "Generated Sources Root"
4. See [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md) for detailed troubleshooting

## Architecture

### Core Components

**Parser Layer** (`com.raditha.hql.parser`):
- `HQLParser`: Main entry point - parses HQL queries using ANTLR-generated parsers
- `QueryAnalysisVisitor`: Walks the parse tree to extract metadata (entities, fields, parameters)
- `ParseException`: Custom exception for parse errors

**Model Layer** (`com.raditha.hql.model`):
- `QueryAnalysis`: Immutable result object containing query metadata
- `QueryType`: Enum for SELECT, UPDATE, DELETE, INSERT

**Converter Layer** (`com.raditha.hql.converter`):
- `HQLToPostgreSQLConverter`: Converts HQL to SQL with entity/field mapping support
- Uses visitor pattern with `PostgreSQLConversionVisitor` (inner class) to traverse parse tree
- Requires registration of entity→table and field→column mappings before conversion

**Grammar** (`src/main/antlr4/com/raditha/hql/grammar/HQL.g4`):
- Single combined ANTLR4 grammar defining both lexer and parser rules
- Supports: SELECT/UPDATE/DELETE/INSERT, joins, aggregates, case expressions, parameters
- Case-insensitive keywords

### Design Patterns

- **Visitor Pattern**: Used extensively for both query analysis and SQL conversion
- **Builder Pattern**: `QueryAnalysis` accumulates data via add methods
- **Factory Pattern**: `HQLParser.analyze()` creates `QueryAnalysis` instances
- **Error Handling**: Custom error listeners capture parse errors without throwing immediately

### Data Flow

1. Raw HQL string → ANTLR Lexer/Parser → Parse Tree
2. Parse Tree → QueryAnalysisVisitor → QueryAnalysis (for analysis)
3. Parse Tree → PostgreSQLConversionVisitor → SQL string (for conversion)

## Important Implementation Notes

### Working with ANTLR-Generated Code
- Generated parsers/lexers are in `com.raditha.hql.grammar` package
- Don't manually edit generated files - they're recreated on each compile
- The `HQLParser` class wraps `com.raditha.hql.grammar.HQLParser` to avoid confusion

### Query Analysis
- Entities and fields are tracked per-entity in `Map<String, Set<String>>`
- Parameters are stored without prefix (`:userName` → `userName`)
- Aliases are preserved in order as they appear

### SQL Conversion Requirements
- Entity mappings MUST be registered before calling `convert()`
- Field mappings are optional - unmapped fields use their original name
- Alias-to-entity tracking is maintained during conversion
- FETCH joins are silently ignored (HQL-specific)

### Testing with JUnit 5 and AssertJ
- Tests use fluent AssertJ assertions (`assertThat()`)
- Each test should focus on a single HQL feature
- Use `@BeforeEach` to initialize fresh `HQLParser` instances

## Common Development Patterns

### Adding Support for New HQL Features
1. Update `src/main/antlr4/com/raditha/hql/grammar/HQL.g4`
2. Run `mvn clean compile` to regenerate parser
3. Update `QueryAnalysisVisitor` if metadata extraction needed
4. Update `PostgreSQLConversionVisitor` in converter if SQL conversion needed
5. Add tests to `HQLParserTest` and `PostgreSQLConverterTest`

### Creating New Visitors
- Extend `HQLBaseVisitor<T>` where `T` is the return type
- Override `aggregateResult()` and `defaultResult()` for accumulation logic
- Visit specific context methods (e.g., `visitSelectStatement()`)

### Parameter Handling
- Named parameters: `:paramName` in HQL
- Positional parameters: `?1`, `?2` in HQL
- PostgreSQL conversion currently keeps original format (not converted to `$1`, `$2`)

## Project Dependencies

- **ANTLR 4.13.1**: Parser generation runtime
- **JUnit 5.10.1**: Testing framework
- **AssertJ 3.24.2**: Fluent assertions for tests
- **Java 11+**: Minimum required version
