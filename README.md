# Transport
Клиент-серверное приложение для маркетплейса пассажирских перевозок. 
1. Клонирование репозитория
Откройте терминал. Выполните команду для клонирования репозитория:
git clone https://github.com/Transport2.git
2. Перейдите в директорию проекта
3. Сборка проекта с помощью Maven
Убедитесь, что Maven установлен. Проверьте версию:
mvn -v
Выполните команду для сборки проекта:
mvn clean install
После успешной сборки в папке target появится файл Transport2.jar.
4. Запуск приложения
Запуск сервера
java -cp target/Transport2.jar server2.ServerMain
Сервер будет доступен по адресу http://localhost:12345.
Запуск клиента
java -cp target/Transport2.jar client2.MainApp
