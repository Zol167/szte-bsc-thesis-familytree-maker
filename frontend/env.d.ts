/// <reference types="vite/client" />

interface ImportMetaEnv {
  // a backend címe külön kiszolgált build esetén (fejlesztéskor a Vite proxy miatt nem kell)
  readonly VITE_API_URL?: string
}
