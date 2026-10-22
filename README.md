# TicketTrack

Aplicación Android para anotar gastos personales: monto, categoría, nota, fecha, foto del ticket y lugar. Cada usuario ve solo lo suyo. La app está hecha con Kotlin y Jetpack Compose.

Paquete: `com.example.tickettrack`. Versión 1.0. Mínimo Android 8 (API 26). Compila y apunta a API 37.

## Cómo está armada

Las pantallas están en `app/src/main/java/com/example/tickettrack/view/`. Una sola actividad, `MainActivity`, decide qué pantalla mostrar. Toda la lógica de sesión, gastos, tema, idioma, red y la IA pasa por `TicketTrackViewModel`. Ese ViewModel habla directo con Firebase, Cloudinary y Gemini. No hay una capa Repository aparte.

La navegación es un texto guardado en la actividad: `home`, `mapa`, `resumen`, `asistente`, `perfil`, `add`, `detail` y `edit`. La barra de abajo se esconde en alta, detalle y edición. El botón **+** solo aparece en el listado. Si se abre un gasto desde el mapa o el resumen, **Volver** regresa a esa pestaña.

## Pantallas

| Pestaña | Qué hace |
| --- | --- |
| Gastos | Listado, total del filtro, alta con **+** |
| Mapa | Todos los gastos que tienen coordenadas |
| Resumen | Totales del mes |
| IA | Chat sobre los gastos cargados |
| Perfil | Cuenta, apariencia, idioma, recordatorio y salida |

## Alta, baja y modificación de gastos

La colección de Firestore se llama `gastos`. El id del documento es un UUID que genera la app al crear. Cada documento lleva el `userId` de quien lo cargó.

Campos de `Gasto`: `id`, `userId`, `amount`, `category`, `description`, `photoUrl`, `latitude`, `longitude`, `placeLabel`, `createdAt`, `updatedAt`.

Categorías: Supermercado, Transporte, Servicios, Ocio, Salud y Otros.

**Alta.** En **Nuevo gasto** se cargan monto, categoría, nota, fecha, foto y lugar. **Guardar gasto** crea el documento. La fecha la elige el usuario. `updatedAt` es el momento del guardado.

**Detalle.** Al tocar un gasto se ven categoría, monto, fecha y hora, nota, foto y lugar. La foto se puede ampliar. El mapa del lugar se puede ver aunque no haya una clave de Google Maps.

**Edición.** **Editar** abre el mismo formulario, ya completado. **Guardar cambios** actualiza ese mismo documento. Si no se elige otra foto, se conserva la anterior. Si no se toca el mapa, se conservan latitud, longitud y lugar.

**Baja.** **Eliminar** pide confirmación. Al aceptar se borra el documento y, si había foto, también la imagen en Cloudinary. La lista se actualiza sola porque el ViewModel escucha la colección con `addSnapshotListener`.

El listado se puede filtrar por período y por categoría. El período es una barra: Todos, Esta semana y Este mes. La semana arranca el lunes. La categoría es un menú desplegable, con la opción Todas y un ícono por rubro. Arriba se muestra el total de lo que quedó visible.

## Sesión

- Registro con email y contraseña, en un formulario aparte.
- Ingreso con email y contraseña.
- Ingreso con Google. Hace falta `GOOGLE_WEB_CLIENT_ID` en `local.properties`.
- Los errores de Firebase se muestran en español.
- **Cerrar sesión** en Perfil también cierra la sesión de Google y vacía la lista en memoria. El recordatorio se cancela al salir.

## Foto del ticket

En el alta y en la edición se puede usar la cámara o la galería. La galería usa el selector del sistema y no pide permiso de almacenamiento. La cámara pide permiso y guarda un archivo temporal con `FileProvider` (`app/src/main/res/xml/file_paths.xml`).

La foto no se sube a Firebase Storage. Al guardar, `FotoTicket` la comprime a JPEG de hasta 1280 px, la endereza si el EXIF venía rotado, y `FotosCloudinary` la sube firmada a Cloudinary. El `public_id` es `tickettrack/{uid}/{gastoId}`. La URL segura queda en `photoUrl`. En el listado se ve como miniatura. Si no hay foto, el gasto muestra el ícono de su categoría.

Al borrar el gasto, la app llama a Cloudinary para destruir esa imagen.

## Ubicación y mapa

**Usar mi ubicación** pide el permiso de ubicación precisa y toma el punto con Fused Location Provider (el GPS del teléfono, combinado por Play Services). El nombre de la calle no sale de Google: se pide a Nominatim, el servicio de direcciones de OpenStreetMap, con un User-Agent propio de la app. Se guardan `latitude`, `longitude` y `placeLabel`.

