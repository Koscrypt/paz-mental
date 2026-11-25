# Paz Mental

**Filtro de llamadas para Android que silencia a desconocidos, sin enviar un solo dato fuera de tu teléfono.**

Un proyecto de [Koscrypt](https://github.com/Koscrypt) · Licencia MIT · Android 11+

## Reglas

| Llamada | Qué pasa |
|---|---|
| De un contacto | Suena siempre. Android ni siquiera se la muestra a la app. |
| Número oculto o privado | Se rechaza. |
| De un país que no permitiste | Se silencia: no timbra y queda como llamada perdida. |
| Desconocido de un país permitido, primera vez | Se silencia. |
| El mismo número vuelve a llamar en menos de 5 minutos | **Suena**: si alguien insiste, quizás es urgente. |
| Desconocido del país del **modo domicilio**, mientras esté abierto | **Suena** a la primera. |

Los países permitidos se eligen en la app. Por defecto solo está Colombia.

### Modo domicilio

¿Pediste un domicilio y te va a llamar un número que no tienes? En **Modo domicilio** eliges un país y
una duración (15 min, 30 min, 1 h o 2 h). Mientras esté abierto, los desconocidos de ese país suenan a la
primera, aunque ese país no esté entre los permitidos. Al vencer se cierra solo; **Terminar ahora** lo
cierra antes. Los números ocultos se siguen rechazando.

## Pantallas

- **Inicio:** estado del filtro, llamadas silenciadas hoy y en la semana, modo domicilio con su cuenta
  regresiva y las últimas llamadas.
- **Estadísticas** de los últimos 7, 30 o 90 días: qué pasó con cada llamada, llamadas por día (o por
  semana), a qué hora llaman (mapa de calor), de qué países, los números que más insisten y el historial.
  **Exportar CSV** guarda el periodo en el archivo que elijas, sin pedir permisos de almacenamiento.
- **Ajustes:** tema **Sistema**, **Blanco** o **Negro** (negro puro para pantallas OLED), países
  permitidos y borrado del historial.

## Privacidad

- **Sin permisos peligrosos:** no lee tus contactos ni tu registro de llamadas.
- **Sin internet:** el manifiesto no declara el permiso `INTERNET`.
- **Sin cuentas, anuncios ni analítica.**
- El historial de llamadas filtradas (últimas 1.000) vive solo en el almacenamiento privado de la app.
  Las estadísticas se calculan en el teléfono a partir de ese historial.
- El CSV exportado sale del teléfono solo si tú lo compartes. Las celdas que una hoja de cálculo tomaría
  por fórmula (`=`, `+`, `-`, `@`) llevan un apóstrofo delante.

Usa la API oficial de Android [`CallScreeningService`](https://developer.android.com/reference/android/telecom/CallScreeningService)
con el rol `ROLE_CALL_SCREENING`.

## Límites conocidos

- No identifica quién llama ni usa listas de spam: aplica tus reglas, nada más.
- Un número sin prefijo internacional se asume colombiano (+57).
- Un celular colombiano que el operador entrega con un +1 delante (`+1 57 3XX…`, 12 dígitos tras el +1)
  se lee como colombiano.
- Si tu app de Teléfono tiene su propio filtro de spam, ambos actúan.

## Instalar

1. Descarga el APK desde **Releases** e instálalo (Android pedirá permitir "instalar apps desconocidas").
2. Abre Paz Mental → **Activar Paz Mental** → acepta usarla como app de identificación de llamadas y spam.
3. Marca los países que quieres permitir.

## Compilar

Requiere Android Studio (JDK 21 incluido) y el SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

La lógica de las reglas está en [`Reglas.java`](app/src/main/java/co/pazmental/Reglas.java), sin dependencias de Android, con pruebas en
[`ReglasTest.java`](app/src/test/java/co/pazmental/ReglasTest.java). Las estadísticas, el historial y el CSV
(`Evento`, `Estadisticas`, `Csv`, `Textos`) también son Java puro, con pruebas en
[`EstadisticasTest.java`](app/src/test/java/co/pazmental/EstadisticasTest.java). Los gráficos se dibujan a mano en
[`Graficos.java`](app/src/main/java/co/pazmental/Graficos.java): la app no tiene dependencias externas.

## Seguridad

Reporta vulnerabilidades en privado desde **Security → Report a vulnerability**. No abras un issue público.
