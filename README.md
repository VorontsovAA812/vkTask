
---

# Tarantool gRPC Key-Value Storage

Микросервис на **Spring Boot**, реализующий высокопроизводительное хранилище данных на базе **Tarantool** с интерфейсом **gRPC**.

## 1. Подготовка Tarantool
Запустите экземпляр Tarantool и выполните следующий скрипт для инициализации спейса и первичного индекса:

```lua
-- Инициализация схемы данных
box.schema.space.create('KV', {if_not_exists = true})

box.space.KV:format({
    {name = 'key', type = 'string'},
    {name = 'value', type = 'string', is_nullable = true}
})

-- Создание индекса TREE для поддержки Range-запросов
box.space.KV:create_index('primary', {
    type = 'tree', 
    parts = {'key'}, 
    if_not_exists = true
})
```

## 2. Сборка и запуск
Для работы проекта требуются **Java 17** и **Maven**.

1. **Сборка проекта** (компиляция `.proto` файлов и создание JAR):
   ```bash
   mvn clean package
   ```

2. **Запуск приложения**:
   ```bash
   # Запуск собранного артефакта
   java -jar target/grpc-tarantool-kv-*.jar
   ```
   *Или используйте команду Maven для быстрой разработки:*
   ```bash
   mvn spring-boot:run
   ```

## 3. Стек технологий
* **Core:** Java 17 / Spring Boot 3.2.4
* **RPC:** gRPC (Netty Shaded) + Protobuf 3
* **Database:** Tarantool 3.2 (Java SDK 1.5.0)
* **Tools:** Lombok, Maven

## 4. API (gRPC методы)
Микросервис предоставляет следующие операции над данными:
* **Put:** Создает или обновляет значение по ключу (идемпотентная операция).
* **Get:** Получает значение по конкретному ключу.
* **Delete:** Удаляет запись (типизация ключа строго соответствует индексу Tarantool).
* **Range:** Стриминговый метод. Возвращает поток данных в заданном алфавитном диапазоне `[key_since, key_to]`.

