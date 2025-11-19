# HQL/JPQL Parser

A comprehensive Java parser for Hibernate Query Language (HQL) and Java Persistence Query Language (JPQL) with PostgreSQL conversion support.

## Features

- ✅ **Complete HQL/JPQL Grammar Support**: Parse SELECT, UPDATE, DELETE, and INSERT statements
- ✅ **Query Analysis**: Extract entities, fields, parameters, and aliases from queries
- ✅ **PostgreSQL Conversion**: Convert HQL/JPQL queries to PostgreSQL SQL
- ✅ **Entity Detection**: Identify all Java entity classes involved in a query
- ✅ **Field Mapping**: Track entity fields referenced in queries
- ✅ **Parameter Extraction**: Identify both named (:param) and positional (?1) parameters
- ✅ **Join Support**: Handle INNER, LEFT, and RIGHT joins
- ✅ **Aggregate Functions**: Support for COUNT, SUM, AVG, MAX, MIN
- ✅ **Query Validation**: Check if queries are syntactically correct

## Requirements

- Java 11 or higher (project targets Java 11, developed with Java 21)
- Maven 3.6+

## IDE Setup

**IntelliJ IDEA Users:** If you're getting "Cannot find symbol" errors for ANTLR-generated classes, see [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md) for configuration instructions.

## Installation

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.raditha</groupId>
    <artifactId>hql-parser</artifactId>
    <version>0.0.2</version>
</dependency>
```

### Build from Source

```bash
git clone <repository-url>
cd hql-parser
mvn clean install
```

## Quick Start

### 1. Parse and Validate Queries

```java
import com.raditha.hql.parser.HQLParser;

HQLParser parser = new HQLParser();

String query = "SELECT u FROM User u WHERE u.age > 18";

if (parser.isValid(query)) {
    System.out.println("Query is valid!");
}
```

### 2. Analyze Queries

Extract entities, fields, and parameters from HQL queries:

```java
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.model.MetaData;

HQLParser parser = new HQLParser();

String query = "SELECT u.name, u.email FROM User u WHERE u.active = true AND u.age > :minAge";

MetaData analysis = parser.analyze(query);

System.out.println("Query Type: " + analysis.getQueryType());
System.out.println("Entities: " + analysis.getEntityNames());
System.out.println("Fields: " + analysis.getEntityFields());
System.out.println("Parameters: " + analysis.getParameters());
```

**Output:**
```
Query Type: SELECT
Entities: [User]
Fields: {User=[name, email, active, age]}
Parameters: [minAge]
```

### 3. Convert to PostgreSQL

Convert HQL/JPQL queries to PostgreSQL SQL:

```java
import com.raditha.hql.parser.HQLParser;
import com.raditha.hql.model.MetaData;
import com.raditha.hql.converter.HQLToPostgreSQLConverter;

HQLParser parser = new HQLParser();
HQLToPostgreSQLConverter converter = new HQLToPostgreSQLConverter();

// Register entity-to-table mappings
converter.registerEntityMapping("User", "users");
converter.registerEntityMapping("Order", "orders");

// Register field-to-column mappings
converter.registerFieldMapping("User", "userName", "user_name");
converter.registerFieldMapping("User", "firstName", "first_name");

String hql = "SELECT u.userName FROM User u WHERE u.active = true";

// First analyze the query
MetaData analysis = parser.analyze(hql);

// Then convert using both the query and analysis
String sql = converter.convert(hql, analysis);

System.out.println("SQL: " + sql);
```

**Output:**
```
SQL: SELECT u.user_name FROM users u WHERE u.active = true
```

## Usage Examples

### Complex Query with Joins

```java
HQLParser parser = new HQLParser();

String query = "SELECT u.name, o.total " +
              "FROM User u " +
              "INNER JOIN u.orders o " +
              "WHERE u.active = true AND o.total > 100 " +
              "ORDER BY o.total DESC";

MetaData analysis = parser.analyze(query);

System.out.println("Entities: " + analysis.getEntityNames());
System.out.println("Aliases: " + analysis.getAliases());
```

### Constructor Expressions

```java
HQLParser parser = new HQLParser();

// Constructor expressions allow creating DTOs directly in queries
String query = "SELECT NEW com.example.dto.UserDTO(u.name, u.email) " +
              "FROM User u WHERE u.active = true";

MetaData analysis = parser.analyze(query);
System.out.println("Query Type: " + analysis.getQueryType()); // SELECT
System.out.println("Entities: " + analysis.getEntityNames()); // [User]
System.out.println("Fields: " + analysis.getEntityFields()); // {User=[name, email, active]}

