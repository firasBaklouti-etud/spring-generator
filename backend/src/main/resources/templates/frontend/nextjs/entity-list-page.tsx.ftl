<#-- Determine primary key field -->
<#assign pkField = "id">
<#assign pkType = "number">
<#list table.columns as col>
<#if col.primaryKey>
<#assign pkField = col.fieldName>
<#if col.javaType == "String">
<#assign pkType = "string">
<#else>
<#assign pkType = "number">
</#if>
</#if>
</#list>
<#-- Compute route -->
<#assign route = table.name?lower_case>
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { apiGet, apiDelete } from "@/lib/api";
import { ${table.className} } from "@/types";
import DataTable from "@/components/ui/data-table";
import Button from "@/components/ui/button";
import Modal from "@/components/ui/modal";

export default function ${table.className}ListPage() {
  const router = useRouter();
  const [items, setItems] = useState<${table.className}[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<${table.className} | null>(null);
  const [page, setPage] = useState(0);
  const pageSize = 20;

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const data = await apiGet<${table.className}[]>("/api/${route}");
      setItems(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load data");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await apiDelete(${r"`/api/${route}/${deleteTarget."} + pkField + ${r"}`"});
      setDeleteTarget(null);
      loadData();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  }

  const filtered = items.filter((item) =>
    Object.values(item).some((v) =>
      String(v).toLowerCase().includes(search.toLowerCase())
    )
  );

  const paginated = filtered.slice(page * pageSize, (page + 1) * pageSize);
  const totalPages = Math.ceil(filtered.length / pageSize);

  if (loading) return <div className="py-8 text-center text-gray-500">Loading...</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">${table.className}</h1>
        <Link href="/${route}/new">
          <Button>Create New</Button>
        </Link>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>
      )}

      <div className="mb-4">
        <input
          type="text"
          placeholder="Search..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="input-field max-w-sm"
        />
      </div>

      <DataTable<${table.className}>
        keyField="${pkField}"
        columns={[
<#list table.columns as column>
          { key: "${column.fieldName}", label: "${column.fieldName}" },
</#list>
        ]}
        rows={paginated}
        onRowClick={(row) => router.push(${r"`/${route}/${row."} + pkField + ${r"}`"})}
        actions={(row) => (
          <div className="flex justify-end space-x-2">
            <Link href={${r"`/${route}/${row."} + pkField + ${r"}/edit`"}} onClick={(e) => e.stopPropagation()}>
              <Button variant="secondary" size="sm">Edit</Button>
            </Link>
            <Button
              variant="danger"
              size="sm"
              onClick={() => setDeleteTarget(row)}
            >
              Delete
            </Button>
          </div>
        )}
      />

      {totalPages > 1 && (
        <div className="flex justify-center items-center space-x-4 mt-4">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </Button>
          <span className="text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
            Next
          </Button>
        </div>
      )}

      <Modal
        open={!!deleteTarget}
        title="Confirm Delete"
        message="Are you sure you want to delete this record? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
