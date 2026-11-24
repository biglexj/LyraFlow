/**
 * Componente «Acerca de» compartido del ecosistema Biglex.
 *
 * Un único contenido (identidad, actualizaciones, ayuda, apoyo y pie) montado en
 * tres presentaciones: ventana flotante, página de Ajustes y cabecera de producto.
 * Los datos y los enlaces salen de la configuración real del producto.
 */

const ICONOS = {
  github: `<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path fill="currentColor" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/></svg>`,
  corazon: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M12 21s-7.5-4.7-9.4-9A5.4 5.4 0 0 1 12 5.6 5.4 5.4 0 0 1 21.4 12c-1.9 4.3-9.4 9-9.4 9Z"/></svg>`,
  cafe: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M4 4h12v6a6 6 0 0 1-6 6H10a6 6 0 0 1-6-6V4Zm12 1h2.5a3.5 3.5 0 0 1 0 7H16v-2h2.5a1.5 1.5 0 0 0 0-3H16V5ZM3 19h14v2H3v-2Z"/></svg>`,
  globo: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm0 2c1.3 0 2.9 2.5 3.2 6H8.8C9.1 6.5 10.7 4 12 4Zm-7 8c0-1 .2-2 .5-2.8h3.4a22 22 0 0 0 0 5.6H5.5A8 8 0 0 1 5 12Zm2.7 4.6c-.3-.8-.5-1.7-.5-2.6h3.9c-.3 2.3-1 4-1.7 5.2a8 8 0 0 1-1.7-2.6ZM12 20c-1.3 0-2.9-2.5-3.2-6h6.4c-.3 3.5-1.9 6-3.2 6Zm3.8-2.6c-.7-1.2-1.4-2.9-1.7-5.2h3.9c0 .9-.2 1.8-.5 2.6a8 8 0 0 1-1.7 2.6Zm2.2-4.8a22 22 0 0 0 0-5.6h3.4c.3.8.5 1.6.5 2.6s-.2 1.8-.5 2.6h-3.4Z"/></svg>`,
  externo: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M14 3h7v7h-2V6.4l-8.3 8.3-1.4-1.4L17.6 5H14V3ZM5 5h5v2H6v11h11v-4h2v6H4V5h1Z"/></svg>`,
  refrescar: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M12 5V2L8 6l4 4V7a5 5 0 1 1-5 5H5a7 7 0 1 0 7-7Z"/></svg>`,
  info: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16Zm-1 3v2h2V7h-2Zm0 4v6h2v-6h-2Z"/></svg>`,
  chispas: `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="currentColor" d="m12 2 1.9 5.6L19.5 9l-5.6 1.9L12 16.5l-1.9-5.6L4.5 9l5.6-1.4L12 2Z"/></svg>`,
};

/** Datos reales del producto. Cambian por app; la estructura no. */
const PRODUCTO = {
  nombre: "LyraFlow",
  version: "1.1.9",
  compilacion: "1.1.9 (119)",
  canal: "Estable",
  lema: "Habla. Ya está escrito.",
  descripcion:
    "Convierte voz en texto mediante dictado y transcripción de audio. Incluye flujos de subtitulación para trabajar con contenido hablado.",
  plataforma: "Windows 64-bit",
  icono: "../../../assets/branding/icons/icon-transparent.png",
  repositorio: "https://github.com/biglexj/LyraFlow",
  repositorioNombre: "biglexj/LyraFlow",
  paginaApp: "https://biglexj.com/desarrollo/lyraflow",
  soporte: "https://www.biglexj.com/donaciones",
  soporteNombre: "biglexj.com/donaciones",
  cafe: "https://buymeacoffee.com/biglexj",
  autor: "biglexj",
  licencia: "MIT",
  publica: true,
  actualizadorEnApp: true,
};

const ESTADO_ACTUALIZACION = {
  alDia: { titulo: "Estás al día", detalle: "Última versión instalada y comprobada" },
  nueva: { titulo: "Hay una versión nueva", detalle: "" },
};

