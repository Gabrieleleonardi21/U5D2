/// <reference types="vite/client" />
// Dichiara a TypeScript i tipi che Vite aggiunge: fra questi c'e' quello che
// permette di importare un file .css come effetto collaterale (import './index.css').
// Senza questa riga tsc si ferma con "Cannot find module ... side-effect import".
