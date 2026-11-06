/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        mfarm: {
          dark: '#1b4332',
          primary: '#2d6a4f',
          light: '#40916c',
          accent: '#52b788',
          bg: '#f8f9fa'
        }
      }
    },
  },
  plugins: [],
}