function elemento(tag, clase, texto) {
  const nodo = document.createElement(tag);
  if (clase) nodo.className = clase;
  if (texto !== undefined) nodo.textContent = texto;
  return nodo;
}

function enlace(href, clase, contenido) {
  const nodo = document.createElement("a");
  nodo.className = clase;
  nodo.href = href;
  nodo.target = "_blank";
  nodo.rel = "noreferrer";
  if (contenido) nodo.append(contenido);
  return nodo;
}

function boton(clase, contenido, href) {
  if (href) {
    return enlace(href, `boton ${clase}`, contenido);
  }
  const nodo = elemento("button", `boton ${clase}`);
  nodo.type = "button";
  nodo.append(contenido);
  return nodo;
}

function icono(nombre) {
  const envoltura = elemento("span", "icono");
  envoltura.innerHTML = nombre;
  return envoltura;
}

/** Tarjeta de actualizaciones, a todo el ancho. */
function tarjetaActualizaciones(producto, estado) {
  const tarjeta = elemento("section", "tarjeta actualizaciones");

  const cabecera = elemento("div", "actualizaciones-cabecera");
  const marca = icono(ICONOS.chispas);
  marca.classList.add("marca");
  const textos = elemento("div", "actualizaciones-textos");
  textos.append(elemento("strong", "titulo", estado.titulo));
  const detalle = elemento(
    "span",
    "detalle",
    estado.detalle || `La versión ${estado.nuevaVersion} está disponible`,
  );
  textos.append(detalle);
  if (estado.nuevaVersion) {
    const linea = elemento("span", "detalle", `Instalada: ${producto.version}`);
    textos.append(linea);
  }
  cabecera.append(marca, textos);

  const acciones = elemento("div", "actualizaciones-acciones");
  const novedades = boton(
    "secundario",
    elemento("span", "texto", "Ver novedades"),
    `${producto.repositorio}/releases`,
  );
  acciones.append(novedades);

  if (estado.nuevaVersion) {
    const accion = producto.actualizadorEnApp ? "Actualizar" : "Descargar";
    const principal = boton(
      "primario",
      elemento("span", "texto", accion),
      producto.actualizadorEnApp ? null : `${producto.repositorio}/releases/latest`,
    );
    if (!producto.actualizadorEnApp) principal.setAttribute("download", "");
    acciones.append(principal);
  } else {
    const principal = boton(
      "primario",
      elemento("span", "texto", "Buscar actualizaciones"),
    );
    principal.dataset.accion = "buscar-actualizaciones";
    acciones.append(principal);
  }

  tarjeta.append(cabecera, acciones);
  return tarjeta;
}

/** Columna izquierda: ayuda y proyecto. */
function columnaAyuda(producto) {
  const columna = elemento("div", "columna");
  columna.append(elemento("h4", "columna-titulo", "Ayuda y proyecto"));

  const lista = elemento("div", "lista-enlaces");
  const enlaces = [
    { href: producto.soporte, icono: ICONOS.globo, titulo: "Soporte oficial", nota: producto.soporteNombre },
    { href: producto.repositorio, icono: ICONOS.github, titulo: "Repositorio de GitHub", nota: producto.repositorioNombre },
    { href: producto.paginaApp, icono: ICONOS.externo, titulo: "Página de la app", nota: "Ficha en BiglexJ.com" },
  ];
  for (const item of enlaces) {
    const fila = enlace(item.href, "enlace-fila");
    const marca = elemento("span", "enlace-icono");
    marca.innerHTML = item.icono;
    const textos = elemento("span", "enlace-textos");
    textos.append(elemento("strong", "enlace-titulo", item.titulo));
    textos.append(elemento("span", "enlace-nota", item.nota));
    const flecha = elemento("span", "enlace-flecha");
    flecha.innerHTML = ICONOS.externo;
    fila.append(marca, textos, flecha);
    lista.append(fila);
  }
  columna.append(lista);
  return columna;
}

