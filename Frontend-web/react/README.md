# YourPeak · Frontend React

Las instrucciones de configuración, ejecución, validación y pruebas están en el [README principal](../../README.md#4-configurar-y-arrancar-el-frontend-react).

## Pantalla de práctica de React

La práctica de comunicación entre componentes está disponible desde la opción **Práctica** del panel administrativo. La página `src/pages/PlanesPractica.jsx` permite cambiar entre dos partes:

- **Parte 1 · Catálogo y carrito:** mini cafetería con datos locales de `src/practica/datos.js`. `Practica` conserva el texto de búsqueda, el carrito y el aviso; `Catalogo` pasa cada producto a `ProductoCard`; el carrito calcula unidades y total a partir de sus líneas. Los datos no se envían a Django ni a Spring.
- **Parte 2 · Panel de planes:** simulación de gestión de planes con estado local. `PlanesPractica` comparte la lista y la búsqueda con `BuscadorPlanes`, `ResumenPlanes` y `ListaPlanes`; `ListaPlanes` renderiza `FilaPlan`; `FormularioPlan` valida y devuelve los datos al padre mediante `onGuardar`.

Los componentes de la práctica están en `src/components/practica/`. La página incluye un mapa del árbol de componentes y la Parte 1 incluye notas con la tabla de props, el manejo de eventos, las actualizaciones inmutables, los valores derivados con `reduce()` y los efectos con `useEffect`.
