package com.raditha.hql.converter;

/**
 * Represents the mapping information for entity relationships and joins.
 * This class contains information about how entity relationships should be
 * converted to SQL joins, including join columns and join types.
 */
public class JoinMapping {
    private final String propertyName;
    private final String targetEntity;
    private final String joinColumn;
    private final String referencedColumn;
    private final JoinType joinType;
    private final String sourceTable;
    private final String targetTable;

    public JoinMapping(String propertyName, String targetEntity, String joinColumn, String referencedColumn,
                       JoinType joinType, String sourceTable, String targetTable) {
        this.propertyName = propertyName;
        this.targetEntity = targetEntity;
        this.joinColumn = joinColumn;
        this.referencedColumn = referencedColumn;
        this.joinType = joinType;
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
    }

    public String propertyName() {
        return propertyName;
    }

    public String targetEntity() {
        return targetEntity;
    }

    public String joinColumn() {
        return joinColumn;
    }

    public String referencedColumn() {
        return referencedColumn;
    }

    public JoinType joinType() {
        return joinType;
    }

    public String sourceTable() {
        return sourceTable;
    }

    public String targetTable() {
        return targetTable;
    }
}

