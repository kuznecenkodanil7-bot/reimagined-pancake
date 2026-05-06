# Moderation Helper GUI

Клиентский Minecraft-мод для **Java Edition / Fabric / 1.21.11**. Мод открывает GUI наказаний по СКМ в чате, делает скриншот до открытия меню, сортирует скриншоты, хранит недавних игроков, показывает статистику сессии и умеет запускать/останавливать запись OBS через obs-websocket.

## Что уже заложено

- Fabric client entrypoint.
- Настраиваемые бинды в Minecraft Controls:
  - `H` — открыть статистику и недавних игроков.
  - `G` — остановить запись OBS.
- `G` не останавливает запись, если открыт чат Minecraft.
- Mixin на `ChatScreen#mouseClicked` для СКМ.
- Парсер ника с игнором рангов:
  - `HT5`, `LT5`, `HT4`, `LT4`, `HT3`, `LT3`, `HT2`, `LT2`, `HT1`, `LT1`
  - `RHT3`, `RLT3`, `RHT2`, `RLT2`, `RHT1`, `RLT1`
  - `XHT5`, `XLT5`, `XHT4`, `XLT4`, `XHT3`, `XLT3`, `XHT2`, `XLT2`, `XHT1`, `XLT1`
  - `I`, `II`, `III`, `IV`, `V`, `VI`, `VII`, `VIII`, `IX`, `X`
- Игнор серверных маркеров перед ником:
  - `anarchy-alpha`, `anarchy-beta`, `anarchy-gamma`, `anarchy-new`, `duels`
- Фильтр ника Minecraft: латиница, цифры, `_`, длина 3–16.
- Исключения для скриншотов:
  - `Tick Speed`
  - `Reach`
  - `Fighting suspiciously`
  - `Block Interaction`
- GUI:
  - главное меню наказаний;
  - выбор времени;
  - выбор причины;
  - статистика;
  - недавние игроки без нумерации.
- Скриншоты:
  - временно: `moderation_screenshots/temp/`
  - после наказания: `warn/`, `mute/`, `ban/`, `ipban/`
  - архив: `archive/`
- Автоочистка старых скриншотов при старте клиента.
- OBS websocket v5:
  - `StartRecord`
  - `StopRecord`
  - авторизация через пароль OBS.
- Кнопка проверки:
  - отправляет `/check {nick}`;
  - отправляет `/tell {nick} ...` с текстом проверки;
  - запускает OBS-запись;
  - включает таймер над хотбаром `Идёт запись: 00:00`.
- При `ipban` запись OBS останавливается автоматически, кроме причины `3.8`.

> Важно: точное определение строки под курсором в чате зависит от Yarn/Fabric internals. В проекте сделан безопасный best-effort: сначала попытка извлечь строку через reflection, затем fallback на последние полученные сообщения чата. Это не крашит игру, но после первого запуска лучше проверить на твоём сервере и при необходимости подогнать `ChatMessageExtractor`.

## Сборка

Требования:

- Java 21+
- Gradle 8.14+ или IDE с Gradle
- Minecraft Java Edition 1.21.11
- Fabric Loader 0.18.1+
- Fabric API для 1.21.11

Команды:

```bash
gradle build
```

Готовый jar будет здесь:

```text
build/libs/moderation-helper-gui-1.0.0.jar
```

Положи jar в:

```text
.minecraft/mods/
```

Также положи Fabric API для 1.21.11 в папку `mods`.

## Настройка OBS

В OBS:

1. Открой `Tools / Инструменты`.
2. Открой `WebSocket Server Settings`.
3. Включи websocket-сервер.
4. Укажи порт `4455`.
5. Включи/задай пароль, если он нужен.
6. Перезапусти OBS при необходимости.

После первого запуска Minecraft появится конфиг:

```text
.minecraft/config/moderation-helper-gui.json
```

Пример важных параметров:

