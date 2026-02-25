"use client";

import Link from "next/link";
<#if hasSecurity>
import { useAuth } from "@/lib/auth";
</#if>

export default function Navbar() {
<#if hasSecurity>
  const { isAuthenticated, user, logout } = useAuth();
</#if>

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <div className="flex items-center space-x-8">
            <Link href="/" className="text-xl font-bold text-gray-900">
              ${projectName}
            </Link>
            <div className="hidden md:flex space-x-4">
<#list tables as table>
              <Link
                href="/${table.name?lower_case}"
                className="text-gray-600 hover:text-gray-900 text-sm font-medium"
              >
                ${table.className}
              </Link>
</#list>
            </div>
          </div>
<#if hasSecurity>
          <div className="flex items-center space-x-4">
            {isAuthenticated ? (
              <>
                <span className="text-sm text-gray-600">{user?.username}</span>
                <button onClick={logout} className="text-sm text-red-600 hover:text-red-800 font-medium">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="text-sm text-gray-600 hover:text-gray-900 font-medium">
                  Login
                </Link>
                <Link href="/register" className="btn-primary text-sm">
                  Register
                </Link>
              </>
            )}
          </div>
</#if>
        </div>
      </div>
    </nav>
  );
}
