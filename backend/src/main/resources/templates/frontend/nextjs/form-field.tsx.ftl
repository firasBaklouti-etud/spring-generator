import React from "react";

interface FormFieldProps {
  label: string;
  name: string;
  type?: "text" | "number" | "email" | "password" | "date" | "datetime-local" | "checkbox" | "select" | "textarea";
  value: string | number | boolean;
  onChange: (name: string, value: string | number | boolean) => void;
  required?: boolean;
  options?: { label: string; value: string | number }[];
  placeholder?: string;
}

export default function FormField({
  label,
  name,
  type = "text",
  value,
  onChange,
  required = false,
  options = [],
  placeholder,
}: FormFieldProps) {
  if (type === "checkbox") {
    return (
      <div className="flex items-center space-x-2">
        <input
          id={name}
          type="checkbox"
          checked={!!value}
          onChange={(e) => onChange(name, e.target.checked)}
          className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
        />
        <label htmlFor={name} className="text-sm font-medium text-gray-700">
          {label}
        </label>
      </div>
    );
  }

  if (type === "select") {
    return (
      <div>
        <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
        <select
          id={name}
          value={String(value)}
          onChange={(e) => onChange(name, e.target.value)}
          required={required}
          className="input-field"
        >
          <option value="">-- Select --</option>
          {options.map((opt) => (
            <option key={String(opt.value)} value={String(opt.value)}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>
    );
  }

  if (type === "textarea") {
    return (
      <div>
        <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
        <textarea
          id={name}
          value={String(value)}
          onChange={(e) => onChange(name, e.target.value)}
          required={required}
          placeholder={placeholder}
          rows={4}
          className="input-field"
        />
      </div>
    );
  }

  return (
    <div>
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input
        id={name}
        type={type}
        value={type === "number" ? Number(value) || "" : String(value ?? "")}
        onChange={(e) =>
          onChange(name, type === "number" ? Number(e.target.value) : e.target.value)
        }
        required={required}
        placeholder={placeholder}
        className="input-field"
      />
    </div>
  );
}
