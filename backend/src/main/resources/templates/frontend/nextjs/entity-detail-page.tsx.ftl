<#-- Determine primary key field -->
<#assign pkField = "id">
<#list table.columns as col>
<#if col.primaryKey><#assign pkField = col.fieldName></#if>
</#list>
<#assign route = table.name?lower_case>
"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import { ${table.className} } from "@/types";
import Button from "@/components/ui/button";

export default function ${table.className}DetailPage() {
  const params = useParams();
  const id = params.id as string;
  const [item, setItem] = useState<${table.className} | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const data = await apiGet<${table.className}>(${r"`/api/${route}/${id}`"});
        setItem(data);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : "Failed to load");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) return <div className="py-8 text-center text-gray-500">Loading...</div>;
  if (error) return <div className="py-8 text-center text-red-600">{error}</div>;
  if (!item) return <div className="py-8 text-center text-gray-500">Not found</div>;

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">${table.className} Detail</h1>
        <div className="flex space-x-3">
          <Link href={${r"`/${route}/${id}/edit`"}}>
            <Button>Edit</Button>
          </Link>
          <Link href="/${route}">
            <Button variant="secondary">Back to List</Button>
          </Link>
        </div>
      </div>

      <div className="card">
        <dl className="divide-y divide-gray-100">
<#list table.columns as column>
          <div className="py-3 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm font-medium text-gray-500">${column.fieldName}</dt>
            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
              {String(item.${column.fieldName} ?? "")}
            </dd>
          </div>
</#list>
        </dl>
      </div>
    </div>
  );
}