// Works with BETWEEN clause and parameters
String query2 = "SELECT NEW dto.ReportDTO(r.id, r.total) " +
               "FROM Report r WHERE r.date BETWEEN :start AND :end";
MetaData analysis2 = parser.analyze(query2);
System.out.println("Parameters: " + analysis2.getParameters()); // [start, end]
```

### Update Queries

```java
HQLParser parser = new HQLParser();

String query = "UPDATE User SET active = false WHERE lastLogin < :cutoffDate";

MetaData analysis = parser.analyze(query);
System.out.println("Query Type: " + analysis.getQueryType()); // UPDATE
System.out.println("Parameters: " + analysis.getParameters()); // [cutoffDate]
```

### Delete Queries

```java
// DELETE without alias
String query = "DELETE FROM User WHERE age < 18";

MetaData analysis = parser.analyze(query);
System.out.println("Query Type: " + analysis.getQueryType()); // DELETE

// DELETE with alias (for complex WHERE clauses)
String query2 = "DELETE FROM Purchase p WHERE p.status = 'CANCELLED' AND p.createdDate < :cutoffDate";
MetaData analysis2 = parser.analyze(query2);
System.out.println("Fields: " + analysis2.getEntityFields()); // {Purchase=[status, createdDate]}
```

## Supported HQL/JPQL Features

### Query Types
- SELECT statements with projection
- Constructor expressions with `SELECT NEW ClassName(args...)`
- UPDATE statements (with/without alias)
- DELETE statements (with/without alias)
- INSERT ... SELECT statements (grammar support only - parsing works but SQL conversion not yet implemented)

### Clauses
- SELECT with DISTINCT
- Constructor expressions for creating DTOs directly in queries
- FROM with entity aliases
- WHERE with complex predicates
- GROUP BY
- HAVING
- ORDER BY (ASC/DESC, NULLS FIRST/LAST)

### Joins
- INNER JOIN
- LEFT [OUTER] JOIN
- RIGHT [OUTER] JOIN
- FETCH joins

### Operators
- Comparison: =, !=, <>, <, <=, >, >=
- Logical: AND, OR, NOT
- Arithmetic: +, -, *, /, %
- Special: BETWEEN, IN, LIKE, IS NULL, MEMBER OF, EXISTS

### Functions
- Aggregate: COUNT, SUM, AVG, MAX, MIN
- String: UPPER, LOWER, TRIM, LENGTH, CONCAT, SUBSTRING
- Date/Time: CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP
- Math: ABS, SQRT, MOD
- Other: COALESCE, NULLIF, CAST, SIZE

**Note:** The parser supports all these functions in the grammar, but the PostgreSQL converter currently only implements conversion for: COUNT, SUM, AVG, MAX, MIN, UPPER, LOWER, LENGTH, CONCAT, COALESCE, SIZE, ABS, SQRT, CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP. Functions like TRIM, SUBSTRING, NULLIF, CAST, and MOD are parsed but not yet converted to SQL.

### Parameters
- Named parameters: `:paramName` (avoid using HQL keywords like `:end`, `:and` as parameter names)
- Positional parameters: `?1`, `?2`, etc.

## API Documentation

### HQLParser

Main parser class for HQL/JPQL queries.

**Methods:**
- `ParseTree parse(String query)` - Parse query and return parse tree
- `MetaData analyze(String query)` - Analyze query and extract metadata
- `boolean isValid(String query)` - Validate query syntax

### MetaData

Contains analysis results of a parsed query.

**Methods:**
- `QueryType getQueryType()` - Get query type (SELECT, UPDATE, DELETE, INSERT)
- `Set<String> getEntityNames()` - Get all entity names referenced
- `Map<String, Set<String>> getEntityFields()` - Get fields for each entity
- `List<String> getAliases()` - Get all aliases used
- `Set<String> getParameters()` - Get all parameters
- `Map<String, String> getAliasToEntity()` - Get mapping of aliases to entity names
- `String getEntityForAlias(String alias)` - Get entity name for a given alias

### HQLToPostgreSQLConverter

Converts HQL/JPQL to PostgreSQL SQL.

**Methods:**
- `void registerEntityMapping(String entityName, String tableName)` - Map entity to table
- `void registerFieldMapping(String entityName, String fieldName, String columnName)` - Map field to column
- `String convert(String hqlQuery, MetaData analysis)` - Convert HQL to SQL using query analysis

## Project Structure

```
hql-parser/
├── src/
│   ├── main/
│   │   ├── java/com/raditha/hql/
│   │   │   ├── converter/       # SQL conversion
│   │   │   ├── examples/        # Usage examples
│   │   │   ├── model/           # Data models
│   │   │   └── parser/          # Core parser and analysis
│   │   └── antlr4/com/raditha/hql/grammar/
│   │       └── HQL.g4           # ANTLR grammar definition
│   └── test/
│       └── java/com/raditha/hql/
│           ├── HQLParserTest.java
│           ├── AdvancedHQLParserTest.java
│           ├── PostgreSQLConverterTest.java
│           ├── AdvancedConverterTest.java
│           └── JoinAnalysisTest.java
├── pom.xml
└── README.md
```

## Running Tests

```bash
mvn test
```

## Running Examples

```bash
mvn compile exec:java -Dexec.mainClass="com.raditha.hql.examples.UsageExamples"
```

## Building

```bash
# Compile and generate parser from grammar
mvn clean compile

