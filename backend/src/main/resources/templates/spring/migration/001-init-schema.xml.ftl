<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="001-init-schema" author="generator">
<#list tables as table>
<#if !table.joinTable>
        <createTable tableName="${table.name}">
<#list table.columns as col>
            <column name="${col.name}" type="${col.type}">
<#if col.primaryKey>
                <constraints primaryKey="true" nullable="false"/>
<#elseif !col.nullable || col.unique>
                <constraints<#if !col.nullable> nullable="false"</#if><#if col.unique> unique="true"</#if>/>
</#if>
            </column>
</#list>
        </createTable>

</#if>
</#list>
<#list tables as table>
<#if table.joinTable>
        <createTable tableName="${table.name}">
<#list table.columns as col>
            <column name="${col.name}" type="${col.type}">
<#if !col.nullable>
                <constraints nullable="false"/>
</#if>
            </column>
</#list>
        </createTable>

</#if>
</#list>
<#list tables as table>
<#list table.columns as col>
<#if col.foreignKey>
        <addForeignKeyConstraint
            constraintName="fk_${table.name}_${col.name}"
            baseTableName="${table.name}"
            baseColumnNames="${col.name}"
            referencedTableName="${col.referencedTable}"
            referencedColumnNames="${col.referencedColumn}"/>

</#if>
</#list>
</#list>
    </changeSet>

</databaseChangeLog>
