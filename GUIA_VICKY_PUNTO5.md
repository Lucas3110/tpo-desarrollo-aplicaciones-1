# Guía de entrega - Punto 5

## Qué incluye esta versión

- Botón **Publicar** en Home y modal tutorial.
- Carga guiada en tres pasos.
- Selección y vista previa de hasta 10 fotos de la galería.
- Guardado, recuperación y descarte del borrador mediante la API.
- Creación de la publicación mediante la API.
- Pantalla **Mis publicaciones**, resumen y filtro por estado.
- Acciones para pausar y reactivar.
- Manejo de errores de API, conexión y sesión vencida.

## Limitación conocida del backend

El backend entregado no tiene endpoint para subir archivos: el body de publicación
solamente acepta URLs públicas. Android selecciona fotos como URI locales
(`content://...`), que el servidor no puede abrir. Por eso la app permite elegir y
previsualizar fotos, pero crea la publicación sin enviarlas. Para completar esa parte,
backend debe incorporar un endpoint de upload y devolver una URL por cada archivo.

## Cómo probar

1. Abrir XAMPP y tocar **Start** en MySQL.
2. En el repositorio del backend ejecutar `npm run dev`.
3. Abrir este proyecto con Android Studio.
4. Esperar a que termine **Gradle Sync**.
5. Ejecutar la app en el emulador.
6. Iniciar sesión con `sofia.demo@ronda.app` / `demo1234`.
7. En Home tocar **Publicar**.
8. Avanzar uno o dos pasos, cerrar la pantalla y volver: debe recuperar el borrador.
9. Completar todos los datos y publicar.
10. Entrar a **Mis avisos**, pausar la publicación y volver a reactivarla.

## Git: comandos que debe ejecutar Vicky

Estos comandos van en la terminal integrada de Android Studio, abierta desde
**View > Tool Windows > Terminal**. Deben ejecutarse dentro del repositorio real,
no dentro del ZIP.

```bash
git checkout main
git pull
git checkout -b feature/app-publicar-articulo
```

Luego se copian los archivos de este proyecto sobre el repositorio real y se ejecuta:

```bash
git status
git add app/src/main GUIA_VICKY_PUNTO5.md
git commit -m "feat(publicaciones): implementar carga guiada y mis publicaciones"
git push -u origin feature/app-publicar-articulo
```

En GitHub aparecerá **Compare & pull request**. Entrar, poner como título
`feat(publicaciones): implementar punto 5`, comprobar que la base sea `main`, crear
el Pull Request y enviar el enlace al grupo para revisión. No tocar **Merge** hasta
que un compañero lo revise y confirme que no hay conflictos con sus cambios.
