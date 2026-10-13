# Qué se implementó y dónde está

TicketTrack guarda los gastos en Firestore, en la colección `gastos`. Cada documento tiene el `userId` de quien lo cargó. La pantalla que se ve la decide `EnrutadorDeSesion` en `app/src/main/java/com/example/tickettrack/MainActivity.kt`: si no hay sesión muestra el login; si hay sesión, cambia entre listado, alta, detalle, edición y resumen.

La lógica de Firebase está en `TicketTrackViewModel` (`app/src/main/java/com/example/tickettrack/viewmodel/TicketTrackViewModel.kt`). Las pantallas no escriben en Firebase directo: le piden al ViewModel y muestran lo que él publica.

Antes de estas partes ya existían el login, el registro, el cierre de sesión, el listado y el alta de un gasto (monto, categoría y nota). Lo de abajo es lo que se sumó después.

## 1. Detalle del gasto

**Qué hace.** Al tocar una tarjeta del listado se abre ese gasto: categoría, monto, fecha y hora, nota, comprobante y lugar.

**Cómo funciona.** La tarjeta no manda el gasto entero. Manda solo el `id`. `MainActivity` lo guarda en `gastoIdSeleccionado` y cambia la pantalla a `"detail"`. `DetailGastoScreen` busca ese `id` dentro de la lista que el ViewModel ya tiene. Esa lista se actualiza sola porque `obtenerGastos()` escucha la colección con `addSnapshotListener`.

**Dónde.**

- Toque de la tarjeta: `ItemGasto` en `app/src/main/java/com/example/tickettrack/view/HomeScreen.kt`
- Pantalla: `app/src/main/java/com/example/tickettrack/view/DetailGastoScreen.kt`
- Navegación: `EnrutadorDeSesion` en `MainActivity.kt`

## 2. Editar un gasto

**Qué hace.** En el detalle, **Editar** abre el mismo formulario del alta, ya completado con los datos de ese gasto. **Guardar cambios** escribe encima del mismo documento.

**Cómo funciona.** La pantalla sigue siendo `AddGastoScreen`. Si `gastoId` es `null`, crea. Si tiene un id, es edición: copia monto, categoría, nota, foto y ubicación una sola vez, y al guardar llama a `actualizarGasto`. Esa función solo cambia monto, categoría, nota y `updatedAt`. La fecha de creación queda igual. Si eligieron otra foto o movieron el punto, también actualiza `photoUrl` o la ubicación. Al terminar vuelve al detalle, que ya muestra los datos nuevos porque el listener del listado se actualizó.

**Dónde.**

- Botón **Editar**: `DetailGastoScreen.kt`
- Formulario: `app/src/main/java/com/example/tickettrack/view/AddGastoScreen.kt`
- Escritura: `actualizarGasto` en `TicketTrackViewModel.kt`
- Documento: `gastos/{id}` en Firestore

## 3. Eliminar un gasto

**Qué hace.** En el detalle, **Eliminar** pregunta antes de borrar. **Cancelar** cierra el cartel. Si se confirma, el gasto desaparece del listado.

**Cómo funciona.** El cartel es un `AlertDialog` en `DetailGastoScreen`. Recién al confirmar se llama a `eliminarGasto`. Esa función comprueba que el gasto sea del usuario logueado y borra el documento `gastos/{id}`. Si había foto, también borra el archivo `users/{uid}/tickets/{id}.jpg` en Storage. Después la app vuelve al listado. El listener saca la tarjeta sola.

**Dónde.**

- Cartel y botones: `DetailGastoScreen.kt`
- Borrado: `eliminarGasto` en `TicketTrackViewModel.kt`

## 4. Reglas de Firestore

**Qué hacen.** Aunque la app filtra por usuario, las reglas son las que impiden que otra persona lea o escriba esos documentos llamando a Firebase directo.

**Cómo funcionan.** Solo un usuario logueado puede crear un gasto, y el `userId` del documento tiene que ser el suyo. Leer, editar o borrar solo se permite si `resource.data.userId` es el uid de quien está autenticado. En una edición, el `userId` no puede pasar a ser el de otra persona. Cualquier otra ruta queda cerrada.

**Dónde.**

- Archivo: `firestore.rules` (raíz del proyecto)
- El proyecto de Firebase está indicado en `.firebaserc` y `firebase.json`

Estas reglas protegen la base cuando están publicadas en la consola de Firebase (Firestore → Rules → Publicar). El archivo del repo no las activa solo.

## 5. Foto del ticket

**Qué hace.** En el alta y en la edición se puede sacar una foto con la cámara o elegir una de la galería. Se ve en el formulario, como miniatura en el listado y grande en el detalle. En el detalle, tocarla la amplía.

**Cómo funciona.** La foto elegida queda primero solo en el teléfono, como vista previa. Al guardar, `subirFoto` la comprime a un JPEG de hasta 1280 px, la endereza si la cámara la guardó rotada y la sube a Storage en `users/{uid}/tickets/{idDelGasto}.jpg`. Firebase devuelve una URL y esa URL se guarda en el campo `photoUrl` del documento. Si se edita sin elegir otra foto, se conserva la anterior. La galería usa el selector de fotos del sistema, así que no pide permiso de almacenamiento. La cámara sí pide permiso y usa un `FileProvider` para crear el archivo temporal.

**Dónde.**

