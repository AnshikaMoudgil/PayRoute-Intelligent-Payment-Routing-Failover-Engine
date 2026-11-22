/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        fintech: {
          dark: '#0B132B',
          card: '#1C2541',
          border: '#2A375A',
          muted: '#627D98',
          accent: '#00B4D8',
          success: '#06D6A0',
          danger: '#EF476F',
          warning: '#FFD166',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      }
    },
  },
  plugins: [],
}
