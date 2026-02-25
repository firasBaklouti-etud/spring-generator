const API_URL = process.env.NEXT_PUBLIC_API_URL || "${backendUrl}";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  };

<#if hasSecurity>
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  if (token) {
    headers["Authorization"] = ${r"`Bearer ${token}`"};
  }
</#if>

  const res = await fetch(${r"`${API_URL}${path}`"}, {
    ...options,
    headers,
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(body || ${r"`Request failed with status ${res.status}`"});
  }

  const text = await res.text();
  return text ? JSON.parse(text) : ({} as T);
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { method: "GET" });
}

export function apiPost<T>(path: string, data?: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    body: data ? JSON.stringify(data) : undefined,
  });
}

export function apiPut<T>(path: string, data?: unknown): Promise<T> {
  return request<T>(path, {
    method: "PUT",
    body: data ? JSON.stringify(data) : undefined,
  });
}

export function apiDelete<T>(path: string): Promise<T> {
  return request<T>(path, { method: "DELETE" });
}
