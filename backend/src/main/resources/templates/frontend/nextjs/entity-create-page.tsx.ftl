<#-- Determine primary key field -->
<#assign pkField = "id">
<#list table.columns as col>
<#if col.primaryKey><#assign pkField = col.fieldName></#if>
</#list>
<#assign route = table.name?lower_case>
<#-- Helper to get form field type -->
<#function formType javaType>
  <#if javaType == "Long" || javaType == "Integer" || javaType == "int" || javaType == "long" || javaType == "Double" || javaType == "Float" || javaType == "BigDecimal" || javaType == "double" || javaType == "float">
    <#return "number">
  <#elseif javaType == "Boolean" || javaType == "boolean">
    <#return "checkbox">
  <#elseif javaType == "LocalDate">
    <#return "date">
  <#elseif javaType == "LocalDateTime" || javaType == "Instant" || javaType == "ZonedDateTime">
    <#return "datetime-local">
  <#else>
    <#return "text">
  </#if>
</#function>
<#-- Helper to get default value -->
<#function defaultVal javaType>
  <#if javaType == "Long" || javaType == "Integer" || javaType == "int" || javaType == "long" || javaType == "Double" || javaType == "Float" || javaType == "BigDecimal" || javaType == "double" || javaType == "float">
    <#return "0">
  <#elseif javaType == "Boolean" || javaType == "boolean">
    <#return "false">
  <#else>
    <#return '""'>
  </#if>
</#function>
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiPost } from "@/lib/api";
import { ${table.className} } from "@/types";
import FormField from "@/components/ui/form-field";
import Button from "@/components/ui/button";

export default function ${table.className}CreatePage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<Partial<${table.className}>>({
<#list table.columns as column>
<#if !column.primaryKey || !column.autoIncrement>
    ${column.fieldName}: ${defaultVal(column.javaType)},
</#if>
</#list>
  });

  function handleChange(name: string, value: string | number | boolean) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      setSaving(true);
      setError(null);
      await apiPost("/api/${route}", form);
      router.push("/${route}");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Create ${table.className}</h1>
        <Link href="/${route}">
          <Button variant="secondary">Cancel</Button>
        </Link>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="card space-y-4">
<#list table.columns as column>
<#if !column.primaryKey || !column.autoIncrement>
        <FormField
          label="${column.fieldName}"
          name="${column.fieldName}"
          type="${formType(column.javaType)}"
          value={form.${column.fieldName} ?? ${defaultVal(column.javaType)}}
          onChange={handleChange}
          required={${(!column.nullable && !column.primaryKey)?c}}
        />
</#if>
</#list>
        <div className="flex justify-end space-x-3 pt-4">
          <Link href="/${route}">
            <Button variant="secondary" type="button">Cancel</Button>
          </Link>
          <Button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Create"}
          </Button>
        </div>
      </form>
    </div>
  );
}