En el formulario se puede tocar el mapa para mover el marcador. El mapa de un gasto y el mapa de todos usan osmdroid. El fondo es Esri World Street Map, que no pide clave. No se usan los servidores voluntarios de OpenStreetMap (`tile.openstreetmap.org` bloquea apps) ni el fondo de CARTO (pide clave). Abajo del mapa figura `© Esri, HERE, Garmin, OpenStreetMap`.

Si todavía no hay gastos con coordenadas, la pestaña Mapa muestra Buenos Aires. Cada marcador abre el detalle de ese gasto. El detalle de un gasto también muestra su mapa.

## Sensores, hardware y permisos

| Uso | Qué usa la app | Permiso |
| --- | --- | --- |
| Lugar del gasto | GPS a través de Fused Location Provider | `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` |
| Foto del ticket | Cámara. Si el teléfono no tiene, la función no es obligatoria | `CAMERA` |
| Dictado de un gasto | Micrófono y el reconocedor de voz del sistema, en español de Argentina | `RECORD_AUDIO` |
| Aviso de sin conexión | Estado de la red | `ACCESS_NETWORK_STATE` |
| Recordatorio | Notificación | `POST_NOTIFICATIONS` (Android 13 en adelante) |
| Mapas, fotos, Firestore y la IA | Internet | `INTERNET` |

No se usan acelerómetro, giroscopio ni brújula. El dictado no es un modelo propio: es `RecognizerIntent` del teléfono. La frase reconocida después la interpreta Gemini.

En el emulador el GPS no se mueve solo. Hay que fijar un punto en el panel Location de Android Studio.

## Asistente de IA

