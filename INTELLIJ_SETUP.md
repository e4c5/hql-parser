# IntelliJ IDEA Setup Guide

This guide explains how to properly import and configure this project in IntelliJ IDEA.

## Issue: Missing ANTLR Generated Files

If IntelliJ IDEA shows errors like "Cannot find symbol: HQLLexer" or "Cannot find com.raditha.hql.grammar", this is because the ANTLR-generated source files haven't been created or aren't recognized by the IDE.

## Solution

### Option 1: Reimport Maven Project (Recommended)

1. Open the project in IntelliJ IDEA
2. Open the Maven tool window (View → Tool Windows → Maven)
3. Click the "Reload All Maven Projects" button (circular arrows icon)
4. Run Maven generate-sources:
   - In the Maven tool window: `hql-parser → Lifecycle → clean` 
   - Then: `hql-parser → Lifecycle → generate-sources`
5. IntelliJ should now recognize the generated sources in `target/generated-sources/antlr4/`

### Option 2: Manual Directory Marking

If the Maven import doesn't automatically mark the directory:

1. Run `mvn clean generate-sources` from the terminal
2. In IntelliJ, right-click on `target/generated-sources/antlr4` directory
3. Select "Mark Directory as" → "Generated Sources Root"

### Option 3: Clean Reimport

If you're still having issues:

1. Close IntelliJ IDEA
2. Delete the `.idea` directory in the project root
3. Run `mvn clean compile` from terminal
4. Reopen the project in IntelliJ IDEA
5. When prompted, import as a Maven project
6. Wait for indexing to complete

## Understanding the Project Structure

### Source Directories

- `src/main/java/` - Main Java source code
- `src/main/antlr4/` - ANTLR grammar files (`.g4` files)
- `src/test/java/` - Test code
- `target/generated-sources/antlr4/` - **ANTLR-generated Java files** (created by Maven)

### ANTLR Generated Files

The ANTLR Maven plugin generates the following files in `target/generated-sources/antlr4/com/raditha/hql/grammar/`:

- `HQLLexer.java` - Tokenizer for HQL queries
- `HQLParser.java` - Parser for HQL queries (ANTLR-generated)
- `HQLBaseVisitor.java` - Base visitor class
- `HQLBaseListener.java` - Base listener class
- Various context classes for parse tree nodes

**Note:** The project also has a custom `HQLParser.java` in `src/main/java/com/raditha/hql/parser/` which wraps the ANTLR-generated parser. Don't confuse these two!

## Build Configuration

The `pom.xml` includes:

1. **antlr4-maven-plugin** - Generates Java files from the `.g4` grammar
2. **build-helper-maven-plugin** - Tells Maven and IDEs about the generated sources directory

## Common Issues

### "Package does not exist" errors
Run `mvn clean generate-sources` to regenerate the ANTLR files.

### Changes to `.g4` file not reflected
1. Run `mvn clean` to remove old generated files
2. Run `mvn generate-sources` to regenerate
3. Refresh the project in IntelliJ (Ctrl+Shift+O or Cmd+Shift+I on Mac)

### IntelliJ doesn't see changes after Maven build
Use "File → Invalidate Caches / Restart" if IntelliJ's index is stuck.

## Development Workflow

1. Make changes to grammar in `src/main/antlr4/com/raditha/hql/grammar/HQL.g4`
2. Run `mvn clean compile` to regenerate and compile
3. IntelliJ should automatically detect the changes
4. If not, click the "Reload All Maven Projects" button in the Maven tool window

## Verifying Setup

After setup, you should be able to:

- Navigate to `com.raditha.hql.grammar.HQLLexer` (Ctrl+N / Cmd+O)
- See no compilation errors in the Problems view
- Run tests successfully from IntelliJ

If you can do all of the above, your setup is correct!
