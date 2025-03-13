#!/bin/bash

# Сборка проекта
mvn clean install

# Сборка Docker-образа
docker build -t Transport2.

# Запуск контейнера
docker run -d -p 12345:12345 Transport2