El modelo es `gemini-3.8-flash`, llamado por HTTP a `generateContent` desde `GeminiCliente`. La clave es `GEMINI_API_KEY` en `local.properties` (se crea en [Google AI Studio](https://aistudio.google.com/apikey)). Sin clave, o con una clave inválida, la app avisa en español. Hace falta internet.

Hay tres usos. Ninguno guarda el gasto solo: primero completa o responde, y la persona confirma.

1. **Foto del ticket.** Después de sacar o elegir una foto nueva aparece **Completar con la foto**. Gemini lee la imagen y propone monto, categoría, nota y lugar. El formulario muestra "Revisá los datos antes de guardar".
2. **Frase o voz.** En el alta, la tarjeta verde **Con una frase** acepta un texto ("Gasté 4500 en el súper") o **Dictar**. **Armar** pide a Gemini que complete los mismos campos.
3. **Chat.** La pestaña IA responde preguntas sobre los gastos ya cargados. El contexto es el resumen del mes actual y los últimos 40 gastos. Si no alcanza la información, tiene que decirlo. No inventa gastos que no estén en esa lista.

## Modo sin conexión

Hay dos capas.

- Firestore tiene la persistencia local activada, así que puede volver a mostrar datos que ya había bajado.
- Además, cada vez que llega la lista se guarda un JSON en el teléfono: `filesDir/tickettrack_offline/{uid}.json` (`CacheGastos`). Al abrir la lista, primero se muestra ese archivo y después se reemplaza si el listener de Firestore responde.

`ConnectivityManager` publica si hay red. Sin conexión, el listado muestra un cartel: se están viendo los gastos guardados en el teléfono. Crear o editar un gasto necesita llegar a Firebase. Si la red vuelve, el listener vuelve a traer la colección.

Las fotos, el mapa, el nombre de la calle y la IA siguen necesitando internet.

## Apariencia

En Perfil hay tres modos, guardados en las preferencias `tickettrack_apariencia`:

- **Sistema.** Sigue el modo claro u oscuro del teléfono.
- **Día.** Forzado claro.
- **Noche.** Forzado oscuro.

La paleta es verde oscuro sobre fondo gris suave. De noche el verde es más claro para que se lea. Botones, campos y tarjetas usan esquinas grandes. La barra de abajo flota, con la pestaña elegida marcada. El monto del listado, del resumen y del detalle va sobre un bloque verde.

## Icono

El ícono del launcher es adaptativo. El fondo es `#E7ECE8` (`launcher_background`). El dibujo es `app/src/main/res/drawable/ic_launcher_ticket.png`, el cuadrado celeste con la palabra TICKETTrack. Sirve para el ícono normal y para el redondo (`ic_launcher.xml` e `ic_launcher_round.xml`).

La notificación del recordatorio usa otro dibujo chico: `app/src/main/res/drawable/ic_recordatorio.xml`.

Si el teléfono sigue mostrando el ícono anterior, hay que desinstalar la app y volver a instalarla. El launcher guarda el ícono viejo.

## Idiomas

En Perfil: Español, English y Português. Se guarda en `tickettrack_idioma`. Al entrar, el idioma es español.

Traducidos: los nombres de las pestañas, los filtros del listado, las categorías en la lista, los textos del mapa, y en Perfil la apariencia, el idioma, el recordatorio y cerrar sesión.

Siguen en español el ingreso, el formulario de gasto, el detalle, el resumen y el chat de la IA.

## Recordatorio

En Perfil se puede activar un aviso diario. La hora de fábrica es 21:00 y el interruptor arranca apagado. WorkManager programa un trabajo único, `recordatorio_diario`, para esa hora, y al dispararse agenda el del día siguiente. La notificación dice "¿Cargaste tus gastos de hoy?" y abre la app. El canal se llama Recordatorios. El aviso no mira si ese día ya se cargó algo. Desde el diálogo de la hora se puede lanzar una prueba.

Al cerrar sesión se cancela. Al volver a entrar, si seguía activo, se programa de nuevo.

## Resumen del mes

Se puede ir al mes anterior y volver. Muestra el total, la cantidad de gastos, el promedio, el total del mes previo y un texto de si se gastó más, menos o igual. El gasto más alto se puede abrir. Cada categoría muestra cantidad, porcentaje y una barra.

## Pruebas

Las pruebas unitarias están en `app/src/test/java/com/example/tickettrack/model/LogicaGastoTest.kt`. Corren en la computadora, sin emulador. Cubren el formato de pesos, la comparación con el mes anterior, el filtro por categoría y período, el resumen (total, promedio, mayor gasto y mes anterior), un mes vacío, y el texto del lugar.

En Android Studio: abrir ese archivo y correr `LogicaGastoTest`. También sirve la tarea Gradle `:app:testDebugUnitTest`.

`ExampleInstrumentedTest` es la prueba de ejemplo del teléfono: solo comprueba que el paquete sea `com.example.tickettrack`. Hay que tener un emulador o un dispositivo.

## Configuración local

`local.properties` no se sube a git. Sin commitear ese archivo, estas claves se leen al compilar y entran en `BuildConfig`:

| Clave | Para qué |
| --- | --- |
| `sdk.dir` | Android SDK |
| `GOOGLE_WEB_CLIENT_ID` | Ingreso con Google |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Fotos |
| `GEMINI_API_KEY` | Lectura del ticket, frase y chat |
| `MAPS_API_KEY` | Quedó en el archivo por si hacía falta. El mapa actual no la usa |

`google-services.json` conecta la app al proyecto de Firebase. Las reglas de `firestore.rules` y `storage.rules` protegen los datos cuando están publicadas en la consola. El archivo en el repo no las activa solo. Las fotos ya no pasan por Storage. Firestore sigue siendo la base de los gastos.

## Tecnologías

- Kotlin 2.2.10, Android Gradle Plugin 9.1.1, Jetpack Compose (BOM 2026.02.01), Material 3
- Firebase Auth y Cloud Firestore (BOM 33.7.0). La dependencia de Storage sigue en el proyecto. Las fotos salen por Cloudinary
- Cloudinary Android 2.5.0
- osmdroid 6.1.18 y teselas de Esri
- Play Services Location y Auth
- WorkManager 2.10.0
- Coil, para mostrar las fotos
- JUnit, para las pruebas de la lógica

## Dónde está cada cosa

- Actividad y pestañas: `MainActivity.kt`
- Gastos, filtros y tarjetas: `view/HomeScreen.kt`
- Formulario: `view/AddGastoScreen.kt`
- Detalle: `view/DetailGastoScreen.kt`
- Mapa general: `view/MapaGastosScreen.kt`
- Mapa de un gasto: `view/MapaGasto.kt`
- Fondo del mapa: `view/MapaBase.kt`
- Resumen: `view/ResumenScreen.kt`
- Chat: `view/AsistenteScreen.kt`
- Perfil: `view/PerfilScreen.kt`
- Ingreso y registro: `view/LoginScreen.kt`
- ViewModel: `viewmodel/TicketTrackViewModel.kt`
- Gemini: `data/GeminiCliente.kt`
- Fotos: `data/FotoTicket.kt` y `data/FotosCloudinary.kt`
- Dirección y texto del lugar: `data/Ubicacion.kt`
- Copia local: `data/CacheGastos.kt`
- Filtros y resumen: `model/FiltroGastos.kt` y `model/ResumenMes.kt`
- Idiomas: `ui/Idioma.kt`
- Tema: `ui/theme/`
- Recordatorio: `notification/Recordatorios.kt` y `RecordatorioWorker.kt`
- Pruebas: `app/src/test/java/com/example/tickettrack/model/LogicaGastoTest.kt`
