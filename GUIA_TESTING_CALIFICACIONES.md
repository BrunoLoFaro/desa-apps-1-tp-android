# Guía de Testing - Cambios en "Mis Calificaciones"

## Cambios Implementados

Se han realizado ajustes visuales y funcionales en la sección "Mis Calificaciones" de "Mis Actividades":

---

## 1. Testing del Fondo de la Card

**Lo que se debe verificar:**
- [ ] La card de calificación tiene fondo blanco (#FFFFFF)
- [ ] El fondo es consistente en todos los elementos de la lista
- [ ] No hay conflictos visuales con el tema oscuro/claro

**Pasos:**
1. Abrir la app
2. Navegar a "Mis Actividades" → "Mis Calificaciones"
3. Verificar que el fondo de cada card sea blanco puro

**Archivos relacionados:**
- `app/src/main/res/layout/item_review.xml` (líneas 11-12)

---

## 2. Testing de Estrellas de Calificación - Color Amarillo

**Lo que se debe verificar:**
- [ ] Las estrellas de "Actividad" son amarillas (#FFC107)
- [ ] Las estrellas de "Guía" son amarillas (#FFC107)
- [ ] El color es visible en ambos temas (claro y oscuro)
- [ ] Las estrellas llenas y vacías se distinguen bien

**Pasos:**
1. Abrir "Mis Calificaciones"
2. Verificar que todas las estrellas (tanto llenas como vacías) se muestren en amarillo
3. Validar en dispositivos con tema claro y oscuro

**Archivos relacionados:**
- `app/src/main/res/layout/item_review.xml` (líneas 56, 83)

---

## 3. Testing de Redondeo de Calificaciones

**Lo que se debe verificar:**
- [ ] Calificación 4.26 se muestra como 4.3
- [ ] Calificación 4.24 se muestra como 4.2
- [ ] Calificación 4.25 se muestra como 4.3 (redondeo correcto)
- [ ] El redondeo afecta SOLO la visualización, no los datos internos
- [ ] Otras vistas de calificaciones no se ven afectadas

**Pasos:**
1. Buscar calificaciones con decimales en la BD
2. Verificar que se muestren redondeadas a un decimal en la UI
3. Consultar la API/DB directamente para confirmar que los datos originales no cambien
4. Verificar otras vistas (detalle de actividad, etc.) no muestren el redondeo

**Ejemplos de testing:**
```
Original: 4.26 → Mostrado: 4.3 ✓
Original: 3.12 → Mostrado: 3.1 ✓
Original: 5.00 → Mostrado: 5.0 ✓
Original: 2.75 → Mostrado: 2.8 ✓
```

**Archivos relacionados:**
- `app/src/main/java/com/example/myapplication/ui/bookings/ReviewAdapter.java` (líneas 40-47)

---

## 4. Testing de Zona Horaria - UTC+3

**Lo que se debe verificar:**
- [ ] Las fechas se muestran en formato "dd/MM/yyyy HH:mm"
- [ ] Las horas son 3 horas más adelante que UTC
- [ ] Ejemplo: Si UTC es 14:30, debe mostrarse 17:30
- [ ] La fecha es consistente en diferentes dispositivos
- [ ] La fecha es consistente después de cerrar y rearir la app

**Pasos de Testing:**

1. **Testing local (en el dispositivo):**
   - Abrir "Mis Calificaciones"
   - Anotar una fecha/hora mostrada (ej: "01/05/2026 15:30")
   - Verificar manualmente en la API que el timestamp UTC sea 3 horas menos
   
2. **Testing en múltiples dispositivos:**
   - Instalar la app en 2+ dispositivos
   - Verificar que muestren la misma fecha/hora
   - Cambiar la zona horaria del dispositivo y reabrir (debe mantener UTC+3, no convertir al timezone del dispositivo)

3. **Testing de timestamps especiales:**
   ```
   UTC: 2026-04-30T21:30:00Z → Mostrado: 01/05/2026 00:30 (cambio de día)
   UTC: 2026-05-01T00:00:00Z → Mostrado: 01/05/2026 03:00
   UTC: 2026-05-01T21:00:00Z → Mostrado: 02/05/2026 00:00 (cambio de día)
   ```

**Archivos relacionados:**
- `app/src/main/java/com/example/myapplication/util/FormatUtils.java` (líneas 62-82)
- `app/src/main/java/com/example/myapplication/ui/bookings/ReviewAdapter.java` (línea 60)

---

## Testing Completo de la Card

**Caso de uso integral:**

1. Usuario con múltiples calificaciones
2. Algunas con comentarios, otras sin
3. Con calificación de actividad y guía
4. Verificar que:
   - [ ] Fondo blanco está presente
   - [ ] Estrellas amarillas se muestren correctamente
   - [ ] Valores redondeados a un decimal
   - [ ] Fecha en UTC+3 con formato correcto
   - [ ] Sin cambios en otras secciones de la app

---

## Validación en Diferentes Escenarios

### Escenario 1: Usuario con pocos datos
- [ ] Card se muestra correctamente incluso con una sola calificación
- [ ] Layout no se quiebra

### Escenario 2: Scroll de lista larga
- [ ] Las cards mantienen el estilo visual al hacer scroll
- [ ] No hay problemas de rendimiento
- [ ] Los datos se cargan correctamente

### Escenario 3: Offline/Online
- [ ] En modo offline, las calificaciones guardadas mantienen el formato
- [ ] Al sincronizar, la hora UTC+3 se actualiza correctamente
- [ ] No hay inconsistencias entre offline y online

### Escenario 4: Tema claro/oscuro
- [ ] Fondo blanco se ve en ambos temas
- [ ] Estrellas amarillas tienen suficiente contraste en ambos temas
- [ ] Texto es legible

---

## Checklist Final

- [ ] Compilación sin errores
- [ ] Fondo blanco visible
- [ ] Estrellas amarillas visibles
- [ ] Calificaciones redondeadas correctamente
- [ ] Fechas en UTC+3
- [ ] Sin regresiones en otras vistas
- [ ] Funciona en múltiples dispositivos
- [ ] Funciona en tema claro y oscuro
- [ ] Performance no se ve afectado

---

## Nota Importante

Si los cambios no se ven reflejados después de instalar:
1. Limpiar caché de la app: Ajustes → Aplicaciones → [App] → Almacenamiento → Borrar caché
2. Desinstalar y reinstalar la app
3. Asegurarse de que la compilación include los cambios (rebuild)