# Run tests
mvn test

# Create JAR
mvn package
```

## Current Limitations

### Parser Limitations

1. **Parameter Naming**: Avoid using HQL keywords (like `end`, `and`, `or`) as parameter names. Use descriptive names like `:endDate`, `:startDate` instead.

2. **Subquery Limitations**: While subqueries are parsed, entity/field extraction from deeply nested subqueries may be incomplete.

3. **Collection Join Analysis**: For implicit joins (e.g., `u.orders`), the parser infers entity names using simple heuristics (singularization + capitalization). This may not work for irregular plurals or custom naming.

### Converter Limitations

1. **Implicit Join ON Clauses**: HQL allows implicit joins without ON clauses (using JPA metadata). The converter cannot generate ON clauses automatically - you must provide explicit ON clauses for SQL compatibility.
   ```java
   // HQL (implicit ON clause based on metadata)
   "FROM User u INNER JOIN u.orders o"
   
   // SQL requires explicit ON clause
   "FROM users u INNER JOIN orders o ON o.user_id = u.id"
   ```

2. **Parameter Format**: The converter keeps HQL parameter format (`:param`, `?1`) rather than converting to PostgreSQL format (`$1`, `$2`). You'll need to handle parameter binding separately.

3. **FETCH Joins**: FETCH joins are HQL-specific for eager loading. The converter ignores the FETCH keyword, converting them to regular joins.

4. **Entity Metadata Requirements**: The converter requires explicit entity-to-table and field-to-column mappings. It does not introspect JPA annotations or Hibernate configuration.

5. **Nested Paths**: Paths like `u.address.city` are parsed but may not convert correctly if intermediate relationships aren't mapped.

6. **Collection Functions**: HQL-specific functions like `SIZE()`, `MEMBER OF` may not have direct PostgreSQL equivalents.

7. **Incomplete Function Support**: While the parser grammar supports all HQL/JPQL functions, the converter currently only implements conversion for: COUNT, SUM, AVG, MAX, MIN, UPPER, LOWER, LENGTH, CONCAT, COALESCE, SIZE, ABS, SQRT, CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP. Functions like TRIM, SUBSTRING, NULLIF, CAST, and MOD are parsed but passed through as-is without proper conversion.

8. **INSERT Statement Conversion**: While INSERT ... SELECT statements can be parsed, the converter does not yet implement SQL conversion for INSERT statements. Only SELECT, UPDATE, and DELETE statements are fully supported for conversion.

### Field Extraction Behavior

1. **UPDATE/DELETE Statements**: 
   - With alias (e.g., `UPDATE User u SET u.active = false`): Fields referenced with alias are extracted
   - Without alias (e.g., `UPDATE User SET active = false`): Unqualified fields are extracted and mapped in conversion

2. **Unqualified Fields**: In UPDATE/DELETE statements without aliases, unqualified field names are mapped to columns using registered field mappings. This is the expected behavior but differs from SELECT queries where fields are typically qualified with aliases.

### Grammar Coverage

While the parser supports most common HQL/JPQL features, some advanced features are not yet implemented:
- `TREAT()` operator for polymorphic queries
- `INDEX()` function for indexed collections
- `KEY()` and `VALUE()` functions for map collections
- `TYPE()` operator for inheritance queries
- Bulk INSERT with VALUES clause (only INSERT ... SELECT supported)

## Grammar

The HQL/JPQL grammar is defined in `src/main/antlr4/com/raditha/hql/grammar/HQL.g4` using ANTLR4. 

The grammar supports:
- Case-insensitive keywords
- Line and block comments
- String literals with escape sequences
- Numeric literals (integers and decimals)
- Identifiers and paths

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Author

Raditha Dissanayake

## Acknowledgments

- Built with [ANTLR4](https://www.antlr.org/)
- Implements HQL/JPQL grammar based on Hibernate and JPA specifications