- Botones **Cámara** y **Galería**, y la vista previa: `AddGastoScreen.kt`
- Comprimir y rotar: `app/src/main/java/com/example/tickettrack/data/FotoTicket.kt`
- Subida: `subirFoto` en `TicketTrackViewModel.kt`
- Miniatura: `ItemGasto` en `HomeScreen.kt`
- Foto ampliable: `DetailGastoScreen.kt`
- Permiso de cámara y `FileProvider`: `app/src/main/AndroidManifest.xml`
- Carpeta temporal de la cámara: `app/src/main/res/xml/file_paths.xml`
- Reglas de Storage: `storage.rules` (también hay que publicarlas en la consola, en Storage → Rules)

## 6. Ubicación y mapa

**Qué hace.** En el alta y en la edición, **Usar mi ubicación** toma el GPS y, si puede, una dirección corta. Se puede tocar el mapa para correr el punto. El listado muestra ese lugar. El detalle muestra el texto y un mapa con un marcador.

**Cómo funciona.** El formulario pide el permiso de ubicación y usa Fused Location Provider. Con la latitud y la longitud, `etiquetaDeLugar` busca una dirección con el `Geocoder` de Android. Se guardan tres campos: `latitude`, `longitude` y `placeLabel`. En edición, si no se toca la ubicación, se vuelven a guardar los valores que ya tenía el gasto. El mapa es Google Maps. Los mosaicos se muestran solo si en `local.properties` existe `MAPS_API_KEY`; sin esa clave igual se guardan y se ven las coordenadas o la dirección, pero no el mapa. En el emulador hay que fijar un punto desde el panel **Location**, porque el GPS virtual no se mueve solo.

**Dónde.**

- Botón, permiso y punto elegido: `AddGastoScreen.kt`
- Dirección a partir de las coordenadas: `app/src/main/java/com/example/tickettrack/data/Ubicacion.kt`
- Mapa y marcador: `app/src/main/java/com/example/tickettrack/view/MapaGasto.kt`
- Mapa del detalle y texto del lugar: `DetailGastoScreen.kt`
- Lugar en la tarjeta: `HomeScreen.kt`
- Guardado de los tres campos: `agregarGasto` y `actualizarGasto` en `TicketTrackViewModel.kt`
- Permisos de ubicación y clave del mapa: `AndroidManifest.xml` y `app/build.gradle.kts` (lee `MAPS_API_KEY` desde `local.properties`)

## 7. Recordatorio local

**Qué hace.** Todos los días, a la hora elegida, muestra la notificación **¿Cargaste tus gastos de hoy?** La hora inicial es las 21:00. Se cambia desde el listado, en **Recordatorio a las 21:00**. **Probar ahora** muestra la misma notificación en el momento.

**Cómo funciona.** La hora queda en las preferencias del teléfono, no en Firestore. Al haber un usuario logueado, `Recordatorios.programar` calcula cuántos milisegundos faltan para esa hora y deja un trabajo de WorkManager. Cuando llega el momento, `RecordatorioWorker` muestra la notificación y agenda la del día siguiente. Tocar la notificación abre la app. Al cerrar sesión se cancela el trabajo. Android 13 o superior pide el permiso de notificaciones la primera vez que se entra al listado.

**Dónde.**

- Agenda, canal y texto de la notificación: `app/src/main/java/com/example/tickettrack/notification/Recordatorios.kt`
- Trabajo que se ejecuta a la hora: `app/src/main/java/com/example/tickettrack/notification/RecordatorioWorker.kt`
- Ícono: `app/src/main/res/drawable/ic_recordatorio.xml`
- Botón, diálogo de la hora y **Probar ahora**: `HomeScreen.kt`
- Cuándo se agenda o se cancela: el listener de sesión del `TicketTrackViewModel` (`programar` al entrar, `cancelar` al salir), más `cambiarHoraRecordatorio` y `probarRecordatorio`
- Permiso: `POST_NOTIFICATIONS` en `AndroidManifest.xml`

## 8. Resumen del mes

**Qué hace.** Desde el listado, **Resumen del mes** muestra el total del mes en curso, cuántos gastos son y cuánto fue a cada categoría. Cada categoría tiene una barra: es la parte de ese rubro sobre el total.

**Cómo funciona.** No hace otra consulta a Firebase. `resumenDelMes` filtra la lista que ya está en memoria y se queda con los gastos cuyo `createdAt` cae en el mes y el año actuales. Agrupa por categoría, suma los montos y calcula la proporción contra el total. Si el mes no tiene gastos, la pantalla lo dice. Crear, editar o borrar actualiza el resumen solo, porque lee la misma lista.

**Dónde.**

- Botón: `HomeScreen.kt`
- Cálculo y formato de pesos: `app/src/main/java/com/example/tickettrack/model/ResumenMes.kt` (`resumenDelMes` y `formatoPesos`)
- Pantalla: `app/src/main/java/com/example/tickettrack/view/ResumenScreen.kt`
- Navegación: pantalla `"resumen"` en `MainActivity.kt`

## Modelo que usan todas estas partes

El gasto está definido en `app/src/main/java/com/example/tickettrack/model/Gasto.kt`.

| Campo | Para qué se usa |
| --- | --- |
| `id` | Identifica el documento y el archivo de la foto |
| `userId` | Dueño. Lo miran la app y las reglas de Firestore |
| `amount`, `category`, `description` | Alta, edición, detalle y resumen |
| `photoUrl` | URL de la foto en Storage |
| `latitude`, `longitude`, `placeLabel` | Ubicación y mapa |
| `createdAt` | Fecha del gasto, filtro del resumen y orden del listado |
| `updatedAt` | Se actualiza al editar |
