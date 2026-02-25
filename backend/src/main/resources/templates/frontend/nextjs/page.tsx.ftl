"use client";

import Link from "next/link";

export default function HomePage() {
  return (
    <div>
      <h1 className="text-3xl font-bold mb-2">${projectName}</h1>
      <p className="text-gray-600 mb-8">Manage your data with the dashboard below.</p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
<#list tables as table>
        <Link href="/${table.name?lower_case}" className="card hover:shadow-md transition-shadow">
          <h2 className="text-xl font-semibold mb-2">${table.className}</h2>
          <p className="text-gray-500 text-sm">
            Manage ${table.className} records &mdash; ${table.columns?size} fields
          </p>
          <span className="inline-block mt-4 text-blue-600 text-sm font-medium">
            View all &rarr;
          </span>
        </Link>
</#list>
      </div>
    </div>
  );
}
