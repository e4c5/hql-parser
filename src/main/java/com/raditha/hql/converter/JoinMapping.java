package com.raditha.hql.converter;

/**
 * Represents the mapping information for entity relationships and joins.
 * This class contains information about how entity relationships should be
 * converted to SQL joins, including join columns and join types.
 */
public class JoinMapping {
    private String propertyName;
    private String targetEntity;
    private String joinColumn;
    private String referencedColumn = "id";
    private String sourceTable;
    private String targetTable;
    private JoinType joinType = JoinType.LEFT;

    public JoinMapping() {
    }

    public JoinMapping(String propertyName, String joinColumn, String sourceTable, String targetTable) {
        this.propertyName = propertyName;
        this.joinColumn = joinColumn;
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
    }

    public String propertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String targetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String joinColumn() {
        return joinColumn;
    }

    public void setJoinColumn(String joinColumn) {
        this.joinColumn = joinColumn;
    }

    public String referencedColumn() {
        return referencedColumn;
    }

    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }

    public JoinType joinType() {
        return joinType;
    }

    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }

    public String sourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String targetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }
}

