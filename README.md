# SkinMod — структура проекта

```
skinmod/
├── src/main/java/com/example/skinmod/
│   ├── SkinMod.java
│   ├── client/
│   │   ├── CustomTextureManager.java
│   │   └── mixin/
│   │       └── AbstractClientPlayerMixin.java
│   ├── command/
│   │   └── SetTextureCommand.java
│   └── network/
│       ├── ModNetwork.java
│       └── SetTexturePacket.java
└── src/main/resources/
    ├── skinmod.mixins.json
    └── META-INF/
        └── mods.toml
```

## Что нужно сделать после распаковки

1. Скопируйте содержимое папки `src` в корень вашего Forge MDK проекта (1.19.2),
   заменив/дополнив существующие файлы.

2. В `build.gradle` подключите mixin-манифест (если ещё не подключено):

```groovy
jar {
    manifest {
        attributes([
            "MixinConfigs": "skinmod.mixins.json"
        ])
    }
}
```

   Если используется отдельный Mixin Gradle плагин — добавьте:

```groovy
mixin {
    config "skinmod.mixins.json"
}
```

3. Убедитесь, что в `build.gradle` подключена зависимость Mixin (обычно уже
   идёт в составе ForgeGradle, отдельно подключать не нужно для 1.19.2).

4. Файлы скинов (`.png`, 64x64 или 64x32) кладите на **сервер** в папку

    ```
    <папка_сервера>/versions/<версия>/skins/
    ```

    Сервер сам раздаёт содержимое скина всем игрокам, поэтому у клиентов
    ничего класть не нужно — мод полностью работает в мультиплеере. Папка
    `skins` создаётся автоматически при первом применении команды.
    Файл должен весить не более 1 МБ.

5. Команды в игре (требуют уровень прав 2 — оператор):

```
/skin set <ник_или_UUID> <имя_файла.png> [model]
/skin reset <ник_или_UUID>
/skin list [ник_или_UUID]
```

   - `model` — необязательный аргумент, `default` или `slim` (доступны по Tab).
   - Имя файла скина можно дополнять по Tab (в одиночной игре/LAN — из папки
     `versions/<версия>/skins`).
   - `/skin list` показывает назначенные скины; с указанием игрока — только его.

6. Соберите мод командой:

```
./gradlew build
```

   Готовый jar будет в `build/libs/`.