/** Bloque de identidad: icono real, nombre, versión y canal. */
function bloqueIdentidad(producto, variante) {
  if (variante === "producto") {
    const cabecera = elemento("header", "cabecera-producto");
    const marca = elemento("div", "cabecera-producto-icono");
    const halo = elemento("span", "halo");
    const img = document.createElement("img");
    img.src = producto.icono;
    img.alt = `${producto.nombre} logo`;
    marca.append(halo, img);

    const textos = elemento("div", "cabecera-producto-textos");
    textos.append(elemento("h3", "cabecera-producto-nombre", producto.nombre));
    const meta = elemento("div", "cabecera-producto-meta");
    meta.append(elemento("span", "insignia", `v${producto.version}`));
    meta.append(elemento("span", "insignia", producto.canal));
    textos.append(meta);
    textos.append(elemento("p", "cabecera-producto-lema", producto.lema));

    cabecera.append(marca, textos);
    return cabecera;
  }

  const cabecera = elemento("header", "cabecera-sencilla");
  const marca = elemento("span", "cabecera-icono");
  const img = document.createElement("img");
  img.src = producto.icono;
  img.alt = `${producto.nombre} logo`;
  marca.append(img);
  const textos = elemento("div", "cabecera-textos");
  textos.append(elemento("h3", "cabecera-nombre", producto.nombre));
  const meta = elemento("span", "cabecera-meta", `Versión ${producto.version} · ${producto.canal}`);
  textos.append(meta);
  cabecera.append(marca, textos);
  return cabecera;
}

/** Columna derecha: apoyar el desarrollo. */
function columnaApoyo(producto) {
  const columna = elemento("div", "columna");
  columna.append(elemento("h4", "columna-titulo", "Apoyar el desarrollo"));

  const botones = elemento("div", "botones-apoyo");
  const biglex = elemento("span", "boton-contenido");
  const marca = elemento("span", "boton-icono");
  marca.innerHTML = ICONOS.corazon;
  biglex.append(marca, elemento("span", "texto", "Apoyar en BiglexJ.com"));
  botones.append(boton("tonal", biglex, producto.soporte));

  const cafe = elemento("span", "boton-contenido");
  const marcaCafe = elemento("span", "boton-icono");
  marcaCafe.innerHTML = ICONOS.cafe;
  cafe.append(marcaCafe, elemento("span", "texto", "Cómprame un café"));
  botones.append(boton("tonal", cafe, producto.cafe));

  columna.append(botones);
  columna.append(
    elemento(
      "p",
      "columna-nota",
      "Si esta herramienta agiliza tu flujo de trabajo diario, considera apoyar su desarrollo continuo.",
    ),
  );
  return columna;
}

/** Pie: información técnica, créditos o licencia de uso. */
function pie(producto) {
  const pie = elemento("footer", "pie");

  const tecnico = elemento("details", "tecnico");
  const resumen = elemento("summary", "tecnico-resumen");
  const marca = elemento("span", "tecnico-icono");
  marca.innerHTML = ICONOS.info;
  resumen.append(marca, elemento("span", "texto", "Información técnica"));
  tecnico.append(resumen);

  const datos = elemento("dl", "tecnico-datos");
  const filas = [
    ["Versión", producto.version],
    ["Canal", producto.canal],
    ["Compilación", producto.compilacion || "1.1.9 (119)"],
    ["Plataforma", producto.plataforma],
    ["Repositorio", producto.repositorioNombre],
  ];
  for (const [clave, valor] of filas) {
    datos.append(elemento("dt", null, clave));
    datos.append(elemento("dd", null, valor));
  }
  tecnico.append(datos);
  pie.append(tecnico);

  const cierre = elemento("div", "pie-cierre");
  if (producto.publica) {
    const creditos = enlace(`${producto.repositorio}/blob/main/LICENSE`, "enlace-sencillo", elemento("span", "texto", "Créditos"));
    cierre.append(creditos);
  } else {
    const licencia = elemento("span", "enlace-sencillo", "Licencia de uso");
    cierre.append(licencia);
  }
  cierre.append(elemento("span", "pie-nota", `© 2026 ${producto.autor} · Licencia ${producto.licencia}`));
  pie.append(cierre);

  return pie;
}

