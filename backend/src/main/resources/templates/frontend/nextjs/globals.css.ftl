@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  --foreground-rgb: 15, 23, 42;
  --background-rgb: 248, 250, 252;
}

body {
  color: rgb(var(--foreground-rgb));
  background: rgb(var(--background-rgb));
}

@layer components {
  .btn-primary {
    @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium;
  }
  .btn-secondary {
    @apply bg-gray-200 text-gray-800 px-4 py-2 rounded-lg hover:bg-gray-300 transition-colors font-medium;
  }
  .btn-danger {
    @apply bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors font-medium;
  }
  .card {
    @apply bg-white rounded-xl shadow-sm border border-gray-200 p-6;
  }
  .input-field {
    @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent;
  }
}
