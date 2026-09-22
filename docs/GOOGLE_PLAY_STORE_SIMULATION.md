# Expediente de Simulación de Publicación en Google Play Store - IDATE

Este documento contiene la ficha técnica, los textos oficiales de marketing/ASO (App Store Optimization), metadatos, clasificación de contenido, especificaciones de capturas y checklist de lanzamiento requeridos para la publicación oficial de **IDATE** en Google Play Console.

---

## 1. Ficha Principal de Play Store (Store Listing)

### Datos Generales
- **Nombre de la Aplicación:** IDATE - Citas y Planes en Pareja
- **Título Corto (Short Description, máx 80 caracteres):**
  > Encuentra los mejores planes para citas y coincide con tu pareja en tiempo real.
- **Categoría:** Citas / Estilo de Vida (Dating / Lifestyle)
- **Etiquetas (Tags):** Citas, Parejas, Amor, Planes de fin de semana, Salidas, Social, Restaurantes.
- **Correo de contacto del desarrollador:** `soporte.idateapp@gmail.com`
- **Sitio web oficial:** `https://idateapp.com`

---

### Descripción Completa (Full Description, máx 4000 caracteres)
```text
¿Cansados de preguntarse siempre "¿Qué hacemos hoy?" o "¿A dónde vamos a cenar?"? 💖

IDATE es la aplicación definitiva diseñada para parejas y amigos que buscan romper la rutina y descubrir experiencias inolvidables juntos. Con una interfaz moderna, ágil y divertida inspirada en el deslizamiento de tarjetas, planear tu próxima cita nunca fue tan emocionante.

✨ CARACTERÍSTICAS PRINCIPALES:

🔥 COINCIDENCIA EN TIEMPO REAL (LIVE MATCH PARA 2 DISPOSITIVOS)
• Conéctate con tu pareja mediante un código PIN único de 6 dígitos.
• Deslicen planes simultáneamente desde sus propios teléfonos.
• ¡IT'S A MATCH! Cuando ambos dan Like al mismo plan, la app los alerta al instante con una celebración en pantalla y notificación en vivo.

🎨 CATÁLOGO DE PLANES VARIADOS Y PERSONALIZABLES
• Explora categorías: Cenas Románticas, Cine y Películas, Noches de Fiesta y Drinks, Escape Rooms, Picnics al Aire Libre, Talleres de Arte y más.
• Filtra por presupuesto, duración y tipo de experiencia.
• ¿Tienes una idea original? Crea tus propios planes personalizados con ubicación, costos y etiquetas.

💾 PERSISTENCIA TOTAL Y MODO OFFLINE
• Tus planes guardados y favoritos se almacenan de manera local y en la nube.
• Accede a tus citas guardadas incluso sin conexión a internet gracias a su arquitectura offline-first con base de datos SQLite/Room.

♿ ACCESIBILIDAD Y DISEÑO INCLUSIVO
• Totalmente optimizada para lectores de pantalla TalkBack.
• Botones de acción táctiles de gran tamaño, contraste visual optimizado y tipografía dinámica adaptable para todo tipo de usuarios.

📱 ADAPTABLE A TODOS LOS DISPOSITIVOS
• Disfruta de una experiencia fluida tanto en teléfonos inteligentes compactos como en tablets y dispositivos plegables con vista dividida en dos columnas.

🔔 NOTIFICACIONES Y RECORDATORIOS INTELIGENTES
• Recibe avisos cuando tu pareja se une a la sala o cuando ocurre una coincidencia para que nunca olviden agendar su salida.

¡Descarga IDATE hoy mismo y convierte cada cita en una aventura memorable!
```

---

## 2. Clasificación de Contenido y Audiencia (Content Rating)
- **Clasificación IARC / PEGI:** PEGI 12 / Teens (Adolescentes).
- **Acceso a la Aplicación:** Todas las funciones están disponibles sin restricciones especiales de pago.
- **Anuncios:** La aplicación NO contiene anuncios intrusivos.
- **Público Objetivo:** Jóvenes y adultos (16+ años).
- **Recolección de Datos de Menores:** No recopila intencionalmente datos de menores de 13 años.

---

## 3. Seguridad de los Datos (Data Safety Section)
- **Datos recopilados:**
  - **Identificadores:** Nombre/Apodo de usuario (para la sala en vivo).
  - **Actividad de la App:** Interacciones con planes y votos (para generar coincidencias).
- **Prácticas de seguridad:**
  - Los datos se transmiten mediante conexión cifrada HTTPS/WSS.
  - El usuario puede borrar sus planes guardados y desvincularse de las salas en cualquier momento.

---

## 4. Guía Técnica de Generación de APK y Android App Bundle (AAB)

### Compilación de APK de Prueba (Debug)
Para generar el archivo APK de depuración listo para instalar en cualquier dispositivo físico:
```bash
./gradlew assembleDebug
```
Ubicación de salida: `app/build/outputs/apk/debug/app-debug.apk`

### Compilación de APK de Producción (Release)
Para generar el APK de lanzamiento:
```bash
./gradlew assembleRelease
```
Ubicación de salida: `app/build/outputs/apk/release/app-release.apk`

### Generación de Android App Bundle (AAB) para Google Play Console
Google Play requiere el formato `.aab` para nuevas aplicaciones:
```bash
./gradlew bundleRelease
```
Ubicación de salida: `app/build/outputs/bundle/release/app-release.aab`

---

## 5. Checklist de Lanzamiento en Google Play Console

1. [x] **Identificador único de aplicación (ApplicationId):** `com.example.idate`
2. [x] **Target SDK:** 34 (Android 14) conforme a los requisitos más recientes de Google Play.
3. [x] **Permisos declarados:** Manejo en tiempo de ejecución de `POST_NOTIFICATIONS` y `INTERNET`.
4. [x] **Iconos y Recursos Gráficos:**
   - Icono de alta resolución: 512 x 512 px (PNG 32-bit con canal alfa).
   - Gráfico de funciones (Feature Graphic): 1024 x 500 px.
   - Capturas de pantalla: Mínimo 4 capturas en formato vertical (1080 x 1920 px) y 2 para tablets (7" y 10").
5. [x] **Política de Privacidad:** Alojada y vinculada (ver `docs/PRIVACY_POLICY.md`).
6. [x] **Firma de la Aplicación (Keystore):** Habilitar Google Play App Signing en la consola.
7. [x] **Pistas de Prueba:** Despliegue inicial en *Pruebas Internas (Internal Testing)* para validar sincronización multi-dispositivo antes del lanzamiento público en Producción.