```json
{
  "obsEnabled": true,
  "obsHost": "localhost",
  "obsPort": 4455,
  "obsPassword": "твой_пароль",
  "recentPlayersLimit": 15,
  "screenshotCleanupMode": "ARCHIVE",
  "screenshotRetentionDays": 30,
  "screenshotDirectory": "moderation_screenshots",
  "checkCommandTemplate": "/check {nick}",
  "checkTellTemplate": "/tell {nick} Здравствуйте, проверка на читы. В течении 5 минут жду ваш Anydesk(наилучший вариант, скачать можно в любом браузере)/Discord. Также сообщаю, что в случае признания на наличие чит-клиентов срок бана составит 20 дней, вместо 30."
}
```

## Использование

### СКМ по нику в чате

1. Открой чат.
2. Наведи на сообщение с ником.
3. Нажми колёсиком мыши.
4. Мод попытается определить ник.
5. Если сообщение не содержит `Tick Speed`, `Reach`, `Fighting suspiciously`, `Block Interaction`, то будет сделан скриншот до открытия GUI.
6. Откроется меню наказаний.

### Наказания

Команды отправляются от имени игрока:

```text
/warn {nick} {reason}
/mute {nick} {duration} {reason}
/ban {nick} {duration} {reason}
/ipban {nick} {duration} {reason}
```

По умолчанию быстрые причины отправляют в команду код правила, например `2.2`, `3.7`, `3.8`. Это можно изменить в конфиге через поле `commandReason`.

### Вызов на проверку

Кнопка `Вызвать на проверку` делает:

```text
/check {nick}
/tell {nick} Здравствуйте, проверка на читы. В течении 5 минут жду ваш Anydesk(наилучший вариант, скачать можно в любом браузере)/Discord. Также сообщаю, что в случае признания на наличие чит-клиентов срок бана составит 20 дней, вместо 30.
```

Потом запускает OBS-запись и таймер над хотбаром.

### Клавиша H

Открывает только окно статистики и недавних игроков. Не ищет ник в последнем сообщении и не делает скриншот.

### Клавиша G

Останавливает запись OBS и убирает таймер. Если открыт чат Minecraft, `G` не должен остановить запись.

## Где лежат скриншоты

По умолчанию:

```text
.minecraft/moderation_screenshots/
├─ temp/
├─ warn/
├─ mute/
├─ ban/
├─ ipban/
└─ archive/
```

Итоговый файл после наказания:

```text
{nick}_{punishment}_{duration}_{reason}_{datetime}.png
```

Запрещённые символы в названии заменяются на `_`.

## Настройка быстрых причин

Причины лежат в `quickReasons` в конфиге. Категории:

```text
WARN
MUTE
BAN
IPBAN
```

Пример:

```json
{
  "type": "IPBAN",
  "code": "3.7",
  "title": "Стороннее ПО",
  "description": "Использование/хранение стороннего ПО.",
  "defaultDuration": "30d",
  "commandReason": "3.7"
}
```

## GitHub

Чтобы залить проект на GitHub:

```bash
git init
git add .
git commit -m "Initial Moderation Helper GUI mod"
git branch -M main
git remote add origin https://github.com/USERNAME/ModerationHelperGUI.git
git push -u origin main
```

## Что проверить первым делом

1. Запускается ли клиент через `gradle runClient`.
2. Появились ли бинды в Controls.
3. Открывается ли статистика по `H`.
4. Работает ли СКМ в чате.
5. Делается ли скрин до открытия GUI.
6. Отправляются ли команды наказаний.
7. Переносятся ли скрины из `temp` в нужную папку.
8. Работает ли OBS при `obsEnabled = true`.

## Возможные доработки

- Точное сопоставление координаты клика с конкретной строкой чата можно усилить в `ChatMessageExtractor` под конкретные Yarn mappings сервера/клиента.
- Можно добавить полноценные hover-подсказки к причинам.
- Можно сделать отдельный экран категорий правил с прокруткой, если причин станет больше.
