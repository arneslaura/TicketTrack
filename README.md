# TicketTrack

**Trabajo Integrador - Módulo 2: Aplicación Android nativa con Firebase**  
**Materia:** Aplicaciones Móviles - Universidad Nacional Arturo Jauretche (UNAJ)

### Integrantes del Grupo:
* Laura Arnés
* Hernan Parma

---

## Descripción de la App
Aplicación Android nativa para el registro y control de gastos personales. Permite anotar montos, categorías, notas, fechas, fotos de comprobantes (tickets) y geolocalización en mapas. Cada usuario gestiona su propia información de forma individual y segura mediante autenticación.

La app está desarrollada en **Kotlin** y **Jetpack Compose** (Material 3), apuntando a Android 8 (API 26) o superior.

## Guía de Configuración y Ejecución (Para la Cátedra)

Como los archivos de credenciales y claves privadas están excluidos por seguridad en el `.gitignore`, para compilar y ejecutar el proyecto en tu entorno seguir estos dos pasos obligatorios:

### 1. Configurar Firebase (`google-services.json`)
1. Creá o abrí un proyecto en la [Consola de Firebase](https://console.firebase.google.com/) con el paquete `com.example.tickettrack`.
2. Descargá el archivo de configuración **`google-services.json`** desde la configuración del proyecto en Firebase.
3. Ubicá ese archivo dentro de la carpeta del proyecto en la siguiente ruta exacta: `app/google-services.json`

### 2. Configurar credenciales locales (`local.properties`)
En la raíz del proyecto (al mismo nivel que `settings.gradle.kts`), creá un archivo llamado **`local.properties`** y agregá las siguientes claves:

sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
GOOGLE_WEB_CLIENT_ID=tu_cliente_oauth_web_de_firebase.apps.googleusercontent.com
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
GEMINI_API_KEY=tu_gemini_api_key

## Reglas de Seguridad de Firestore
Tal como lo exigen las consideraciones técnicas del trabajo, la base de datos en la nube está protegida para que ningún usuario anónimo pueda leer, modificar o borrar datos ajenos[cite: 6, 10].

Las reglas configuradas en la Consola de Firebase (`Firestore Database` > `Reglas`) son las siguientes:

rules_version = '2';
service cloud.firestore {
match /databases/{database}/documents {
match /gastos/{gastoId} {
allow read, update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
}
}
}

## Cumplimiento de Requerimientos Obligatorios
* **RF1 - Autenticación:** Firebase Auth (Email/Contraseña y Google Sign-In).
* **RF2 & RF3 - Listado y Detalle:** Visualización por usuario y navegación detallada.
* **RF4 - Creación y Edición:** Persistencia completa en Cloud Firestore con `userId.
* **RF5 - Geolocalización:** Fused Location Provider y geolocalización inversa con **Nominatim (OpenStreetMap)**.
* **RF6 - Cámara / Hardware:** Captura con `FileProvider` y almacenamiento en **Cloudinary**.
* **RF7 - Notificaciones:** Recordatorios diarios configurados mediante **WorkManager**.

## Modo sin conexión (Offline-First)
La aplicación cuenta con doble capa de persistencia: persistencia nativa de Firestore (`setPersistenceEnabled(true)`) y una caché local en JSON (`CacheGastos`). Al iniciar sin internet, los datos locales se muestran de inmediato y los nuevos registros se guardan al instante en la caché sin congelar la interfaz, sincronizándose automáticamente con Firebase al recuperar la red.

## Pruebas Unitarias y Tecnologías
Las pruebas unitarias de la lógica se encuentran en `app/src/test/java/com/example/tickettrack/model/LogicaGastoTest.kt`.
* **Stack:** Kotlin, Jetpack Compose, Material 3, Firebase Auth/Firestore, Cloudinary, osmdroid, WorkManager, Coil y SDK de Gemini.