/** Contenido compartido por las tres presentaciones. */
function contenidoCompartido(producto, estado) {
  const contenedor = elemento("div", "contenido");
  contenedor.append(tarjetaActualizaciones(producto, estado));

  const columnas = elemento("div", "columnas");
  columnas.append(columnaAyuda(producto), columnaApoyo(producto));
  contenedor.append(columnas);

  contenedor.append(pie(producto));
  return contenedor;
}

/** Presentación 1: ventana flotante con barra de título propia. */
function montarFlotante(producto, estado) {
  const ventana = elemento("section", "presentacion");
  const marco = elemento("div", "ventana");
  const titulo = elemento("div", "ventana-titulo");
  titulo.append(elemento("span", "titulo", `${producto.nombre} — Acerca de`));
  const marcoInterior = elemento("div", "ventana-cuerpo");
  marcoInterior.append(bloqueIdentidad(producto, "sencilla"));
  marcoInterior.append(contenidoCompartido(producto, estado));
  marco.append(titulo, marcoInterior);
  ventana.append(marco);
  return ventana;
}

/** Navegación lateral de Ajustes, común a las dos presentaciones dentro de Ajustes. */
function navegacionAjustes() {
  const navegacion = elemento("nav", "ajustes-nav");
  navegacion.append(elemento("span", "ajustes-nav-titulo", "Ajustes"));
  for (const item of ["General", "Voz", "Apariencia", "Privacidad", "Acerca de"]) {
    const enlaceNav = elemento("a", "ajustes-nav-item", item);
    if (item === "Acerca de") enlaceNav.classList.add("activo");
    enlaceNav.href = "#";
    navegacion.append(enlaceNav);
  }
  return navegacion;
}

/** Presentación 2: página de Ajustes con cabecera sencilla. */
function montarAjustes(producto, estado) {
  const ventana = elemento("section", "presentacion");
  const marco = elemento("div", "ajustes");

  const pagina = elemento("div", "ajustes-pagina");
  pagina.append(bloqueIdentidad(producto, "sencilla"));
  pagina.append(contenidoCompartido(producto, estado));

  marco.append(navegacionAjustes(), pagina);
  ventana.append(marco);
  return ventana;
}

/** Presentación 3: cabecera de producto sobre superficie Aurora (propuesta principal). */
function montarCabeceraProducto(producto, estado) {
  const ventana = elemento("section", "presentacion");
  const marco = elemento("div", "ajustes");

  const pagina = elemento("div", "ajustes-pagina");
  pagina.append(bloqueIdentidad(producto, "producto"));
  pagina.append(contenidoCompartido(producto, estado));

  marco.append(navegacionAjustes(), pagina);
  ventana.append(marco);
  return ventana;
}

function render(estadoActualizacion) {
  const raiz = document.getElementById("raiz");
  raiz.textContent = "";
  const estado = estadoActualizacion === "nueva"
    ? { ...ESTADO_ACTUALIZACION.nueva, nuevaVersion: "1.2.0" }
    : { ...ESTADO_ACTUALIZACION.alDia, nuevaVersion: null };

  raiz.append(montarFlotante(PRODUCTO, estado));
  raiz.append(montarAjustes(PRODUCTO, estado));
  raiz.append(montarCabeceraProducto(PRODUCTO, estado));
}

document.addEventListener("DOMContentLoaded", () => {
  const tema = document.getElementById("tema");
  const estado = document.getElementById("estado");
  const parametros = new URLSearchParams(location.search);

  const temaParametro = parametros.get("tema");
  const guardada = temaParametro || localStorage.getItem("acerca-tema");
  if (guardada) {
    document.documentElement.dataset.tema = guardada;
    tema.value = guardada;
  }
  const estadoParametro = parametros.get("estado");
  if (estadoParametro) estado.value = estadoParametro;

  tema.addEventListener("change", () => {
    document.documentElement.dataset.tema = tema.value;
    localStorage.setItem("acerca-tema", tema.value);
  });
  estado.addEventListener("change", () => render(estado.value));
  render(estado.value);
});
